package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.input.MouseCaptureRestore;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.FrameTiming;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_310;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mchorse.bbs_mod.ui.framework.UIScreen", remap = false)
public abstract class UIScreenRenderMixin {
    @Shadow private mchorse.bbs_mod.ui.framework.UIBaseMenu menu;
    @Unique private int bsg$loggedFrames;
    @Unique private int bsg$layoutLogs;
    @Unique private int bsg$layoutSamples;

    /**
     * BBS sizes its menus from the window metrics that are current when the screen is opened or
     * resized. With the film "custom size" active those metrics can be the video resolution, so
     * the menu stays laid out for a smaller canvas while the game renders the screen at the
     * window's GUI scale: the whole 2D interface then lands in the top-left corner. Spatial GUI
     * also forces the screen to its effective GUI scale, which makes the mismatch even easier to
     * hit. Re-sync the menu to the metrics the frame is actually rendered with.
     */
    @Unique
    private void bsg$syncMenuLayout() {
        if (!BBSSpatialGui.enabled() || menu == null) return;
        var window = class_310.method_1551().method_22683();
        // The screen renderer temporarily disables Spatial GUI's window overrides, but the
        // extractor re-enables them before building the ortho. Query the metrics the extractor
        // will actually use, otherwise the first frame keeps the film-resolution layout.
        boolean previousSkip = org.tastytrash.spatialGUI.render.SpatialGUIRenderer.skipWindowOverride;
        int width;
        int height;
        try {
            org.tastytrash.spatialGUI.render.SpatialGUIRenderer.skipWindowOverride = false;
            width = window.method_4486();
            height = window.method_4502();
        } finally {
            org.tastytrash.spatialGUI.render.SpatialGUIRenderer.skipWindowOverride = previousSkip;
        }
        if (width <= 0 || height <= 0) return;
        if (BBSSpatialGui.debugLogging() && bsg$layoutSamples++ < 3) {
            System.out.println(
                    "[BBS Spatial GUI] BBS menu layout menu="
                            + menu.width
                            + "x"
                            + menu.height
                            + " scaled="
                            + width
                            + "x"
                            + height
                            + " window="
                            + window.method_4480()
                            + "x"
                            + window.method_4507()
                            + " framebuffer="
                            + window.method_4489()
                            + "x"
                            + window.method_4506()
                            + " guiScale="
                            + window.method_4495()
                            + " video="
                            + mchorse.bbs_mod.client.BBSRendering.getVideoWidth()
                            + "x"
                            + mchorse.bbs_mod.client.BBSRendering.getVideoHeight()
                            + " customSize="
                            + mchorse.bbs_mod.client.BBSRendering.isCustomSize()
                            + " bbsCapture="
                            + RenderContext.bbsWorldCaptureActive()
                            + " skipWindowOverride="
                            + previousSkip);
        }
        int previousWidth = menu.width;
        int previousHeight = menu.height;
        if (Math.abs(previousWidth - width) <= 1 && Math.abs(previousHeight - height) <= 1) return;
        menu.resize(width, height);
        if (BBSSpatialGui.debugLogging() && bsg$layoutLogs++ < 5) {
            System.out.println(
                    "[BBS Spatial GUI] BBS menu re-laid out from "
                            + previousWidth
                            + "x"
                            + previousHeight
                            + " to "
                            + width
                            + "x"
                            + height);
        }
    }

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

    @Inject(method = "method_25426", at = @At("RETURN"), require = 0, remap = false)
    private void bsg$resyncAfterInit(CallbackInfo ci) {
        bsg$syncMenuLayout();
    }

    @Inject(method = "method_25410", at = @At("RETURN"), require = 0, remap = false)
    private void bsg$resyncAfterResize(class_310 client, int width, int height, CallbackInfo ci) {
        bsg$syncMenuLayout();
    }

    @Inject(method = "method_25394", at = @At("HEAD"), require = 0, remap = false)
    private void bsg$beforeRender(CallbackInfo ci) {
        bsg$syncMenuLayout();
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
