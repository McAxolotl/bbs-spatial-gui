package mcaxolotl.bbsspatialgui.cursor;

import net.minecraft.class_310;

import org.lwjgl.glfw.GLFW;

public final class CursorState {
    private static volatile int bbsShape = GLFW.GLFW_ARROW_CURSOR;

    public static void selected(int shape, boolean handleExists) {
        bbsShape = handleExists ? shape : GLFW.GLFW_ARROW_CURSOR;
    }

    public static int standardShape(boolean bbsScreen) {
        return bbsScreen ? bbsShape : GLFW.GLFW_ARROW_CURSOR;
    }

    public static int shape() {
        var screen = class_310.method_1551().field_1755;
        return standardShape(
                screen != null
                        && screen.getClass()
                                .getName()
                                .equals("mchorse.bbs_mod.ui.framework.UIScreen"));
    }

    public static boolean visible(long window) {
        return GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_NORMAL;
    }

    public static CursorGlyphs.Sprite image(long window) {
        if (mcaxolotl.bbsspatialgui.BBSOptions.nativeCursor()
                && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE) {
            CursorGlyphs.Sprite nativeCursor = NativeCursor.capture();
            if (nativeCursor != null) return nativeCursor;
        }
        return CursorGlyphs.standard(shape());
    }

    private CursorState() {}
}
