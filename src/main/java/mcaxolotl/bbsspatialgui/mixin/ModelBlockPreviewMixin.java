package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.ModelBlockPreview;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel", remap = false)
public abstract class ModelBlockPreviewMixin {
    @Inject(method = "render", at = @At("HEAD"), require = 1, remap = false)
    private void bsg$drawModelBlockWorld(@Coerce Object context, CallbackInfo ci) {
        ModelBlockPreview.draw(context);
    }

    @Inject(method = "leaveEditing", at = @At("RETURN"), require = 1, remap = false)
    private void bsg$closeCapture(CallbackInfo ci) {
        ModelBlockPreview.close();
    }
}
