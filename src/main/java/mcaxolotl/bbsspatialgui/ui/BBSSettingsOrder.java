package mcaxolotl.bbsspatialgui.ui;

import mcaxolotl.bbsspatialgui.BBSOptions;

import mchorse.bbs_mod.settings.Settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class BBSSettingsOrder {
    public static List<Settings> arrange(Collection<Settings> settings) {
        List<Settings> ordered = new ArrayList<>(settings);
        Settings base = null, bridge = null;
        for (Settings module : ordered) {
            if (module.getId().equals("bbs")) base = module;
            if (module.getId().equals(BBSOptions.ID)) bridge = module;
        }
        if (base != null && bridge != null) {
            ordered.remove(bridge);
            ordered.add(ordered.indexOf(base) + 1, bridge);
        }
        return ordered;
    }

    private BBSSettingsOrder() {}
}
