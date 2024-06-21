/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.cloudevent;

import io.stackgres.stream.jobs.AbstractStreamJob;
import io.stackgres.stream.jobs.StateHandler;
import io.stackgres.stream.jobs.StreamEventStateHandler;
import io.stackgres.stream.jobs.StreamTargetOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@StreamTargetOperation("CloudEvent")
public class StreamCloudEventJob extends AbstractStreamJob {

  @Inject
  public StreamCloudEventJob(@StateHandler("CloudEvent") StreamEventStateHandler stateHandler) {
    super(stateHandler);
  }

}
