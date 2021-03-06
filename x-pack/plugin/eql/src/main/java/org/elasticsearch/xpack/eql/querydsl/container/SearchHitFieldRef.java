/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.eql.querydsl.container;

import org.elasticsearch.xpack.ql.execution.search.FieldExtraction;
import org.elasticsearch.xpack.ql.execution.search.QlSourceBuilder;
import org.elasticsearch.xpack.ql.type.DataType;

import static org.elasticsearch.xpack.ql.type.DataTypes.DATETIME;
import static org.elasticsearch.xpack.ql.type.DataTypes.KEYWORD;

// NB: this class is taken from SQL - it hasn't been ported over to QL
// since at this stage is unclear whether the whole FieldExtraction infrastructure
// needs porting or just the field extraction
public class SearchHitFieldRef implements FieldExtraction {

    private final String name;
    private final String fullFieldName; // path included. If field full path is a.b.c, full field name is "a.b.c" and name is "c"
    private final DataType dataType;
    private final boolean docValue;
    private final String hitName;

    public SearchHitFieldRef(String name, String fullFieldName, DataType dataType, boolean useDocValueInsteadOfSource, boolean isAlias) {
        this(name, fullFieldName, dataType, useDocValueInsteadOfSource, isAlias, null);
    }

    public SearchHitFieldRef(String name, String fullFieldName, DataType dataType, boolean useDocValueInsteadOfSource, boolean isAlias,
                             String hitName) {
        this.name = name;
        this.fullFieldName = fullFieldName;
        this.dataType = dataType;
        // these field types can only be extracted from docvalue_fields (ie, values already computed by Elasticsearch)
        // because, for us to be able to extract them from _source, we would need the mapping of those fields (which we don't have)
        this.docValue = isAlias ? useDocValueInsteadOfSource : (hasDocValues(dataType) ? useDocValueInsteadOfSource : false);
        this.hitName = hitName;
    }

    public String hitName() {
        return hitName;
    }

    public String name() {
        return name;
    }

    public String fullFieldName() {
        return fullFieldName;
    }

    public DataType getDataType() {
        return dataType;
    }

    public boolean useDocValue() {
        return docValue;
    }

    @Override
    public void collectFields(QlSourceBuilder sourceBuilder) {
        // nested fields are handled by inner hits
        if (hitName != null) {
            return;
        }
        if (docValue) {
            sourceBuilder.addDocField(name, format(dataType));
        } else {
            sourceBuilder.addSourceField(name);
        }
    }

    @Override
    public final boolean supportedByAggsOnlyQuery() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    private static boolean hasDocValues(DataType dataType) {
        return dataType == KEYWORD || dataType == DATETIME;
    }

    private static String format(DataType dataType) {
        return dataType == DATETIME ? "epoch_millis" : null;
    }
}
