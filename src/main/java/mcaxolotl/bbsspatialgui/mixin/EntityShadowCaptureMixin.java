package mcaxolotl.bbsspatialgui.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_1297;
import net.minecraft.class_310;
import net.minecraft.class_4538;
import net.minecraft.class_4587;
import net.minecraft.class_4597;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla draws entity shadows from {@code EntityRenderDispatcher.render} after the entity
 * renderer returns. BBS cancels the local player renderer through MorphRenderer, so hiding the
 * body alone still leaves its shadow baked into the monitor capture. Skip that shadow whenever the
 * local player is hidden inside BBS's capture pass.
 */
@Mixin(targets = "net.minecraft.class_898", remap = false)
public abstract class EntityShadowCaptureMixin {
    @WrapOperation(
            method = "method_3954",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/class_898;method_23166(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;Lnet/minecraft/class_1297;FFLnet/minecraft/class_4538;F)V"),
            require = 1,
            remap = false)
    private void bsg$skipCapturedOperatorShadow(
            class_4587 matrices,
            class_4597 buffers,
            class_1297 entity,
            float opacity,
            float partialTick,
            class_4538 view,
            float radius,
            Operation<Void> original) {
        if (entity == class_310.method_1551().field_1724
                && RenderContext.bbsWorldCaptureActive()
                && RenderContext.currentSpatialBBSScreen()) return;
        original.call(matrices, buffers, entity, opacity, partialTick, view, radius);
    }
}
