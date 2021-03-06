/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal

import org.apache.commons.io.FileUtils
import org.elasticsearch.gradle.fixtures.AbstractGitAwareGradleFuncTest
import org.gradle.testkit.runner.TaskOutcome

class InternalDistributionBwcSetupPluginFuncTest extends AbstractGitAwareGradleFuncTest {

    def setup() {
        internalBuild()
        buildFile << """
            apply plugin: 'elasticsearch.internal-distribution-bwc-setup'
        """
    }

    def "builds distribution from branches via archives assemble"() {
        when:
        def result = gradleRunner(":distribution:bwc:bugfix:buildBwcDarwinTar",
                ":distribution:bwc:bugfix:buildBwcOssDarwinTar",
                "-DtestRemoteRepo=" + remoteGitRepo,
                "-Dbwc.remote=origin")
                .build()
        then:
        result.task(":distribution:bwc:bugfix:buildBwcDarwinTar").outcome == TaskOutcome.SUCCESS
        result.task(":distribution:bwc:bugfix:buildBwcOssDarwinTar").outcome == TaskOutcome.SUCCESS

        and: "assemble task triggered"
        result.output.contains("[8.0.1] > Task :distribution:archives:darwin-tar:assemble")
        result.output.contains("[8.0.1] > Task :distribution:archives:oss-darwin-tar:assemble")
    }

    def "bwc distribution archives can be resolved as bwc project artifact"() {
        setup:
        buildFile << """

        configurations {
            dists
        }

        dependencies {
            dists project(path: ":distribution:bwc:bugfix", configuration:"darwin-tar")
        }

        tasks.register("resolveDistributionArchive") {
            inputs.files(configurations.dists)
            doLast {
                configurations.dists.files.each {
                    println "distfile " + (it.absolutePath - project.rootDir.absolutePath)
                }
            }
        }
        """
        when:
        def result = gradleRunner(":resolveDistributionArchive",
                "-DtestRemoteRepo=" + remoteGitRepo,
                "-Dbwc.remote=origin")
                .build()
        then:
        result.task(":resolveDistributionArchive").outcome == TaskOutcome.SUCCESS
        result.task(":distribution:bwc:bugfix:buildBwcDarwinTar").outcome == TaskOutcome.SUCCESS

        and: "assemble task triggered"
        result.output.contains("[8.0.1] > Task :distribution:archives:darwin-tar:assemble")
        normalized(result.output)
                .contains("distfile /distribution/bwc/bugfix/build/bwc/checkout-8.0/distribution/archives/darwin-tar/" +
                        "build/distributions/elasticsearch-8.0.1-SNAPSHOT-darwin-x86_64.tar.gz")
    }

    def "bwc expanded distribution folder can be resolved as bwc project artifact"() {
        setup:
        buildFile << """

        configurations {
            expandedDist
        }

        dependencies {
            expandedDist project(path: ":distribution:bwc:bugfix", configuration:"expanded-darwin-tar")
        }

        tasks.register("resolveExpandedDistribution") {
            inputs.files(configurations.expandedDist)
            doLast {
                configurations.expandedDist.files.each {
                    println "distfile " + (it.absolutePath - project.rootDir.absolutePath)
                }
            }
        }
        """
        when:
        def result = gradleRunner(":resolveExpandedDistribution",
                "-DtestRemoteRepo=" + remoteGitRepo,
                "-Dbwc.remote=origin")
                .build()
        then:
        result.task(":resolveExpandedDistribution").outcome == TaskOutcome.SUCCESS
        result.task(":distribution:bwc:bugfix:buildBwcDarwinTar").outcome == TaskOutcome.SUCCESS

        and: "assemble task triggered"
        result.output.contains("[8.0.1] > Task :distribution:archives:darwin-tar:assemble")
        normalized(result.output)
                .contains("distfile /distribution/bwc/bugfix/build/bwc/checkout-8.0/" +
                        "distribution/archives/darwin-tar/build/install")
    }

    File setupGitRemote() {
        URL fakeRemote = getClass().getResource("fake_git/remote")
        File workingRemoteGit = new File(remoteRepoDirs.root, 'remote')
        FileUtils.copyDirectory(new File(fakeRemote.file), workingRemoteGit)
        fakeRemote.file + "/.git"
        gradleRunner(workingRemoteGit, "wrapper").build()
        execute("git init", workingRemoteGit)
        execute('git config user.email "build-tool@elastic.co"', workingRemoteGit)
        execute('git config user.name "Build tool"', workingRemoteGit)
        execute("git add .", workingRemoteGit)
        execute('git commit -m"Initial"', workingRemoteGit)
        execute("git checkout -b origin/8.0", workingRemoteGit)
        return workingRemoteGit;
    }
}
