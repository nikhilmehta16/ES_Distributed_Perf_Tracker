/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.spr.utils.results;

import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.Writeable;
import org.spr.utils.MergedStat;
import org.spr.utils.performance.PerfTracker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;


public class ShardPerfResult implements ToXContentObject,Writeable {

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
        builder.field(Fields.EXECUTIONTIME, this.executionTime);
        builder.field(Fields.EXECUTIONDELAY, this.executionDelay);
        builder.field(Fields.PERFSTATS, this.stat.toString());
        builder.endObject();
        return builder;
    }

    public String getPerfStatName() {
        return perfStatName;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public long getExecutionDelay() {
        return executionDelay;
    }

    public MergedStat getMergedStat(){
        return stat.getMergedStat(perfStatName);
    }

    public ShardPerfResult(StreamInput in) throws IOException {
        perfStatName = in.readString();
        executionTime = in.readLong();
        executionDelay = in.readLong();
        verbosity = in.readVInt();
        ObjectInputStream ois = new ObjectInputStream(in);
        try {
            stat = (PerfTracker.Stat) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(perfStatName);
        out.writeLong(executionTime);
        out.writeLong(executionDelay);
        out.writeVInt(verbosity);
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(stat);

    }
}
