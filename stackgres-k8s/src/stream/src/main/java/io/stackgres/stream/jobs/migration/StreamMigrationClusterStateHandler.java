/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.migration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.debezium.connector.jdbc.JdbcChangeEventSink;
import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.QueryBinderResolver;
import io.debezium.connector.jdbc.RecordWriter;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.dialect.DatabaseDialectResolver;
import io.debezium.embedded.Connect;
import io.debezium.engine.ChangeEvent;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetJdbcSinkDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetSgCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.stream.jobs.DebeziumEngineHandler;
import io.stackgres.stream.jobs.DebeziumUtil;
import io.stackgres.stream.jobs.Metrics;
import io.stackgres.stream.jobs.StateHandler;
import io.stackgres.stream.jobs.StreamEventStateHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StateHandler("SGCluster")
public class StreamMigrationClusterStateHandler implements StreamEventStateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamMigrationClusterStateHandler.class);

  public static String name(StackGresStream stream) {
    return stream.getMetadata().getNamespace() + "." + stream.getMetadata().getName();
  }

  public static String topic(StackGresStream stream) {
    return name(stream);
  }

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Inject
  DebeziumEngineHandler debeziumEngineHandler;

  @Inject
  Metrics metrics;

  @Override
  public CompletableFuture<Void> sendEvents(StackGresStream stream) {
    final JdbcHandler handler = new JdbcHandler(stream);
    handler.start();
    return debeziumEngineHandler.streamChangeEvents(stream, Connect.class, handler, handler::stop);
  }

  private String getSecretKeyValue(String namespace, String secretName, String secretKey) {
    return Optional.of(secretFinder.findByNameAndNamespace(secretName, namespace)
        .orElseThrow(() -> new IllegalArgumentException("Secret " + secretName + " not found")))
        .map(Secret::getData)
        .map(data -> data.get(secretKey))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalArgumentException("key " + secretKey + " not found in Secret " + secretName));
  }

  class JdbcHandler implements Consumer<ChangeEvent<SourceRecord, SourceRecord>> {

    final StackGresStream stream;

    boolean started = false;
    JdbcChangeEventSink changeEventSink;
    SessionFactory sessionFactory;
    long counter = 0L;
    long lastLsn = 0L;

    JdbcHandler(StackGresStream stream) {
      this.stream = stream;
    }

    public void start() {
      if (started) {
        throw new IllegalStateException("Already started");
      }
      started = true;
      final Properties props = new Properties();
      var sgCluster = Optional.of(stream.getSpec().getTarget().getSgCluster());
      sgCluster
          .map(StackGresStreamTargetSgCluster::getDebeziumProperties)
          .map(StackGresStreamTargetJdbcSinkDebeziumProperties::getPrimaryKeyMode)
          .filter("kafka"::equalsIgnoreCase)
          .ifPresent(mode -> {
            throw new IllegalArgumentException("primaryKeyMode kafka is not supported");
          });
      String namespace = stream.getMetadata().getNamespace();
      props.setProperty("name", name(stream));
      props.setProperty("topic", topic(stream));
      DebeziumUtil.configureDebeziumSectionProperties(
          props,
          sgCluster
          .map(StackGresStreamTargetSgCluster::getDebeziumProperties)
          .orElse(null),
          StackGresStreamTargetJdbcSinkDebeziumProperties.class);
      String clusterName = sgCluster.map(StackGresStreamTargetSgCluster::getName)
          .orElseThrow(() -> new IllegalArgumentException("The name of SGCluster is not specified"));
      String usernameSecretName = sgCluster
          .map(StackGresStreamTargetSgCluster::getUsername)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(clusterName));
      String usernameSecretKey = sgCluster
          .map(StackGresStreamTargetSgCluster::getUsername)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_USERNAME_KEY);
      var username = getSecretKeyValue(namespace, usernameSecretName, usernameSecretKey);
      String passwordSecretName = sgCluster
          .map(StackGresStreamTargetSgCluster::getPassword)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(clusterName));
      String passwordSecretKey = sgCluster
          .map(StackGresStreamTargetSgCluster::getPassword)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY);
      var password = getSecretKeyValue(namespace, passwordSecretName, passwordSecretKey);

      props.setProperty("connection.url", "jdbc:postgresql://%s:%s/%s"
          .formatted(
              clusterName,
              String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT),
              Optional.ofNullable(stream.getSpec().getSource().getSgCluster())
              .map(StackGresStreamSourceSgCluster::getDatabase)
              .orElse("postgres")));
      props.setProperty("connection.username", username);
      props.setProperty("connection.password", password);
      final JdbcSinkConnectorConfig config = new JdbcSinkConnectorConfig(props
          .entrySet()
          .stream()
          .map(e -> Map.entry(e.getKey().toString(), e.getValue().toString()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
      config.validate();

      // Sync the code below with the code of the method
      // io.debezium.connector.jdbc.JdbcSinkConnectorTask.start(java.util.Map<String, String>)
      sessionFactory = config.getHibernateConfiguration().buildSessionFactory();
      StatelessSession session = sessionFactory.openStatelessSession();
      DatabaseDialect databaseDialect = DatabaseDialectResolver.resolve(config, sessionFactory);
      QueryBinderResolver queryBinderResolver = new QueryBinderResolver();
      RecordWriter recordWriter = new RecordWriter(session, queryBinderResolver, config, databaseDialect);

      changeEventSink = new JdbcChangeEventSink(config, session, databaseDialect, recordWriter);
    }

    @Override
    public void accept(ChangeEvent<SourceRecord, SourceRecord> changeEvent) {
      if (!started) {
        throw new IllegalStateException("Not started");
      }
      try {
        final SourceRecord sourceRecord = changeEvent.value();
        String sourceOffset = sourceRecord.sourceOffset()
            .entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue().toString())
            .collect(Collectors.joining(" "));
        LOGGER.trace("SourceRecord: {}", sourceOffset);
        long lsn = Long.parseLong(sourceRecord.sourceOffset().get("lsn").toString());
        if (lastLsn != lsn) {
          lastLsn = lsn;
          counter = 0L;
        }
        long kafkaPartition = (lastLsn << 32) & (counter & ((1 << 32) - 1));
        counter++;
        SinkRecord sinkRecord = new SinkRecord(
            sourceRecord.topic(),
            Optional.ofNullable(changeEvent.partition()).orElse(0).intValue(),
            sourceRecord.keySchema(),
            sourceRecord.key(),
            sourceRecord.valueSchema(),
            sourceRecord.value(),
            kafkaPartition,
            sourceRecord.timestamp(),
            TimestampType.CREATE_TIME,
            sourceRecord.headers());
        changeEventSink.execute(List.of(sinkRecord));
        metrics.incrementTotalNumberOfEventsSent();
        metrics.setLastEventSent(sourceOffset);
        metrics.setLastEventWasSent(true);
      } catch (RuntimeException ex) {
        metrics.incrementTotalNumberOfErrorsSeen();
        metrics.setLastEventWasSent(false);
        throw ex;
      } catch (Exception ex) {
        metrics.incrementTotalNumberOfErrorsSeen();
        metrics.setLastEventWasSent(false);
        throw new RuntimeException(ex);
      }
    }

    public void stop() {
      if (changeEventSink != null) {
        changeEventSink.close();
      }
      if (sessionFactory != null && sessionFactory.isOpen()) {
        sessionFactory.close();
      }
    }
  }

}

