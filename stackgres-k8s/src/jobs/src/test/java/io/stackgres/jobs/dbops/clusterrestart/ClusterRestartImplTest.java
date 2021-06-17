/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@QuarkusTest
class ClusterRestartImplTest {

  private static final String NAMESPACE = "test";
  private static final String CLUSTER_NAME = "test-cluster";
  private static final String IN_PLACE_METHOD = "InPlace";
  private static final String REDUCED_IMPACT_METHOD = "ReducedImpact";

  @Inject
  ClusterRestartImpl clusterRestart;

  @InjectMock
  PodRestartImpl podRestart;

  @InjectMock
  ClusterSwitchoverHandlerImpl switchoverHandler;

  @InjectMock
  ClusterInstanceManagerImpl instanceManager;

  @InjectMock
  ClusterWatcher clusterWatcher;

  @InjectMock
  PostgresRestart postgresRestart;

  Pod primary = new PodBuilder()
      .withNewMetadata()
      .addToAnnotations(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString())
      .withName(CLUSTER_NAME + "-1")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "master")
      .endMetadata()
      .build();

  Pod replica1 = new PodBuilder()
      .withNewMetadata()
      .addToAnnotations(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString())
      .withName(CLUSTER_NAME + "-2")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "replica")
      .endMetadata()
      .build();

  Pod replica2 = new PodBuilder()
      .withNewMetadata()
      .addToAnnotations(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString())
      .withName(CLUSTER_NAME + "-3")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "replica")
      .endMetadata()
      .build();

  Pod additionalPod = new PodBuilder()
      .withNewMetadata()
      .addToAnnotations(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString())
      .withName(CLUSTER_NAME + "-4")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "replica")
      .endMetadata()
      .build();

  StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(NAMESPACE);
    cluster.getSpec().setInstances(3);

    when(clusterWatcher.waitUntilIsReady(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(cluster));
  }

  @Test
  void givenACleanState_itShouldRestartAllPods() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it should restart the primary postgres");

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher, postgresRestart);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica1);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(6)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(3)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithARestartedPod_shouldNotRestartThatPod() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it shouldn't restart the primary postgres");

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(4)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(2)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAPodInPendingRestartWithOnlyPendingRestart_shouldOnlyRestartThatPod() {
    Pod pendingRestartReplica1 = new PodBuilder(replica1)
        .editMetadata()
        .addToAnnotations("status", "{\"pending_restart\":true}")
        .endMetadata()
        .build();

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRrestart(true)
        .podStatuses(ImmutableList.of())
        .statefulSet(Optional.empty())
        .primaryInstance(primary)
        .addInitialInstances(primary, pendingRestartReplica1, replica2)
        .addTotalInstances(primary, pendingRestartReplica1, replica2)
        .addRestartedInstances()
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should not perform a switchover");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(pendingRestartReplica1);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(2)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithPrimaryInPendingRestartWithOnlyPendingRestart_shouldOnlyRestartThatPod() {
    Pod pendingRestartPrimary = new PodBuilder(primary)
        .editMetadata()
        .addToAnnotations("status", "{\"pending_restart\":true}")
        .endMetadata()
        .build();

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRrestart(true)
        .podStatuses(ImmutableList.of())
        .statefulSet(Optional.empty())
        .primaryInstance(pendingRestartPrimary)
        .addInitialInstances(pendingRestartPrimary, replica1, replica2)
        .addTotalInstances(pendingRestartPrimary, replica1, replica2)
        .addRestartedInstances()
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(pendingRestartPrimary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(4)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAllReplicasRestarted_shouldRestartOnlyThePrimaryNode() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it shouldn't restart the primary postgres");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(3)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAllReplicasRestartedAndSwitchoverInitiated_shouldNotPerformSwitchover() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2)
        .isSwitchoverInitiated(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should not perform a switchover");
    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it shouldn't restart the primary postgres");


    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(2)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  private void checkFinalSgClusterOnInPlace() {
    verify(instanceManager, never()).increaseClusterInstances(any(), any());
    verify(instanceManager, never()).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenACleanStateWithReduceImpact_itShouldRestartAllPods() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .isSwitchoverInitiated(false)
        .build();

    when(instanceManager.increaseClusterInstances(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(additionalPod));

    when(instanceManager.decreaseClusterInstances(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();

    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should send an event for every pod created");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");
    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it should restart the primary postgres");

    final InOrder order = inOrder(podRestart, switchoverHandler, instanceManager, postgresRestart, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica1);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(7)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(3)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(1)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenAClusterWithARestartedPodAndReducedImpact_shouldNotRestartThatPod() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2, additionalPod)
        .addRestartedInstances(replica1, additionalPod)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    when(instanceManager.decreaseClusterInstances(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());


    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it shouldn't restart the primary postgres");


    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(4)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(2)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenAClusterWithAllReplicasRestartedAndSwitchoverInitiatedAndReducedImpact_shouldNotPerformSwitchover() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2, additionalPod)
        .addRestartedInstances(additionalPod, replica1, replica2)
        .isSwitchoverInitiated(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should not perform a switchover");
    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it shouldn't restart the primary postgres");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(2)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
 }

  @Test
  void givenAClusterWithAInstancedDecreasedAndReducedImpact_shouldNotDecreaseInstances() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRrestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2, primary)
        .isSwitchoverInitiated(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should not perform a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTART)
            .count(), "it shouldn't restart the primary postgres");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(1)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(0)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(0)).decreaseClusterInstances(any(), any());
  }

}