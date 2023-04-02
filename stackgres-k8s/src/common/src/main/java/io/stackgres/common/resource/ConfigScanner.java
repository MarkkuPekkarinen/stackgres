/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigList;

@ApplicationScoped
public class ConfigScanner
    extends AbstractCustomResourceScanner<StackGresConfig, StackGresConfigList> {

  /**
   * Create a {@code OperatorConfigScanner} instance.
   */
  @Inject
  public ConfigScanner(KubernetesClient client) {
    super(client, StackGresConfig.class, StackGresConfigList.class);
  }

}
