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

import java.io.IOException;
import java.util.Iterator;

public class PerfResults implements Iterable<PhasePerfResult>, ToXContentFragment {

    private final PhasePerfResult[]  phasePerfResults;
    private final int maxPhaseVerbosity;
    public static final class Fields {
        public static final String PERFSTATS = "perf_stats";
    }

    public PerfResults(PhasePerfResult[] phasePerfResults) {
        this.phasePerfResults = phasePerfResults;
        int maxPhaseVerbosity = 0;
        //merge logic of PerfResults here.
        for(PhasePerfResult phasePerfResult : phasePerfResults){
            maxPhaseVerbosity = Math.max(maxPhaseVerbosity,phasePerfResult.getMaxShardVerbosity());
        }
        this.maxPhaseVerbosity = maxPhaseVerbosity;
    }

    @Override
    public Iterator<PhasePerfResult> iterator() {
        return null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if(maxPhaseVerbosity>0) {
           toInnerXContent(builder,params);
        }
        return builder;
    }

    public XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(PerfResults.Fields.PERFSTATS);
        if(maxPhaseVerbosity>1) {
            for (PhasePerfResult phasePerfResult : phasePerfResults) {
                phasePerfResult.toXContent(builder, params);
            }
        }

        builder.endObject();
        return builder;
    }
}
