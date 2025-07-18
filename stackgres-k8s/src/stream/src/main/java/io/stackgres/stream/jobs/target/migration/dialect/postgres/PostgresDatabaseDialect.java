/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.stackgres.stream.jobs.target.migration.dialect.postgres;

import static io.debezium.connector.jdbc.type.debezium.DebeziumZonedTimestampType.NEGATIVE_INFINITY;
import static io.debezium.connector.jdbc.type.debezium.DebeziumZonedTimestampType.POSITIVE_INFINITY;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Optional;

import org.apache.kafka.connect.data.Schema;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.JdbcSinkRecord;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.dialect.DatabaseDialectProvider;
import io.debezium.connector.jdbc.dialect.GeneralDatabaseDialect;
import io.debezium.connector.jdbc.dialect.SqlStatementBuilder;
import io.debezium.connector.jdbc.relational.ColumnDescriptor;
import io.debezium.connector.jdbc.relational.TableDescriptor;
import io.debezium.connector.jdbc.type.Type;
import io.debezium.metadata.CollectionId;

/**
 * A {@link DatabaseDialect} implementation for PostgreSQL.
 *
 * @author Chris Cranford
 */
public class PostgresDatabaseDialect extends GeneralDatabaseDialect {

  public static class PostgresDatabaseDialectProvider implements DatabaseDialectProvider {
    @Override
    public boolean supports(Dialect dialect) {
      return dialect instanceof PostgreSQLDialect;
    }

    @Override
    public Class<?> name() {
      return PostgresDatabaseDialect.class;
    }

    @Override
    public DatabaseDialect instantiate(JdbcSinkConnectorConfig config,
        SessionFactory sessionFactory) {
      return new PostgresDatabaseDialect(config, sessionFactory);
    }
  }

  protected PostgresDatabaseDialect(JdbcSinkConnectorConfig config, SessionFactory sessionFactory) {
    super(config, sessionFactory);
  }

  @Override
  public int getMaxTimestampPrecision() {
    return 6;
  }

  @Override
  public boolean tableExists(Connection connection, CollectionId collectionId) throws SQLException {
    if (!getConfig().isQuoteIdentifiers()) {
      // This means that the table will be stored as lower-case
      collectionId = collectionId.toLowerCase();
    }
    return super.tableExists(connection, collectionId);
  }

  @Override
  public TableDescriptor readTable(Connection connection, CollectionId collectionId)
      throws SQLException {
    if (!getConfig().isQuoteIdentifiers()) {
      // This means that the table will be stored as lower-case
      collectionId = collectionId.toLowerCase();
    }
    return super.readTable(connection, collectionId);
  }

  @Override
  public String getAlterTablePrefix() {
    return "";
  }

  @Override
  public String getAlterTableSuffix() {
    return "";
  }

  @Override
  public String getAlterTableColumnPrefix() {
    return "ADD COLUMN ";
  }

  @Override
  public String getUpsertStatement(TableDescriptor table, JdbcSinkRecord record) {
    final SqlStatementBuilder builder = new SqlStatementBuilder();
    builder.append("INSERT INTO ");
    builder.append(getQualifiedTableName(table.getId()));
    builder.append(" (");
    builder.appendLists(",", record.keyFieldNames(), record.getNonKeyFieldNames(),
        (name) -> columnNameFromField(name, record));
    builder.append(") VALUES (");
    builder.appendLists(",", record.keyFieldNames(), record.getNonKeyFieldNames(),
        (name) -> columnQueryBindingFromField(name, table, record));
    builder.append(") ON CONFLICT (");
    builder.appendList(",", record.keyFieldNames(), (name) -> columnNameFromField(name, record));
    if (record.getNonKeyFieldNames().isEmpty()) {
      builder.append(") DO NOTHING");
    } else {
      builder.append(") DO UPDATE SET ");
      builder.appendList(",", record.getNonKeyFieldNames(), (name) -> {
        final String columnNme = columnNameFromField(name, record);
        return columnNme + "=EXCLUDED." + columnNme;
      });
    }
    return builder.build();
  }

  @Override
  public String getQueryBindingWithValueCast(ColumnDescriptor column, Schema schema, Type type) {
    if (schema.type() == Schema.Type.STRING) {
      final String typeName = column.getTypeName().toLowerCase(Locale.US);
      if ("uuid".equals(typeName)) {
        return "cast(? as uuid)";
      } else if ("json".equals(typeName)) {
        return "cast(? as json)";
      } else if ("jsonb".equals(typeName)) {
        return "cast(? as jsonb)";
      }
    }
    return super.getQueryBindingWithValueCast(column, schema, type);
  }

  @Override
  public String getByteArrayFormat() {
    return "'\\x%s'";
  }

  @Override
  public String getFormattedBoolean(boolean value) {
    // PostgreSQL maps logical TRUE/FALSE for boolean data types
    return value ? "TRUE" : "FALSE";
  }

  @Override
  public String getFormattedDateTimeWithNanos(TemporalAccessor value) {
    return String.format("'%s'", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value));
  }

  @Override
  public String getFormattedTime(TemporalAccessor value) {
    return String.format("'%s'", DateTimeFormatter.ISO_LOCAL_TIME.format(value));
  }

  @Override
  protected Optional<String> getDatabaseTimeZoneQuery() {
    return Optional.of("SELECT CURRENT_SETTING('TIMEZONE')");
  }

  @Override
  protected void registerTypes() {
    super.registerTypes();

    registerType(ConnectStringType.INSTANCE);
    registerType(TimeWithTimezoneType.INSTANCE);
    registerType(ZonedTimestampType.INSTANCE);
    registerType(IntervalType.INSTANCE);
    registerType(SerialType.INSTANCE);
    registerType(BitType.INSTANCE);
    registerType(BytesType.INSTANCE);
    registerType(JsonType.INSTANCE);
    registerType(UuidType.INSTANCE);
    registerType(EnumType.INSTANCE);
    registerType(PointType.INSTANCE);
    registerType(GeometryType.INSTANCE);
    registerType(GeographyType.INSTANCE);
    registerType(MoneyType.INSTANCE);
    registerType(XmlType.INSTANCE);
    registerType(LtreeType.INSTANCE);
    registerType(MapToHstoreType.INSTANCE);
    registerType(ArrayType.INSTANCE);

    // Allows binding string-based types if column type propagation is enabled
    registerType(RangeType.INSTANCE);
    registerType(CidrType.INSTANCE);
    registerType(MacAddressType.INSTANCE);
    registerType(InetType.INSTANCE);
    registerType(CaseInsensitiveTextType.INSTANCE);
    registerType(OidType.INSTANCE);

    registerType(SparseDoubleVectorType.INSTANCE);
    registerType(FloatVectorType.INSTANCE);
    registerType(DoubleVectorType.INSTANCE);
  }

  @Override
  public int getMaxVarcharLengthInKey() {
    // Setting to Integer.MAX_VALUE forces PostgreSQL to use TEXT data types in
    // primary keys
    // when no explicit size on the column is specified.
    return Integer.MAX_VALUE;
  }

  @Override
  protected String resolveColumnNameFromField(String fieldName) {
    String columnName = super.resolveColumnNameFromField(fieldName);
    if (!getConfig().isQuoteIdentifiers()) {
      // There are specific use cases where we explicitly quote the column name, even
      // if the
      // quoted identifiers is not enabled, such as the Kafka primary key mode column
      // names.
      // If they're quoted, we shouldn't lowercase the column name.
      if (!getIdentifierHelper().toIdentifier(columnName).isQuoted()) {
        // PostgreSQL defaults to lower case for identifiers
        columnName = columnName.toLowerCase(Locale.US);
      }
    }
    return columnName;
  }

  @Override
  public String getTimestampPositiveInfinityValue() {
    return POSITIVE_INFINITY;
  }

  @Override
  public String getTimestampNegativeInfinityValue() {
    return NEGATIVE_INFINITY;
  }
}
