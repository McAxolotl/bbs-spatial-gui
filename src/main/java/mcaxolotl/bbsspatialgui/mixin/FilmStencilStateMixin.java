package mcaxolotl.bbsspatialgui.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import mcaxolotl.bbsspatialgui.render.RenderContext;
import mcaxolotl.bbsspatialgui.render.RenderPassScope;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "mchorse.bbs_mod.ui.film.controller.FilmStencilPicker", remap = false)
public abstract class FilmStencilStateMixin {
    @WrapMethod(method = "renderPreview", require = 1, remap = false)
    private void bsg$scopedStencil(
            @Coerce Object context, @Coerce Object area, Operation<Void> original) {
        try (RenderPassScope scope = RenderContext.spatialScreen() ? new RenderPassScope() : null) {
            original.call(context, area);
        }
    }
}
