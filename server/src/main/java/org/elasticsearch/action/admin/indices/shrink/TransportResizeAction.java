/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.admin.indices.shrink;

import org.apache.lucene.index.IndexWriter;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexClusterStateUpdateRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.stats.IndexShardStats;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetadataCreateIndexService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.shard.DocsStats;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Main class to initiate resizing (shrink / split) an index into a new index
 */
public class TransportResizeAction extends TransportMasterNodeAction<ResizeRequest, ResizeResponse> {
    private final MetadataCreateIndexService createIndexService;
    private final Client client;

    @Inject
    public TransportResizeAction(TransportService transportService, ClusterService clusterService,
                                 ThreadPool threadPool, MetadataCreateIndexService createIndexService,
                                 ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver, Client client) {
        this(ResizeAction.NAME, transportService, clusterService, threadPool, createIndexService, actionFilters,
            indexNameExpressionResolver, client);
    }

    protected TransportResizeAction(String actionName, TransportService transportService, ClusterService clusterService,
                                 ThreadPool threadPool, MetadataCreateIndexService createIndexService,
                                 ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver, Client client) {
        super(actionName, transportService, clusterService, threadPool, actionFilters, ResizeRequest::new, indexNameExpressionResolver,
                ResizeResponse::new, ThreadPool.Names.SAME);
        this.createIndexService = createIndexService;
        this.client = client;
    }

    @Override
    protected ClusterBlockException checkBlock(ResizeRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.METADATA_WRITE, request.getTargetIndexRequest().index());
    }

    @Override
    protected void masterOperation(final ResizeRequest resizeRequest, final ClusterState state,
                                   final ActionListener<ResizeResponse> listener) {

        // there is no need to fetch docs stats for split but we keep it simple and do it anyway for simplicity of the code
        final String sourceIndex = indexNameExpressionResolver.resolveDateMathExpression(resizeRequest.getSourceIndex());
        final String targetIndex = indexNameExpressionResolver.resolveDateMathExpression(resizeRequest.getTargetIndexRequest().index());

        final IndexMetadata sourceMetadata = state.metadata().index(sourceIndex);
        if (sourceMetadata == null) {
            listener.onFailure(new IndexNotFoundException(sourceIndex));
            return;
        }

        // TODO: only fetch indices stats for shrink type resize requests
        client.admin().indices().prepareStats(sourceIndex).clear().setDocs(true).execute(
            ActionListener.delegateFailure(listener, (delegatedListener, indicesStatsResponse) -> {
                final CreateIndexClusterStateUpdateRequest updateRequest;
                try {
                    updateRequest = prepareCreateIndexRequest(resizeRequest, sourceMetadata, i -> {
                        IndexShardStats shard = indicesStatsResponse.getIndex(sourceIndex).getIndexShards().get(i);
                        return shard == null ? null : shard.getPrimary().getDocs();
                    }, targetIndex);
                } catch (Exception e) {
                    delegatedListener.onFailure(e);
                    return;
                }
                createIndexService.createIndex(
                    updateRequest, delegatedListener.map(
                        response -> new ResizeResponse(response.isAcknowledged(), response.isShardsAcknowledged(), updateRequest.index()))
                );
            }));
    }

    // static for unittesting this method
    static CreateIndexClusterStateUpdateRequest prepareCreateIndexRequest(final ResizeRequest resizeRequest,
                                                                          final IndexMetadata sourceMetadata,
                                                                          final IntFunction<DocsStats> perShardDocStats,
                                                                          final String targetIndexName) {
        final CreateIndexRequest targetIndex = resizeRequest.getTargetIndexRequest();
        final Settings.Builder targetIndexSettingsBuilder = Settings.builder().put(targetIndex.settings())
            .normalizePrefix(IndexMetadata.INDEX_SETTING_PREFIX);
        targetIndexSettingsBuilder.remove(IndexMetadata.SETTING_HISTORY_UUID);
        final Settings targetIndexSettings = targetIndexSettingsBuilder.build();
        final int numShards;
        if (IndexMetadata.INDEX_NUMBER_OF_SHARDS_SETTING.exists(targetIndexSettings)) {
            numShards = IndexMetadata.INDEX_NUMBER_OF_SHARDS_SETTING.get(targetIndexSettings);
        } else {
            assert resizeRequest.getResizeType() != ResizeType.SPLIT : "split must specify the number of shards explicitly";
            if (resizeRequest.getResizeType() == ResizeType.SHRINK) {
                numShards = 1;
            } else {
                assert resizeRequest.getResizeType() == ResizeType.CLONE;
                numShards = sourceMetadata.getNumberOfShards();
            }
        }

        for (int i = 0; i < numShards; i++) {
            if (resizeRequest.getResizeType() == ResizeType.SHRINK) {
                Set<ShardId> shardIds = IndexMetadata.selectShrinkShards(i, sourceMetadata, numShards);
                long count = 0;
                for (ShardId id : shardIds) {
                    DocsStats docsStats = perShardDocStats.apply(id.id());
                    if (docsStats != null) {
                        count += docsStats.getCount();
                    }
                    if (count > IndexWriter.MAX_DOCS) {
                        throw new IllegalStateException("Can't merge index with more than [" + IndexWriter.MAX_DOCS
                            + "] docs - too many documents in shards " + shardIds);
                    }
                }
            } else if (resizeRequest.getResizeType() == ResizeType.SPLIT) {
                Objects.requireNonNull(IndexMetadata.selectSplitShard(i, sourceMetadata, numShards));
                // we just execute this to ensure we get the right exceptions if the number of shards is wrong or less then etc.
            } else {
                Objects.requireNonNull(IndexMetadata.selectCloneShard(i, sourceMetadata, numShards));
                // we just execute this to ensure we get the right exceptions if the number of shards is wrong etc.
            }
        }

        if (IndexMetadata.INDEX_ROUTING_PARTITION_SIZE_SETTING.exists(targetIndexSettings)) {
            throw new IllegalArgumentException("cannot provide a routing partition size value when resizing an index");
        }
        if (IndexMetadata.INDEX_NUMBER_OF_ROUTING_SHARDS_SETTING.exists(targetIndexSettings)) {
            // if we have a source index with 1 shards it's legal to set this
            final boolean splitFromSingleShards =
                    resizeRequest.getResizeType() == ResizeType.SPLIT && sourceMetadata.getNumberOfShards() == 1;
            if (splitFromSingleShards == false) {
                throw new IllegalArgumentException("cannot provide index.number_of_routing_shards on resize");
            }
        }
        if (IndexSettings.INDEX_SOFT_DELETES_SETTING.get(sourceMetadata.getSettings()) &&
            IndexSettings.INDEX_SOFT_DELETES_SETTING.exists(targetIndexSettings) &&
            IndexSettings.INDEX_SOFT_DELETES_SETTING.get(targetIndexSettings) == false) {
            throw new IllegalArgumentException("Can't disable [index.soft_deletes.enabled] setting on resize");
        }
        String cause = resizeRequest.getResizeType().name().toLowerCase(Locale.ROOT) + "_index";
        targetIndex.cause(cause);
        Settings.Builder settingsBuilder = Settings.builder().put(targetIndexSettings);
        settingsBuilder.put("index.number_of_shards", numShards);
        targetIndex.settings(settingsBuilder);

        return new CreateIndexClusterStateUpdateRequest(cause, targetIndex.index(), targetIndexName)
                // mappings are updated on the node when creating in the shards, this prevents race-conditions since all mapping must be
                // applied once we took the snapshot and if somebody messes things up and switches the index read/write and adds docs we
                // miss the mappings for everything is corrupted and hard to debug
                .ackTimeout(targetIndex.timeout())
                .masterNodeTimeout(targetIndex.masterNodeTimeout())
                .settings(targetIndex.settings())
                .aliases(targetIndex.aliases())
                .waitForActiveShards(targetIndex.waitForActiveShards())
                .recoverFrom(sourceMetadata.getIndex())
                .resizeType(resizeRequest.getResizeType())
                .copySettings(resizeRequest.getCopySettings() == null ? false : resizeRequest.getCopySettings());
    }

    @Override
    protected String getMasterActionName(DiscoveryNode node) {
        if (node.getVersion().onOrAfter(ResizeAction.COMPATIBILITY_VERSION)){
            return super.getMasterActionName(node);
        } else {
            // this is for BWC - when we send this to version that doesn't have ResizeAction.NAME registered
            // we have to send to shrink instead.
            return ShrinkAction.NAME;
        }
    }
}
