/**
 * Copyright (c) Connexta
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ditto.replication.api.impl;

import static org.apache.commons.lang3.Validate.notNull;

import ddf.catalog.filter.FilterBuilder;
import ddf.security.Subject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.collections4.queue.UnmodifiableQueue;
import org.codice.ddf.security.common.Security;
import org.codice.ditto.replication.api.Direction;
import org.codice.ditto.replication.api.ReplicationException;
import org.codice.ditto.replication.api.ReplicationPersistentStore;
import org.codice.ditto.replication.api.ReplicationStatus;
import org.codice.ditto.replication.api.ReplicationStore;
import org.codice.ditto.replication.api.Replicator;
import org.codice.ditto.replication.api.ReplicatorConfig;
import org.codice.ditto.replication.api.ReplicatorHistory;
import org.codice.ditto.replication.api.ReplicatorStoreFactory;
import org.codice.ditto.replication.api.Status;
import org.codice.ditto.replication.api.SyncRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicatorImpl implements Replicator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReplicatorImpl.class);

  private final ReplicatorStoreFactory replicatorStoreFactory;

  private final ReplicationStore localStore;

  private final ReplicatorHistory history;

  private final ReplicationPersistentStore persistentStore;

  private final ExecutorService executor;

  private final FilterBuilder builder;

  /** Does not contain duplicates */
  private final BlockingQueue<SyncRequest> pendingSyncRequests = new LinkedBlockingQueue<>();

  /** Does not contain duplicates */
  private final BlockingQueue<SyncRequest> activeSyncRequests = new LinkedBlockingQueue<>();

  public ReplicatorImpl(
      ReplicatorStoreFactory replicatorStoreFactory,
      ReplicationStore localStore,
      ReplicatorHistory history,
      ReplicationPersistentStore persistentStore,
      ExecutorService executor,
      FilterBuilder builder) {
    this.replicatorStoreFactory = notNull(replicatorStoreFactory);
    this.localStore = notNull(localStore);
    this.history = notNull(history);
    this.persistentStore = notNull(persistentStore);
    this.executor = notNull(executor);
    this.builder = notNull(builder);
  }

  public void init() {
    LOGGER.trace("Configuring the single-thread scheduler to execute sync requests from the queue");

    executor.execute(
        () -> {
          while (true) {
            try {
              // wait for something to be available in the queue and take it
              final SyncRequest syncRequest = pendingSyncRequests.take();
              LOGGER.trace(
                  "Just took sync request {} from the pendingSyncRequests queue. There are {} pending sync requests now in the queue.",
                  syncRequest,
                  pendingSyncRequests.size());

              if (activeSyncRequests.contains(syncRequest)) { // TODO improve comparing syncRequests
                LOGGER.debug(
                    "activeSyncRequests already contains sync request {}. Not executing again.",
                    syncRequest);
              } else {
                LOGGER.trace("Marking sync request {} as active", syncRequest);
                activeSyncRequests.put(syncRequest);
                executeSyncRequest(syncRequest);
              }
            } catch (InterruptedException e) {
              LOGGER.trace("InterruptedException in executor. This is expected during shutdown.");
              Thread.currentThread().interrupt();
            }
          }
        });

    LOGGER.trace(
        "Successfully configured the single-thread scheduler to execute sync requests from the queue");
  }

  private void executeSyncRequest(final SyncRequest syncRequest) {
    final Security security = Security.getInstance();
    final Subject systemSubject = security.runAsAdmin(security::getSystemSubject);
    systemSubject.execute(
        () -> {
          LOGGER.trace("Executing sync request {} with subject", syncRequest);

          ReplicationStatus status = syncRequest.getStatus();
          status.markStartTime();

          ReplicatorConfig config = syncRequest.getConfig();
          ReplicationStore store;

          try {
            store = getStoreForConfig(config);
          } catch (Exception e) {
            final Status connectionUnavailable = Status.CONNECTION_UNAVAILABLE;
            LOGGER.warn(
                "Error getting store for config {}. Setting status to {}",
                config.getName(),
                connectionUnavailable,
                e);
            status.setStatus(connectionUnavailable);
            completeActiveSyncRequest(syncRequest, status);
            return;
          }
          try (ReplicationStore remoteStore = store) {
            Status pullStatus = Status.SUCCESS;
            if (Direction.PULL.equals(config.getDirection())
                || Direction.BOTH.equals(config.getDirection())) {
              status.setStatus(Status.PULL_IN_PROGRESS);
              SyncResponse response =
                  SyncHelper.performSync(
                      remoteStore, localStore, config, persistentStore, history, builder);
              status.setPullCount(response.getItemsReplicated());
              status.setPullFailCount(response.getItemsFailed());
              status.setPullBytes(response.getBytesTransferred());
              pullStatus = response.getStatus();
              status.setStatus(pullStatus);
            }

            if (pullStatus.equals(Status.SUCCESS)
                && (Direction.PUSH.equals(config.getDirection())
                    || Direction.BOTH.equals(config.getDirection()))) {
              status.setStatus(Status.PUSH_IN_PROGRESS);
              SyncResponse response =
                  SyncHelper.performSync(
                      localStore, remoteStore, config, persistentStore, history, builder);
              status.setPushCount(response.getItemsReplicated());
              status.setPushFailCount(response.getItemsFailed());
              status.setPushBytes(response.getBytesTransferred());
              status.setStatus(response.getStatus());
            }

          } catch (Exception e) {
            final Status failureStatus = Status.FAILURE;
            LOGGER.warn(
                "Error getting store for config {}. Setting status to {}",
                config.getName(),
                failureStatus,
                e);
            status.setStatus(failureStatus);
          } finally {
            completeActiveSyncRequest(syncRequest, status);
          }
        });
  }

  private void completeActiveSyncRequest(SyncRequest syncRequest, ReplicationStatus status) {
    status.setDuration();
    LOGGER.trace("Removing sync request {} from the active queue", syncRequest);
    if (!activeSyncRequests.remove(syncRequest)) {
      LOGGER.debug("Failed to remove sync request {} from the active queue", syncRequest);
    }
    LOGGER.trace("Adding replication event to history: {}", status);
    history.addReplicationEvent(status);
    LOGGER.trace("Successfully added replication event to history: {}", status);
  }

  public void cleanUp() {
    final RetryPolicy retryPolicy =
        new RetryPolicy()
            .retryWhen(false)
            .withDelay(1, TimeUnit.SECONDS)
            .withMaxDuration(30, TimeUnit.SECONDS);

    Failsafe.with(retryPolicy)
        .onSuccess(
            isEmpty ->
                LOGGER.trace(
                    "Successfully waited for all pending or active sync requests to be completed"))
        .onRetry(
            isEmpty ->
                LOGGER.debug(
                    "There are currently {} pending and {} active sync requests. Waiting another second for all sync requests to be completed.",
                    pendingSyncRequests.size(),
                    activeSyncRequests.size()))
        .onFailure(
            isEmpty ->
                LOGGER.debug(
                    "There are currently {} pending and {} active sync requests, but the timeout was reached for waiting for all sync requests to be completed.",
                    pendingSyncRequests.size(),
                    activeSyncRequests.size()))
        .get(() -> pendingSyncRequests.isEmpty() && activeSyncRequests.isEmpty());

    LOGGER.trace(
        "Shutting down now the single-thread scheduler that executes sync requests from the queue");
    executor.shutdownNow();
    LOGGER.trace("Successfully shut down replicator thread pool and scheduler");
  }

  @Override
  public void submitSyncRequest(final SyncRequest syncRequest) throws InterruptedException {
    LOGGER.trace("Submitting sync request for name = {}", syncRequest.getConfig().getName());
    if (pendingSyncRequests.contains(syncRequest)) { // TODO improve comparing syncRequests
      LOGGER.debug(
          "The pendingSyncRequests already contains sync request {}. Not adding again.",
          syncRequest);
    } else {
      pendingSyncRequests.put(syncRequest);
    }
  }

  @Override
  public Queue<SyncRequest> getPendingSyncRequests() {
    return UnmodifiableQueue.unmodifiableQueue(pendingSyncRequests);
  }

  @Override
  public Set<SyncRequest> getActiveSyncRequests() {
    return Collections.unmodifiableSet(new HashSet<>(activeSyncRequests));
  }

  private ReplicationStore getStoreForConfig(ReplicatorConfig config) {
    ReplicationStore store;
    try {
      store = replicatorStoreFactory.createReplicatorStore(config.getUrl());
    } catch (Exception e) {
      throw new ReplicationException("Error connecting to remote system at " + config.getUrl(), e);
    }
    if (!store.isAvailable()) {
      throw new ReplicationException("System at " + config.getUrl() + " is currently unavailable");
    }
    return store;
  }
}
