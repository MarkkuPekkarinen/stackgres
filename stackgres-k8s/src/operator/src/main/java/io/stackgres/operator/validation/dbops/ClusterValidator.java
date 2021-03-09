/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ClusterValidator implements DbOpsValidator {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public ClusterValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(StackGresDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        String cluster = review.getRequest().getObject().getSpec().getSgCluster();
        String namespace = review.getRequest().getObject().getMetadata().getNamespace();
        checkIfClusterExists(cluster, namespace,
            "Cluster " + cluster + " not found");
        break;
      default:
    }
  }

  private void checkIfClusterExists(String cluster, String namespace,
      String onError) throws ValidationFailed {
    Optional<StackGresCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(cluster, namespace);

    if (!clusterOpt.isPresent()) {
      fail(onError);
    }
  }

}
