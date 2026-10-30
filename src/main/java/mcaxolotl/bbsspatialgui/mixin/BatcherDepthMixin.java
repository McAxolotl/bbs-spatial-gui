package mcaxolotl.bbsspatialgui.mixin;

import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_286;
import net.minecraft.class_287;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D", remap = false)
public abstract class BatcherDepthMixin {
    @Redirect(
            method = {
                "flushBatch",
                "box",
                "splitBox",
                "dropShadow",
                "dropCircleShadow",
                "texturedBox",
                "texturedArea"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/class_286;method_43433(Lnet/minecraft/class_287$class_7433;)V"),
            require = 0,
            remap = false)
    private void bsg$paintUiInSubmissionOrder(class_287.class_7433 buffer) {
        if (!RenderContext.drawingSpatialGuiTarget()) {
            class_286.method_43433(buffer);
            return;
        }
        int function = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            RenderSystem.depthMask(false);
            class_286.method_43433(buffer);
        } finally {
            RenderSystem.depthFunc(function);
            RenderSystem.depthMask(write);
        }
    }
}
