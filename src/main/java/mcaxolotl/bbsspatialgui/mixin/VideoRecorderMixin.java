package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.capture.RecordingBridge;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(targets = "mchorse.bbs_mod.utils.VideoRecorder", remap = false)
public abstract class VideoRecorderMixin {
    @Shadow private int textureId;
    @Shadow private boolean recording;
    @Unique private boolean bsg$alreadyRecording;

    @Inject(method = "startRecording", at = @At("HEAD"), require = 1, remap = false)
    private void bsg$rememberStart(
            String name, File audio, int texture, int width, int height, CallbackInfo ci) {
        bsg$alreadyRecording = recording;
    }

    @Inject(method = "startRecording", at = @At("RETURN"), require = 1, remap = false)
    private void bsg$fixedOutput(
            String name, File audio, int texture, int width, int height, CallbackInfo ci) {
        if (!bsg$alreadyRecording && recording)
            textureId = RecordingBridge.begin(this, texture, width, height);
    }

    @Inject(
            method = "recordFrame",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void bsg$completedFrameOnly(CallbackInfo ci) {
        if (RecordingBridge.skipVideo(this)) ci.cancel();
    }

    @Inject(method = "stopRecording(Z)V", at = @At("HEAD"), require = 1, remap = false)
    private void bsg$flushLastFrame(boolean effects, CallbackInfo ci) {
        RecordingBridge.beforeStop(this);
    }

    @Inject(method = "stopRecording(Z)V", at = @At("RETURN"), require = 1, remap = false)
    private void bsg$releaseOutput(boolean effects, CallbackInfo ci) {
        RecordingBridge.close(this);
    }
}
