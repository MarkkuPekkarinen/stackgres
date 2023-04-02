/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class DbOpsDecoratorDiscoverer
    extends ResourceDiscoverer<Decorator<StackGresDbOpsContext>>
    implements DecoratorDiscoverer<StackGresDbOpsContext> {

  @Inject
  public DbOpsDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresDbOpsContext>> instance) {
    init(instance);
  }

  @Override
  public List<Decorator<StackGresDbOpsContext>> discoverDecorator(StackGresDbOpsContext context) {
    return resourceHub.get(context.getVersion()).stream()
        .collect(Collectors.toUnmodifiableList());

  }
}
