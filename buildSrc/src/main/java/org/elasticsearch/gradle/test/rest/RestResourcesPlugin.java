/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.gradle.test.rest;

import org.elasticsearch.gradle.VersionProperties;
import org.elasticsearch.gradle.info.BuildParams;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Map;

/**
 * <p>
 * Gradle plugin to help configure {@link CopyRestApiTask}'s and {@link CopyRestTestsTask} that copies the artifacts needed for the Rest API
 * spec and YAML based rest tests.
 * </p>
 * <strong>Rest API specification:</strong> <br>
 * When the {@link RestResourcesPlugin} has been applied the {@link CopyRestApiTask} will automatically copy the core Rest API specification
 * if there are any Rest YAML tests present in source, or copied from {@link CopyRestTestsTask} output. X-pack specs must be explicitly
 * declared to be copied.
 * <br>
 * <i>For example:</i>
 * <pre>
 * restResources {
 *   restApi {
 *     includeXpack 'enrich'
 *   }
 * }
 * </pre>
 * Will copy the entire core Rest API specifications (assuming the project has tests) and any of the the X-pack specs starting with enrich*.
 * It is recommended (but not required) to also explicitly declare which core specs your project depends on to help optimize the caching
 * behavior.
 * <i>For example:</i>
 * <pre>
 * restResources {
 *   restApi {
 *     includeCore 'index', 'cat'
 *     includeXpack 'enrich'
 *   }
 * }
 * </pre>
 * <br>
 * <strong>Rest YAML tests :</strong> <br>
 * When the {@link RestResourcesPlugin} has been applied the {@link CopyRestTestsTask} will copy the Rest YAML tests if explicitly
 * configured with `includeCore` or `includeXpack` through the `restResources.restTests` extension.
 * <i>For example:</i>
 * <pre>
 * restResources {
 *  restApi {
 *      includeXpack 'graph'
 *   }
 *   restTests {
 *     includeXpack 'graph'
 *   }
 * }
 * </pre>
 * Will copy any of the the x-pack tests that start with graph, and will copy the X-pack graph specification, as well as the full core
 * Rest API specification.
 *
 * Additionally you can specify which sourceSetName resources should be copied to. The default is the yamlRestTest source set.
 * @see CopyRestApiTask
 * @see CopyRestTestsTask
 */
public class RestResourcesPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "restResources";

    @Override
    public void apply(Project project) {
        RestResourcesExtension extension = project.getExtensions().create(EXTENSION_NAME, RestResourcesExtension.class);

        // tests
        Configuration testConfig = project.getConfigurations().create("restTestConfig");
        Configuration xpackTestConfig = project.getConfigurations().create("restXpackTestConfig");

        if (BuildParams.isInternal()) {
            // core
            Dependency restTestdependency = project.getDependencies()
                .project(Map.of("path", ":rest-api-spec", "configuration", "restTests"));
            project.getDependencies().add(testConfig.getName(), restTestdependency);
            // x-pack
            Dependency restXPackTestdependency = project.getDependencies()
                .project(Map.of("path", ":x-pack:plugin", "configuration", "restXpackTests"));
            project.getDependencies().add(xpackTestConfig.getName(), restXPackTestdependency);
        } else {
            Dependency dependency = project.getDependencies()
                .create("org.elasticsearch:rest-api-spec:" + VersionProperties.getElasticsearch());
            project.getDependencies().add(testConfig.getName(), dependency);
        }

        project.getConfigurations().create("restTests");
        project.getConfigurations().create("restXpackTests");
        Provider<CopyRestTestsTask> copyRestYamlTestTask = project.getTasks()
            .register("copyYamlTestsTask", CopyRestTestsTask.class, task -> {
                task.includeCore.set(extension.restTests.getIncludeCore());
                task.includeXpack.set(extension.restTests.getIncludeXpack());
                task.coreConfig = testConfig;
                task.sourceSetName = SourceSet.TEST_SOURCE_SET_NAME;
                task.dependsOn(testConfig);
                if (BuildParams.isInternal()) {
                    task.xpackConfig = xpackTestConfig;
                    task.dependsOn(task.xpackConfig);
                }
            });

        // api
        Configuration specConfig = project.getConfigurations().create("restSpec"); // name chosen for passivity
        Configuration xpackSpecConfig = project.getConfigurations().create("restXpackSpec");

        if (BuildParams.isInternal()) {
            Dependency restSpecDependency = project.getDependencies()
                .project(Map.of("path", ":rest-api-spec", "configuration", "restSpecs"));
            project.getDependencies().add(specConfig.getName(), restSpecDependency);
            Dependency restXpackSpecDependency = project.getDependencies()
                .project(Map.of("path", ":x-pack:plugin", "configuration", "restXpackSpecs"));
            project.getDependencies().add(xpackSpecConfig.getName(), restXpackSpecDependency);
        } else {
            Dependency dependency = project.getDependencies()
                .create("org.elasticsearch:rest-api-spec:" + VersionProperties.getElasticsearch());
            project.getDependencies().add(specConfig.getName(), dependency);
        }

        project.getConfigurations().create("restSpecs");
        project.getConfigurations().create("restXpackSpecs");

        Provider<CopyRestApiTask> copyRestYamlSpecTask = project.getTasks()
            .register("copyRestApiSpecsTask", CopyRestApiTask.class, task -> {
                task.includeCore.set(extension.restApi.getIncludeCore());
                task.includeXpack.set(extension.restApi.getIncludeXpack());
                task.dependsOn(copyRestYamlTestTask);
                task.coreConfig = specConfig;
                task.sourceSetName = SourceSet.TEST_SOURCE_SET_NAME;
                task.dependsOn(xpackSpecConfig);
                if (BuildParams.isInternal()) {
                    task.xpackConfig = xpackSpecConfig;
                    task.dependsOn(task.xpackConfig);
                }
            });

        project.afterEvaluate(p -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            SourceSet testSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME);
            if (testSourceSet != null) {
                project.getTasks().named(testSourceSet.getProcessResourcesTaskName()).configure(t -> t.dependsOn(copyRestYamlSpecTask));
            }
        });
    }
}
