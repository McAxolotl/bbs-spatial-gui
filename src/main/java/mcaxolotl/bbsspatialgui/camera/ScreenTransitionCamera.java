package mcaxolotl.bbsspatialgui.camera;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_243;
import net.minecraft.class_310;

public final class ScreenTransitionCamera {
    private static Object world, screen;
    private static class_243 position, visiblePosition;
    private static float yaw, visibleYaw;
    private static int openingFrame, logSamples;

    public static void observeVisibleCamera(net.minecraft.class_4184 camera) {
        visiblePosition = camera.method_19326();
        visibleYaw = camera.method_19330();
    }

    public static void beginFrame() {
        var client = class_310.method_1551();
        if (client.field_1687 != world) {
            world = client.field_1687;
            position = null;
            visiblePosition = null;
            screen = null;
        }
        if (world == null) return;
        if (!RenderContext.currentSpatialBBSScreen()) {
            screen = null;
            return;
        }
        if (screen != client.field_1755) {
            var camera = client.field_1773.method_19418();
            position = visiblePosition != null ? visiblePosition : camera.method_19326();
            yaw = visiblePosition != null ? visibleYaw : camera.method_19330();
            screen = client.field_1755;
            openingFrame = 0;
        }
        if (BBSSpatialGui.debugLogging() && openingFrame < 6 && logSamples++ < 72) {
            var camera = client.field_1773.method_19418();
            System.out.println(
                    "[BBS Spatial GUI opening] frame="
                            + openingFrame
                            + " origin="
                            + position
                            + " originYaw="
                            + yaw
                            + " rawCamera="
                            + camera.method_19326()
                            + " customCapture="
                            + RenderContext.bbsCustomSizeActive());
        }
        openingFrame++;
    }

    public static class_243 position() {
        return position;
    }

    public static float yaw() {
        return yaw;
    }

    private ScreenTransitionCamera() {}
}
