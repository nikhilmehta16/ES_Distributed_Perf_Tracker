/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.elasticsearch.test.VersionUtils.randomVersion;

public class HumanReadableIndexSettingsTests extends ESTestCase {
    public void testHumanReadableSettings() {
        Version versionCreated = randomVersion(random());
        Version versionUpgraded = randomVersion(random());
        long created = System.currentTimeMillis();
        Settings testSettings = Settings.builder()
                .put(IndexMetadata.SETTING_VERSION_CREATED, versionCreated)
                .put(IndexMetadata.SETTING_VERSION_UPGRADED, versionUpgraded)
                .put(IndexMetadata.SETTING_CREATION_DATE, created)
                .build();

        Settings humanSettings = IndexMetadata.addHumanReadableSettings(testSettings);

        assertEquals(versionCreated.toString(), humanSettings.get(IndexMetadata.SETTING_VERSION_CREATED_STRING, null));
        assertEquals(versionUpgraded.toString(), humanSettings.get(IndexMetadata.SETTING_VERSION_UPGRADED_STRING, null));
        ZonedDateTime creationDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(created), ZoneOffset.UTC);
        assertEquals(creationDate.toString(), humanSettings.get(IndexMetadata.SETTING_CREATION_DATE_STRING, null));
    }
}
