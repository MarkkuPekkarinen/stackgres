/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PersistentVolumeClaimFinder extends AbstractResourceFinderAndScanner<PersistentVolumeClaim> {

  @Inject
  public PersistentVolumeClaimFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<PersistentVolumeClaim, ? extends KubernetesResourceList<PersistentVolumeClaim>,
          ? extends Resource<PersistentVolumeClaim>>
      getOperation(KubernetesClient client) {
    return client.persistentVolumeClaims();
  }

}
