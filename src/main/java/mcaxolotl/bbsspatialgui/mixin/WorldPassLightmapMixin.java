package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_310;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_765", remap = false)
public abstract class WorldPassLightmapMixin {
    @Shadow private boolean field_4135;

    @Inject(method = "method_3313", at = @At("HEAD"), require = 1, remap = false)
    private void bsg$refreshPassLighting(float delta, CallbackInfo ci) {
        if (!BackgroundWorldPass.active() && !RenderContext.bbsWorldCaptureActive()) return;
        var world = class_310.method_1551().field_1687;
        if (world != null) {
            world.method_8533();
            field_4135 = true;
        }
    }
}
