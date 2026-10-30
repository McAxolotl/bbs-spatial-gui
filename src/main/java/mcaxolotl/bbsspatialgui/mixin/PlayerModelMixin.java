package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.camera.PlayerRenderState;

import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_742;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_1007", priority = 900, remap = false)
public abstract class PlayerModelMixin {
    @Inject(method = "method_4215", at = @At("HEAD"), require = 1, remap = false)
    private void bsg$observeModel(
            class_742 entity,
            float yaw,
            float delta,
            class_4587 matrices,
            class_4597 consumers,
            int light,
            CallbackInfo ci) {
        PlayerRenderState.sample(entity, "player-renderer", true);
    }
}
