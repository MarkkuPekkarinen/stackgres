/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Group(CommonDefinition.GROUP)
@Version(CommonDefinition.VERSION)
@Kind(StackGresConfig.KIND)
@Singular("sgconfig")
@Plural("sgconfigs")
public final class StackGresConfig
    extends CustomResource<StackGresConfigSpec, StackGresConfigStatus>
    implements Namespaced {

  private static final long serialVersionUID = 1L;

  public static final String KIND = "SGConfig";

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private StackGresConfigSpec spec;

  @JsonProperty("status")
  @Valid
  private StackGresConfigStatus status;

  public StackGresConfig() {
    super();
  }

  @Override
  public StackGresConfigSpec getSpec() {
    return spec;
  }

  @Override
  public void setSpec(StackGresConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public StackGresConfigStatus getStatus() {
    return status;
  }

  @Override
  public void setStatus(StackGresConfigStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfig)) {
      return false;
    }
    StackGresConfig other = (StackGresConfig) obj;
    return Objects.equals(spec, other.spec) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
