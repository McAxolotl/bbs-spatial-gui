package mcaxolotl.bbsspatialgui.render;

public final class ScreenPolicy {
    private static final String BBS_SCREEN = "mchorse.bbs_mod.ui.framework.UIScreen";

    public static boolean excluded(String screenClass, boolean enabled) {
        return !enabled && BBS_SCREEN.equals(screenClass);
    }

    public static boolean included(String screenClass, boolean enabled, boolean listed) {
        return listed || enabled && BBS_SCREEN.equals(screenClass);
    }

    private ScreenPolicy() {}
}
