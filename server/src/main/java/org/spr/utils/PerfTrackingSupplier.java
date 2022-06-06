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

    private String phaseName;
    private String shardId;
    private long creationTime;

    private long executionStartTime;
    private final Supplier<T> supplier;
    public PerfTrackingSupplier(Supplier<T> supplier, String shardId, String phaseName) throws Exception{
        this.supplier = supplier;
        this.creationTime = System.nanoTime();
        this.shardId = shardId;
        this.phaseName = phaseName;
    }
    public T get() throws E{
        executionStartTime = System.nanoTime();
        this.perfStats =  PerfTracker.start();
        PerfTracker.executorDelay(executionStartTime-creationTime);
        PerfTracker.in(shardId);
        T result = this.supplier.get();
        PerfTracker.out(shardId);
        result.setShardPerfResult(new ShardPerfResult(perfStats.stopAndGetStacked(),shardId));
        return result;
    }

    void supply(){}
}

