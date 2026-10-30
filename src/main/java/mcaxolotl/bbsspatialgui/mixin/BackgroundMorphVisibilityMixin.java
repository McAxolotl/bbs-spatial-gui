package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mchorse.bbs_mod.client.renderer.MorphRenderer", remap = false)
public abstract class BackgroundMorphVisibilityMixin {
    @Shadow public static boolean hidePlayer;

    @Redirect(
            method = "renderPlayer",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lmchorse/bbs_mod/client/renderer/MorphRenderer;hidePlayer:Z"),
            require = 1,
            remap = false)
    private static boolean bsg$keepPlayerInBackground() {
        return !BackgroundWorldPass.active() && hidePlayer;
    }

    @Inject(method = "canRender", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void bsg$keepWorldMorph(CallbackInfoReturnable<Boolean> cir) {
        if (BackgroundWorldPass.active()
                || (RenderContext.currentSpatialBBSScreen()
                        && !RenderContext.spatialScreen()
                        && !RenderContext.bbsWorldCaptureActive())) {
            cir.setReturnValue(true);
        }
    }
}
