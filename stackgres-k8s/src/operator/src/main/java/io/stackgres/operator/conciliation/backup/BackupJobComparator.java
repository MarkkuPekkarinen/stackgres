/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.JobComparator;

@ReconciliationScope(value = StackGresBackup.class, kind = "Job")
@ApplicationScoped
public class BackupJobComparator extends JobComparator {

}