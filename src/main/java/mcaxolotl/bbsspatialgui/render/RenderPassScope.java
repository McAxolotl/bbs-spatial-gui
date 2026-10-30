package mcaxolotl.bbsspatialgui.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.class_5944;
import net.minecraft.class_8251;

import org.joml.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.lang.reflect.Field;

public final class RenderPassScope implements AutoCloseable {
    private final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
            read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int[] viewport = new int[4],
            scissorBox = new int[4],
            shaderTextures = new int[12];
    private final int textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    private final float[] clearColor = new float[4];
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
            blend = GL11.glIsEnabled(GL11.GL_BLEND),
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE),
            scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
            write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int cullMode = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
    private final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
            dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
            srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
            dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix()),
            modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
    private final Matrix4f stackPosition =
            new Matrix4f(RenderSystem.getModelViewStack().method_23760().method_23761());
    private final Matrix3f stackNormal =
            new Matrix3f(RenderSystem.getModelViewStack().method_23760().method_23762());
    private final Matrix3f inverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
    private final class_8251 sorting = RenderSystem.getVertexSorting();
    private final class_5944 shader = RenderSystem.getShader();
    private final float clock = RenderSystem.getShaderGameTime(),
            fogStart = RenderSystem.getShaderFogStart(),
            fogEnd = RenderSystem.getShaderFogEnd();
    private final net.minecraft.class_6854 fogShape = RenderSystem.getShaderFogShape();
    private final float[] color = RenderSystem.getShaderColor().clone(),
            fog = RenderSystem.getShaderFogColor().clone();
    private final boolean[] mask = new boolean[4];
    private final Vector3f light0, light1;
    private static Field lights;

    public RenderPassScope() {
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        for (int i = 0; i < shaderTextures.length; i++)
            shaderTextures[i] = RenderSystem.getShaderTexture(i);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var buffer = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, buffer);
            for (int i = 0; i < 4; i++) mask[i] = buffer.get(i) != 0;
        }
        try {
            if (lights == null) {
                lights = RenderSystem.class.getDeclaredField("shaderLightDirections");
                lights.setAccessible(true);
            }
            Vector3f[] live = (Vector3f[]) lights.get(null);
            light0 = new Vector3f(live[0]);
            light1 = new Vector3f(live[1]);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Cannot save 1.20.1 shader lights", e);
        }
    }

    @Override
    public void close() {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GlStateManager._scissorBox(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        RenderSystem.colorMask(mask[0], mask[1], mask[2], mask[3]);
        RenderSystem.depthFunc(depthFunc);
        RenderSystem.depthMask(write);
        GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcA, dstA);
        if (depth) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        if (blend) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
        GL11.glCullFace(cullMode);
        if (cull) RenderSystem.enableCull();
        else RenderSystem.disableCull();
        if (scissor) GlStateManager._enableScissorTest();
        else GlStateManager._disableScissorTest();
        RenderSystem.setProjectionMatrix(projection, sorting);
        RenderSystem.setInverseViewRotationMatrix(inverseView);
        var entry = RenderSystem.getModelViewStack().method_23760();
        entry.method_23761().set(modelView);
        entry.method_23762().set(stackNormal);
        RenderSystem.applyModelViewMatrix();
        entry.method_23761().set(stackPosition);
        RenderSystem.setShader(() -> shader);
        ShaderClock.restore(clock);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        RenderSystem.setShaderFogShape(fogShape);
        RenderSystem.setShaderFogColor(fog[0], fog[1], fog[2], fog[3]);
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        RenderSystem.setShaderLights(light0, light1);
        RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        for (int i = 0; i < shaderTextures.length; i++)
            RenderSystem._setShaderTexture(i, shaderTextures[i]);
        RenderSystem.activeTexture(activeTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBinding);
        GlStateManager._bindTexture(textureBinding);
    }
}
