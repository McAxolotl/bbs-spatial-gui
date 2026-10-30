package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.CameraState;
import mcaxolotl.bbsspatialgui.camera.FilmCameraBridge;
import mcaxolotl.bbsspatialgui.camera.ScreenTransitionCamera;
import mcaxolotl.bbsspatialgui.render.FilmCurveState;

import net.minecraft.class_310;
import net.minecraft.class_4184;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_4184", priority = 1100, remap = false)
public abstract class CameraStateMixin {
    @Shadow
    protected abstract void method_19325(float yaw, float pitch);

    @Shadow
    protected abstract void method_19327(double x, double y, double z);

    @Inject(
            method = "method_19321",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void bsg$reuseSpatialUpdate(CallbackInfo ci) {
        var camera = (class_4184) (Object) this;
        if (camera != class_310.method_1551().field_1773.method_19418()) return;
        if (FilmCameraBridge.replaySpatialCamera(camera)) {
            ci.cancel();
            return;
        }
        FilmCameraBridge.beginCamera();
    }

    @Inject(method = "method_19321", at = @At("TAIL"), require = 0, remap = false)
    private void bsg$observeCamera(CallbackInfo ci) {
        if ((Object) this == class_310.method_1551().field_1773.method_19418()) {
            FilmCameraBridge.Pose pose =
                    FilmCameraBridge.afterSpatialCamera((class_4184) (Object) this);
            ScreenTransitionCamera.observeVisibleCamera((class_4184) (Object) this);
            if (pose != null) {
                method_19327(pose.x(), pose.y(), pose.z());
                method_19325(pose.yaw(), pose.pitch());
            }
            CameraState.sample((class_4184) (Object) this);
            FilmCurveState.sample();
            FilmCameraBridge.endCamera();
        }
    }
}
