/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.concurrent.CompletableFuture;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStreamJob implements StreamJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamJob.class);

  @Inject
  CustomResourceFinder<StackGresStream> streamFinder;

  @Inject
  CustomResourceScheduler<StackGresStream> streamScheduler;

  @Inject
  StreamExecutorService executorService;

  final StreamEventStateHandler stateHandler;

  public AbstractStreamJob(StreamEventStateHandler stateHandler) {
    this.stateHandler = stateHandler;
  }

  public AbstractStreamJob() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.stateHandler = null;
  }

  @Override
  public CompletableFuture<Void> runJob(StackGresStream stream) {
    LOGGER.info("Starting streaming to {} for SGStream {}",
        stream.getSpec().getTarget().getType(), stream.getMetadata().getName());

    return stateHandler.sendEvents(stream)
        .whenComplete((ignored, ex) -> {
          if (ex != null) {
            reportFailure(stream, ex);
          }
        });
  }

  private void reportFailure(StackGresStream stream, Throwable ex) {
    String message = ex.getMessage();
    String streamName = stream.getMetadata().getName();
    String namespace = stream.getMetadata().getNamespace();

    streamFinder.findByNameAndNamespace(streamName, namespace)
        .ifPresent(savedStream -> {
          if (savedStream.getStatus() == null) {
            savedStream.setStatus(new StackGresStreamStatus());
          }

          savedStream.getStatus().setFailure(message);

          streamScheduler.update(savedStream);
        });
  }

}
