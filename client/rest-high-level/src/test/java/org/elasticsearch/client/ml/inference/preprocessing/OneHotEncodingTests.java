/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.client.ml.inference.preprocessing;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.AbstractXContentTestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;


public class OneHotEncodingTests extends AbstractXContentTestCase<OneHotEncoding> {

    @Override
    protected OneHotEncoding doParseInstance(XContentParser parser) throws IOException {
        return OneHotEncoding.fromXContent(parser);
    }

    @Override
    protected boolean supportsUnknownFields() {
        return true;
    }

    @Override
    protected Predicate<String> getRandomFieldsExcludeFilter() {
        return field -> !field.isEmpty();
    }

    @Override
    protected OneHotEncoding createTestInstance() {
        return createRandom();
    }

    public static OneHotEncoding createRandom() {
        int valuesSize = randomIntBetween(1, 10);
        Map<String, String> valueMap = new HashMap<>();
        for (int i = 0; i < valuesSize; i++) {
            valueMap.put(randomAlphaOfLength(10), randomAlphaOfLength(10));
        }
        return new OneHotEncoding(randomAlphaOfLength(10), valueMap, randomBoolean() ? null : randomBoolean());
    }

}
