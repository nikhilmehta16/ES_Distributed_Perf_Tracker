/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.IndexAnalyzers;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.MapperService.MergeReason;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.indices.mapper.MapperRegistry;
import org.elasticsearch.test.IndexSettingsModule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import static org.apache.lucene.util.LuceneTestCase.expectThrows;
import static org.elasticsearch.test.ESTestCase.createTestAnalysis;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class MapperTestUtils {

    public static MapperService newMapperService(NamedXContentRegistry xContentRegistry,
                                                 Path tempDir,
                                                 Settings indexSettings,
                                                 String indexName) throws IOException {
        IndicesModule indicesModule = new IndicesModule(Collections.emptyList());
        return newMapperService(xContentRegistry, tempDir, indexSettings, indicesModule, indexName);
    }

    public static MapperService newMapperService(NamedXContentRegistry xContentRegistry, Path tempDir, Settings settings,
                                                 IndicesModule indicesModule, String indexName) throws IOException {
        Settings.Builder settingsBuilder = Settings.builder()
            .put(Environment.PATH_HOME_SETTING.getKey(), tempDir)
            .put(settings);
        if (settings.get(IndexMetadata.SETTING_VERSION_CREATED) == null) {
            settingsBuilder.put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT);
        }
        Settings finalSettings = settingsBuilder.build();
        MapperRegistry mapperRegistry = indicesModule.getMapperRegistry();
        IndexSettings indexSettings = IndexSettingsModule.newIndexSettings(indexName, finalSettings);
        IndexAnalyzers indexAnalyzers = createTestAnalysis(indexSettings, finalSettings).indexAnalyzers;
        SimilarityService similarityService = new SimilarityService(indexSettings, null, Collections.emptyMap());
        return new MapperService(indexSettings,
            indexAnalyzers,
            xContentRegistry,
            similarityService,
            mapperRegistry,
            () -> null, () -> false, null);
    }

    public static void assertConflicts(String mapping1,
                                       String mapping2,
                                       MapperService mapperService,
                                       String... conflicts) throws IOException {
        DocumentMapper docMapper = mapperService.parse("type", new CompressedXContent(mapping1), false);
        if (conflicts.length == 0) {
            docMapper.merge(mapperService.parse("type", new CompressedXContent(mapping2), false).mapping(), MergeReason.MAPPING_UPDATE);
        } else {
            Exception e = expectThrows(IllegalArgumentException.class,
                () -> docMapper.merge(mapperService.parse("type", new CompressedXContent(mapping2), false).mapping(),
                    MergeReason.MAPPING_UPDATE));
            for (String conflict : conflicts) {
                assertThat(e.getMessage(), containsString(conflict));
            }
        }
    }
}
