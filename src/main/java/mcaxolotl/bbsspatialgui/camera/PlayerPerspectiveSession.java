package mcaxolotl.bbsspatialgui.camera;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;

import net.minecraft.class_310;
import net.minecraft.class_5498;

import org.tastytrash.spatialGUI.client.SpatialGUIClient;

public final class PlayerPerspectiveSession {
    private static class_5498 original;
    private static Object world;

    public static void begin(class_5498 perspective) {
        original = perspective;
        world = class_310.method_1551().field_1687;
    }

    public static class_5498 originalForSpatial() {
        if (original == null || !BBSSpatialGui.enabled() || !SpatialGUIClient.isEnabled())
            return null;
        var client = class_310.method_1551();
        if (world == null || client.field_1687 != world) return null;
        var screen = client.field_1755;
        if (screen != null) {
            return screen.getClass().getName().equals("mchorse.bbs_mod.ui.framework.UIScreen")
                    ? original
                    : null;
        }
        var renderer = SpatialGUIClient.renderer();
        return renderer != null && renderer.isClosing() ? original : null;
    }

    private PlayerPerspectiveSession() {}
}
