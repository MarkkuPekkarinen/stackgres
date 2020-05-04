/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterSpec implements KubernetesResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @JsonProperty("instances")
  @Positive(message = "You need at least 1 instance in the cluster")
  private int instances;

  @JsonProperty("postgresVersion")
  @NotBlank(message = "PostgreSQL version is required")
  private String postgresVersion;

  @JsonProperty("configurations")
  @Valid
  @NotNull(message = "cluster configuration cannot be null")
  private StackgresClusterConfiguration configuration;

  @JsonProperty("sgInstanceProfile")
  @NotNull(message = "resource profile must not be null")
  private String resourceProfile;

  @JsonProperty("initialData")
  private StackGresClusterInitData initData;

  @JsonProperty("pods")
  @Valid
  @NotNull(message = "pod description must be specified")
  private StackGresClusterPod pod;

  @JsonProperty("distributedLogs")
  private StackGresClusterDistributedLogs distributedLogs;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("nonProductionOptions")
  private NonProduction nonProduction;

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public StackgresClusterConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(StackgresClusterConfiguration configuration) {
    this.configuration = configuration;
  }

  public StackGresClusterPod getPod() {
    return pod;
  }

  public void setPod(StackGresClusterPod pod) {
    this.pod = pod;
  }

  public String getResourceProfile() {
    return resourceProfile;
  }

  public void setResourceProfile(String resourceProfile) {
    this.resourceProfile = resourceProfile;
  }

  public Boolean getPrometheusAutobind() {
    return prometheusAutobind;
  }

  public void setPrometheusAutobind(Boolean prometheusAutobind) {
    this.prometheusAutobind = prometheusAutobind;
  }

  public NonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(NonProduction nonProduction) {
    this.nonProduction = nonProduction;
  }

  public StackGresClusterInitData getInitData() {
    return initData;
  }

  public void setInitData(StackGresClusterInitData initData) {
    this.initData = initData;
  }

  public StackGresClusterDistributedLogs getDistributedLogs() {
    return distributedLogs;
  }

  public void setDistributedLogs(StackGresClusterDistributedLogs distributedLogs) {
    this.distributedLogs = distributedLogs;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("postgresVersion", postgresVersion)
        .add("instances", instances)
        .add("sgInstanceProfile", resourceProfile)
        .add("pods", pod)
        .add("configurations", configuration)
        .add("initData", initData)
        .add("distributedLogs", distributedLogs)
        .add("nonProductionOptions", nonProduction)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresClusterSpec that = (StackGresClusterSpec) o;
    return instances == that.instances && Objects.equals(postgresVersion, that.postgresVersion)
        && Objects.equals(configuration, that.configuration)
        && Objects.equals(resourceProfile, that.resourceProfile)
        && Objects.equals(initData, that.initData) && Objects.equals(pod, that.pod)
        && Objects.equals(prometheusAutobind, that.prometheusAutobind)
        && Objects.equals(distributedLogs, that.distributedLogs)
        && Objects.equals(nonProduction, that.nonProduction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instances, postgresVersion, configuration, resourceProfile,
        initData, pod, prometheusAutobind, distributedLogs, nonProduction);
  }
}
