package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.input.MouseCaptureRestore;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.FrameTiming;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mchorse.bbs_mod.ui.framework.UIScreen", remap = false)
public abstract class UIScreenRenderMixin {
    @Unique private int bsg$loggedFrames;

    @Inject(method = "method_25432", at = @At("RETURN"), require = 0, remap = false)
    private void bsg$restoreMouseAfterClose(CallbackInfo ci) {
        MouseCaptureRestore.schedule();
    }

    @Inject(
            method = "renderInWorld",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private void bsg$preserveEditorWorldContext(CallbackInfo ci) {
        if (BackgroundWorldPass.active()) ci.cancel();
    }

    @Inject(method = "method_25394", at = @At("HEAD"), require = 0, remap = false)
    private void bsg$beforeRender(CallbackInfo ci) {
        FrameTiming.beforeUi();
        RenderContext.begin(BBSSpatialGui.debugLogging() && bsg$loggedFrames < 3);
        if (BBSSpatialGui.debugLogging() && bsg$loggedFrames < 3) bsg$log("before");
    }

    @Inject(method = "method_25394", at = @At("RETURN"), require = 0, remap = false)
    private void bsg$afterRender(CallbackInfo ci) {
        FrameTiming.afterUi();
        if (BBSSpatialGui.debugLogging() && bsg$loggedFrames < 3) {
            bsg$log("after");
            bsg$loggedFrames++;
        }
        RenderContext.end();
    }

    @Unique
    private void bsg$log(String phase) {
        System.out.println(
                "[BBS Spatial GUI] UIScreen render "
                        + phase
                        + " frame="
                        + bsg$loggedFrames
                        + " drawFramebuffer="
                        + GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                        + " readFramebuffer="
                        + GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
                        + " depthTest="
                        + GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                        + " depthFunc="
                        + GL11.glGetInteger(GL11.GL_DEPTH_FUNC)
                        + " scissor="
                        + GL11.glIsEnabled(GL11.GL_SCISSOR_TEST));
    }
}
