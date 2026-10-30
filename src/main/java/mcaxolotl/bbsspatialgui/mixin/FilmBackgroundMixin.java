package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mchorse.bbs_mod.client.BBSRendering", remap = false)
public abstract class FilmBackgroundMixin {
    @Inject(method = "onRenderBeforeScreen", at = @At("RETURN"), require = 0, remap = false)
    private static void bsg$renderPlayerWorld(CallbackInfo ci) {
        BackgroundWorldPass.renderAfterPreview();
    }
}
