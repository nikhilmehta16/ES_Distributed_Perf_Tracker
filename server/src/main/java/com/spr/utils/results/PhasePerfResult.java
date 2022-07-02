package com.spr.utils.results;

import com.spr.utils.PerfTrackerSettings;
import org.elasticsearch.common.xcontent.ToXContentFragment;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchPhaseResult;
import com.spr.utils.MergedStat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PhasePerfResult implements ToXContentFragment {
    private final String name;
    private final ShardPerfResult[] shardPerfResults;
    private final MergedStat mergedStat;
    private final int maxShardVerbosity;
    public static final String FETCH_PHASE = "Fetch Phase";
    public static final String QUERY_PHASE = "Query Phase";
    public static final String DFS_PHASE = "DFS Phase";

    private PhasePerfResult(ShardPerfResult[] shardPerfResults, int maxShardVerbosity, MergedStat mergedStat, String phaseName) {
        this.name = phaseName;
        this.shardPerfResults = shardPerfResults;
        this.mergedStat = mergedStat;
        this.maxShardVerbosity = maxShardVerbosity;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (maxShardVerbosity > PerfTrackerSettings.VerbosityLevels.LEVEL_1) {
            toInnerXContent(builder,params);
        }
        return builder;
    }

    public XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(this.name);
        builder.startObject();
        mergedStat.toXContent(builder, params);
        if (maxShardVerbosity > PerfTrackerSettings.VerbosityLevels.LEVEL_2) {
            for (ShardPerfResult shardPerfResult : shardPerfResults) {
                if (shardPerfResult == null) {
                    continue;
                }
                shardPerfResult.toXContent(builder, params);
            }
        }
        builder.endObject();
        return builder;
    }

    public static PhasePerfResult createPhasePerfResult(Collection<? extends SearchPhaseResult> searchPhaseResults, String phaseName) {
        List<ShardPerfResult> shardPerfResults = new ArrayList<>();
        int maxShardVerbosity = 0;
        MergedStat mergedStat = null;
        for (SearchPhaseResult searchPhaseResult : searchPhaseResults) {
            ShardPerfResult shardPerfResult = searchPhaseResult.getShardPerfResult();
            if (shardPerfResult == null) {
                continue;
            }

            shardPerfResults.add(shardPerfResult);
            maxShardVerbosity = Math.max(maxShardVerbosity, shardPerfResult.getVerbosity());
            if (mergedStat != null) {
                //merge with a previous mergedResult
                mergedStat.mergeStat(shardPerfResult.getMergedStat());
            } else {
                //If no previous exists we create a new mergedResult and use it for subsequent ones.
                mergedStat = shardPerfResult.getMergedStat();
            }

        }

        return new PhasePerfResult(shardPerfResults.toArray(new ShardPerfResult[0]), maxShardVerbosity, mergedStat, phaseName);
    }

    public int getMaxShardVerbosity() {
        return maxShardVerbosity;
    }
}
