/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ilm;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateObserver;
import org.elasticsearch.cluster.metadata.IndexMetadata;

final class OpenFollowerIndexStep extends AsyncActionStep {

    static final String NAME = "open-follower-index";

    OpenFollowerIndexStep(StepKey key, StepKey nextStepKey, Client client) {
        super(key, nextStepKey, client);
    }

    @Override
    public void performAction(IndexMetadata indexMetadata, ClusterState currentClusterState,
                              ClusterStateObserver observer, Listener listener) {
        if (indexMetadata.getState() == IndexMetadata.State.CLOSE) {
            OpenIndexRequest request = new OpenIndexRequest(indexMetadata.getIndex().getName())
                .masterNodeTimeout(getMasterTimeout(currentClusterState));
            getClient().admin().indices().open(request, ActionListener.wrap(
                r -> {
                    assert r.isAcknowledged() : "open index response is not acknowledged";
                    listener.onResponse(true);
                },
                listener::onFailure
            ));
        } else {
            listener.onResponse(true);
        }
    }
}
