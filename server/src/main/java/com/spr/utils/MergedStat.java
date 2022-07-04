package com.spr.utils;

import com.spr.utils.performance.PerfTracker;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MergedStat implements ToXContentObject, Serializable {

    private static final int MAX_CALL_STACK_DEPTH = Integer.parseInt(System.getProperty("perf.stat.max.call.stack.depth", "200"));

    private final String name;
    private int totalCount;
    private long minTimeTaken;
    private long maxTimeTaken;
    private String maxTimeTakenName;
    private MergedStat child;
    private MergedStat peer;

    public static final class Fields {
        public static final String MERGED_PERF_STATS = "MergedPerfStats";
    }

    public MergedStat(String name, int count, long timeTaken, String rootName, MergedStat child, MergedStat peer) {
        this.name = name;
        this.totalCount = count;
        this.minTimeTaken = timeTaken;
        this.maxTimeTaken = timeTaken;
        this.maxTimeTakenName = rootName;
        this.child = child;
        this.peer = peer;
    }

    public static MergedStat fromStat(String resultName, PerfTracker.Stat stat) {
        if (stat == null) {
            return null;
        }
        return new MergedStat(stat.getName(), stat.getCount(), stat.getTimeTaken(), resultName,
            fromStat(resultName, stat.getChild()), fromStat(resultName, stat.getPeer()));
    }

    public void mergeStat(MergedStat stat) {
        if (stat == null) {
            return;
        }
        if (Objects.equals(name, stat.getName())) {
            minTimeTaken = Math.min(minTimeTaken, stat.getMinTimeTaken());
            totalCount += stat.getTotalCount();
            mergeMaxTimeTaken(stat);
            mergeWithPeer(stat.getPeer());
            mergeWithChild(stat.getChild());
        } else {
            mergeWithPeer(stat);
        }
   }

    private void mergeWithPeer(MergedStat stat) {
        if (peer == null) {
            peer = stat;
        } else {
            peer.mergeStat(stat);
        }
   }

    private void mergeWithChild(MergedStat stat) {
        if (child == null) {
            child = stat;
        } else{
            child.mergeStat(stat);
        }
   }

    private void mergeMaxTimeTaken(MergedStat stat) {
        if (maxTimeTaken < stat.getMaxTimeTaken()) {
            maxTimeTaken = stat.getMaxTimeTaken();
            maxTimeTakenName = stat.getMaxTimeTakenName();
        }
   }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.field(Fields.MERGED_PERF_STATS,this.toString());
        return builder;
    }

    public String getName() {
        return name;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public long getMinTimeTaken() {
        return minTimeTaken;
    }

    public void setMinTimeTaken(long minTimeTaken) {
        this.minTimeTaken = minTimeTaken;
    }

    public long getMaxTimeTaken() {
        return maxTimeTaken;
    }

    public void setMaxTimeTaken(long maxTimeTaken) {
        this.maxTimeTaken = maxTimeTaken;
    }

    public String getMaxTimeTakenName() {
        return maxTimeTakenName;
    }

    public void setMaxTimeTakenName(String maxTimeTakenName) {
        this.maxTimeTakenName = maxTimeTakenName;
    }

    public MergedStat getChild() {
        return child;
    }

    public void setChild(MergedStat child) {
        this.child = child;
    }

    public MergedStat getPeer() {
        return peer;
    }

    public void setPeer(MergedStat peer) {
        this.peer = peer;
    }

    private void toStacked(int depth, StringBuilder sb, int callStackDepth) {
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append(name).append(": ").append(totalCount)
            .append(" : ").append(TimeUnit.NANOSECONDS.toMillis(maxTimeTaken)).append(" : ").append(maxTimeTakenName).append('\n');

        if (child != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
            child.toStacked(depth + 1, sb, callStackDepth);
        }
        if (peer != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
            peer.toStacked(depth, sb, callStackDepth + 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toStacked(0, sb, 0);
        return sb.toString();
    }
}
