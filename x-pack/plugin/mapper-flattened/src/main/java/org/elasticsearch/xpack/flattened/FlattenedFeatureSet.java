/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.flattened;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.core.XPackFeatureSet;
import org.elasticsearch.xpack.core.XPackField;
import org.elasticsearch.xpack.core.flattened.FlattenedFeatureSetUsage;
import org.elasticsearch.xpack.flattened.mapper.FlattenedFieldMapper;

import java.util.Map;

public class FlattenedFeatureSet implements XPackFeatureSet {

    private final XPackLicenseState licenseState;
    private final ClusterService clusterService;

    @Inject
    public FlattenedFeatureSet(XPackLicenseState licenseState, ClusterService clusterService) {
        this.licenseState = licenseState;
        this.clusterService = clusterService;
    }

    @Override
    public String name() {
        return XPackField.FLATTENED;
    }

    @Override
    public boolean available() {
        return licenseState != null && licenseState.isAllowed(XPackLicenseState.Feature.FLATTENED);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public Map<String, Object> nativeCodeInfo() {
        return null;
    }

    @Override
    public void usage(ActionListener<Usage> listener) {
        int fieldCount = 0;
        if (available() && enabled() && clusterService.state() != null) {
            for (IndexMetadata indexMetadata : clusterService.state().metadata()) {
                MappingMetadata mappingMetadata = indexMetadata.mapping();

                if (mappingMetadata != null) {
                    Map<String, Object> mappings = mappingMetadata.getSourceAsMap();

                    if (mappings.containsKey("properties")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, Object>> fieldMappings = (Map<String, Map<String, Object>>) mappings.get("properties");

                        for (Map<String, Object> fieldMapping : fieldMappings.values()) {
                            String fieldType = (String) fieldMapping.get("type");
                            if (fieldType != null && fieldType.equals(FlattenedFieldMapper.CONTENT_TYPE)) {
                                fieldCount++;
                            }
                        }
                    }
                }
            }
        }

        listener.onResponse(new FlattenedFeatureSetUsage(available(), fieldCount));
    }
}
