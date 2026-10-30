package mcaxolotl.bbsspatialgui.capture;

import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.cursor.CursorGlyphs;
import mcaxolotl.bbsspatialgui.cursor.CursorState;

import net.minecraft.class_310;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

public final class RecordingPointer {
    public static void drawScreenPointer(int width, int height, int dx, int dy, int dw, int dh) {
        var client = class_310.method_1551();
        if (client.field_1755 == null) return;
        var window = client.method_22683();
        long handle = window.method_4490();
        if (!CursorState.visible(handle)) return;
        double x, y;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var px = stack.mallocDouble(1);
            var py = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(handle, px, py);
            x = px.get(0);
            y = py.get(0);
        }
        if (x < 0 || y < 0 || x >= window.method_4480() || y >= window.method_4507()) return;
        var image = CursorState.image(handle);
        float scale =
                image.nativeImage()
                        ? dw / (float) window.method_4489()
                        : Math.max(1, dw / (float) window.method_4486());
        drawSprite(
                width,
                height,
                (float) (dx + x * dw / window.method_4480()),
                (float) (dy + y * dh / window.method_4507()),
                scale,
                image);
    }

    public static void drawSprite(
            int width, int height, float x, float y, float scale, CursorGlyphs.Sprite image) {
        boolean visible = false;
        for (int pixel : image.argb())
            if ((pixel >>> 24) != 0) {
                visible = true;
                break;
            }
        if (!visible) return;
        var matrix = new org.joml.Matrix4f().setOrtho(0, width, height, 0, -1, 1);
        RenderSystem.setProjectionMatrix(matrix, net.minecraft.class_8251.field_43361);
        var view = RenderSystem.getModelViewStack();
        view.method_34426();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        com.mojang.blaze3d.platform.GlStateManager._blendFuncSeparate(
                org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(net.minecraft.class_757::method_34540);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        var builder = net.minecraft.class_289.method_1348().method_1349();
        builder.method_1328(
                net.minecraft.class_293.class_5596.field_27382, net.minecraft.class_290.field_1576);
        float left = x - image.hotX() * scale, top = y - image.hotY() * scale;
        for (int py = 0; py < image.height(); py++)
            for (int px = 0; px < image.width(); ) {
                int color = image.argb()[py * image.width() + px], end = px + 1;
                while (end < image.width() && image.argb()[py * image.width() + end] == color)
                    end++;
                if ((color >>> 24) != 0) {
                    float l = left + px * scale,
                            r = left + end * scale,
                            t = top + py * scale,
                            b = t + scale;
                    for (float[] point : new float[][] {{l, t}, {l, b}, {r, b}, {r, t}})
                        builder.method_22912(point[0], point[1], 0)
                                .method_39415(color)
                                .method_1344();
                }
                px = end;
            }
        net.minecraft.class_286.method_43433(builder.method_1326());
    }

    private RecordingPointer() {}
}
