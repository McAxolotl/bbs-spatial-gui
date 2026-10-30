package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.FilmCameraBridge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(targets = "net.minecraft.class_4184", priority = 1100, remap = false)
public abstract class CameraPitchTransitionMixin {
    @Redirect(
            method = "applyCameraUpdate",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lorg/tastytrash/spatialGUI/client/SpatialGUIClient;getWasThirdPersonCamera()Z"),
            require = 1,
            remap = false)
    private boolean bsg$interpolatePitchFromEitherWorldPerspective() {
        return SpatialGUIClient.getWasThirdPersonCamera() || FilmCameraBridge.nativeDimensions();
    }
}
