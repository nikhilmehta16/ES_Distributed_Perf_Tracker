/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.analysis.common;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.reverse.ReverseStringFilter;
import org.elasticsearch.Version;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;


public class EdgeNGramTokenFilterFactory extends AbstractTokenFilterFactory {

    private static final DeprecationLogger DEPRECATION_LOGGER =  DeprecationLogger.getLogger(EdgeNGramTokenFilterFactory.class);

    private final int minGram;

    private final int maxGram;

    public static final int SIDE_FRONT = 1;
    public static final int SIDE_BACK = 2;
    private final int side;
    private final boolean preserveOriginal;
    private static final String PRESERVE_ORIG_KEY = "preserve_original";

    EdgeNGramTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        this.minGram = settings.getAsInt("min_gram", 1);
        this.maxGram = settings.getAsInt("max_gram", 2);
        this.side = parseSide(settings.get("side", "front"));
        this.preserveOriginal = settings.getAsBoolean(PRESERVE_ORIG_KEY, false);
    }

    static int parseSide(String side) {
        switch(side) {
            case "front": return SIDE_FRONT;
            case "back": return SIDE_BACK;
            default: throw new IllegalArgumentException("invalid side: " + side);
        }
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        TokenStream result = tokenStream;

        // side=BACK is not supported anymore but applying ReverseStringFilter up-front and after the token filter has the same effect
        if (side == SIDE_BACK) {
            result = new ReverseStringFilter(result);
        }

        result = new EdgeNGramTokenFilter(result, minGram, maxGram, preserveOriginal);

        // side=BACK is not supported anymore but applying ReverseStringFilter up-front and after the token filter has the same effect
        if (side == SIDE_BACK) {
            result = new ReverseStringFilter(result);
        }

        return result;
    }

    @Override
    public boolean breaksFastVectorHighlighter() {
        return true;
    }

    @Override
    public TokenFilterFactory getSynonymFilter() {
        if (indexSettings.getIndexVersionCreated().onOrAfter(Version.V_7_0_0)) {
            throw new IllegalArgumentException("Token filter [" + name() + "] cannot be used to parse synonyms");
        }
        else {
            DEPRECATION_LOGGER.deprecate("synonym_tokenfilters", "Token filter [" + name()
                + "] will not be usable to parse synonyms after v7.0");
            return this;
        }
    }
}
