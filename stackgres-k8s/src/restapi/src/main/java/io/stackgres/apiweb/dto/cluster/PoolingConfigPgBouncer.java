/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PoolingConfigPgBouncer {

  @JsonProperty("pgbouncer.ini")
  private PoolingConfigPgBouncerPgbouncerIni parameters;

  public PoolingConfigPgBouncerPgbouncerIni getParameters() {
    return parameters;
  }

  public void setParameters(PoolingConfigPgBouncerPgbouncerIni parameters) {
    this.parameters = parameters;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
