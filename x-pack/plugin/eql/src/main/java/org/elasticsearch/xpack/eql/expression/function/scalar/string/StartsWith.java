/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.eql.expression.function.scalar.string;

import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.session.Configuration;
import org.elasticsearch.xpack.ql.tree.Source;

public class StartsWith extends org.elasticsearch.xpack.ql.expression.function.scalar.string.StartsWith {

    public StartsWith(Source source, Expression input, Expression pattern, Configuration configuration) {
        super(source, input, pattern, configuration);
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

}
