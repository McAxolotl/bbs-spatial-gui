package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.capture.RecordingBridge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mchorse.bbs_mod.audio.MinecraftSoundCapture", remap = false)
public abstract class RecordingAudioMixin {
    @Inject(
            method = "captureFrame",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void bsg$oneAudioFrame(CallbackInfo ci) {
        if (RecordingBridge.skipAudio()) ci.cancel();
    }
}
