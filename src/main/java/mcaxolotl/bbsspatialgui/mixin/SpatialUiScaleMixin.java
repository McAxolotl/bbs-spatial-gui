package mcaxolotl.bbsspatialgui.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import mcaxolotl.bbsspatialgui.ui.SpatialUiScale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "org.tastytrash.spatialGUI.config.SpatialGUIConfig", remap = false)
public abstract class SpatialUiScaleMixin {
    @ModifyReturnValue(method = "getEffectiveGuiScale", at = @At("RETURN"), require = 1, remap = false)
    private int bsg$bbsInterfaceScale(int original) {
        return SpatialUiScale.effective(original);
    }
}
