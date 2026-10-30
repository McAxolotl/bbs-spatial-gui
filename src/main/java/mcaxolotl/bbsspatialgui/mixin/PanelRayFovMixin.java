package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.FilmCameraBridge;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;

import net.minecraft.class_4184;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.tastytrash.spatialGUI.mixin.accessor.GameRendererInvoker;

@Mixin(targets = "org.tastytrash.spatialGUI.util.RenderUtil", remap = false)
public abstract class PanelRayFovMixin {
    @Redirect(
            method = "buildCameraRayDirection",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lorg/tastytrash/spatialGUI/mixin/accessor/GameRendererInvoker;spatialGUI$getFov(Lnet/minecraft/class_4184;FZ)D"),
            require = 0,
            remap = false)
    private static double bsg$usePlayerProjection(
            GameRendererInvoker renderer, class_4184 camera, float delta, boolean useSetting) {
        if (!BackgroundWorldPass.active()
                && FilmCameraBridge.active()
                && BackgroundWorldPass.hasPanelFov()) return BackgroundWorldPass.panelFov();
        return renderer.spatialGUI$getFov(camera, delta, useSetting);
    }
}
