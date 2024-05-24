/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgscript.ScriptEventReason;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ScriptReconciliator
    extends AbstractReconciliator<StackGresScript, StackGresScriptReview> {

  @Dependent
  public static class Parameters {
    @Inject MutationPipeline<StackGresScript, StackGresScriptReview> mutatingPipeline;
    @Inject ValidationPipeline<StackGresScriptReview> validatingPipeline;
    @Inject CustomResourceScanner<StackGresScript> scanner;
    @Inject CustomResourceFinder<StackGresScript> finder;
    @Inject AbstractConciliator<StackGresScript> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresScript> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject ObjectMapper objectMapper;
    @Inject EventEmitter<StackGresScript> eventController;
    @Inject CustomResourceScheduler<StackGresScript> scriptScheduler;
    @Inject ScriptStatusManager statusManager;
    @Inject OperatorLockHolder operatorLockReconciliator;
  }

  private final EventEmitter<StackGresScript> eventController;
  private final CustomResourceScheduler<StackGresScript> scriptScheduler;
  private final ScriptStatusManager statusManager;

  @Inject
  public ScriptReconciliator(Parameters parameters) {
    super(
        parameters.mutatingPipeline, parameters.validatingPipeline,
        parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.objectMapper,
        parameters.operatorLockReconciliator,
        StackGresScript.KIND);
    this.eventController = parameters.eventController;
    this.scriptScheduler = parameters.scriptScheduler;
    this.statusManager = parameters.statusManager;
  }

  @Override
  protected StackGresScriptReview createReview() {
    return new StackGresScriptReview();
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresScript configKey, boolean load) {
    super.reconciliationCycle(configKey, load);
  }

  @Override
  protected void onPreReconciliation(StackGresScript config) {
    scriptScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onPostReconciliation(StackGresScript config) {
    // Nothing to do
  }

  @Override
  protected void onConfigCreated(StackGresScript script, ReconciliationResult result) {
    // Nothing to do
  }

  @Override
  protected void onConfigUpdated(StackGresScript script, ReconciliationResult result) {
    // Nothing to do
  }

  @Override
  protected void onError(Exception ex, StackGresScript script) {
    String message = MessageFormatter.arrayFormat(
        "Script reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ScriptEventReason.SCRIPT_CONFIG_ERROR,
        message + ": " + ex.getMessage(), script);
  }

}
