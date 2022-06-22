package com.spr.utils;

import org.elasticsearch.common.settings.Setting;

public class PerfTrackerSettings {
    public static final Setting<Integer> CLUSTER_VERBOSITY_LEVEL =
        Setting.intSetting("cluster.perf_verbosity_level", 0, 0, 3, Setting.Property.Dynamic,
                            Setting.Property.NodeScope);

    public static final Setting<Integer> INDEX_VERBOSITY_LEVEL =
        Setting.intSetting("index.perf_verbosity_level", 0, 0, 3, Setting.Property.Dynamic,
                            Setting.Property.IndexScope);

    public static final class VerbosityLevels {
        public static final int Level_0 = 0;
        public static final int Level_1 = 1;
        public static final int Level_2 = 2;
        public static final int Level_3 = 3;
    }
}
