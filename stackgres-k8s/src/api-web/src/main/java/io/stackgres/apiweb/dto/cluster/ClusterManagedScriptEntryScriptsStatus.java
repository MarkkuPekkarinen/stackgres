/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterManagedScriptEntryScriptsStatus {

  @JsonProperty("id")
  private Integer id;

  @JsonProperty("version")
  private Integer version;

  @JsonProperty("failureCode")
  private String failureCode;

  @JsonProperty("failure")
  private String failure;

  @JsonProperty("failures")
  private Integer failures;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public void setFailureCode(String failureCode) {
    this.failureCode = failureCode;
  }

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  public Integer getFailures() {
    return failures;
  }

  public void setFailures(Integer failures) {
    this.failures = failures;
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, failureCode, failures, id, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterManagedScriptEntryScriptsStatus)) {
      return false;
    }
    ClusterManagedScriptEntryScriptsStatus other = (ClusterManagedScriptEntryScriptsStatus) obj;
    return Objects.equals(failure, other.failure) && Objects.equals(failureCode, other.failureCode)
        && Objects.equals(failures, other.failures) && Objects.equals(id, other.id)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}