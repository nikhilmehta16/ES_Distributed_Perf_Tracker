/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.optimizer;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ql.expression.function.FunctionRegistry;
import org.elasticsearch.xpack.ql.index.EsIndex;
import org.elasticsearch.xpack.ql.index.IndexResolution;
import org.elasticsearch.xpack.ql.plan.logical.LogicalPlan;
import org.elasticsearch.xpack.ql.type.EsField;
import org.elasticsearch.xpack.sql.SqlTestUtils;
import org.elasticsearch.xpack.sql.analysis.analyzer.Analyzer;
import org.elasticsearch.xpack.sql.analysis.analyzer.Verifier;
import org.elasticsearch.xpack.sql.parser.SqlParser;
import org.elasticsearch.xpack.sql.stats.Metrics;
import org.elasticsearch.xpack.sql.types.SqlTypesTests;

import java.util.Map;

public class OptimizerRunTests extends ESTestCase {

    private final SqlParser parser;
    private final IndexResolution getIndexResult;
    private final FunctionRegistry functionRegistry;
    private final Analyzer analyzer;
    private final Optimizer optimizer;

    public OptimizerRunTests() {
        parser = new SqlParser();
        functionRegistry = new FunctionRegistry();

        Map<String, EsField> mapping = SqlTypesTests.loadMapping("mapping-multi-field-variation.json");

        EsIndex test = new EsIndex("test", mapping);
        getIndexResult = IndexResolution.valid(test);
        analyzer = new Analyzer(SqlTestUtils.TEST_CFG, functionRegistry, getIndexResult, new Verifier(new Metrics()));
        optimizer = new Optimizer();
    }

    private LogicalPlan plan(String sql) {
        return optimizer.optimize(analyzer.analyze(parser.createStatement(sql)));
    }

    public void testWhereClause() {
        LogicalPlan p = plan("SELECT some.string l FROM test WHERE int IS NOT NULL AND int < 10005 ORDER BY int");
        assertNotNull(p);
    }
}
