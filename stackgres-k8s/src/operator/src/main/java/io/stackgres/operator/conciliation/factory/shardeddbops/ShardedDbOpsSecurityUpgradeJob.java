/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSecurityUpgrade;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;

@Singleton
@OperatorVersionBinder
@ShardedDbOpsJob("securityUpgrade")
public class ShardedDbOpsSecurityUpgradeJob extends AbstractShardedDbOpsJob {

  @Inject
  public ShardedDbOpsSecurityUpgradeJob(
      ResourceFactory<StackGresShardedDbOpsContext, PodSecurityContext> podSecurityFactory,
      ShardedDbOpsEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactoryForShardedDbOps dbOpsLabelFactory,
      ObjectMapper jsonMapper,
      KubectlUtil kubectl,
      ShardedDbOpsVolumeMounts dbOpsVolumeMounts,
      ShardedDbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory) {
    super(podSecurityFactory, clusterStatefulSetEnvironmentVariables,
        dbOpsLabelFactory, jsonMapper, kubectl, dbOpsVolumeMounts, dbOpsTemplatesVolumeFactory);
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresShardedDbOpsContext context) {
    StackGresShardedDbOps dbOps = context.getSource();
    StackGresShardedDbOpsSecurityUpgrade securityUpgrade =
        dbOps.getSpec().getSecurityUpgrade();
    return ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
                .withName("METHOD")
                .withValue(Optional.ofNullable(securityUpgrade)
                    .map(StackGresShardedDbOpsSecurityUpgrade::getMethod)
                    .orElse(DbOpsMethodType.IN_PLACE.toString()))
                .build(),
            new EnvVarBuilder()
                .withName("CRD_GROUP")
                .withValue(CommonDefinition.GROUP)
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_CRD_NAME")
                .withValue(HasMetadata.getPlural(StackGresShardedCluster.class))
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_NAMESPACE")
                .withValue(context.getSource().getMetadata().getNamespace())
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_NAME")
                .withValue(context.getSource().getSpec().getSgShardedCluster())
                .build(),
            new EnvVarBuilder()
                .withName("SERVICE_ACCOUNT")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withFieldRef(new ObjectFieldSelector("v1", "spec.serviceAccountName"))
                    .build())
                .build(),
            new EnvVarBuilder()
                .withName("POD_NAME")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                    .build())
                .build(),
            new EnvVarBuilder()
                .withName("DBOPS_CRD_NAME")
                .withValue(CustomResource.getCRDName(StackGresShardedDbOps.class))
                .build(),
            new EnvVarBuilder()
                .withName("DBOPS_NAME")
                .withValue(dbOps.getMetadata().getName())
                .build(),
            new EnvVarBuilder()
                .withName("PATRONI_CONTAINER_NAME")
                .withValue(StackGresContainer.PATRONI.getName())
                .build(),
            new EnvVarBuilder()
                .withName("MAJOR_VERSION_UPGRADE_CONTAINER_NAME")
                .withValue(StackGresInitContainer.MAJOR_VERSION_UPGRADE.getName())
                .build(),
            new EnvVarBuilder()
                .withName("POSTGRES_VERSION_KEY")
                .withValue(StackGresContext.POSTGRES_VERSION_KEY)
                .build(),
            new EnvVarBuilder()
                .withName("LOCK_DURATION")
                .withValue(OperatorProperty.LOCK_DURATION.getString())
                .build(),
            new EnvVarBuilder()
                .withName("LOCK_SLEEP")
                .withValue(OperatorProperty.LOCK_POLL_INTERVAL.getString())
                .build(),
            new EnvVarBuilder()
                .withName("LOCK_SERVICE_ACCOUNT_KEY")
                .withValue(StackGresContext.LOCK_SERVICE_ACCOUNT_KEY)
                .build(),
            new EnvVarBuilder()
                .withName("LOCK_POD_KEY")
                .withValue(StackGresContext.LOCK_POD_KEY)
                .build(),
            new EnvVarBuilder()
                .withName("LOCK_TIMEOUT_KEY")
                .withValue(StackGresContext.LOCK_TIMEOUT_KEY)
                .build())
        .build();
  }

  @Override
  protected String getRunImage(StackGresShardedDbOpsContext context) {
    return kubectl.getImageName(context.getShardedCluster());
  }

  @Override
  protected ShardedClusterPath getRunScript() {
    return ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_SECURITY_UPGRADE_SH_PATH;
  }

  @Override
  protected boolean isExclusiveOp() {
    return true;
  }

}
