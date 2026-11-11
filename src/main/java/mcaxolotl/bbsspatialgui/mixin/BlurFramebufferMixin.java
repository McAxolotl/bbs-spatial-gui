package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_276;
import net.minecraft.class_310;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(targets = "mchorse.bbs_mod.ui.utils.InterfaceBlur", remap = false)
public abstract class BlurFramebufferMixin {
    @ModifyVariable(method = "pass", at = @At("HEAD"), argsOnly = true, require = 1, remap = false)
    private static String bsg$preserveBlurCoverage(String program) {
        if (!RenderContext.spatialScreen()) return program;
        return switch (program) {
            case "bbs_kawase_down" -> "bbs_spatial_gui_kawase_down";
            case "bbs_kawase_up" -> "bbs_spatial_gui_kawase_up";
            default -> program;
        };
    }

    @Redirect(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/class_310;method_1522()Lnet/minecraft/class_276;"),
            require = 1,
            remap = false)
    private static class_276 bsg$blurCapturedInterface(class_310 client) {
        if (RenderContext.drawingSpatialGuiTarget()) {
            var target = SpatialGUIClient.renderer().getScreenTexture().getTarget();
            if (target != null && target.field_1476 == RenderContext.spatialGuiFramebuffer())
                return target;
        }
        return client.method_1522();
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(
            method = "render",
            require = 1,
            remap = false)
    private static void bsg$scopedBlur(
            int radius, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        try (mcaxolotl.bbsspatialgui.render.RenderPassScope scope =
                RenderContext.spatialScreen()
                        ? new mcaxolotl.bbsspatialgui.render.RenderPassScope()
                        : null) {
            original.call(radius);
        }
    }
}
