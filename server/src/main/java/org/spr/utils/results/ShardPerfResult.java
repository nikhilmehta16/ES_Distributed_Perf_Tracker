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

import java.io.IOException;

public class ShardPerfResult implements ToXContentObject {
    private final String perfStatName;
    private final String tempData;


    public ShardPerfResult(String tempData, String perfStatName) {
        this.perfStatName = perfStatName;
        this.tempData = tempData;
    }

    public static final class Fields {
        public static final String DATA = "data";
        public static final String TOTAL = "total";

    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(perfStatName);
        builder.startObject();
        builder.field("data",this.tempData);
        builder.endObject();
        return builder;
    }

    public String getPerfStatName(){
        return perfStatName;
    }


}
