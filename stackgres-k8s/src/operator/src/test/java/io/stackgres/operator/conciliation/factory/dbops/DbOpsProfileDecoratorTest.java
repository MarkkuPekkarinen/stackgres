/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresKind;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractProfileDecoratorTestCase;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsProfileDecoratorTest extends AbstractProfileDecoratorTestCase {

  private static final StackGresKind KIND = StackGresKind.DBOPS;

  private final DbOpsProfileDecorator profileDecorator = new DbOpsProfileDecorator();

  @Mock
  private StackGresDbOpsContext context;

  private StackGresDbOps dbOps;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  private Job job;

  private List<HasMetadata> resources;

  @BeforeEach
  void setUp() {
    dbOps = JsonUtil
        .readFromJson("stackgres_dbops/dbops_restart.json", StackGresDbOps.class);
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    profile = JsonUtil
        .readFromJson("stackgres_profiles/size-xs.json", StackGresProfile.class);

    final ObjectMeta metadata = dbOps.getMetadata();
    metadata.getAnnotations().put(StackGresContext.VERSION_KEY,
        StackGresProperty.OPERATOR_VERSION.getString());
    resources = KubernetessMockResourceGenerationUtil
        .buildResources(metadata.getName(), metadata.getNamespace());
    job = resources.stream()
        .filter(Job.class::isInstance)
        .map(Job.class::cast)
        .findFirst()
        .orElseThrow();
    profile.getSpec().setContainers(new HashMap<>());
    profile.getSpec().setInitContainers(new HashMap<>());
    Seq.seq(job.getSpec()
            .getTemplate().getSpec().getContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    Seq.seq(job.getSpec()
            .getTemplate().getSpec().getInitContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getInitContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    StackGresProfileContainer containerProfile = new StackGresProfileContainer();
    containerProfile.setCpu(new Random().nextInt(32000) + "m");
    containerProfile.setMemory(new Random().nextInt(32) + "Gi");
    profile.getSpec().getContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);
    profile.getSpec().getInitContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);

    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getProfile()).thenReturn(profile);
  }

  @Override
  protected StackGresProfile getProfile() {
    return profile;
  }

  @Override
  protected PodSpec getPodSpec() {
    return job.getSpec().getTemplate().getSpec();
  }

  @Override
  protected StackGresKind getKind() {
    return KIND;
  }

  @Override
  protected void decorate() {
    profileDecorator.decorate(context, resources);
  }

  @Override
  protected void disableResourceRequirements() {
    cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    cluster.getSpec().getNonProductionOptions().setDisableClusterResourceRequirements(true);
  }

  @Override
  protected void enableRequests() {
    cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    cluster.getSpec().getNonProductionOptions().setEnableSetClusterCpuRequests(true);
    cluster.getSpec().getNonProductionOptions().setEnableSetClusterMemoryRequests(true);
  }

}