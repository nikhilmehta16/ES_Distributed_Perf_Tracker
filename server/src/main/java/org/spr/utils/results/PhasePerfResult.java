/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.spr.utils.results;

import org.elasticsearch.common.xcontent.ToXContentFragment;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchPhaseResult;
import org.spr.utils.MergedStat;

import java.io.IOException;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;



public class PhasePerfResult implements  Iterable<ShardPerfResult>, ToXContentFragment {
    final String name;
    final ShardPerfResult[]  shardPerfResults;
    final long maxExecutionDelay;
    final long maxExecutionTime;
    final MergedStat mergedStat;

    public PhasePerfResult(ShardPerfResult[] shardPerfResults,long maxExecutionDelay,
                           long maxExecutionTime, MergedStat mergedStat ,String phaseName) {
        this.name = phaseName;
        this.shardPerfResults = shardPerfResults;
        this.maxExecutionDelay = maxExecutionDelay;
        this.maxExecutionTime  = maxExecutionTime;
        this.mergedStat = mergedStat;
    }
    public final class Fields {
//        public final String PHASE = name;
        public static final String MAX_EXECUTION_TIME = "Max Execution Time";
        public static final String MAX_EXECUTION_DELAY = "Max Execution Delay";
    }

    @Override
    public Iterator<ShardPerfResult> iterator() {
        return null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(this.name);
        builder.startObject();
        mergedStat.toXContent(builder,params);
        builder.field(Fields.MAX_EXECUTION_TIME,maxExecutionTime);
        builder.field(Fields.MAX_EXECUTION_DELAY,maxExecutionDelay);
        for(ShardPerfResult shardPerfResult : shardPerfResults){
            shardPerfResult.toXContent(builder,params);
        }
        builder.endObject();
        return builder;
    }

    public static PhasePerfResult createPhasePerfResult(Collection<? extends SearchPhaseResult> searchPhaseResults, String phaseName){
        List<ShardPerfResult> shardPerfResults = new ArrayList<>();
        long maxExecutionDelay = 0;
        long maxExecutionTime = 0;
        MergedStat mergedStat = null;
        for(SearchPhaseResult searchPhaseResult: searchPhaseResults){
            ShardPerfResult shardPerfResult = searchPhaseResult.getShardPerfResult();
            shardPerfResults.add(shardPerfResult);
            maxExecutionDelay = Math.max(maxExecutionDelay,shardPerfResult.getExecutionDelay());
            maxExecutionTime = Math.max(maxExecutionTime,shardPerfResult.getExecutionTime());
            if(mergedStat!=null){
                mergedStat.mergeStat(shardPerfResult.getMergedStat());
            }else{
                mergedStat = shardPerfResult.getMergedStat();
            }

        }

        return new PhasePerfResult(shardPerfResults.toArray(new ShardPerfResult[0]),
                                    maxExecutionDelay,maxExecutionTime, mergedStat, phaseName);
    }
}
