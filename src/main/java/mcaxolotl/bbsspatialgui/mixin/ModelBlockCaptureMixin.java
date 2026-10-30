package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.ModelBlockPreview;

import net.minecraft.class_276;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mchorse.bbs_mod.client.BBSRendering", remap = false)
public abstract class ModelBlockCaptureMixin {
    @Inject(method = "onWorldRenderBegin", at = @At("HEAD"), require = 1, remap = false)
    private static void bsg$beginModelBlockCapture(CallbackInfo ci) {
        ModelBlockPreview.beforeWorld();
    }

    @Redirect(
            method = "toggleFramebuffer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_276;method_1237(II)V"),
            require = 1,
            remap = false)
    private static void bsg$keepPreviewInsidePanel(class_276 framebuffer, int width, int height) {
        if (!ModelBlockPreview.active()) framebuffer.method_1237(width, height);
    }
}
