/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.migration;

import io.stackgres.stream.jobs.AbstractStreamJob;
import io.stackgres.stream.jobs.StateHandler;
import io.stackgres.stream.jobs.StreamEventStateHandler;
import io.stackgres.stream.jobs.StreamTargetOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@StreamTargetOperation("SGCluster")
public class StreamMigrationClusterJob extends AbstractStreamJob {

  @Inject
  public StreamMigrationClusterJob(@StateHandler("SGCluster") StreamEventStateHandler stateHandler) {
    super(stateHandler);
  }

}
