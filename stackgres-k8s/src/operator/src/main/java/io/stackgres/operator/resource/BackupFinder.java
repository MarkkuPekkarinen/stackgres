/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackupDoneable;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;

@ApplicationScoped
public class BackupFinder
    extends AbstractCustomResourceFinder<StackGresBackup> {

  /**
   * Create a {@code BackupFinder} instance.
   */
  @Inject
  public BackupFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresBackupDefinition.NAME,
        StackGresBackup.class, StackGresBackupList.class,
        StackGresBackupDoneable.class);
  }

  public BackupFinder() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
