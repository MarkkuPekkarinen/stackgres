/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class MockWatcherHandler implements WatcherHandler {

  @Override
  public void startWatchers() {
    //For integration testing purposes we don't need watchers
  }

  @Override
  public void stopWatchers() {
    //Do nothing
  }

}
