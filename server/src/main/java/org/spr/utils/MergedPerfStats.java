
package org.spr.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MergedPerfStats {
    private HashMap<String, Object> perfResult = new HashMap<String, Object>();

    public String tempResults = "";

    public void mergeResult(PerfTrackerResult perfTrackerResult){
        Map<String, Object> temp = perfTrackerResult.getResult();
        try{
            perfResult.putAll(temp);
        }catch (Exception e){
            throw e;
        }
    }

    public PerfTrackerResult getPerfTrackerResult(){
        return new PerfTrackerResult(this.perfResult);
    }
}
