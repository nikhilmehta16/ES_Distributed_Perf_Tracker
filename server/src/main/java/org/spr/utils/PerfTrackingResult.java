/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.spr.utils;


import org.spr.utils.performance.PerfTracker;
//Not used
public interface PerfTrackingResult {

    void setPerfStats(String perfStats);
    String getPerfStats();
//    String mergedperfResponse()

}
