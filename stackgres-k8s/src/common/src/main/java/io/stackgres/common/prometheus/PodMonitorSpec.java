/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.prometheus;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class PodMonitorSpec {

  private String jobName;
  private LabelSelector selector;
  private NamespaceSelector namespaceSelector;
  private List<Endpoint> podMetricsEndpoints;

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public LabelSelector getSelector() {
    return selector;
  }

  public void setSelector(LabelSelector selector) {
    this.selector = selector;
  }

  public NamespaceSelector getNamespaceSelector() {
    return namespaceSelector;
  }

  public void setNamespaceSelector(NamespaceSelector namespaceSelector) {
    this.namespaceSelector = namespaceSelector;
  }

  public List<Endpoint> getPodMetricsEndpoints() {
    return podMetricsEndpoints;
  }

  public void setPodMetricsEndpoints(List<Endpoint> podMetricsEndpoints) {
    this.podMetricsEndpoints = podMetricsEndpoints;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("namespaceSelector", namespaceSelector)
        .add("selector", selector)
        .add("podMetricsEndpoints", podMetricsEndpoints)
        .toString();
  }

}
