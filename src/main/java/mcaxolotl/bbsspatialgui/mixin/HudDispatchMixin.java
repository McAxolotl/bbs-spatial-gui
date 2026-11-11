package mcaxolotl.bbsspatialgui.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.camera.FilmCameraBridge;
import mcaxolotl.bbsspatialgui.camera.ScreenTransitionCamera;
import mcaxolotl.bbsspatialgui.input.MouseCaptureRestore;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.FrameTiming;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_310;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_8251;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(targets = "net.minecraft.class_757", remap = false)
public abstract class HudDispatchMixin {
    @Unique private final ByteBuffer bsg$colorMask = ByteBuffer.allocateDirect(4);
    @Unique private boolean bsg$hudReady;
    @Unique private int bsg$hudWidth;
    @Unique private int bsg$hudHeight;
    @Unique private int bsg$crosshairLogs;

    @Inject(method = "method_3192", at = @At("HEAD"), require = 0, remap = false)
    private void bsg$beginFrame(float tickDelta, long frameTime, boolean tick, CallbackInfo ci) {
        bsg$hudReady = false;
        mcaxolotl.bbsspatialgui.capture.RecordingBridge.beginFrame();
        ScreenTransitionCamera.beginFrame();
        FilmCameraBridge.beginFrame();
        BackgroundWorldPass.beginFrame(tickDelta, frameTime);
        FrameTiming.beginFrame();
    }

    @Redirect(
            method = "method_3192",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/class_329;method_1753(Lnet/minecraft/class_332;F)V"),
            require = 0,
            remap = false)
    private void bsg$saveHudState(class_329 hud, class_332 graphics, float tickDelta) {
        var window = class_310.method_1551().method_22683();
        bsg$hudWidth = window.method_4486();
        bsg$hudHeight = window.method_4502();
        hud.method_1753(graphics, tickDelta);
        bsg$hudReady =
                RenderContext.nativeCrosshairRequested()
                        && bsg$hudWidth > 0
                        && bsg$hudHeight > 0;
    }

    @Inject(method = "method_3192", at = @At("TAIL"), require = 0, remap = false)
    private void bsg$crosshairAfterSpatialPanel(CallbackInfo ci) {
        MouseCaptureRestore.afterFrame();
        if (!bsg$hudReady || !RenderContext.nativeCrosshairRequested()) return;
        var client = class_310.method_1551();
        var target = client.method_1522();
        NativeCrosshairMixin hud = (NativeCrosshairMixin) client.field_1705;
        int width = hud.bsg$getWidth(), height = hud.bsg$getHeight();
        int hudWidth = bsg$hudWidth, hudHeight = bsg$hudHeight;
        if (hudWidth <= 0 || hudHeight <= 0) return;
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        class_8251 oldSorting = RenderSystem.getVertexSorting();
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        bsg$colorMask.clear();
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, bsg$colorMask);
        float[] color = RenderSystem.getShaderColor().clone();
        var modelView = RenderSystem.getModelViewStack();
        modelView.method_22903();
        try {
            // Rebuild the vanilla GUI ortho for the dimensions the HUD actually used. Reusing
            // whatever projection/model-view BBS leaves behind can be a 3D or film-ortho matrix,
            // which draws only part of the crosshair (or the wrong size) over the panel.
            Matrix4f hudProjection =
                    new Matrix4f().setOrtho(0.0f, (float) hudWidth, (float) hudHeight, 0.0f, 1000.0f, 21000.0f);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.field_1476);
            GlStateManager._viewport(0, 0, target.field_1480, target.field_1477);
            RenderSystem.setProjectionMatrix(hudProjection, class_8251.field_43361);
            modelView.method_34426();
            modelView.method_46416(0.0f, 0.0f, -11000.0f);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            GlStateManager._disableScissorTest();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            hud.bsg$setWidth(hudWidth);
            hud.bsg$setHeight(hudHeight);
            RenderContext.lateCrosshair(true);
            class_332 graphics = new class_332(client, client.method_22940().method_23000());
            hud.bsg$renderCrosshair(graphics);
            graphics.method_51452();
        } finally {
            RenderContext.lateCrosshair(false);
            hud.bsg$setWidth(width);
            hud.bsg$setHeight(height);
            modelView.method_22909();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.colorMask(
                    bsg$colorMask.get(0) != 0,
                    bsg$colorMask.get(1) != 0,
                    bsg$colorMask.get(2) != 0,
                    bsg$colorMask.get(3) != 0);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (scissor) GlStateManager._enableScissorTest();
            else GlStateManager._disableScissorTest();
        }
        if (BBSSpatialGui.debugLogging() && bsg$crosshairLogs++ < 3) {
            System.out.println(
                    "[BBS Spatial GUI] Composited vanilla crosshair at "
                            + hudWidth
                            + "x"
                            + hudHeight
                            + " on "
                            + target.field_1480
                            + "x"
                            + target.field_1477);
        }
    }

    @Inject(method = "method_3192", at = @At("TAIL"), require = 1, remap = false)
    private void bsg$exposeDisplayedCamera(CallbackInfo ci) {
        FilmCameraBridge.finishFrame();
        FrameTiming.endFrame();
        mcaxolotl.bbsspatialgui.capture.RecordingBridge.afterFrame();
    }
}
