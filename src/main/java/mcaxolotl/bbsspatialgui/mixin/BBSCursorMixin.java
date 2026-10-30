package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.cursor.CursorState;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "mchorse.bbs_mod.graphics.window.Window", remap = false)
public abstract class BBSCursorMixin {
    @Redirect(
            method = "setStandardCursor",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursor(JJ)V"),
            require = 0,
            remap = false)
    private static void bsg$selectedCursor(long window, long cursor, int shape) {
        GLFW.glfwSetCursor(window, cursor);
        CursorState.selected(shape, cursor != 0);
    }
}
