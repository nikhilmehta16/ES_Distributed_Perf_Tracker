/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.spr.utils.results;

import org.elasticsearch.common.xcontent.ToXContentFragment;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Iterator;

public class PhasePerfResult implements  Iterable<ShardPerfResult>, ToXContentFragment {
    final String name;
    final ShardPerfResult[]  shardPerfResults;

    public PhasePerfResult(ShardPerfResult[] shardPerfResults, String name) {
        this.name = name;
        this.shardPerfResults = shardPerfResults;
    }
    public final class Fields {
//        public final String PHASE = name;
        public static final String MAX_TIME = "max_time";
    }

    @Override
    public Iterator<ShardPerfResult> iterator() {
        return null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(this.name);
//        builder.startArray();
        builder.startObject();
        for(ShardPerfResult shardPerfResult : shardPerfResults){
            shardPerfResult.toXContent(builder,params);
        }
        builder.endObject();
//        builder.endArray();
        return builder;
    }


}
