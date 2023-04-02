/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.testutil.SetterGetterTestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CrdSetterGetterTest extends SetterGetterTestCase {

  @ParameterizedTest
  @ValueSource(classes = {
      StackGresConfig.class,
      StackGresCluster.class,
      StackGresProfile.class,
      StackGresPostgresConfig.class,
      StackGresPoolingConfig.class,
      StackGresBackupConfig.class,
      StackGresObjectStorage.class,
      StackGresDbOps.class,
      StackGresDistributedLogs.class,
      StackGresScript.class,
      StackGresShardedCluster.class,
  })
  @Override
  protected void assertSettersAndGetters(Class<?> sourceClazz) {
    super.assertSettersAndGetters(sourceClazz);
  }

}
