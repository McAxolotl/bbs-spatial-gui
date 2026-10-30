package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.camera.PlayerPerspectiveSession;

import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_5498;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(targets = "mchorse.bbs_mod.ui.dashboard.UIDashboard", remap = false)
public abstract class DashboardPerspectiveMixin {
    @Unique private class_5498 bsg$savedNativePerspective;

    @Redirect(
            method = "onOpen",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/class_315;method_31044()Lnet/minecraft/class_5498;"),
            require = 1,
            remap = false)
    private class_5498 bsg$saveRealPlayerPerspective(class_315 options) {
        bsg$savedNativePerspective = null;
        if (!BBSSpatialGui.enabled() || !SpatialGUIClient.isEnabled())
            return options.method_31044();
        bsg$savedNativePerspective = ((NativePerspectiveAccess) options).bsg$getStoredPerspective();
        PlayerPerspectiveSession.begin(bsg$savedNativePerspective);
        if (BBSSpatialGui.debugLogging()) {
            System.out.println(
                    "[BBS Spatial GUI perspective] savedNative=" + bsg$savedNativePerspective);
        }
        return bsg$savedNativePerspective;
    }

    @Inject(method = "onClose", at = @At("RETURN"), require = 1, remap = false)
    private void bsg$restoredPerspective(CallbackInfo ci) {
        if (!BBSSpatialGui.debugLogging() || bsg$savedNativePerspective == null) return;
        var options = class_310.method_1551().field_1690;
        var stored = ((NativePerspectiveAccess) options).bsg$getStoredPerspective();
        System.out.println(
                "[BBS Spatial GUI perspective] restoredNative="
                        + stored
                        + " expected="
                        + bsg$savedNativePerspective
                        + " matched="
                        + (stored == bsg$savedNativePerspective));
    }
}
