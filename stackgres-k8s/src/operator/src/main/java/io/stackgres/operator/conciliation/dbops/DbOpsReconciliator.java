/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgdbops.DbOpsEventReason;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DbOpsReconciliator
    extends AbstractReconciliator<StackGresDbOps> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresDbOps> scanner;
    @Inject CustomResourceFinder<StackGresDbOps> finder;
    @Inject AbstractConciliator<StackGresDbOps> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresDbOps> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject EventEmitter<StackGresDbOps> eventController;
    @Inject DbOpsStatusManager statusManager;
    @Inject CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
    @Inject Metrics metrics;
  }

  private final EventEmitter<StackGresDbOps> eventController;
  private final PatchResumer<StackGresDbOps> patchResumer;
  private final DbOpsStatusManager statusManager;
  private final CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  public DbOpsReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        parameters.metrics,
        StackGresDbOps.KIND);
    this.eventController = parameters.eventController;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
    this.statusManager = parameters.statusManager;
    this.dbOpsScheduler = parameters.dbOpsScheduler;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresDbOps configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected void onPreReconciliation(StackGresDbOps config) {
  }

  @Override
  protected void onPostReconciliation(StackGresDbOps config) {
    dbOpsScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onConfigCreated(StackGresDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(DbOpsEventReason.DBOPS_CREATED,
        "SGDbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " created: " + resourceChanged, dbOps);
  }

  @Override
  protected void onConfigUpdated(StackGresDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(DbOpsEventReason.DBOPS_UPDATED,
        "SGDbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " updated: " + resourceChanged, dbOps);
  }

  @Override
  protected void onError(Exception ex, StackGresDbOps dbOps) {
    String message = MessageFormatter.arrayFormat(
        "SGDbOps reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(DbOpsEventReason.DBOPS_CONFIG_ERROR,
        message + ": " + ex.getMessage(), dbOps);
  }

}
