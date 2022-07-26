/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Optional;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.stackgres.common.StackGresKind;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class DbOpsProfileDecorator extends AbstractProfileDecorator
    implements Decorator<StackGresDbOpsContext> {

  @Override
  protected StackGresKind getKind() {
    return StackGresKind.DBOPS;
  }

  @Override
  public void decorate(StackGresDbOpsContext context, Iterable<? extends HasMetadata> resources) {
    if (Optional.of(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisableClusterResourceRequirements)
        .orElse(false)) {
      return;
    }

    Seq.seq(resources)
        .filter(Job.class::isInstance)
        .map(Job.class::cast)
        .forEach(job -> setProfileContainers(context.getProfile(),
            () -> Optional.of(job)
            .map(Job::getSpec)
            .map(JobSpec::getTemplate)
            .map(PodTemplateSpec::getSpec),
            Optional.ofNullable(context.getCluster().getSpec().getNonProductionOptions())
            .map(StackGresClusterNonProduction::getEnableSetClusterCpuRequests)
            .orElse(false),
            Optional.ofNullable(context.getCluster().getSpec().getNonProductionOptions())
            .map(StackGresClusterNonProduction::getEnableSetClusterMemoryRequests)
            .orElse(false)));
  }

}