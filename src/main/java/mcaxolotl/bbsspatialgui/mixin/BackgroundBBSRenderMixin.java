package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.FilmCameraBridge;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;

import net.minecraft.class_757;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mchorse.bbs_mod.client.BBSRendering", remap = false)
public abstract class BackgroundBBSRenderMixin {
    @Inject(
            method = {"onWorldRenderBegin", "onWorldRenderEnd"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void bsg$avoidRepeatingEditorFrame(CallbackInfo ci) {
        if (BackgroundWorldPass.active()) ci.cancel();
    }

    @Inject(
            method = "canReplaceFramebuffer",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void bsg$nativeWorldResolution(CallbackInfoReturnable<Boolean> cir) {
        if (BackgroundWorldPass.active() || FilmCameraBridge.nativeDimensions())
            cir.setReturnValue(false);
    }

    @Inject(
            method = "getOrthoProjection",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void bsg$playerPerspective(
            class_757 renderer,
            Matrix4f projection,
            float minimum,
            CallbackInfoReturnable<Matrix4f> cir) {
        if (BackgroundWorldPass.active()) cir.setReturnValue(projection);
    }
}
