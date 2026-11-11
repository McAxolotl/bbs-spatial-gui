package mcaxolotl.bbsspatialgui.ui;

import mcaxolotl.bbsspatialgui.BBSOptions;
import mcaxolotl.bbsspatialgui.BBSSpatialGui;

import net.minecraft.class_310;

/**
 * Optional bridge that lets BBS's interface scale drive Spatial GUI's effective GUI scale for BBS
 * screens. Spatial GUI's layout, ortho projection and mouse mapping all consume the integer
 * effective scale, so BBS's fractional scale is rounded to the nearest supported step.
 */
public final class SpatialUiScale {
    private static final String BBS_SCREEN = "mchorse.bbs_mod.ui.framework.UIScreen";

    public static int effective(int original) {
        if (!BBSSpatialGui.enabled() || !BBSOptions.bbsUiScale()) return original;
        var client = class_310.method_1551();
        if (client == null || client.field_1755 == null) return original;
        if (!BBS_SCREEN.equals(client.field_1755.getClass().getName())) return original;
        float scale = mchorse.bbs_mod.BBSModClient.getCustomGUIScale();
        if (scale <= 0f) return original;
        return toIntegerScale(scale);
    }

    public static int toIntegerScale(float scale) {
        return Math.max(1, Math.min(6, Math.round(scale)));
    }

    private SpatialUiScale() {}
}
