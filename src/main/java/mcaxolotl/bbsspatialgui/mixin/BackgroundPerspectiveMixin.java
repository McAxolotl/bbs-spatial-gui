package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;

import net.minecraft.class_4587;
import net.minecraft.class_8251;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.tastytrash.spatialGUI.render.SpatialGUIRenderer", remap = false)
public abstract class BackgroundPerspectiveMixin {
    @Inject(method = "capturePerspectiveState", at = @At("HEAD"), require = 0, remap = false)
    private void bsg$recordPlayerProjection(
            Matrix4f projection, class_8251 sorting, class_4587 matrices, CallbackInfo ci) {
        BackgroundWorldPass.recordPerspective(projection);
    }
}
