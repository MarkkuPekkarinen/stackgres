/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class ConfigDecoratorDiscoverer
    extends ResourceDiscoverer<Decorator<StackGresConfigContext>>
    implements DecoratorDiscoverer<StackGresConfigContext> {

  @Inject
  public ConfigDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresConfigContext>> instance) {
    init(instance);
  }

  @Override
  public List<Decorator<StackGresConfigContext>> discoverDecorator(
      StackGresConfigContext context) {
    return resourceHub.get(context.getVersion()).stream()
        .collect(Collectors.toUnmodifiableList());
  }

}
