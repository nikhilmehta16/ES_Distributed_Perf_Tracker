package com.spr.utils;

import org.elasticsearch.common.CheckedSupplier;
import org.elasticsearch.search.SearchPhaseResult;
import com.spr.utils.performance.PerfTracker;
import com.spr.utils.results.ShardPerfResult;

public class PerfTrackingSupplier<T extends SearchPhaseResult, E extends Exception> implements CheckedSupplier<T, Exception> {

    private final String shardId;
    private final long creationTime;
    /**
     * Various level of PerfStats Verbosity is used
     * for amount of PerfStats to be sent back to request response
     */
    private final int verbosity;
    private final CheckedSupplier<T, E> supplier;

    public PerfTrackingSupplier(CheckedSupplier<T, E> supplier, String shardId, int indexLevelVerbosity, int clusterLevelVerbosity) {
        this.supplier = supplier;
        this.creationTime = System.nanoTime();
        this.shardId = shardId;
        this.verbosity = Math.max(indexLevelVerbosity, clusterLevelVerbosity);
    }

    public T get() throws E {
        // get.totalTime
        long executionStartTime = System.nanoTime();
        long executionDelay = executionStartTime - creationTime;
        PerfTracker.reset();
        PerfTracker.PerfStats perfStats = PerfTracker.start();
        PerfTracker.executorDelay(executionDelay);
        T result;
        try {
            result = this.supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long executionTime = System.nanoTime() - executionStartTime;
        result.setShardPerfResult(new ShardPerfResult(executionTime, executionDelay, perfStats.stopAndGetStat(), shardId, verbosity));
        return result;
    }
}

