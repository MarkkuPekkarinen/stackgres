/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.postgres;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.google.common.collect.Maps;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import org.jetbrains.annotations.NotNull;

public interface PostgresDefaultValues {

  enum PostgresDefaulValuesProperties {
    PG_14_VALUES("/postgresql-default-values-pg14.properties"),
    PG_13_VALUES("/postgresql-default-values-pg13.properties"),
    PG_12_VALUES("/postgresql-default-values-pg12.properties"),
    PG_14_VALUES_V_1_14("/v1.14/postgresql-default-values-pg14-v1.14.properties"),
    PG_13_VALUES_V_1_14("/v1.14/postgresql-default-values-pg13-v1.14.properties"),
    PG_12_VALUES_V_1_14("/v1.14/postgresql-default-values-pg12-v1.14.properties"),
    PG_14_VALUES_V_1_13("/v1.13/postgresql-default-values-pg14-v1.13.properties"),
    PG_13_VALUES_V_1_13("/v1.13/postgresql-default-values-pg13-v1.13.properties"),
    PG_12_VALUES_V_1_13("/v1.13/postgresql-default-values-pg12-v1.13.properties");

    private final @NotNull Properties properties;

    PostgresDefaulValuesProperties(@NotNull String file) {
      this.properties = StackGresUtil.loadProperties(file);
    }
  }

  static @NotNull Properties getProperties(
      @NotNull String pgVersion) {
    return getProperties(StackGresVersion.LATEST, pgVersion);
  }

  static @NotNull Properties getProperties(
      @NotNull StackGresVersion version,
      @NotNull String pgVersion) {
    Objects.requireNonNull(version, "operatorVersion parameter is null");
    Objects.requireNonNull(pgVersion, "pgVersion parameter is null");
    int majorVersion = Integer.parseInt(pgVersion.split("\\.")[0]);

    if (version.getVersionAsNumber() <= StackGresVersion.V_1_13.getVersionAsNumber()) {
      if (majorVersion <= 12) {
        return PostgresDefaulValuesProperties.PG_12_VALUES_V_1_13.properties;
      }
      if (majorVersion <= 13) {
        return PostgresDefaulValuesProperties.PG_13_VALUES_V_1_13.properties;
      }
      return PostgresDefaulValuesProperties.PG_14_VALUES_V_1_13.properties;
    }

    if (version.getVersionAsNumber() <= StackGresVersion.V_1_14.getVersionAsNumber()) {
      if (majorVersion <= 12) {
        return PostgresDefaulValuesProperties.PG_12_VALUES_V_1_14.properties;
      }
      if (majorVersion <= 13) {
        return PostgresDefaulValuesProperties.PG_13_VALUES_V_1_14.properties;
      }
      return PostgresDefaulValuesProperties.PG_14_VALUES_V_1_14.properties;
    }

    if (majorVersion <= 12) {
      return PostgresDefaulValuesProperties.PG_12_VALUES.properties;
    }
    if (majorVersion <= 13) {
      return PostgresDefaulValuesProperties.PG_13_VALUES.properties;
    }
    return PostgresDefaulValuesProperties.PG_14_VALUES.properties;
  }

  static @NotNull Map<String, String> getDefaultValues(
      @NotNull String pgVersion) {
    return getDefaultValues(StackGresVersion.LATEST, pgVersion);
  }

  static @NotNull Map<String, String> getDefaultValues(
      @NotNull StackGresVersion version,
      @NotNull String pgVersion) {
    return Maps.fromProperties(getProperties(version, pgVersion));
  }

}
