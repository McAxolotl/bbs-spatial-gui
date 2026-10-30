package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.tastytrash.spatialGUI.render.SpatialGUIRenderer", remap = false)
public abstract class SpatialRenderOrderMixin {
    @Unique private int bsg$deferredLogs;

    @Inject(
            method = "renderInWorldPost",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private void bsg$deferUntilWorldCaptureRestored(CallbackInfo ci) {
        if (RenderContext.bbsWorldCaptureActive()) {
            if (BBSSpatialGui.debugLogging() && bsg$deferredLogs++ < 3) {
                System.out.println(
                        "[BBS Spatial GUI] Deferred spatial panel until BBS world framebuffer is restored");
            }
            ci.cancel();
        }
    }
}
