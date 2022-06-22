/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package com.spr.utils.results;

import com.spr.utils.PerfTrackerSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.Writeable;
import com.spr.utils.MergedStat;
import com.spr.utils.performance.PerfTracker;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;


public class ShardPerfResult implements ToXContentObject,Writeable {
    private static final Logger logger = LogManager.getLogger(ShardPerfResult.class);
    private final String perfStatName;
    private final long executionTime;
    private final long executionDelay;
    private final int verbosity;
    private final PerfTracker.Stat stat;

    public ShardPerfResult(long executionTime, long executionDelay, PerfTracker.Stat stat, String perfStatName, int verbosity) {
        this.perfStatName = perfStatName;
        this.executionTime = TimeUnit.NANOSECONDS.toMillis(executionTime);
        this.executionDelay = TimeUnit.NANOSECONDS.toMillis(executionDelay);
        this.verbosity = verbosity;
        this.stat = stat;
    }

    public static final class Fields {
        public static final String EXECUTIONTIME = "executionTime";
        public static final String EXECUTIONDELAY = "executionDelay";
        public static final String PERFSTATS = "shardPerfStats";
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
       if(verbosity> PerfTrackerSettings.VerbosityLevels.Level_2){
           toInnerXContent(builder,params);
       }
       return builder;
    }

    public XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
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

    public int getVerbosity(){
        return verbosity;
    }

    public MergedStat getMergedStat(){
        return stat.getMergedStat(perfStatName);
    }

    public ShardPerfResult(StreamInput in) throws IOException {
        perfStatName = in.readString();
        executionTime = in.readLong();
        executionDelay = in.readLong();
        verbosity = in.readVInt();
        try {
            stat = (PerfTracker.Stat) convertFromBytes(in.readByteArray());
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
        out.writeByteArray(convertToBytes(stat));

    }

    public static ShardPerfResult readShardPerfResult(StreamInput in) throws IOException {
        if(in.readBoolean()==false) {
            return null;
        }else{
            return new ShardPerfResult(in);
        }
    }
    public static void writeShardPerfResult(ShardPerfResult shardPerfResult,StreamOutput out) throws IOException {
        if(shardPerfResult==null) {
            out.writeBoolean(false);
        }else{
            out.writeBoolean(true);
            shardPerfResult.writeTo(out);
        }
    }
    private byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return bos.toByteArray();
        }
    }
    private Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(bis)) {
            return oin.readObject();
        }
    }
}
