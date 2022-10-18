/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replicate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.backup.AbstractBackupConfigMap;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class ReplicateConfigMap extends AbstractBackupConfigMap
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StatefulSetDynamicVolumes.REPLICATE_ENV.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build());
  }

  private Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.REPLICATE_ENV.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .build())
        .build();
  }

  private ConfigMap buildSource(StackGresClusterContext context) {
    final Map<String, String> data = new HashMap<>();
    final StackGresCluster cluster = context.getSource();

    context.getReplicateStorage()
        .ifPresent(storage -> data.putAll(
                getBackupEnvVars(
                    context,
                    context.getReplicatePath().orElseThrow(),
                    storage
                )
            )
        );

    context.getReplicateConfiguration()
        .ifPresent(config -> data.putAll(
                getBackupEnvVars(
                    config
                )
            )
        );

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build();
  }

  @Override
  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterStatefulSetPath.REPLICATE_SECRET_PATH.path(context)
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}