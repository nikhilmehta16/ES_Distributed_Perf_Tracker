/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.ESSingleNodeTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class FieldNamesFieldMapperTests extends ESSingleNodeTestCase {

    private static SortedSet<String> extract(String path) {
        SortedSet<String> set = new TreeSet<>();
        for (String fieldName : FieldNamesFieldMapper.extractFieldNames(path)) {
            set.add(fieldName);
        }
        return set;
    }

    private static SortedSet<String> set(String... values) {
        return new TreeSet<>(Arrays.asList(values));
    }

    void assertFieldNames(Set<String> expected, ParsedDocument doc) {
        IndexableField[] fields = doc.rootDoc().getFields("_field_names");
        List<String> result = new ArrayList<>();
        for (IndexableField field : fields) {
            if (field.name().equals(FieldNamesFieldMapper.Defaults.NAME)) {
                result.add(field.stringValue());
            }
        }
        assertEquals(expected, set(result.toArray(new String[0])));
    }

    public void testExtractFieldNames() {
        assertEquals(set("abc"), extract("abc"));
        assertEquals(set("a", "a.b"), extract("a.b"));
        assertEquals(set("a", "a.b", "a.b.c"), extract("a.b.c"));
        // and now corner cases
        assertEquals(set("", ".a"), extract(".a"));
        assertEquals(set("a", "a."), extract("a."));
        assertEquals(set("", ".", ".."), extract(".."));
    }

    public void testFieldType() throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
            .startObject("_field_names").endObject()
            .endObject().endObject());

        DocumentMapper docMapper = createIndex("test").mapperService().parse("type", new CompressedXContent(mapping), false);
        FieldNamesFieldMapper fieldNamesMapper = docMapper.metadataMapper(FieldNamesFieldMapper.class);
        assertFalse(fieldNamesMapper.fieldType().hasDocValues());

        assertEquals(IndexOptions.DOCS, FieldNamesFieldMapper.Defaults.FIELD_TYPE.indexOptions());
        assertFalse(FieldNamesFieldMapper.Defaults.FIELD_TYPE.tokenized());
        assertFalse(FieldNamesFieldMapper.Defaults.FIELD_TYPE.stored());
        assertTrue(FieldNamesFieldMapper.Defaults.FIELD_TYPE.omitNorms());
    }

    public void testInjectIntoDocDuringParsing() throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type").endObject().endObject());
        DocumentMapper defaultMapper = createIndex("test").mapperService().parse("type", new CompressedXContent(mapping), false);

        ParsedDocument doc = defaultMapper.parse(new SourceToParse("test", "type", "1",
            BytesReference.bytes(XContentFactory.jsonBuilder()
                        .startObject()
                            .field("a", "100")
                            .startObject("b")
                                .field("c", 42)
                            .endObject()
                        .endObject()),
                XContentType.JSON));

        assertFieldNames(Collections.emptySet(), doc);
    }

    public void testExplicitEnabled() throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
            .startObject("_field_names").field("enabled", true).endObject()
            .startObject("properties")
            .startObject("field").field("type", "keyword").field("doc_values", false).endObject()
            .endObject().endObject().endObject());
        DocumentMapper docMapper = createIndex("test").mapperService().parse("type", new CompressedXContent(mapping), false);
        FieldNamesFieldMapper fieldNamesMapper = docMapper.metadataMapper(FieldNamesFieldMapper.class);
        assertTrue(fieldNamesMapper.fieldType().isEnabled());

        ParsedDocument doc = docMapper.parse(new SourceToParse("test", "type", "1",
            BytesReference.bytes(XContentFactory.jsonBuilder()
                .startObject()
                .field("field", "value")
                .endObject()),
            XContentType.JSON));

        assertFieldNames(set("field"), doc);
        assertWarnings(FieldNamesFieldMapper.ENABLED_DEPRECATION_MESSAGE);
    }

    public void testDisabled() throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
            .startObject("_field_names").field("enabled", false).endObject()
            .endObject().endObject());
        DocumentMapper docMapper = createIndex("test").mapperService().parse("type", new CompressedXContent(mapping), false);
        FieldNamesFieldMapper fieldNamesMapper = docMapper.metadataMapper(FieldNamesFieldMapper.class);
        assertFalse(fieldNamesMapper.fieldType().isEnabled());

        ParsedDocument doc = docMapper.parse(new SourceToParse("test", "type", "1",
            BytesReference.bytes(XContentFactory.jsonBuilder()
                .startObject()
                .field("field", "value")
                .endObject()),
            XContentType.JSON));

        assertNull(doc.rootDoc().get("_field_names"));
        assertWarnings(FieldNamesFieldMapper.ENABLED_DEPRECATION_MESSAGE.replace("{}", "test"));
    }

    public void testMergingMappings() throws Exception {
        String enabledMapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
            .startObject("_field_names").field("enabled", true).endObject()
            .endObject().endObject());
        String disabledMapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
            .startObject("_field_names").field("enabled", false).endObject()
            .endObject().endObject());
        MapperService mapperService = createIndex("test").mapperService();

        mapperService.merge("type", new CompressedXContent(enabledMapping), MapperService.MergeReason.MAPPING_UPDATE);
        DocumentMapper mapperDisabled = mapperService.merge("type", new CompressedXContent(disabledMapping),
            MapperService.MergeReason.MAPPING_UPDATE);
        assertFalse(mapperDisabled.metadataMapper(FieldNamesFieldMapper.class).fieldType().isEnabled());

        DocumentMapper mapperEnabled
            = mapperService.merge("type", new CompressedXContent(enabledMapping), MapperService.MergeReason.MAPPING_UPDATE);
        assertTrue(mapperEnabled.metadataMapper(FieldNamesFieldMapper.class).fieldType().isEnabled());
        assertWarnings(FieldNamesFieldMapper.ENABLED_DEPRECATION_MESSAGE.replace("{}", "test"));
    }
}
