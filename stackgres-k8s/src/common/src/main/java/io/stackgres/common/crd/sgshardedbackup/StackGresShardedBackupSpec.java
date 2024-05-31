/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedBackupSpec {

  @NotNull(message = "The sharded cluster name is required")
  private String sgShardedCluster;

  private Boolean managedLifecycle;

  private Integer timeout;

  private Integer reconciliationTimeout;

  @Min(value = 0, message = "maxRetries must be greather or equals to 0.")
  private Integer maxRetries;

  public String getSgShardedCluster() {
    return sgShardedCluster;
  }

  public void setSgShardedCluster(String sgShardedCluster) {
    this.sgShardedCluster = sgShardedCluster;
  }

  public Boolean getManagedLifecycle() {
    return managedLifecycle;
  }

  public void setManagedLifecycle(Boolean managedLifecycle) {
    this.managedLifecycle = managedLifecycle;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public Integer getReconciliationTimeout() {
    return reconciliationTimeout;
  }

  public void setReconciliationTimeout(Integer reconciliationTimeout) {
    this.reconciliationTimeout = reconciliationTimeout;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  @Override
  public int hashCode() {
    return Objects.hash(managedLifecycle, maxRetries, reconciliationTimeout, sgShardedCluster,
        timeout);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedBackupSpec)) {
      return false;
    }
    StackGresShardedBackupSpec other = (StackGresShardedBackupSpec) obj;
    return Objects.equals(managedLifecycle, other.managedLifecycle)
        && Objects.equals(maxRetries, other.maxRetries)
        && Objects.equals(reconciliationTimeout, other.reconciliationTimeout)
        && Objects.equals(sgShardedCluster, other.sgShardedCluster)
        && Objects.equals(timeout, other.timeout);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
