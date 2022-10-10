/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterResources {

  @JsonProperty("enableClusterLimitsRequirements")
  private Boolean enableClusterLimitsRequirements;

  public Boolean getEnableClusterLimitsRequirements() {
    return enableClusterLimitsRequirements;
  }

  public void setEnableClusterLimitsRequirements(Boolean enableClusterLimitsRequirements) {
    this.enableClusterLimitsRequirements = enableClusterLimitsRequirements;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enableClusterLimitsRequirements);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterResources)) {
      return false;
    }
    StackGresClusterResources other = (StackGresClusterResources) obj;
    return Objects.equals(enableClusterLimitsRequirements, other.enableClusterLimitsRequirements);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}