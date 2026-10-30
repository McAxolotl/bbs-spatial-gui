package mcaxolotl.bbsspatialgui.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_5944;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_5944", remap = false)
public abstract class PickerShaderBlendMixin {
    @Unique private int bsg$pickerLogs;

    @Inject(method = "method_34586", at = @At("RETURN"), require = 0, remap = false)
    private void bsg$applyOverlayBlendAfterShaderState(CallbackInfo ci) {
        if (!RenderContext.drawingSpatialGuiTarget()) return;
        String shader = ((class_5944) (Object) this).method_35787();
        if (!shader.endsWith("picker_preview")) return;
        RenderSystem.enableBlend();
        GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (BBSSpatialGui.debugLogging() && bsg$pickerLogs++ < 3) {
            System.out.println(
                    "[BBS Spatial GUI] Applied GUI overlay blend after picker shader state");
        }
    }
}
