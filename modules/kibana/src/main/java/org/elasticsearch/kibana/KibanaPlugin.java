/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.kibana;

import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.index.reindex.RestDeleteByQueryAction;
import org.elasticsearch.indices.SystemIndexDescriptor;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SystemIndexPlugin;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.action.admin.indices.RestCreateIndexAction;
import org.elasticsearch.rest.action.admin.indices.RestGetAliasesAction;
import org.elasticsearch.rest.action.admin.indices.RestGetIndicesAction;
import org.elasticsearch.rest.action.admin.indices.RestIndexPutAliasAction;
import org.elasticsearch.rest.action.admin.indices.RestRefreshAction;
import org.elasticsearch.rest.action.admin.indices.RestUpdateSettingsAction;
import org.elasticsearch.rest.action.document.RestBulkAction;
import org.elasticsearch.rest.action.document.RestDeleteAction;
import org.elasticsearch.rest.action.document.RestGetAction;
import org.elasticsearch.rest.action.document.RestIndexAction;
import org.elasticsearch.rest.action.document.RestIndexAction.AutoIdHandler;
import org.elasticsearch.rest.action.document.RestIndexAction.CreateHandler;
import org.elasticsearch.rest.action.document.RestMultiGetAction;
import org.elasticsearch.rest.action.document.RestUpdateAction;
import org.elasticsearch.rest.action.search.RestClearScrollAction;
import org.elasticsearch.rest.action.search.RestSearchAction;
import org.elasticsearch.rest.action.search.RestSearchScrollAction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public class KibanaPlugin extends Plugin implements SystemIndexPlugin {

    public static final Setting<List<String>> KIBANA_INDEX_NAMES_SETTING = Setting.listSetting(
        "kibana.system_indices",
        unmodifiableList(Arrays.asList(".kibana", ".kibana_*", ".reporting-*", ".apm-agent-configuration", ".apm-custom-link")),
        Function.identity(),
        Property.NodeScope
    );

    @Override
    public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
        return unmodifiableList(
            KIBANA_INDEX_NAMES_SETTING.get(settings)
                .stream()
                .map(pattern -> new SystemIndexDescriptor(pattern, "System index used by kibana"))
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        // TODO need to figure out what subset of system indices Kibana should have access to via these APIs
        return unmodifiableList(
            Arrays.asList(
                // Based on https://github.com/elastic/kibana/issues/49764
                // apis needed to perform migrations... ideally these will go away
                new KibanaWrappedRestHandler(new RestCreateIndexAction()),
                new KibanaWrappedRestHandler(new RestGetAliasesAction()),
                new KibanaWrappedRestHandler(new RestIndexPutAliasAction()),
                new KibanaWrappedRestHandler(new RestRefreshAction()),

                // apis needed to access saved objects
                new KibanaWrappedRestHandler(new RestGetAction()),
                new KibanaWrappedRestHandler(new RestMultiGetAction(settings)),
                new KibanaWrappedRestHandler(new RestSearchAction()),
                new KibanaWrappedRestHandler(new RestBulkAction(settings)),
                new KibanaWrappedRestHandler(new RestDeleteAction()),
                new KibanaWrappedRestHandler(new RestDeleteByQueryAction()),

                // api used for testing
                new KibanaWrappedRestHandler(new RestUpdateSettingsAction()),

                // apis used specifically by reporting
                new KibanaWrappedRestHandler(new RestGetIndicesAction()),
                new KibanaWrappedRestHandler(new RestIndexAction()),
                new KibanaWrappedRestHandler(new CreateHandler()),
                new KibanaWrappedRestHandler(new AutoIdHandler(nodesInCluster)),
                new KibanaWrappedRestHandler(new RestUpdateAction()),
                new KibanaWrappedRestHandler(new RestSearchScrollAction()),
                new KibanaWrappedRestHandler(new RestClearScrollAction())
            )
        );

    }

    @Override
    public List<Setting<?>> getSettings() {
        return Collections.singletonList(KIBANA_INDEX_NAMES_SETTING);
    }

    static class KibanaWrappedRestHandler extends BaseRestHandler.Wrapper {

        KibanaWrappedRestHandler(BaseRestHandler delegate) {
            super(delegate);
        }

        @Override
        public String getName() {
            return "kibana_" + super.getName();
        }

        @Override
        public boolean allowSystemIndexAccessByDefault() {
            return true;
        }

        @Override
        public List<Route> routes() {
            return unmodifiableList(
                super.routes().stream()
                    .map(route -> new Route(route.getMethod(), "/_kibana" + route.getPath()))
                    .collect(Collectors.toList())
            );
        }
    }
}
