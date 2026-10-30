package mcaxolotl.bbsspatialgui.mixin;

import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.capture.ZeroedPixels;

import net.minecraft.class_310;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(targets = "mchorse.bbs_mod.utils.resources.Pixels", remap = false)
public abstract class GuiPixelsAllocationMixin {
    @Inject(method = "fromSize", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void bsg$nativeZeroing(
            int width, int height, CallbackInfoReturnable<Object> cir) {
        if (!BBSSpatialGui.enabled() || !RenderSystem.isOnRenderThread()) return;
        var screen = class_310.method_1551().field_1755;
        if (screen != null
                && screen.getClass().getName().equals("mchorse.bbs_mod.ui.framework.UIScreen")
                && SpatialGUIClient.isEnabled())
            cir.setReturnValue(ZeroedPixels.allocate(width, height));
    }
}
