package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.PlayerPerspectiveSession;

import net.minecraft.class_5498;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.tastytrash.spatialGUI.client.SpatialGUIClient", remap = false)
public abstract class SpatialOriginalPerspectiveMixin {
    @Inject(
            method = "getWasThirdPersonCamera",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$originalWasThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        var original = PlayerPerspectiveSession.originalForSpatial();
        if (original != null) cir.setReturnValue(original != class_5498.field_26664);
    }

    @Inject(
            method = "getOriginalCameraType",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$originalCameraType(CallbackInfoReturnable<class_5498> cir) {
        var original = PlayerPerspectiveSession.originalForSpatial();
        if (original != null) cir.setReturnValue(original);
    }
}
