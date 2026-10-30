package mcaxolotl.bbsspatialgui;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public final class BBSOptions {
    public static final String ID = "bbs_spatial_gui";
    private static ValueBoolean enabled,
            recordSpatialGui,
            recordMousePointer,
            useNativeCursor,
            debugLogging;

    public static void register(SettingsBuilder builder) {
        builder.category("general", Icons.FULLSCREEN);
        enabled = builder.getBoolean("enabled", true);
        builder.category("recording", Icons.VIDEO_CAMERA);
        recordSpatialGui = builder.getBoolean("recordSpatialGui", true);
        recordMousePointer = builder.getBoolean("recordMousePointer", true);
        useNativeCursor = builder.getBoolean("useNativeCursor", true);
        builder.category("advanced", Icons.CODE);
        debugLogging = builder.getBoolean("debugLogging", false);
    }

    public static boolean enabled(boolean fallback) {
        return enabled == null ? fallback : enabled.get();
    }

    public static boolean recording(boolean fallback) {
        return recordSpatialGui == null ? fallback : recordSpatialGui.get();
    }

    public static boolean recordPointer() {
        return recordMousePointer == null || recordMousePointer.get();
    }

    public static boolean nativeCursor() {
        return useNativeCursor == null || useNativeCursor.get();
    }

    public static boolean debug(boolean fallback) {
        return debugLogging == null ? fallback : debugLogging.get();
    }

    private BBSOptions() {}
}
