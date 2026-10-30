package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_329", remap = false)
public abstract class NativeCrosshairVisibilityMixin {
    @Inject(
            method = "method_1736",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private void bsg$deferCrosshairToFinalComposition(CallbackInfo ci) {
        if (!RenderContext.lateCrosshair() && RenderContext.nativeCrosshairRequested()) ci.cancel();
    }
}
