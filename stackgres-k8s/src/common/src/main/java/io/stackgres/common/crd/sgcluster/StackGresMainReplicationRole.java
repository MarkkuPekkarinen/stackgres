/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresMainReplicationRole {

  HA(StackGresReplicationRole.HA),
  HA_READ(StackGresReplicationRole.HA_READ);

  private final @NotNull StackGresReplicationRole role;

  StackGresMainReplicationRole(@NotNull StackGresReplicationRole role) {
    this.role = role;
  }

  @Override
  public @NotNull String toString() {
    return role.toString();
  }
}