/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.admin.cluster.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportClusterUpdateSettingsAction extends
    TransportMasterNodeAction<ClusterUpdateSettingsRequest, ClusterUpdateSettingsResponse> {

    private static final Logger logger = LogManager.getLogger(TransportClusterUpdateSettingsAction.class);

    private final AllocationService allocationService;

    private final ClusterSettings clusterSettings;

    @Inject
    public TransportClusterUpdateSettingsAction(TransportService transportService, ClusterService clusterService,
                                                ThreadPool threadPool, AllocationService allocationService, ActionFilters actionFilters,
                                                IndexNameExpressionResolver indexNameExpressionResolver, ClusterSettings clusterSettings) {
        super(ClusterUpdateSettingsAction.NAME, false, transportService, clusterService, threadPool, actionFilters,
            ClusterUpdateSettingsRequest::new, indexNameExpressionResolver, ClusterUpdateSettingsResponse::new, ThreadPool.Names.SAME);
        this.allocationService = allocationService;
        this.clusterSettings = clusterSettings;
    }

    @Override
    protected ClusterBlockException checkBlock(ClusterUpdateSettingsRequest request, ClusterState state) {
        // allow for dedicated changes to the metadata blocks, so we don't block those to allow to "re-enable" it
        if (request.transientSettings().size() + request.persistentSettings().size() == 1) {
            // only one setting
            if (Metadata.SETTING_READ_ONLY_SETTING.exists(request.persistentSettings())
                || Metadata.SETTING_READ_ONLY_SETTING.exists(request.transientSettings())
                || Metadata.SETTING_READ_ONLY_ALLOW_DELETE_SETTING.exists(request.transientSettings())
                || Metadata.SETTING_READ_ONLY_ALLOW_DELETE_SETTING.exists(request.persistentSettings())) {
                // one of the settings above as the only setting in the request means - resetting the block!
                return null;
            }
        }
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected void masterOperation(final ClusterUpdateSettingsRequest request, final ClusterState state,
                                   final ActionListener<ClusterUpdateSettingsResponse> listener) {
        final SettingsUpdater updater = new SettingsUpdater(clusterSettings);
        clusterService.submitStateUpdateTask("cluster_update_settings",
                new AckedClusterStateUpdateTask(Priority.IMMEDIATE, request, listener) {

            private volatile boolean changed = false;

            @Override
            protected ClusterUpdateSettingsResponse newResponse(boolean acknowledged) {
                return new ClusterUpdateSettingsResponse(acknowledged, updater.getTransientUpdates(), updater.getPersistentUpdate());
            }

            @Override
            public void onAllNodesAcked(@Nullable Exception e) {
                if (changed) {
                    reroute(true);
                } else {
                    super.onAllNodesAcked(e);
                }
            }

            @Override
            public void onAckTimeout() {
                if (changed) {
                    reroute(false);
                } else {
                    super.onAckTimeout();
                }
            }

            private void reroute(final boolean updateSettingsAcked) {
                // We're about to send a second update task, so we need to check if we're still the elected master
                // For example the minimum_master_node could have been breached and we're no longer elected master,
                // so we should *not* execute the reroute.
                if (!clusterService.state().nodes().isLocalNodeElectedMaster()) {
                    logger.debug("Skipping reroute after cluster update settings, because node is no longer master");
                    listener.onResponse(new ClusterUpdateSettingsResponse(updateSettingsAcked, updater.getTransientUpdates(),
                        updater.getPersistentUpdate()));
                    return;
                }

                // The reason the reroute needs to be send as separate update task, is that all the *cluster* settings are encapsulate
                // in the components (e.g. FilterAllocationDecider), so the changes made by the first call aren't visible
                // to the components until the ClusterStateListener instances have been invoked, but are visible after
                // the first update task has been completed.
                clusterService.submitStateUpdateTask("reroute_after_cluster_update_settings",
                        new AckedClusterStateUpdateTask(Priority.URGENT, request, listener) {

                    @Override
                    public boolean mustAck(DiscoveryNode discoveryNode) {
                        //we wait for the reroute ack only if the update settings was acknowledged
                        return updateSettingsAcked;
                    }

                    @Override
                    // we return when the cluster reroute is acked or it times out but the acknowledged flag depends on whether the
                    // update settings was acknowledged
                    protected ClusterUpdateSettingsResponse newResponse(boolean acknowledged) {
                        return new ClusterUpdateSettingsResponse(updateSettingsAcked && acknowledged, updater.getTransientUpdates(),
                            updater.getPersistentUpdate());
                    }

                    @Override
                    public void onNoLongerMaster(String source) {
                        logger.debug("failed to preform reroute after cluster settings were updated - current node is no longer a master");
                        listener.onResponse(new ClusterUpdateSettingsResponse(updateSettingsAcked, updater.getTransientUpdates(),
                            updater.getPersistentUpdate()));
                    }

                    @Override
                    public void onFailure(String source, Exception e) {
                        //if the reroute fails we only log
                        logger.debug(() -> new ParameterizedMessage("failed to perform [{}]", source), e);
                        listener.onFailure(new ElasticsearchException("reroute after update settings failed", e));
                    }

                    @Override
                    public ClusterState execute(final ClusterState currentState) {
                        // now, reroute in case things that require it changed (e.g. number of replicas)
                        return allocationService.reroute(currentState, "reroute after cluster update settings");
                    }
                });
            }

            @Override
            public void onFailure(String source, Exception e) {
                logger.debug(() -> new ParameterizedMessage("failed to perform [{}]", source), e);
                super.onFailure(source, e);
            }

            @Override
            public ClusterState execute(final ClusterState currentState) {
                final ClusterState clusterState =
                        updater.updateSettings(
                                currentState,
                                clusterSettings.upgradeSettings(request.transientSettings()),
                                clusterSettings.upgradeSettings(request.persistentSettings()),
                                logger);
                changed = clusterState != currentState;
                return clusterState;
            }
        });
    }

}
