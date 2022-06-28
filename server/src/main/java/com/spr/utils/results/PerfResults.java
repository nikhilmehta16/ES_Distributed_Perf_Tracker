/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package com.spr.utils.results;

import com.spr.utils.PerfTrackerSettings;
import org.elasticsearch.common.xcontent.ToXContentFragment;
import org.elasticsearch.common.xcontent.XContentBuilder;
import com.spr.utils.performance.PerfTracker;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class PerfResults implements Iterable<PhasePerfResult>, ToXContentFragment {
    private final List<PhasePerfResult>  phasePerfResults = new ArrayList<>();
    private int maxPhaseVerbosity;
    private final HashMap<String, PerfTracker.Stat> perfStatsMap = new HashMap<>();

    public static final class Fields {
        public static final String PERFSTATS = "perf_stats";
    }

    public PerfResults(List<PhasePerfResult> phasePerfResults) {
        for (PhasePerfResult phasePerfResult : phasePerfResults) {
            this.addPhasePerfResult(phasePerfResult);
        }
    }

    @Override
    public Iterator<PhasePerfResult> iterator() {
        return phasePerfResults.iterator();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (maxPhaseVerbosity > PerfTrackerSettings.VerbosityLevels.Level_0) {
           toInnerXContent(builder,params);
        }
        return builder;
    }

    public XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(PerfResults.Fields.PERFSTATS);

        for (Map.Entry<String, PerfTracker.Stat> entry : perfStatsMap.entrySet()) {
            builder.field(entry.getKey(), entry.getValue().toString());
        }
        if (maxPhaseVerbosity > PerfTrackerSettings.VerbosityLevels.Level_1) {
            for (PhasePerfResult phasePerfResult : this.phasePerfResults.toArray(new PhasePerfResult[0])) {
                phasePerfResult.toXContent(builder, params);
            }
        }

        builder.endObject();
        return builder;
    }
//    public PerfTracker.Stat getPerfStats(){return this.coordinatorPerfStats;}

    public void addPhasePerfResult(PhasePerfResult phasePerfResult) {
        this.phasePerfResults.add(phasePerfResult);
        maxPhaseVerbosity = Math.max(maxPhaseVerbosity, phasePerfResult.getMaxShardVerbosity());
    }

    public void addPerfStats(String name, PerfTracker.Stat stat) {
        this.perfStatsMap.put(name,stat);
    }
}
