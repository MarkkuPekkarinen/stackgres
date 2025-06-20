/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgcluster;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterInfo;
import io.stackgres.apiweb.dto.cluster.ClusterManagedScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterManagedSql;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceService;
import io.stackgres.apiweb.rest.sgscripts.ScriptResource;
import io.stackgres.apiweb.transformer.ScriptTransformer;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

@Path("sgclusters")
@RequestScoped
@Authenticated
@Tag(name = "sgcluster")
@APIResponse(responseCode = "400", description = "Bad Request",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "401", description = "Unauthorized",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "403", description = "Forbidden",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "500", description = "Internal Server Error",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
public class ClusterResource
    extends AbstractCustomResourceService<ClusterDto, StackGresCluster> {

  public static final String DEFAULT_SCRIPT_KEY = ScriptResource.DEFAULT_SCRIPT_KEY;

  private final CustomResourceScanner<ClusterDto> clusterScanner;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final CustomResourceScheduler<StackGresScript> scriptScheduler;
  private final ResourceWriter<Secret> secretWriter;
  private final ResourceWriter<ConfigMap> configMapWriter;
  private final CustomResourceFinder<StackGresScript> scriptFinder;
  private final ScriptTransformer scriptTransformer;
  private final ResourceFinder<Secret> secretFinder;
  private final ResourceFinder<ConfigMap> configMapFinder;
  private final ResourceFinder<Service> serviceFinder;

  @Inject
  public ClusterResource(
      CustomResourceScanner<ClusterDto> clusterScanner,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceScheduler<StackGresScript> scriptScheduler,
      ResourceWriter<Secret> secretWriter,
      ResourceWriter<ConfigMap> configMapWriter,
      CustomResourceFinder<StackGresScript> scriptFinder,
      ScriptTransformer scriptTransformer,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<ConfigMap> configMapFinder,
      ResourceFinder<Service> serviceFinder) {
    this.clusterScanner = clusterScanner;
    this.clusterFinder = clusterFinder;
    this.scriptScheduler = scriptScheduler;
    this.secretWriter = secretWriter;
    this.configMapWriter = configMapWriter;
    this.scriptFinder = scriptFinder;
    this.scriptTransformer = scriptTransformer;
    this.secretFinder = secretFinder;
    this.configMapFinder = configMapFinder;
    this.serviceFinder = serviceFinder;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(type = SchemaType.ARRAY, implementation = ClusterDto.class))})
  @Operation(summary = "List sgclusters", description = """
      List sgclusters and read values from the referenced secrets and configmaps.

      ### RBAC permissions required

      * sgclusters list
      * pod list
      * services list
      * configmaps get
      """)
  @Override
  public List<ClusterDto> list() {
    return Seq.seq(clusterScanner.getResources())
        .map(this::setScripts)
        .map(this::setConfigMaps)
        .map(this::setInfo)
        .toList();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ClusterDto.class))})
  @Operation(summary = "Create a sgcluster", description = """
      Create a sgcluster.
      If values for some script configmap or secret is provided respectively a secret or
       configmap named as the script if name is provided or as the sgcluster with
       `-init-script-<script index>` suffix is created/patched.

      ### RBAC permissions required

      * sgclusters create
      * secrets get, create, patch
      * configmaps get, create, patch
      """)
  @Override
  public ClusterDto create(ClusterDto resource, @Nullable Boolean dryRun) {
    createOrUpdateScripts(resource, Optional.ofNullable(dryRun).orElse(false));
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ClusterDto.class))})
  @Operation(summary = "Update a sgcluster", description = """
      Update a sgcluster.

      ### RBAC permissions required

      * sgclusters patch
      * secrets get, create, patch
      * configmaps get, create, patch
      """)
  @Override
  public ClusterDto update(ClusterDto resource, @Nullable Boolean dryRun) {
    createOrUpdateScripts(resource, Optional.ofNullable(dryRun).orElse(false));
    return super.update(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sgcluster", description = """
      Delete a sgcluster.

      ### RBAC permissions required

      * sgclusters delete
      """)
  @Override
  public void delete(ClusterDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  ClusterDto setInfo(ClusterDto resource) {
    if (resource.getMetadata() == null) {
      return resource;
    }
    final String namespace = resource.getMetadata().getNamespace();
    final String clusterName = resource.getMetadata().getName();
    final ClusterInfo info = new ClusterInfo();

    var foundCluster = clusterFinder.findByNameAndNamespace(clusterName, namespace);
    foundCluster
        .flatMap(cluster -> serviceFinder.findByNameAndNamespace(
            PatroniUtil.readWriteName(cluster), namespace))
        .ifPresent(service -> info.setPrimaryDns(StackGresUtil.getServiceDnsName(service)));
    serviceFinder.findByNameAndNamespace(PatroniUtil.readOnlyName(clusterName), namespace)
        .ifPresent(service -> info.setReplicasDns(StackGresUtil.getServiceDnsName(service)));

    info.setSuperuserUsername(SUPERUSER_USERNAME);
    info.setSuperuserSecretName(clusterName);
    info.setSuperuserUsernameKey(SUPERUSER_USERNAME_KEY);
    info.setSuperuserPasswordKey(SUPERUSER_PASSWORD_KEY);

    resource.setInfo(info);
    return resource;
  }

  ClusterDto setScripts(ClusterDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    Seq.of(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .forEach(managedScriptEntry -> {
          var script = scriptFinder
              .findByNameAndNamespace(managedScriptEntry.getSgScript(), namespace);
          managedScriptEntry.setScriptSpec(script
              .map(s -> scriptTransformer.toResource(s, List.of()))
              .map(ScriptDto::getSpec)
              .orElse(null));
        });
    return resource;
  }

  ClusterDto setConfigMaps(ClusterDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream))
        .flatMap(managedScriptEntry -> Seq.seq(
            Optional.ofNullable(managedScriptEntry.getScriptSpec())
                .map(ScriptSpec::getScripts)
                .stream()
                .flatMap(List::stream))
            .zipWithIndex()
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null
            && t.v2.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> extractConfigMapInfo(t.v1, t.v2, t.v3.intValue()))
        .filter(t -> t.v2.v3 != null)
        .grouped(t -> t.v2.v3.getName())
        .flatMap(t -> {
          var configMaps = configMapFinder
              .findByNameAndNamespace(t.v1, namespace)
              .map(ConfigMap::getData);
          return configMaps
              .map(s -> t.v2.map(tt -> Tuple.tuple(
                  s.get(tt.v2.v3.getKey()), tt.v2.v2)))
              .orElse(Seq.empty());
        })
        .forEach(t -> t.v2.accept(t.v1));
    return resource;
  }

  private void createOrUpdateScripts(ClusterDto resource, boolean dryRun) {
    var scriptsToCreate = getScriptsToCreate(resource)
        .stream()
        .filter(t -> isNotDefaultScript(t.v2))
        .map(t -> t.concat(
            scriptFinder.findByNameAndNamespace(
                t.v2.getMetadata().getName(),
                t.v2.getMetadata().getNamespace())))
        .toList();
    if (Seq.seq(scriptsToCreate)
        .grouped(t -> t.v2.getMetadata().getName())
        .anyMatch(t -> t.v2.count() > 1)) {
      throw new IllegalArgumentException(
          "script entries can not reference the same script more than once. Repeated SGScripts are: "
              + Seq.seq(scriptsToCreate)
              .grouped(t -> t.v2.getMetadata().getName())
              .map(t -> t.map2(Stream::toList))
              .filter(t -> t.v2.size() > 1)
              .map(Tuple2::v1)
              .toString(", "));
    }
    var secretsToCreate = Seq.seq(getSecretsToCreate(resource))
        .grouped(secret -> secret.getMetadata().getName())
        .flatMap(t -> t.v2.reduce(
            Optional.<Secret>empty(),
            (merged, secret) -> merged
            .or(() -> Optional.of(secret))
            .map(mergedSecret -> {
              mergedSecret.getData().putAll(secret.getData());
              return mergedSecret;
            }),
            (u, v) -> v)
            .stream())
        .map(secret -> Tuple.tuple(secret,
            secretFinder.findByNameAndNamespace(
                secret.getMetadata().getName(),
                secret.getMetadata().getNamespace())))
        .toList();
    var configMapsToCreate = Seq.seq(getConfigMapsToCreate(resource))
        .grouped(configMap -> configMap.getMetadata().getName())
        .flatMap(t -> t.v2.reduce(
            Optional.<ConfigMap>empty(),
            (merged, configMap) -> merged
            .or(() -> Optional.of(configMap))
            .map(mergedConfigMap -> {
              mergedConfigMap.getData().putAll(configMap.getData());
              return mergedConfigMap;
            }),
            (u, v) -> v)
            .stream())
        .map(configMap -> Tuple.tuple(configMap,
            configMapFinder.findByNameAndNamespace(
                configMap.getMetadata().getName(),
                configMap.getMetadata().getNamespace())))
        .toList();

    configMapsToCreate.stream()
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(configMap -> configMapWriter.create(configMap, dryRun));
    configMapsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(configMap -> configMapWriter.update(configMap, dryRun));
    secretsToCreate.stream()
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(secret -> secretWriter.create(secret, dryRun));
    secretsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(secret -> secretWriter.update(secret, dryRun));
    scriptsToCreate.stream()
        .filter(t -> t.v3.isEmpty())
        .forEach(t -> addFieldPrefixOnScriptValidationError(
            t.v1, t.v2, script -> scriptScheduler.create(script, dryRun)));
    scriptsToCreate.stream()
        .filter(t -> t.v3.isPresent())
        .forEach(t -> addFieldPrefixOnScriptValidationError(
            t.v1, t.v2, script -> scriptScheduler.update(script, dryRun)));
  }

  private boolean isNotDefaultScript(StackGresScript script) {
    return !script.getMetadata().getName().endsWith(ManagedSqlUtil.DEFAULT_SCRIPT_NAME_SUFFIX);
  }

  private void addFieldPrefixOnScriptValidationError(
      Integer sgScriptIndex,
      StackGresScript script,
      Consumer<StackGresScript> consumer) {
    try {
      consumer.accept(script);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422
          && ex.getStatus() != null
          && ex.getStatus().getDetails() != null
          && ex.getStatus().getDetails().getName() != null
          && ex.getStatus().getDetails().getName().startsWith("spec.")) {
        final String fieldPrefix = "spec.managedSql.scripts[" + sgScriptIndex + "].scriptSpec.";
        ex.getStatus().getDetails().setName(
            fieldPrefix + ex.getStatus().getDetails().getName().substring("spec.".length()));
        Optional.ofNullable(ex.getStatus().getDetails().getCauses())
            .orElse(List.of())
            .stream()
            .filter(cause -> cause.getField().startsWith("spec."))
            .forEach(cause -> cause.setField(
                fieldPrefix + cause.getField().substring("spec.".length())));
      }
      throw ex;
    }
  }

  private List<Tuple2<Integer, StackGresScript>> getScriptsToCreate(ClusterDto resource) {
    return Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream))
        .zipWithIndex()
        .filter(t -> t.v1.getScriptSpec() != null)
        .map(t -> {
          StackGresScript script = new StackGresScript();
          script.setMetadata(new ObjectMeta());
          if (t.v1.getSgScript() == null) {
            t.v1.setSgScript(scriptResourceName(
                resource, t.v2.intValue()));
          }
          script.getMetadata().setName(t.v1.getSgScript());
          script.getMetadata().setNamespace(resource.getMetadata().getNamespace());
          script.setSpec(new StackGresScriptSpec());
          script.getSpec().setContinueOnError(
              t.v1.getScriptSpec().isContinueOnError());
          if (t.v1.getScriptSpec().getScripts() != null) {
            script.getSpec().setScripts(t.v1.getScriptSpec().getScripts().stream()
                .map(scriptTransformer::getCustomResourceScriptEntry)
                .toList());
          }
          return Tuple.tuple(t.v2.intValue(), script);
        })
        .toList();
  }

  private List<ConfigMap> getConfigMapsToCreate(ClusterDto resource) {
    return Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream))
        .flatMap(managedScriptEntry -> Seq.seq(
            Optional.ofNullable(managedScriptEntry.getScriptSpec())
                .map(ScriptSpec::getScripts)
                .stream()
                .flatMap(List::stream))
            .zipWithIndex()
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null)
        .filter(t -> t.v2.getScriptFrom().getConfigMapScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v2.getScriptFrom();
          final String configMapScript = clusterScriptFrom.getConfigMapScript();
          if (clusterScriptFrom.getConfigMapKeyRef() == null) {
            String configMapName = scriptEntryResourceName(t.v1, t.v3.intValue());
            ConfigMapKeySelector configMapKeyRef = new ConfigMapKeySelector();
            configMapKeyRef.setName(configMapName);
            configMapKeyRef.setKey(DEFAULT_SCRIPT_KEY);
            clusterScriptFrom.setConfigMapKeyRef(configMapKeyRef);
          }
          return new ConfigMapBuilder()
              .withNewMetadata()
              .withName(clusterScriptFrom.getConfigMapKeyRef().getName())
              .withNamespace(resource.getMetadata().getNamespace())
              .endMetadata()
              .withData(Map.of(clusterScriptFrom.getConfigMapKeyRef().getKey(),
                  configMapScript))
              .build();
        })
        .toList();
  }

  private List<Secret> getSecretsToCreate(ClusterDto resource) {
    return Seq.seq(Optional
        .ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream))
        .flatMap(managedScriptEntry -> Seq.seq(
            Optional.ofNullable(managedScriptEntry.getScriptSpec())
                .map(ScriptSpec::getScripts)
                .stream()
                .flatMap(List::stream))
            .zipWithIndex()
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null)
        .filter(t -> t.v2.getScriptFrom().getSecretScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v2.getScriptFrom();
          final String secretScript = ResourceUtil
              .encodeSecret(clusterScriptFrom.getSecretScript());
          if (clusterScriptFrom.getSecretKeyRef() == null) {
            String secretName = scriptEntryResourceName(t.v1, t.v3.intValue());
            SecretKeySelector secretKeyRef = new SecretKeySelector();
            secretKeyRef.setName(secretName);
            secretKeyRef.setKey(DEFAULT_SCRIPT_KEY);
            clusterScriptFrom.setSecretKeyRef(secretKeyRef);
          }
          return new SecretBuilder()
              .withNewMetadata()
              .withName(clusterScriptFrom.getSecretKeyRef().getName())
              .withNamespace(resource.getMetadata().getNamespace())
              .endMetadata()
              .withData(Map.of(clusterScriptFrom.getSecretKeyRef().getKey(),
                  secretScript))
              .build();
        })
        .toList();
  }

  private
      Tuple2<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>>
      extractConfigMapInfo(
          ClusterManagedScriptEntry managedScriptEntry,
          ScriptEntry scriptEntry,
          int index) {
    return Tuple
        .<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>>tuple(
            scriptEntryResourceName(managedScriptEntry, index),
            Tuple
                .<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>tuple(
                    scriptEntry.getScriptFrom().getConfigMapScript(),
                    scriptEntry.getScriptFrom()::setConfigMapScript,
                    scriptEntry.getScriptFrom().getConfigMapKeyRef(),
                    scriptEntry.getScriptFrom()::setConfigMapKeyRef));
  }

  private String scriptResourceName(ClusterDto cluster,
      int index) {
    return cluster.getMetadata().getName() + "-managed-sql-" + index;
  }

  private String scriptEntryResourceName(ClusterManagedScriptEntry managedScriptEntry, int index) {
    return ScriptResource.scriptEntryResourceName(managedScriptEntry.getSgScript(), index);
  }

  @Override
  protected void updateSpec(StackGresCluster resourceToUpdate, StackGresCluster resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
