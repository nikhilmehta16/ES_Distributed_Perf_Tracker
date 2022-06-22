/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package com.spr.utils;

import org.elasticsearch.common.CheckedSupplier;
import org.elasticsearch.search.SearchPhaseResult;
import com.spr.utils.performance.PerfTracker;
import com.spr.utils.results.ShardPerfResult;

public class PerfTrackingSupplier<T extends SearchPhaseResult,E extends Exception> implements CheckedSupplier<T, Exception> {

    private final String shardId;
    private final long creationTime;

    /**
     * Various level of PerfStats Verbosity is used
     * for amount of PerfStats to be sent back to request response
     */
    private final int verbosity;
    private final CheckedSupplier<T, E> supplier;

    public PerfTrackingSupplier(CheckedSupplier<T, E> supplier, String shardId){
        this(supplier,shardId,0);
    }

    public PerfTrackingSupplier(CheckedSupplier<T, E> supplier, String shardId, int indexLevelVerbosity,
                                int clusterLevelVerbosity){
       this(supplier,shardId,Math.max(indexLevelVerbosity, clusterLevelVerbosity));
    }
    public PerfTrackingSupplier(CheckedSupplier<T, E> supplier, String shardId, int verbosity) {
        this.supplier = supplier;
        this.creationTime = System.nanoTime();
        this.shardId = shardId;
        this.verbosity = verbosity;
    }

    public T get() throws E{
        long executionStartTime = System.nanoTime();
        long executionDelay = executionStartTime -creationTime;
        PerfTracker.reset();
        PerfTracker.PerfStats perfStats = PerfTracker.start("Shard");
        PerfTracker.executorDelay(executionDelay);
        T result;
        try {
            result = this.supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long executionTime = System.nanoTime()- executionStartTime;
        result.setShardPerfResult(new ShardPerfResult(executionTime, executionDelay, perfStats.stopAndGetStat(), shardId, verbosity));
        return result;
    }

    void supply(){}
}

