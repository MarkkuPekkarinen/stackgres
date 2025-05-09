/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedClusterShardsPgBouncerDefaultPoolingConfig
    implements ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;
  private final DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  @Inject
  public ShardedClusterShardsPgBouncerDefaultPoolingConfig(
      LabelFactoryForShardedCluster labelFactory,
      DefaultPoolingConfigFactory defaultPoolingConfigFactory) {
    this.labelFactory = labelFactory;
    this.defaultPoolingConfigFactory = defaultPoolingConfigFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Stream
        .of(Optional.of(context.getSource().getSpec().getShards().getPods())
            .map(StackGresClusterPods::getDisableConnectionPooling)
            .orElse(false))
        .filter(disabled -> !disabled)
        .filter(ignored -> context.getShardsPoolingConfig().isEmpty()
            || context.getShardsPoolingConfig()
            .filter(poolingConfig -> labelFactory.defaultConfigLabels(context.getSource())
                .entrySet()
                .stream()
                .allMatch(label -> Optional
                    .ofNullable(poolingConfig.getMetadata().getLabels())
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .anyMatch(label::equals)))
            .map(postgresConfig -> postgresConfig.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .filter(ignored -> !context.getSource().getSpec()
            .getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig()
            .equals(context.getSource().getSpec().getShards().getConfigurations().getSgPoolingConfig()))
        .map(ignored -> getDefaultConfig(context.getSource()));
  }

  private StackGresPoolingConfig getDefaultConfig(StackGresShardedCluster cluster) {
    return new StackGresPoolingConfigBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig())
        .withLabels(labelFactory.defaultConfigLabels(cluster))
        .endMetadata()
        .withSpec(defaultPoolingConfigFactory.buildResource(cluster).getSpec())
        .build();
  }

}
