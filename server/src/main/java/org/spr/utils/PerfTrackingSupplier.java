/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.spr.utils;

import org.elasticsearch.common.CheckedSupplier;
import org.elasticsearch.search.SearchPhaseResult;
import org.spr.utils.performance.PerfTracker;
import org.spr.utils.results.ShardPerfResult;

import java.util.function.Supplier;

public class PerfTrackingSupplier<T extends SearchPhaseResult,E extends Exception> implements CheckedSupplier<T, Exception> {
    private PerfTracker.PerfStats perfStats;

    private final String phaseName;
    private final String shardId;
    private final long creationTime;

    /**
     * Various level of PerfStats Verbosity is used
     * for amount of PerfStats to be sent back to request response
     */
    private final int verbosity;
    private long executionStartTime;
    private final Supplier<T> supplier;

    public PerfTrackingSupplier(Supplier<T> supplier, String shardId, String phaseName) throws Exception{
        //If no verbosity is provided, we take it as 0
        this(supplier,shardId,phaseName,0);
    }

    public PerfTrackingSupplier(Supplier<T> supplier, String shardId, String phaseName, int indexLevelVerbosity,
                                int clusterLevelVerbosity) throws Exception{
       this(supplier,shardId,phaseName,Math.max(indexLevelVerbosity,clusterLevelVerbosity));
    }
    public PerfTrackingSupplier(Supplier<T> supplier, String shardId, String phaseName, int verbosity) throws Exception{
        this.supplier = supplier;
        this.creationTime = System.nanoTime();
        this.shardId = shardId;
        this.phaseName = phaseName;
        this.verbosity = verbosity;
    }
    public T get() throws E{
        executionStartTime = System.nanoTime();
        long executionDelay = executionStartTime-creationTime;
        this.perfStats =  PerfTracker.start();
        PerfTracker.in(shardId);
        PerfTracker.executorDelay(executionDelay);
        T result = this.supplier.get();
        PerfTracker.out(shardId);
        long executionTime = System.nanoTime()-executionStartTime;
        result.setShardPerfResult(new ShardPerfResult(executionTime,executionDelay,perfStats.stopAndGetStat(),shardId, verbosity));
        return result;
    }

    void supply(){}
}

