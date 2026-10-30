package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_310;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(targets = "net.minecraft.class_4184", remap = false)
public abstract class BackgroundPlayerCameraMixin {
    @Inject(
            method = "method_19333",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void bsg$useSpatialThirdPersonForWorld(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != class_310.method_1551().field_1773.method_19418()) return;
        if (RenderContext.currentSpatialBBSScreen()
                && !SpatialGUIClient.getEffectiveFirstPersonMode()
                && (BackgroundWorldPass.active()
                        || (!RenderContext.spatialScreen()
                                && !RenderContext.bbsWorldCaptureActive())))
            cir.setReturnValue(true);
    }
}
