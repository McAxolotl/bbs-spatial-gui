package mcaxolotl.bbsspatialgui.input;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;

import net.minecraft.class_310;

import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

public final class MouseCaptureRestore {
    private static boolean pending;
    private static Method isClosing;

    public static void schedule() {
        if (BBSSpatialGui.enabled()) pending = true;
    }

    public static void afterFrame() {
        if (!pending) return;
        var client = class_310.method_1551();
        if (client.field_1755 != null) {
            if (!client.field_1755
                    .getClass()
                    .getName()
                    .equals("mchorse.bbs_mod.ui.framework.UIScreen")) pending = false;
            return;
        }
        if (client.field_1687 == null || client.field_1724 == null) {
            pending = false;
            return;
        }
        if (!client.method_1569() || client.method_18506() != null) return;
        try {
            if (isClosing == null)
                isClosing =
                        Class.forName("org.tastytrash.spatialGUI.api.SpatialGUIAPI")
                                .getMethod("isClosingScreen");
            if ((boolean) isClosing.invoke(null)) return;
        } catch (ReflectiveOperationException | LinkageError e) {
            System.err.println(
                    "[BBS Spatial GUI] Cannot determine closing animation state; mouse restoration postponed to vanilla: "
                            + e);
            pending = false;
            return;
        }
        var mouse = client.field_1729;
        if (!mouse.method_1613()
                || GLFW.glfwGetInputMode(client.method_22683().method_4490(), GLFW.GLFW_CURSOR)
                        != GLFW.GLFW_CURSOR_DISABLED) {
            if (mouse.method_1613()) mouse.method_1610();
            mouse.method_1612();
            if (BBSSpatialGui.debugLogging())
                System.out.println("[BBS Spatial GUI] Restored mouse capture after BBS closed");
        }
        pending = false;
    }

    private MouseCaptureRestore() {}
}
