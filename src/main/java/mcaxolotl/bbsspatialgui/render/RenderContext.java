package mcaxolotl.bbsspatialgui.render;

import com.mojang.blaze3d.platform.GlStateManager;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public final class RenderContext {
    private static boolean active;
    private static int remaining;
    private static boolean spatialScreen;
    private static int guiFramebuffer;
    private static final int[] guiViewport = new int[4];
    private static int sourceRgb, destinationRgb, sourceAlpha, destinationAlpha;
    private static boolean apiFailureReported;
    private static java.lang.reflect.Method isActive;
    private static java.lang.reflect.Method crosshairMode;
    private static java.lang.reflect.Field worldCapture, customSize;
    private static boolean lateCrosshair;

    public static boolean lateCrosshair() {
        return lateCrosshair;
    }

    public static void lateCrosshair(boolean value) {
        lateCrosshair = value;
    }

    public static void begin(boolean enabled) {
        active = enabled;
        remaining = 16;
        spatialScreen = BBSSpatialGui.enabled() && spatialActive();
        if (spatialScreen) {
            guiFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, guiViewport);
            sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        }
    }

    public static void end() {
        boolean restoreBlend = spatialScreen;
        active = false;
        spatialScreen = false;
        if (restoreBlend)
            GlStateManager._blendFuncSeparate(
                    sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
    }

    public static boolean spatialScreen() {
        return spatialScreen;
    }

    public static int spatialGuiFramebuffer() {
        return guiFramebuffer;
    }

    public static int[] spatialGuiViewport() {
        return guiViewport;
    }

    public static boolean drawingSpatialGuiTarget() {
        return spatialScreen
                && GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == guiFramebuffer;
    }

    public static boolean nativeCrosshairRequested() {
        if (!currentSpatialBBSScreen()) return false;
        try {
            if (crosshairMode == null) {
                crosshairMode =
                        Class.forName("org.tastytrash.spatialGUI.api.SpatialGUIAPI")
                                .getMethod("isCrosshairModeActive");
            }
            return (boolean) crosshairMode.invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!apiFailureReported) {
                System.err.println("[BBS Spatial GUI] Cannot query Spatial crosshair mode: " + e);
                apiFailureReported = true;
            }
            return false;
        }
    }

    public static boolean currentSpatialBBSScreen() {
        var screen = net.minecraft.class_310.method_1551().field_1755;
        return BBSSpatialGui.enabled()
                && screen != null
                && "mchorse.bbs_mod.ui.framework.UIScreen".equals(screen.getClass().getName())
                && spatialActive();
    }

    public static boolean bbsCustomSizeActive() {
        if (!currentSpatialBBSScreen()) return false;
        try {
            if (customSize == null) {
                customSize =
                        Class.forName("mchorse.bbs_mod.client.BBSRendering")
                                .getDeclaredField("customSize");
                customSize.setAccessible(true);
            }
            return customSize.getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!apiFailureReported) {
                System.err.println(
                        "[BBS Spatial GUI] Cannot query BBS film framebuffer mode: " + e);
                apiFailureReported = true;
            }
            return false;
        }
    }

    public static boolean bbsWorldCaptureActive() {
        if (!currentSpatialBBSScreen()) return false;
        try {
            if (worldCapture == null) {
                worldCapture =
                        Class.forName("mchorse.bbs_mod.client.BBSRendering")
                                .getDeclaredField("toggleFramebuffer");
                worldCapture.setAccessible(true);
            }
            return worldCapture.getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!apiFailureReported) {
                System.err.println(
                        "[BBS Spatial GUI] Cannot query BBS world framebuffer; render-order fix unavailable: "
                                + e);
                apiFailureReported = true;
            }
            return false;
        }
    }

    private static boolean spatialActive() {
        try {
            if (isActive == null) {
                isActive =
                        Class.forName("org.tastytrash.spatialGUI.api.SpatialGUIAPI")
                                .getMethod("isActive");
            }
            return (boolean) isActive.invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!apiFailureReported) {
                System.err.println(
                        "[BBS Spatial GUI] Spatial GUI API unavailable; framebuffer fix disabled: "
                                + e);
                apiFailureReported = true;
            }
            return false;
        }
    }

    public static void snapshot(String label) {
        if (!active || remaining-- <= 0) return;
        System.out.println(
                "[BBS Spatial GUI] "
                        + label
                        + " drawFramebuffer="
                        + GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                        + " readFramebuffer="
                        + GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING));
    }

    private RenderContext() {}
}
