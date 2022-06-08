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

    final PhasePerfResult[]  phasePerfResults;

    public static final class Fields {
        public static final String PERFSTATS = "perf_stats";
    }

    public PerfResults(PhasePerfResult[] phasePerfResults) {
        this.phasePerfResults = phasePerfResults;
        //merge logic of PerfResults here.
    }

    @Override
    public Iterator<PhasePerfResult> iterator() {
        return null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(PerfResults.Fields.PERFSTATS);
        for (PhasePerfResult phasePerfResult : phasePerfResults) {
            phasePerfResult.toXContent(builder, params);
        }

        builder.endObject();
        return builder;
    }






}
