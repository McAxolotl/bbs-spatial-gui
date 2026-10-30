package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_310;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_276", remap = false)
public abstract class GuiFramebufferRestoreMixin {
    @Inject(
            method = "method_1235",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void bsg$keepRestoredGuiTarget(boolean updateViewport, CallbackInfo ci) {
        if (!RenderContext.spatialScreen()) return;
        class_310 client = class_310.method_1551();
        if ((Object) this != client.method_1522()) return;

        ci.cancel();
        int target = RenderContext.spatialGuiFramebuffer();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target);
        if (updateViewport) {
            int[] viewport = RenderContext.spatialGuiViewport();
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }
}
