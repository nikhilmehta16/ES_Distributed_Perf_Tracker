/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.repositories.url;

import org.elasticsearch.test.fixture.AbstractHttpFixture;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * This {@link URLFixture} exposes a filesystem directory over HTTP. It is used in repository-url
 * integration tests to expose a directory created by a regular FS repository.
 */
public class URLFixture extends AbstractHttpFixture {

    private final Path repositoryDir;

    /**
     * Creates a {@link URLFixture}
     */
    private URLFixture(final String workingDir, final String repositoryDir) {
        super(workingDir);
        this.repositoryDir = dir(repositoryDir);
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException("URLFixture <working directory> <repository directory>");
        }

        final URLFixture fixture = new URLFixture(args[0], args[1]);
        fixture.listen();
    }

    @Override
    protected AbstractHttpFixture.Response handle(final Request request) throws IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String path = request.getPath();
            if (path.length() > 0 && path.charAt(0) == '/') {
                path = path.substring(1);
            }

            Path normalizedRepositoryDir = repositoryDir.normalize();
            Path normalizedPath = normalizedRepositoryDir.resolve(path).normalize();

            if (normalizedPath.startsWith(normalizedRepositoryDir)) {
                if (Files.exists(normalizedPath) && Files.isReadable(normalizedPath) && Files.isRegularFile(normalizedPath)) {
                    byte[] content = Files.readAllBytes(normalizedPath);
                    final Map<String, String> headers = new HashMap<>(contentType("application/octet-stream"));
                    headers.put("Content-Length", String.valueOf(content.length));
                    return new Response(RestStatus.OK.getStatus(), headers, content);
                } else {
                    return new Response(RestStatus.NOT_FOUND.getStatus(), TEXT_PLAIN_CONTENT_TYPE, EMPTY_BYTE);
                }
            } else {
                return new Response(RestStatus.FORBIDDEN.getStatus(), TEXT_PLAIN_CONTENT_TYPE, EMPTY_BYTE);
            }
        }
        return null;
    }

    @SuppressForbidden(reason = "Paths#get is fine - we don't have environment here")
    private static Path dir(final String dir) {
        return Paths.get(dir);
    }
}
