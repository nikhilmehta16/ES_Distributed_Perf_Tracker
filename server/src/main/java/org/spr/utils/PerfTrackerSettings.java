
package org.spr.utils;

import org.elasticsearch.common.settings.Setting;


public class PerfTrackerSettings {

    public static final Setting<Integer> CLUSTER_VERBOSITY_LEVEL =
        Setting.intSetting("cluster.perf_verbosity_level", 0, 0, 4, Setting.Property.Dynamic,
                            Setting.Property.NodeScope);

    public static final Setting<Integer> INDEX_VERBOSITY_LEVEL =
        Setting.intSetting("index.perf_verbosity_level", 0, 0,4, Setting.Property.Dynamic,
                            Setting.Property.IndexScope);

}
