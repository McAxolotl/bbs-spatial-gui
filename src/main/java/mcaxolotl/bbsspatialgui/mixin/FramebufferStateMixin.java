package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.render.RenderContext;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mchorse.bbs_mod.graphics.Framebuffer", remap = false)
public abstract class FramebufferStateMixin {
    @Shadow public int id;
    @Unique private boolean bsg$saved;
    @Unique private int bsg$draw;
    @Unique private int bsg$read;
    @Unique private final int[] bsg$viewport = new int[4];

    @Inject(
            method = {"apply", "bind"},
            at = @At("HEAD"),
            require = 0,
            remap = false)
    private void bsg$beforeBind(CallbackInfo ci) {
        if (!RenderContext.spatialScreen() || bsg$saved) return;
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        if (draw == id && read == id) return;
        bsg$draw = draw;
        bsg$read = read;
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, bsg$viewport);
        bsg$saved = true;
    }

    @Inject(method = "unbind", at = @At("HEAD"), require = 0, remap = false)
    private void bsg$beforeUnbind(CallbackInfo ci) {
        RenderContext.snapshot("BBS framebuffer unbind before");
    }

    @Inject(method = "unbind", at = @At("RETURN"), require = 0, remap = false)
    private void bsg$afterUnbind(CallbackInfo ci) {
        RenderContext.snapshot("BBS framebuffer unbind before restore");
        if (bsg$saved) {
            if (RenderContext.spatialScreen()) {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, bsg$draw);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, bsg$read);
                GL11.glViewport(bsg$viewport[0], bsg$viewport[1], bsg$viewport[2], bsg$viewport[3]);
            }
            bsg$saved = false;
        }
        RenderContext.snapshot("BBS framebuffer unbind after restore");
    }
}
