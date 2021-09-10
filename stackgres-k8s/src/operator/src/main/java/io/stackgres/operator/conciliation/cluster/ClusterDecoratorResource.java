/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.operator.conciliation.AbstractDecoratorResource;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class ClusterDecoratorResource extends AbstractDecoratorResource<StackGresClusterContext> {

  @Inject
  public ClusterDecoratorResource(DecoratorDiscoverer<StackGresClusterContext> decoratorDiscoverer,
      ResourceGenerationDiscoverer<StackGresClusterContext> generators) {
    super(decoratorDiscoverer, generators);
  }

  public ClusterDecoratorResource() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}