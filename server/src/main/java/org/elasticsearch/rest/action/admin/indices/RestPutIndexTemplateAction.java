/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.rest.action.admin.indices;

import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestPutIndexTemplateAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestPutIndexTemplateAction.class);
    public static final String TYPES_DEPRECATION_MESSAGE = "[types removal]" +
            " Specifying include_type_name in put index template requests is deprecated."+
            " The parameter will be removed in the next major version.";

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(
            new Route(POST, "/_template/{name}"),
            new Route(PUT, "/_template/{name}")));
    }

    @Override
    public String getName() {
        return "put_index_template_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        boolean includeTypeName = request.paramAsBoolean(INCLUDE_TYPE_NAME_PARAMETER, DEFAULT_INCLUDE_TYPE_NAME_POLICY);

        PutIndexTemplateRequest putRequest = new PutIndexTemplateRequest(request.param("name"));
        if (request.hasParam(INCLUDE_TYPE_NAME_PARAMETER)) {
            deprecationLogger.deprecate("put_index_template_with_types", TYPES_DEPRECATION_MESSAGE);
        }
        if (request.hasParam("template")) {
            deprecationLogger.deprecate("put_index_template_deprecated_parameter",
                "Deprecated parameter [template] used, replaced by [index_patterns]");
            putRequest.patterns(Collections.singletonList(request.param("template")));
        } else {
            putRequest.patterns(Arrays.asList(request.paramAsStringArray("index_patterns", Strings.EMPTY_ARRAY)));
        }
        putRequest.order(request.paramAsInt("order", putRequest.order()));
        putRequest.masterNodeTimeout(request.paramAsTime("master_timeout", putRequest.masterNodeTimeout()));
        putRequest.create(request.paramAsBoolean("create", false));
        putRequest.cause(request.param("cause", ""));

        Map<String, Object> sourceAsMap = XContentHelper.convertToMap(request.requiredContent(), false,
            request.getXContentType()).v2();
        sourceAsMap = RestCreateIndexAction.prepareMappings(sourceAsMap, includeTypeName);
        putRequest.source(sourceAsMap);

        return channel -> client.admin().indices().putTemplate(putRequest, new RestToXContentListener<>(channel));
    }
}
