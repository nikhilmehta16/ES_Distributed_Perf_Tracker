/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.admin.cluster.snapshots.get;

import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.snapshots.SnapshotId;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.snapshots.SnapshotInfoTests;
import org.elasticsearch.snapshots.SnapshotShardFailure;
import org.elasticsearch.test.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class GetSnapshotsResponseTests extends AbstractSerializingTestCase<GetSnapshotsResponse> {

    @Override
    protected GetSnapshotsResponse doParseInstance(XContentParser parser) throws IOException {
        return GetSnapshotsResponse.fromXContent(parser);
    }

    @Override
    protected GetSnapshotsResponse createTestInstance() {
        ArrayList<SnapshotInfo> snapshots = new ArrayList<>();
        for (int i = 0; i < randomIntBetween(5, 10); ++i) {
            SnapshotId snapshotId = new SnapshotId("snapshot " + i, UUIDs.base64UUID());
            String reason = randomBoolean() ? null : "reason";
            ShardId shardId = new ShardId("index", UUIDs.base64UUID(), 2);
            List<SnapshotShardFailure> shardFailures = Collections.singletonList(new SnapshotShardFailure("node-id", shardId, "reason"));
            snapshots.add(new SnapshotInfo(snapshotId, Arrays.asList("index1", "index2"), Collections.singletonList("ds"),
                System.currentTimeMillis(), reason, System.currentTimeMillis(), randomIntBetween(2, 3), shardFailures, randomBoolean(),
                SnapshotInfoTests.randomUserMetadata()));
        }
        return new GetSnapshotsResponse(snapshots);
    }

    @Override
    protected Writeable.Reader<GetSnapshotsResponse> instanceReader() {
        return GetSnapshotsResponse::new;
    }

    @Override
    protected Predicate<String> getRandomFieldsExcludeFilter() {
        // Don't inject random fields into the custom snapshot metadata, because the metadata map is equality-checked after doing a
        // round-trip through xContent serialization/deserialization. Even though the rest of the object ignores unknown fields,
        // `metadata` doesn't ignore unknown fields (it just includes them in the parsed object, because the keys are arbitrary), so any
        // new fields added to the metadata before it gets deserialized that weren't in the serialized version will cause the equality
        // check to fail.

        // The actual fields are nested in an array, so this regex matches fields with names of the form `snapshots.3.metadata`
        return Pattern.compile("snapshots\\.\\d+\\.metadata.*").asPredicate();
    }
}
