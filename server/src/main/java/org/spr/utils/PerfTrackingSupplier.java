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

import java.util.function.Supplier;

public class PerfTrackingSupplier<T extends SearchPhaseResult,E extends Exception> implements CheckedSupplier<T, Exception> {
    private PerfTracker.PerfStats perfStats;
    private int shardId;
    private long creationTime;

    //when execution starte
    private long executionStartTime;
    private final Supplier<T> supplier;
    public PerfTrackingSupplier(Supplier<T> supplier, int shardId) throws Exception{
        this.supplier = supplier;
        this.creationTime = System.nanoTime();
        this.shardId = shardId;
    }


    void supply() {

    }

    public T get() throws E{
        executionStartTime = System.nanoTime();
        this.perfStats =  PerfTracker.start();
        PerfTracker.executorDelay(executionStartTime-creationTime);
        PerfTracker.in("ShardId("+shardId+")");
        T result = this.supplier.get();
        PerfTracker.out("ShardId("+shardId+")");
        result.setPerfStats(perfStats.stopAndGetStacked());
        return result;
    }
}

