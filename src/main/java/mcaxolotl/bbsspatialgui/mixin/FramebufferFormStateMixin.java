package mcaxolotl.bbsspatialgui.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mcaxolotl.bbsspatialgui.render.RenderContext;
import mcaxolotl.bbsspatialgui.render.RenderPassScope;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "mchorse.bbs_mod.forms.renderers.FramebufferFormRenderer", remap = false)
public abstract class FramebufferFormStateMixin {
    @WrapOperation(
            method = "renderBodyParts",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lmchorse/bbs_mod/forms/renderers/FramebufferFormRenderer;renderFramebuffer(Lmchorse/bbs_mod/forms/renderers/FormRenderingContext;Lmchorse/bbs_mod/graphics/Framebuffer;)V"),
            require = 1,
            remap = false)
    private void bsg$scopedForm(
            @Coerce Object renderer,
            @Coerce Object context,
            @Coerce Object framebuffer,
            Operation<Void> original) {
        try (RenderPassScope scope = RenderContext.spatialScreen() ? new RenderPassScope() : null) {
            original.call(renderer, context, framebuffer);
        }
    }
}
