/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.security.rest.action.rolemapping;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.core.security.action.rolemapping.PutRoleMappingRequestBuilder;
import org.elasticsearch.xpack.core.security.action.rolemapping.PutRoleMappingResponse;
import org.elasticsearch.xpack.core.security.client.SecurityClient;
import org.elasticsearch.xpack.security.rest.action.SecurityBaseRestHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

/**
 * Rest endpoint to add a role-mapping to the native store
 *
 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class RestPutRoleMappingAction extends SecurityBaseRestHandler {

    public RestPutRoleMappingAction(Settings settings, XPackLicenseState licenseState) {
        super(settings, licenseState);
    }

    @Override
    public List<Route> routes() {
        return Collections.emptyList();
    }

    @Override
    public List<ReplacedRoute> replacedRoutes() {
        // TODO: remove deprecated endpoint in 8.0.0
        return Collections.unmodifiableList(Arrays.asList(
            new ReplacedRoute(POST, "/_security/role_mapping/{name}", POST, "/_xpack/security/role_mapping/{name}"),
            new ReplacedRoute(PUT, "/_security/role_mapping/{name}", PUT, "/_xpack/security/role_mapping/{name}")
        ));
    }

    @Override
    public String getName() {
        return "security_put_role_mappings_action";
    }

    @Override
    public RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) throws IOException {
        final String name = request.param("name");
        PutRoleMappingRequestBuilder requestBuilder = new SecurityClient(client)
                .preparePutRoleMapping(name, request.requiredContent(), request.getXContentType())
                .setRefreshPolicy(request.param("refresh"));
        return channel -> requestBuilder.execute(
                new RestBuilderListener<PutRoleMappingResponse>(channel) {
                    @Override
                    public RestResponse buildResponse(PutRoleMappingResponse response, XContentBuilder builder) throws Exception {
                        return new BytesRestResponse(RestStatus.OK, builder.startObject().field("role_mapping", response).endObject());
                    }
                });
    }
}
