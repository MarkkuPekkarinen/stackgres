/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterReplicateFrom {

  @JsonProperty("instance")
  @Valid
  @NotNull(message = "instance section is required")
  private StackGresClusterReplicateFromInstance instance;

  @JsonProperty("users")
  @Valid
  private StackGresClusterReplicateFromUsers users;

  @ReferencedField("users")
  interface Users extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "Users is required when replicating from external instance",
      payload = { Users.class })
  public boolean isUsersNotNullWithExternal() {
    return instance == null || instance.getExternal() == null || users != null;
  }

  @JsonIgnore
  @AssertTrue(message = "Users is forbidden when replicating from an SGCluster",
      payload = { Users.class })
  public boolean isUsersNullWithSgCluster() {
    return instance == null || instance.getSgCluster() == null || users == null;
  }

  public StackGresClusterReplicateFromInstance getInstance() {
    return instance;
  }

  public void setInstance(StackGresClusterReplicateFromInstance instance) {
    this.instance = instance;
  }

  public StackGresClusterReplicateFromUsers getUsers() {
    return users;
  }

  public void setUsers(StackGresClusterReplicateFromUsers users) {
    this.users = users;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance, users);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicateFrom)) {
      return false;
    }
    StackGresClusterReplicateFrom other = (StackGresClusterReplicateFrom) obj;
    return Objects.equals(instance, other.instance) && Objects.equals(users, other.users);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}