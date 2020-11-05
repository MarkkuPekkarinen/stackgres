/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pgconfig;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PostgresConfigSpec {

  @JsonProperty("postgresVersion")
  @NotBlank(message = "The PostgreSQL version is required")
  private String postgresVersion;

  @JsonProperty("postgresql.conf")
  @NotNull(message = "postgresql.conf is required")
  private String postgresqlConf;

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getPostgresqlConf() {
    return postgresqlConf;
  }

  public void setPostgresqlConf(String postgresqlConf) {
    this.postgresqlConf = postgresqlConf;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
