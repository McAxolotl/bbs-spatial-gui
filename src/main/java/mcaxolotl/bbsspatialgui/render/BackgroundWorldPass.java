package mcaxolotl.bbsspatialgui.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.camera.FilmCameraBridge;
import mcaxolotl.bbsspatialgui.mixin.CameraPoseAccess;

import net.minecraft.class_310;
import net.minecraft.class_4587;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public final class BackgroundWorldPass {
    private static boolean active, rendered;
    private static float tickDelta, panelFov;
    private static long frameTime;
    private static Method resizeBuffers;
    private static Field renderingWorld, lightDirections;
    private static int logs;
    private static final ByteBuffer savedColorMask = ByteBuffer.allocateDirect(4);

    public static boolean active() {
        return active;
    }

    public static void beginFrame(float delta, long time) {
        rendered = false;
        tickDelta = delta;
        frameTime = time;
    }

    public static boolean hasPanelFov() {
        return Float.isFinite(panelFov) && panelFov > 0;
    }

    public static float panelFov() {
        return panelFov;
    }

    public static void recordPerspective(Matrix4f projection) {
        if (active && Math.abs(projection.m11()) > 0.0001f) {
            panelFov = (float) Math.toDegrees(2 * Math.atan(1 / Math.abs(projection.m11())));
        }
    }

    public static void renderAfterPreview() {
        if (active
                || rendered
                || !RenderContext.bbsCustomSizeActive()
                || RenderContext.bbsWorldCaptureActive()) return;
        var client = class_310.method_1551();
        if (client.field_1687 == null || client.field_1724 == null) return;
        try {
            if (resizeBuffers == null) {
                Class<?> type = Class.forName("mchorse.bbs_mod.client.BBSRendering");
                resizeBuffers = type.getMethod("resizeExtraFramebuffers");
                renderingWorld = type.getField("renderingWorld");
                lightDirections = RenderSystem.class.getDeclaredField("shaderLightDirections");
                lightDirections.setAccessible(true);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Cannot prepare independent world rendering", e);
        }
        rendered = true;
        var camera = client.field_1773.method_19418();
        var position = camera.method_19326();
        float yaw = camera.method_19330(), pitch = camera.method_19329();
        Quaternionf rotation = new Quaternionf(camera.method_23767());
        Vector3f horizontal = new Vector3f(camera.method_19335());
        Vector3f vertical = new Vector3f(camera.method_19336());
        Vector3f diagonal = new Vector3f(camera.method_35689());
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix3f inverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var shader = RenderSystem.getShader();
        float gameTime = RenderSystem.getShaderGameTime();
        float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        float[] fog = RenderSystem.getShaderFogColor().clone();
        var fogShape = RenderSystem.getShaderFogShape();
        float[] color = RenderSystem.getShaderColor().clone();
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4], scissorBox = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        ByteBuffer colorMask = savedColorMask;
        colorMask.clear();
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE),
                scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                chunkCulling = client.field_1730;
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int textureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        boolean windowOverride = SpatialGUIRenderer.skipWindowOverride;
        Vector3f[] lights;
        boolean wasRenderingWorld;
        try {
            Vector3f[] live = (Vector3f[]) lightDirections.get(null);
            lights = new Vector3f[] {new Vector3f(live[0]), new Vector3f(live[1])};
            wasRenderingWorld = renderingWorld.getBoolean(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("[BBS Spatial GUI] Cannot save world render state", e);
        }
        long start = System.nanoTime();
        float inheritedModelViewZ = RenderSystem.getModelViewMatrix().m32();
        var modelView = RenderSystem.getModelViewStack();
        modelView.method_22903();
        active = true;
        try {
            // This pass begins from InGameHud, after vanilla translated the global
            // model-view by -11000 for GUI rendering. Entity shaders consume that
            // global matrix even though Sodium terrain supplies its own matrices.
            modelView.method_34426();
            RenderSystem.applyModelViewMatrix();
            SpatialGUIRenderer.skipWindowOverride = true;
            client.field_1730 = true;
            renderingWorld.setBoolean(null, true);
            GlStateManager._disableScissorTest();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            resizeBuffers.invoke(null);
            client.method_1522().method_1235(true);
            RenderSystem.clearColor(fog[0], fog[1], fog[2], 1.0f);
            GL11.glClear(
                    GL11.GL_COLOR_BUFFER_BIT
                            | GL11.GL_DEPTH_BUFFER_BIT
                            | GL11.GL_STENCIL_BUFFER_BIT);
            RenderSystem.setShaderGameTime(client.field_1687.method_8510(), tickDelta);
            client.field_1773.method_3188(tickDelta, frameTime, new class_4587());
            FrameTiming.background(System.nanoTime() - start);
            FilmCameraBridge.updatePanelPose(camera.method_19330(), camera.method_19329());
            if (BBSSpatialGui.debugLogging() && logs++ < 3) {
                var target = client.method_1522();
                System.out.println(
                        "[BBS Spatial GUI] Rendered independent player world "
                                + target.field_1480
                                + "x"
                                + target.field_1477
                                + " in "
                                + (System.nanoTime() - start) / 1_000_000.0
                                + " ms"
                                + " inheritedModelViewZ="
                                + inheritedModelViewZ
                                + " worldModelViewZ="
                                + RenderSystem.getModelViewMatrix().m32());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Independent world framebuffer preparation failed", e);
        } finally {
            try {
                renderingWorld.setBoolean(null, wasRenderingWorld);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "[BBS Spatial GUI] Cannot restore BBS render flag", e);
            }
            active = false;
            SpatialGUIRenderer.skipWindowOverride = windowOverride;
            client.field_1730 = chunkCulling;
            CameraPoseAccess access = (CameraPoseAccess) camera;
            access.bsg$setPosition(position.field_1352, position.field_1351, position.field_1350);
            access.bsg$setRotation(yaw, pitch);
            camera.method_23767().set(rotation);
            camera.method_19335().set(horizontal);
            camera.method_19336().set(vertical);
            camera.method_35689().set(diagonal);
            modelView.method_22909();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setInverseViewRotationMatrix(inverseView);
            RenderSystem.setShader(() -> shader);
            ShaderClock.restore(gameTime);
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            RenderSystem.setShaderFogColor(fog[0], fog[1], fog[2], fog[3]);
            RenderSystem.setShaderFogShape(fogShape);
            RenderSystem.setShaderLights(lights[0], lights[1]);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GlStateManager._scissorBox(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            RenderSystem.colorMask(
                    colorMask.get(0) != 0,
                    colorMask.get(1) != 0,
                    colorMask.get(2) != 0,
                    colorMask.get(3) != 0);
            RenderSystem.depthFunc(depthFunction);
            RenderSystem.depthMask(depthWrite);
            GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            if (scissor) GlStateManager._enableScissorTest();
            else GlStateManager._disableScissorTest();
            RenderSystem.activeTexture(textureUnit);
        }
    }

    private BackgroundWorldPass() {}
}
