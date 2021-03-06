/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.lucene.search.Queries;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.index.query.QueryShardContext;

import java.util.List;
import java.util.Map;

/**
 * A {@link MappedFieldType} that has the same value for all documents.
 * Factory methods for queries are called at rewrite time so they should be
 * cheap. In particular they should not read data from disk or perform a
 * network call. Furthermore they may only return a {@link MatchAllDocsQuery}
 * or a {@link MatchNoDocsQuery}.
 */
public abstract class ConstantFieldType extends MappedFieldType {

    public ConstantFieldType(String name, Map<String, String> meta) {
        super(name, true, false, true, TextSearchInfo.SIMPLE_MATCH_WITHOUT_TERMS, meta);
    }

    @Override
    public final boolean isSearchable() {
        return true;
    }

    @Override
    public final boolean isAggregatable() {
        return true;
    }

    /**
     * Return whether the constant value of this field matches the provided {@code pattern}
     * as documented in {@link Regex#simpleMatch}.
     */
    protected abstract boolean matches(String pattern, boolean caseInsensitive, QueryShardContext context);

    private static String valueToString(Object value) {
        return value instanceof BytesRef
                ? ((BytesRef) value).utf8ToString()
                : value.toString();
    }

    @Override
    public final Query termQuery(Object value, QueryShardContext context) {
        String pattern = valueToString(value);
        if (matches(pattern, false, context)) {
            return Queries.newMatchAllQuery();
        } else {
            return new MatchNoDocsQuery();
        }
    }

    @Override
    public final Query termQueryCaseInsensitive(Object value, QueryShardContext context) {
        String pattern = valueToString(value);
        if (matches(pattern, true, context)) {
            return Queries.newMatchAllQuery();
        } else {
            return new MatchNoDocsQuery();
        }
    }

    @Override
    public final Query termsQuery(List<?> values, QueryShardContext context) {
        for (Object value : values) {
            String pattern = valueToString(value);
            if (matches(pattern, false, context)) {
                // `terms` queries are a disjunction, so one matching term is enough
                return Queries.newMatchAllQuery();
            }
        }
        return new MatchNoDocsQuery();
    }

    @Override
    public final Query prefixQuery(String prefix,
                             @Nullable MultiTermQuery.RewriteMethod method,
                             boolean caseInsensitive,
                             QueryShardContext context) {
        String pattern = prefix + "*";
        if (matches(pattern, caseInsensitive, context)) {
            return Queries.newMatchAllQuery();
        } else {
            return new MatchNoDocsQuery();
        }
    }

    @Override
    public final Query wildcardQuery(String value,
                               @Nullable MultiTermQuery.RewriteMethod method,
                               boolean caseInsensitive,
                               QueryShardContext context) {
        if (matches(value, caseInsensitive, context)) {
            return Queries.newMatchAllQuery();
        } else {
            return new MatchNoDocsQuery();
        }
    }
}
