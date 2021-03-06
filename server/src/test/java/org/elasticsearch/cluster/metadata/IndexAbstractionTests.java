/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.Version;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.test.ESTestCase;

import java.util.Objects;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

public class IndexAbstractionTests extends ESTestCase {

    public void testHiddenAliasValidation() {
        final String hiddenAliasName = "hidden_alias";
        AliasMetadata hiddenAliasMetadata = new AliasMetadata.Builder(hiddenAliasName).isHidden(true).build();

        IndexMetadata hidden1 = buildIndexWithAlias("hidden1", hiddenAliasName, true, Version.CURRENT, false);
        IndexMetadata hidden2 = buildIndexWithAlias("hidden2", hiddenAliasName, true, Version.CURRENT, false);
        IndexMetadata hidden3 = buildIndexWithAlias("hidden3", hiddenAliasName, true, Version.CURRENT, false);

        IndexMetadata indexWithNonHiddenAlias = buildIndexWithAlias("nonhidden1", hiddenAliasName, false, Version.CURRENT, false);
        IndexMetadata indexWithUnspecifiedAlias = buildIndexWithAlias("nonhidden2", hiddenAliasName, null, Version.CURRENT, false);

        {
            IndexAbstraction.Alias allHidden = new IndexAbstraction.Alias(hiddenAliasMetadata, hidden1);
            allHidden.addIndex(hidden2);
            allHidden.addIndex(hidden3);
            allHidden.computeAndValidateAliasProperties(); // Should be ok
        }

        {
            IndexAbstraction.Alias allVisible;
            if (randomBoolean()) {
                allVisible = new IndexAbstraction.Alias(hiddenAliasMetadata, indexWithNonHiddenAlias);
                allVisible.addIndex(indexWithUnspecifiedAlias);
            } else {
                allVisible = new IndexAbstraction.Alias(hiddenAliasMetadata, indexWithUnspecifiedAlias);
                allVisible.addIndex(indexWithNonHiddenAlias);
            }

            allVisible.computeAndValidateAliasProperties(); // Should be ok
        }

        {
            IndexAbstraction.Alias oneNonHidden = new IndexAbstraction.Alias(hiddenAliasMetadata, hidden1);
            oneNonHidden.addIndex(hidden2);
            oneNonHidden.addIndex(hidden3);
            oneNonHidden.addIndex(indexWithNonHiddenAlias);
            IllegalStateException exception = expectThrows(IllegalStateException.class,
                () -> oneNonHidden.computeAndValidateAliasProperties());
            assertThat(exception.getMessage(), containsString("alias [" + hiddenAliasName +
                "] has is_hidden set to true on indices ["));
            assertThat(exception.getMessage(), allOf(containsString(hidden1.getIndex().getName()),
                containsString(hidden2.getIndex().getName()),
                containsString(hidden3.getIndex().getName())));
            assertThat(exception.getMessage(), containsString("but does not have is_hidden set to true on indices [" +
                indexWithNonHiddenAlias.getIndex().getName() + "]; alias must have the same is_hidden setting on all indices"));
        }

        {
            IndexAbstraction.Alias oneUnspecified = new IndexAbstraction.Alias(hiddenAliasMetadata, hidden1);
            oneUnspecified.addIndex(hidden2);
            oneUnspecified.addIndex(hidden3);
            oneUnspecified.addIndex(indexWithUnspecifiedAlias);
            IllegalStateException exception = expectThrows(IllegalStateException.class,
                () -> oneUnspecified.computeAndValidateAliasProperties());
            assertThat(exception.getMessage(), containsString("alias [" + hiddenAliasName +
                "] has is_hidden set to true on indices ["));
            assertThat(exception.getMessage(), allOf(containsString(hidden1.getIndex().getName()),
                containsString(hidden2.getIndex().getName()),
                containsString(hidden3.getIndex().getName())));
            assertThat(exception.getMessage(), containsString("but does not have is_hidden set to true on indices [" +
                indexWithUnspecifiedAlias.getIndex().getName() + "]; alias must have the same is_hidden setting on all indices"));
        }

        {
            IndexAbstraction.Alias mostlyVisibleOneHidden;
            if (randomBoolean()) {
                mostlyVisibleOneHidden = new IndexAbstraction.Alias(hiddenAliasMetadata, indexWithNonHiddenAlias);
                mostlyVisibleOneHidden.addIndex(indexWithUnspecifiedAlias);
            } else {
                mostlyVisibleOneHidden = new IndexAbstraction.Alias(hiddenAliasMetadata, indexWithUnspecifiedAlias);
                mostlyVisibleOneHidden.addIndex(indexWithNonHiddenAlias);
            }
            final IndexMetadata hiddenIndex = randomFrom(hidden1, hidden2, hidden3);
            mostlyVisibleOneHidden.addIndex(hiddenIndex);
            IllegalStateException exception = expectThrows(IllegalStateException.class,
                () -> mostlyVisibleOneHidden.computeAndValidateAliasProperties());
            assertThat(exception.getMessage(), containsString("alias [" + hiddenAliasName + "] has is_hidden set to true on " +
                "indices [" + hiddenIndex.getIndex().getName() + "] but does not have is_hidden set to true on indices ["));
            assertThat(exception.getMessage(), allOf(containsString(indexWithUnspecifiedAlias.getIndex().getName()),
                containsString(indexWithNonHiddenAlias.getIndex().getName())));
            assertThat(exception.getMessage(), containsString("but does not have is_hidden set to true on indices ["));
        }
    }

    private IndexMetadata buildIndexWithAlias(String indexName, String aliasName, @Nullable Boolean aliasIsHidden,
                                              Version indexCreationVersion, boolean isSystem) {
        final AliasMetadata.Builder aliasMetadata = new AliasMetadata.Builder(aliasName);
        if (Objects.nonNull(aliasIsHidden) || randomBoolean()) {
            aliasMetadata.isHidden(aliasIsHidden);
        }
        return new IndexMetadata.Builder(indexName)
            .settings(settings(indexCreationVersion))
            .system(isSystem)
            .numberOfShards(1)
            .numberOfReplicas(0)
            .putAlias(aliasMetadata)
            .build();
    }

}
