/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.util;

import org.gradle.api.UncheckedIOException;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class FileUtils {

    /**
     * Like {@link java.io.File#mkdirs()}, except throws an informative error if a dir cannot be created.
     *
     * @param dir The dir to create, including any non existent parent dirs.
     */
    public static void mkdirs(File dir) {
        dir = dir.getAbsoluteFile();
        if (dir.isDirectory()) {
            return;
        }

        if (dir.exists() && !dir.isDirectory()) {
            throw new UncheckedIOException(String.format("Cannot create directory '%s' as it already exists, but is not a directory", dir));
        }

        List<File> toCreate = new LinkedList<File>();
        File parent = dir.getParentFile();
        while (!parent.exists()) {
            toCreate.add(parent);
            parent = parent.getParentFile();
        }
        Collections.reverse(toCreate);
        for (File parentDirToCreate : toCreate) {
            if (parentDirToCreate.isDirectory()) {
                continue;
            }
            File parentDirToCreateParent = parentDirToCreate.getParentFile();
            if (!parentDirToCreateParent.isDirectory()) {
                throw new UncheckedIOException(
                    String.format(
                        "Cannot create parent directory '%s' when creating directory '%s' as '%s' is not a directory",
                        parentDirToCreate,
                        dir,
                        parentDirToCreateParent
                    )
                );
            }
            if (!parentDirToCreate.mkdir() && !parentDirToCreate.isDirectory()) {
                throw new UncheckedIOException(
                    String.format("Failed to create parent directory '%s' when creating directory '%s'", parentDirToCreate, dir)
                );
            }
        }
        if (!dir.mkdir() && !dir.isDirectory()) {
            throw new UncheckedIOException(String.format("Failed to create directory '%s'", dir));
        }
    }
}
