package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.PlayerRenderState;

import net.minecraft.class_1297;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4604;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_898", remap = false)
public abstract class PlayerSubmissionMixin {
    @Inject(method = "method_3950", at = @At("RETURN"), require = 1, remap = false)
    private void bsg$observeCulling(
            class_1297 entity,
            class_4604 frustum,
            double x,
            double y,
            double z,
            CallbackInfoReturnable<Boolean> cir) {
        PlayerRenderState.sample(entity, "culling", cir.getReturnValue());
    }

    @Inject(method = "method_3954", at = @At("HEAD"), require = 1, remap = false)
    private void bsg$observeSubmission(
            class_1297 entity,
            double x,
            double y,
            double z,
            float yaw,
            float delta,
            class_4587 matrices,
            class_4597 consumers,
            int light,
            CallbackInfo ci) {
        PlayerRenderState.sample(entity, "submit", true);
    }
}
