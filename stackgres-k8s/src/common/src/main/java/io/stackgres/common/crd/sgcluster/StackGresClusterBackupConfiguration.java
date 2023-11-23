/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterBackupConfiguration {

  @Positive(message = "retention should be greater than zero")
  private Integer retention;

  private String cronSchedule;

  private String compression;

  @Valid
  private StackGresBaseBackupPerformance performance;

  @NotNull
  private String sgObjectStorage;

  @NotNull
  private String path;

  public Integer getRetention() {
    return retention;
  }

  public void setRetention(Integer retention) {
    this.retention = retention;
  }

  public String getCronSchedule() {
    return cronSchedule;
  }

  public void setCronSchedule(String cronSchedule) {
    this.cronSchedule = cronSchedule;
  }

  public String getCompression() {
    return compression;
  }

  public void setCompression(String compression) {
    this.compression = compression;
  }

  public StackGresBaseBackupPerformance getPerformance() {
    return performance;
  }

  public void setPerformance(StackGresBaseBackupPerformance performance) {
    this.performance = performance;
  }

  public String getSgObjectStorage() {
    return sgObjectStorage;
  }

  public void setSgObjectStorage(String sgObjectStorage) {
    this.sgObjectStorage = sgObjectStorage;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterBackupConfiguration)) {
      return false;
    }
    StackGresClusterBackupConfiguration other = (StackGresClusterBackupConfiguration) obj;
    return Objects.equals(path, other.path)
        && Objects.equals(compression, other.compression)
        && Objects.equals(cronSchedule, other.cronSchedule)
        && Objects.equals(sgObjectStorage, other.sgObjectStorage)
        && Objects.equals(performance, other.performance)
        && Objects.equals(retention, other.retention);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, compression, cronSchedule, sgObjectStorage, performance,
        retention);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
