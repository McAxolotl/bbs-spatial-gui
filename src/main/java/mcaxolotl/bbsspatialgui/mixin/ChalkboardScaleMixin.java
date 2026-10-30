package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.config.ConfigManager;

import java.lang.reflect.Method;

@Mixin(targets = "mchorse.bbs_mod.ui.utils.UIChalkboard", remap = false)
public abstract class ChalkboardScaleMixin {
    @Shadow private boolean drawing;
    @Unique private static Method bsg$nativeScale;

    @Redirect(
            method = {"resize", "render"},
            at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/BBSModClient;getGUIScale()F"),
            require = 2,
            remap = false)
    private float bsg$consistentCanvasScale() {
        if (RenderContext.currentSpatialBBSScreen())
            return ConfigManager.config().getEffectiveGuiScale();
        try {
            if (bsg$nativeScale == null)
                bsg$nativeScale =
                        Class.forName("mchorse.bbs_mod.BBSModClient").getMethod("getGUIScale");
            return ((Number) bsg$nativeScale.invoke(null)).floatValue();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Cannot query native chalkboard scale", e);
        }
    }

    @Inject(method = "resize", at = @At("RETURN"), require = 1, remap = false)
    private void bsg$endClearedStroke(CallbackInfo ci) {
        if (RenderContext.currentSpatialBBSScreen()) drawing = false;
    }
}
