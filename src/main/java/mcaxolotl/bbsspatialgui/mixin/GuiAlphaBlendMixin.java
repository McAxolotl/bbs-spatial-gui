package mcaxolotl.bbsspatialgui.mixin;

import com.mojang.blaze3d.platform.GlStateManager;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlStateManager.class, remap = false)
public abstract class GuiAlphaBlendMixin {
    @Inject(method = "_blendFunc", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void bsg$separateGuiAlpha(int source, int destination, CallbackInfo ci) {
        if (RenderContext.drawingSpatialGuiTarget()) {
            GlStateManager._blendFuncSeparate(
                    source, destination, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = {"_blendFuncSeparate", "glBlendFuncSeparate"},
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2,
            require = 0,
            remap = false)
    private static int bsg$sourceCoverage(int original) {
        return RenderContext.drawingSpatialGuiTarget() ? GL11.GL_ONE : original;
    }

    @ModifyVariable(
            method = {"_blendFuncSeparate", "glBlendFuncSeparate"},
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 3,
            require = 0,
            remap = false)
    private static int bsg$destinationCoverage(int original) {
        return RenderContext.drawingSpatialGuiTarget() ? GL11.GL_ONE_MINUS_SRC_ALPHA : original;
    }
}
