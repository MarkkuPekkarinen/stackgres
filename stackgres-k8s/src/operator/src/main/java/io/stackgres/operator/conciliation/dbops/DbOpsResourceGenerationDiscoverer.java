/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerator;

@ApplicationScoped
public class DbOpsResourceGenerationDiscoverer
    extends ResourceDiscoverer<ResourceGenerator<StackGresDbOpsContext>>
    implements ResourceGenerationDiscoverer<StackGresDbOpsContext> {

  @Inject
  public DbOpsResourceGenerationDiscoverer(
      @Any
          Instance<ResourceGenerator<StackGresDbOpsContext>> instance) {
    init(instance);
  }

  @Override
  public List<ResourceGenerator<StackGresDbOpsContext>> getResourceGenerators(
      StackGresDbOpsContext context) {
    return resourceHub.get(context.getVersion());
  }
}
