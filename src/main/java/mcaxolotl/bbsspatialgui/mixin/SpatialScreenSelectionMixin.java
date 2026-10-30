package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.render.ScreenPolicy;

import net.minecraft.class_437;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "org.tastytrash.spatialGUI.client.SpatialGUIClient", remap = false)
public abstract class SpatialScreenSelectionMixin {
    @Inject(
            method = "shouldHookScreen",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void bsg$excludeDisabledBBS(
            class_437 screen, CallbackInfoReturnable<Boolean> result) {
        if (screen != null
                && ScreenPolicy.excluded(screen.getClass().getName(), BBSSpatialGui.enabled()))
            result.setReturnValue(false);
    }

    @Redirect(
            method = "shouldHookScreen",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/List;contains(Ljava/lang/Object;)Z",
                            ordinal = 2),
            require = 0,
            remap = false)
    private static boolean bsg$includeScreen(List<?> enabledScreens, Object id) {
        return ScreenPolicy.included(
                String.valueOf(id), BBSSpatialGui.enabled(), enabledScreens.contains(id));
    }
}
