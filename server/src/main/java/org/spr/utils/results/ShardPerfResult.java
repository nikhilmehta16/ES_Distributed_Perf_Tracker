/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.spr.utils.results;

import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.spr.utils.performance.PerfTracker;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ShardPerfResult implements ToXContentObject {
    private final String perfStatName;
    private final long executionTime;
    private final long executionDelay;
    private final int verbosity;
    private final PerfTracker.Stat stat;

    public ShardPerfResult(long executionTime, long executionDelay, PerfTracker.Stat stat, String perfStatName, int verbosity) {
        this.perfStatName = perfStatName;
        this.executionTime = TimeUnit.NANOSECONDS.toMillis(executionTime);
        this.executionDelay = TimeUnit.NANOSECONDS.toMillis(executionDelay);
        this.stat = stat;
        this.verbosity = verbosity;
    }

    public static final class Fields {
        public static final String EXECUTIONTIME = "executionTime";
        public static final String EXECUTIONDELAY = "executionDelay";

        public static final String PERFSTATS = "perfStats";
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(perfStatName);
        builder.startObject();
        builder.field(Fields.EXECUTIONTIME,this.executionTime);
        builder.field(Fields.EXECUTIONDELAY,this.executionDelay);
        builder.field(Fields.PERFSTATS,this.stat.toString());
        builder.endObject();
        return builder;
    }

    public String getPerfStatName(){
        return perfStatName;
    }

    public long getExecutionTime(){return executionTime;}

    public long getExecutionDelay(){return executionDelay;}

}
