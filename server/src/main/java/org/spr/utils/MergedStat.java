
package org.spr.utils;


import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MergedStat implements ToXContentObject {
   private final String name;
   private int totalCount;
   private long minTimeTaken;
   private long maxTimeTaken;
   private String maxTimeTakenName;
//   MergedStats parent;
   MergedStat child;
   MergedStat peer;
   private static final int MAX_CALL_STACK_DEPTH = Integer.parseInt(System.getProperty("perf.stat.max.call.stack.depth", "200"));


    public static final class Fields {
    }


   public MergedStat(String name, int count, long timeTaken, String rootName, MergedStat child, MergedStat peer){
       this.name = name;
       this.totalCount = count;
       this.minTimeTaken = timeTaken;
       this.maxTimeTaken = timeTaken;
       this.maxTimeTakenName = rootName;
       this.child = child;
       this.peer = peer;
   }



   public void mergeStat(MergedStat stat){
       if(stat!=null) {
           if (name.equals(stat.getName())) {
               minTimeTaken = Math.min(minTimeTaken, stat.getMinTimeTaken());
               totalCount += stat.getTotalCount();
               mergeMaxTimeTaken(stat);
               mergeWithPeer(stat.getPeer());
               mergeWithChild(stat.getChild());

           } else {
               mergeWithPeer(stat);
           }
       }
   }

   private void mergeWithPeer(MergedStat stat){
       if(peer==null) {
           peer = stat;
       }else{
           peer.mergeStat(stat);
       }
   }

   private void mergeWithChild(MergedStat stat){
       if(child==null) {
           child = stat;
       }else{
           child.mergeStat(stat);
       }
   }

   private void mergeMaxTimeTaken(MergedStat stat){
       if(maxTimeTaken < stat.getMaxTimeTaken()){
           maxTimeTaken = stat.getMaxTimeTaken();
           maxTimeTakenName = stat.getMaxTimeTakenName();
       }
   }
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.field("MergedPerfStats",this.toString());
        return builder;
    }

   public String getName(){
       return this.name;
   }
   public int getTotalCount(){
       return totalCount;
   }
   public long getMinTimeTaken(){
       return minTimeTaken;
   }
   public long getMaxTimeTaken(){
       return maxTimeTaken;
   }
   public MergedStat getChild(){
       return child;
   }
   public MergedStat getPeer(){
       return peer;
   }

   public String getMaxTimeTakenName(){
       return maxTimeTakenName;
   }
   private void toStacked(int depth, StringBuilder sb, int callStackDepth) {
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append(name).append(": ").append(totalCount)
//            .append(" : ").append(TimeUnit.NANOSECONDS.toMillis(minTimeTaken))
            .append(" : ").append(TimeUnit.NANOSECONDS.toMillis(maxTimeTaken)).append(" : ").append(maxTimeTakenName).append('\n');

        if (child != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
            child.toStacked(depth + 1, sb, callStackDepth);
        }
        if (peer != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
            peer.toStacked(depth, sb, callStackDepth + 1);
        }
    }
   @Override
    public String toString(){
       StringBuilder sb = new StringBuilder();
       toStacked(0, sb, 0);
       return sb.toString();
    }
}
