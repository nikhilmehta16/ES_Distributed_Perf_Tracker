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
import java.io.Serializable;


public class ShardPerfResult implements ToXContentObject, Writeable, Serializable {

    private static final Logger logger = LogManager.getLogger(ShardPerfResult.class);

    private final String perfStatName;
    private final int verbosity;
    private final PerfTracker.Stat stat;
    private MergedStat mergedStat;

    public ShardPerfResult(PerfTracker.Stat stat, String perfStatName, int verbosity) {
        this.perfStatName = perfStatName;
        this.verbosity = verbosity;
        this.stat = stat;
    }

    public static final class Fields {
        public static final String PERFSTATS = "shardPerfStats";
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
       if (verbosity > PerfTrackerSettings.VerbosityLevels.LEVEL_2) {
           toInnerXContent(builder);
       }
       return builder;
    }

    public XContentBuilder toInnerXContent(XContentBuilder builder) throws IOException {
        builder.field(perfStatName);
        builder.startObject();
        if (this.stat != null) {
            builder.field(Fields.PERFSTATS, this.stat.toString());
        }
        builder.endObject();
        return builder;
    }

    public String getPerfStatName() {
        return perfStatName;
    }

    public int getVerbosity(){
        return verbosity;
    }

    public MergedStat getMergedStat() {
        if (mergedStat != null) {
            return mergedStat;
        }
        mergedStat = MergedStat.fromStat(perfStatName, stat);
        return mergedStat;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeByteArray(convertToBytes(this));
    }

    public static ShardPerfResult readShardPerfResult(StreamInput in) {
        try {
            if (in.readBoolean() == true) {
                return (ShardPerfResult) convertFromBytes(in.readByteArray());
            }
        } catch (Exception e) {
            logger.error("Cannot read ShardPerfResult from StreamInput", e);
        }
        return null;
    }

    public static void writeShardPerfResult(ShardPerfResult shardPerfResult, StreamOutput out) throws IOException {
        if (shardPerfResult == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            shardPerfResult.writeTo(out);
        }
    }

    private static byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return bos.toByteArray();
        }
    }

    private static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(bis)) {
            return oin.readObject();
        }
    }
}
