/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.mutation.DefaultStateMutator;
import io.stackgres.operator.mutation.DefaultStateMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgBouncerDefaultStateMutatorTest
    extends DefaultStateMutatorTest<StackGresPoolingConfig, PoolingReview> {

  @Override
  protected DefaultStateMutator<StackGresPoolingConfig, PoolingReview> getMutatorInstance() {
    return new PgBouncerDefaultStateMutator();
  }

  @Override
  protected PoolingReview getEmptyReview() {
    PoolingReview review = AdmissionReviewFixtures.poolingConfig().loadCreate().get();
    review.getRequest().getObject().getSpec().getPgBouncer().getPgbouncerIni()
        .setParameters(Map.of());
    return review;
  }

  @Override
  protected PoolingReview getDefaultReview() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

  @Override
  protected StackGresPoolingConfig getDefaultResource() {
    return Fixtures.poolingConfig().loadDefault().get();
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("status").get("pgBouncer").get("defaultParameters");
  }

  @Override
  protected int getMissingParentsCount() {
    return 2;
  }

  @Override
  protected Map<String, String> getConfigParameters(StackGresPoolingConfig resource) {
    return resource.getStatus().getPgBouncer().getDefaultParameters();
  }

}