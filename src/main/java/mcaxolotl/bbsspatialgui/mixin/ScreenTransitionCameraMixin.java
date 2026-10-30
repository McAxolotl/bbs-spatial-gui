package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.ScreenTransitionCamera;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_243;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.tastytrash.spatialGUI.render.SpatialGUIRenderer", remap = false)
public abstract class ScreenTransitionCameraMixin {
    @Inject(
            method = "getCameraStartPos",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void bsg$worldTransitionOrigin(CallbackInfoReturnable<class_243> cir) {
        if (ScreenTransitionCamera.position() != null && RenderContext.currentSpatialBBSScreen()) {
            cir.setReturnValue(ScreenTransitionCamera.position());
        }
    }

    @Inject(
            method = "getCameraStartYRot",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void bsg$worldTransitionYaw(CallbackInfoReturnable<Float> cir) {
        if (ScreenTransitionCamera.position() != null && RenderContext.currentSpatialBBSScreen()) {
            cir.setReturnValue(ScreenTransitionCamera.yaw());
        }
    }
}
