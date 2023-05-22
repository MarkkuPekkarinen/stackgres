/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterDto extends ResourceDto {

  @JsonProperty("spec")
  private ShardedClusterSpec spec;

  @JsonProperty("status")
  private ShardedClusterStatus status;

  @JsonProperty("clusters")
  private List<String> clusters;

  @JsonProperty("grafanaEmbedded")
  private boolean grafanaEmbedded;

  @JsonProperty("info")
  private ShardedClusterInfoDto info;

  public ShardedClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(ShardedClusterSpec spec) {
    this.spec = spec;
  }

  public ShardedClusterStatus getStatus() {
    return status;
  }

  public void setStatus(ShardedClusterStatus status) {
    this.status = status;
  }

  public List<String> getClusters() {
    return clusters;
  }

  public void setClusters(List<String> clusters) {
    this.clusters = clusters;
  }

  public boolean isGrafanaEmbedded() {
    return grafanaEmbedded;
  }

  public void setGrafanaEmbedded(boolean grafanaEmbedded) {
    this.grafanaEmbedded = grafanaEmbedded;
  }

  public ShardedClusterInfoDto getInfo() {
    return info;
  }

  public void setInfo(ShardedClusterInfoDto info) {
    this.info = info;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}