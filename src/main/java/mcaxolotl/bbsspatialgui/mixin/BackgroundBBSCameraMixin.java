package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mchorse.bbs_mod.camera.controller.CameraController", remap = false)
public abstract class BackgroundBBSCameraMixin {
    @Inject(method = "setup", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bsg$keepPreviewControllerState(CallbackInfo ci) {
        if (BackgroundWorldPass.active()) ci.cancel();
    }

    @Inject(method = "getCurrent", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bsg$playerWorldCamera(CallbackInfoReturnable<Object> cir) {
        if (BackgroundWorldPass.active()) cir.setReturnValue(null);
    }
}
