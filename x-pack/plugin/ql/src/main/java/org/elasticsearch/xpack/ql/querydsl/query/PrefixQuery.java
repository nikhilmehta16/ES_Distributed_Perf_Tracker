/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ql.querydsl.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.xpack.ql.tree.Source;

import java.util.Objects;

import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;

public class PrefixQuery extends LeafQuery {

    private final String field, query;

    public PrefixQuery(Source source, String field, String query) {
        super(source);
        this.field = field;
        this.query = query;
    }

    public String field() {
        return field;
    }

    public String query() {
        return query;
    }

    @Override
    public QueryBuilder asBuilder() {
        return prefixQuery(field, query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, query);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        PrefixQuery other = (PrefixQuery) obj;
        return Objects.equals(field, other.field)
                && Objects.equals(query, other.query);
    }

    @Override
    protected String innerToString() {
        return field + ":" + query;
    }
}
