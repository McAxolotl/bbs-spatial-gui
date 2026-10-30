package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.FilmCurveSnapshot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mchorse.bbs_mod.client.BBSRendering", remap = false)
public abstract class FilmCurveSnapshotMixin {
    @Inject(
            method = "getTimeOfDay",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$snapshotTime(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(FilmCurveSnapshot.time(cir.getReturnValue()));
    }

    @Inject(
            method = "getBrightness",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$snapshotBrightness(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(FilmCurveSnapshot.brightness(cir.getReturnValue()));
    }

    @Inject(
            method = "getWeather",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$snapshotWeather(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(FilmCurveSnapshot.weather(cir.getReturnValue()));
    }

    @Inject(
            method = "getSunHorizontalRotation",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$snapshotSunRotation(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(FilmCurveSnapshot.sunRotation(cir.getReturnValue()));
    }

    @Inject(
            method = "getChromaSkyColorArgb",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$snapshotSkyColor(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(FilmCurveSnapshot.skyColor(cir.getReturnValue()));
    }
}
