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
import org.elasticsearch.cluster.metadata.MetadataCreateDataStreamService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.action.CreateDataStreamAction;

public class CreateDataStreamTransportAction extends AcknowledgedTransportMasterNodeAction<CreateDataStreamAction.Request> {

    private final MetadataCreateDataStreamService metadataCreateDataStreamService;

    @Inject
    public CreateDataStreamTransportAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        MetadataCreateDataStreamService metadataCreateDataStreamService
    ) {
        super(
            CreateDataStreamAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            CreateDataStreamAction.Request::new,
            indexNameExpressionResolver,
            ThreadPool.Names.SAME
        );
        this.metadataCreateDataStreamService = metadataCreateDataStreamService;
    }

    @Override
    protected void masterOperation(
        CreateDataStreamAction.Request request,
        ClusterState state,
        ActionListener<AcknowledgedResponse> listener
    ) throws Exception {
        MetadataCreateDataStreamService.CreateDataStreamClusterStateUpdateRequest updateRequest =
            new MetadataCreateDataStreamService.CreateDataStreamClusterStateUpdateRequest(
                request.getName(),
                request.masterNodeTimeout(),
                request.timeout()
            );
        metadataCreateDataStreamService.createDataStream(updateRequest, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(CreateDataStreamAction.Request request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
