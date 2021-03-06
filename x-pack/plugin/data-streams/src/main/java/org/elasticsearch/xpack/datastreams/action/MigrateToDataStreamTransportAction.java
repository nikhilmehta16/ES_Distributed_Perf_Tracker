/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.datastreams.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.AcknowledgedTransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetadataMigrateToDataStreamService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.MigrateToDataStreamAction;

public class MigrateToDataStreamTransportAction extends AcknowledgedTransportMasterNodeAction<MigrateToDataStreamAction.Request> {

    private final MetadataMigrateToDataStreamService metadataMigrateToDataStreamService;

    @Inject
    public MigrateToDataStreamTransportAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        MetadataMigrateToDataStreamService metadataMigrateToDataStreamService
    ) {
        super(
            MigrateToDataStreamAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            MigrateToDataStreamAction.Request::new,
            indexNameExpressionResolver,
            ThreadPool.Names.SAME
        );
        this.metadataMigrateToDataStreamService = metadataMigrateToDataStreamService;
    }

    @Override
    protected void masterOperation(
        MigrateToDataStreamAction.Request request,
        ClusterState state,
        ActionListener<AcknowledgedResponse> listener
    ) throws Exception {
        MetadataMigrateToDataStreamService.MigrateToDataStreamClusterStateUpdateRequest updateRequest =
            new MetadataMigrateToDataStreamService.MigrateToDataStreamClusterStateUpdateRequest(
                request.getAliasName(),
                request.masterNodeTimeout(),
                request.timeout()
            );
        metadataMigrateToDataStreamService.migrateToDataStream(updateRequest, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(MigrateToDataStreamAction.Request request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
