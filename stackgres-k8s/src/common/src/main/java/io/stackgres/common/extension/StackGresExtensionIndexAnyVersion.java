/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.Objects;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;

public class StackGresExtensionIndexAnyVersion {
  private final String name;
  private final String publisher;
  private final String postgresVersion;
  private final String postgresExactVersion;
  private final boolean fromIndex;
  private final String build;
  private final String arch;
  private final String os;

  public StackGresExtensionIndexAnyVersion(StackGresCluster cluster,
      StackGresClusterExtension extension) {
    this.name = extension.getName();
    this.publisher = extension.getPublisherOrDefault();
    this.postgresVersion = StackGresComponent.POSTGRESQL.findMajorVersion(
        cluster.getSpec().getPostgresVersion());
    this.postgresExactVersion = StackGresComponent.POSTGRESQL.findVersion(
        cluster.getSpec().getPostgresVersion());
    this.fromIndex = false;
    this.build = StackGresComponent.POSTGRESQL.findBuildMajorVersion(
        cluster.getSpec().getPostgresVersion());
    this.arch = ExtensionUtil.ARCH_X86_64;
    this.os = ExtensionUtil.OS_LINUX;
  }

  public StackGresExtensionIndexAnyVersion(StackGresExtension extension,
      StackGresExtensionVersionTarget target) {
    this.name = extension.getName();
    this.publisher = extension.getPublisherOrDefault();
    this.postgresVersion = target.getPostgresVersion();
    this.postgresExactVersion = target.getPostgresExactVersion();
    this.fromIndex = true;
    this.build = ExtensionUtil.getMajorBuildOrNull(target.getBuild());
    this.arch = target.getArchOrDefault();
    this.os = target.getOsOrDefault();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, postgresVersion, publisher);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionIndexAnyVersion)) {
      return false;
    }
    StackGresExtensionIndexAnyVersion other = (StackGresExtensionIndexAnyVersion) obj;
    if (Objects.equals(publisher, other.publisher)
        && Objects.equals(name, other.name)
        && Objects.equals(postgresVersion, other.postgresVersion)) {
      if (fromIndex && other.fromIndex) {
        return Objects.equals(arch, other.arch)
            && Objects.equals(os, other.os)
            && Objects.equals(build, other.build)
            && Objects.equals(postgresExactVersion, other.postgresExactVersion);
      }
      if (fromIndex && !other.fromIndex) {
        return (Objects.isNull(postgresExactVersion) // NOPMD
            || Objects.equals(postgresExactVersion, other.postgresExactVersion)) // NOPMD
            && (Objects.isNull(build) // NOPMD
                || (Objects.equals(arch, other.arch) // NOPMD
                && Objects.equals(os, other.os)
                && Objects.equals(build, other.build)));
      }
      if (!fromIndex && other.fromIndex) {
        return (Objects.isNull(other.postgresExactVersion) // NOPMD
            || Objects.equals(postgresExactVersion, other.postgresExactVersion)) // NOPMD
            && (Objects.isNull(other.build) // NOPMD
                || (Objects.equals(arch, other.arch) // NOPMD
                && Objects.equals(os, other.os)
                && Objects.equals(build, other.build)));
      }
      if (!fromIndex && !other.fromIndex) {
        return Objects.equals(arch, other.arch)
            && Objects.equals(os, other.os)
            && Objects.equals(build, other.build)
            && Objects.equals(postgresExactVersion, other.postgresExactVersion);
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s/%s/%s-pg%s%s",
        publisher, arch, os, name, postgresVersion,
        build != null ? "-build-" + build : "");
  }

}
