package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.FilmCameraBridge;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.tastytrash.spatialGUI.util.CameraUtil", remap = false)
public abstract class FilmPanelCameraMixin {
    @Inject(method = "yaw", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void bsg$virtualPanelYaw(CallbackInfoReturnable<Float> cir) {
        if (!BackgroundWorldPass.active() && FilmCameraBridge.active())
            cir.setReturnValue(FilmCameraBridge.panelYaw());
    }

    @Inject(method = "pitch", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void bsg$virtualPanelPitch(CallbackInfoReturnable<Float> cir) {
        if (!BackgroundWorldPass.active() && FilmCameraBridge.active())
            cir.setReturnValue(FilmCameraBridge.panelPitch());
    }

    @Inject(
            method = "getCurrentFov",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void bsg$panelFov(CallbackInfoReturnable<Float> cir) {
        if (!BackgroundWorldPass.active()
                && FilmCameraBridge.active()
                && BackgroundWorldPass.hasPanelFov()) {
            cir.setReturnValue(BackgroundWorldPass.panelFov());
        }
    }
}
