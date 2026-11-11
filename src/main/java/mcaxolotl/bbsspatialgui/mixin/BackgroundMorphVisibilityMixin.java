package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_742;

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
        // The spatial background pass is the only place the player is drawn behind the panel;
        // BBS hides the operator while editing films, but the third-person view still needs the
        // player there. Monitor captures are handled separately below.
        return !BackgroundWorldPass.active() && hidePlayer;
    }

    /**
     * BBS bakes its film monitor and model block preview from the custom framebuffer it renders
     * while {@code toggleFramebuffer} is set. Spatial GUI's third-person mode (and morph forms)
     * make the local player part of that frame, so the operator ends up inside the monitor image.
     * Only this capture pass is suppressed: the spatial background pass and the normal view keep
     * rendering the player so third-person player rendering still works.
     */
    @Inject(method = "renderPlayer", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void bsg$hideOperatorInsideMonitor(
            class_742 player,
            float yaw,
            float delta,
            class_4587 matrices,
            class_4597 consumers,
            int light,
            CallbackInfoReturnable<Boolean> cir) {
        if (!RenderContext.bbsWorldCaptureActive() || !RenderContext.currentSpatialBBSScreen()) return;
        if (player != class_310.method_1551().field_1724) return;
        cir.setReturnValue(true);
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
