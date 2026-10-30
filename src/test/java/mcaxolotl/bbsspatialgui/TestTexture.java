package mcaxolotl.bbsspatialgui;

import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.capture.PixelTransferState;

import net.minecraft.class_1044;
import net.minecraft.class_3300;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * GL texture owned by the bridge; tiles are uploaded directly, without reassembly.
 *
 * <p>LWJGL resolves the native pointer of a NIO buffer through the buffer's own address field,
 * which is only populated for direct buffers. A heap buffer therefore hands the driver a near-null
 * pointer and the driver segfaults while copying, so every tile is staged through one reusable
 * native buffer.
 */
public final class TestTexture extends class_1044 {
    private int width;
    private int height;
    private boolean allocated;
    private ByteBuffer uploadBuffer;

    public void resize(int width, int height) {
        if (this.width == width && this.height == height && allocated) return;
        this.width = width;
        this.height = height;
        RenderSystem.assertOnRenderThread();
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        ByteBuffer empty = MemoryUtil.memCalloc(Math.multiplyExact(width * height, 4));
        try (PixelTransferState state = new PixelTransferState(false)) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, method_4624());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    width,
                    height,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    empty);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            MemoryUtil.memFree(empty);
        }
        allocated = true;
    }

    /** Uploads one decoded RGBA tile. Render thread only. */
    public void uploadTile(int x, int y, int tileWidth, int tileHeight, byte[] pixels) {
        if (!allocated || pixels == null) return;
        if (x < 0
                || y < 0
                || tileWidth <= 0
                || tileHeight <= 0
                || x + tileWidth > width
                || y + tileHeight > height) return;
        int bytes = tileWidth * tileHeight * 4;
        if (pixels.length < bytes) return;

        if (uploadBuffer == null || uploadBuffer.capacity() < bytes) {
            if (uploadBuffer != null) MemoryUtil.memFree(uploadBuffer);
            uploadBuffer = MemoryUtil.memAlloc(bytes);
        }
        uploadBuffer.clear();
        uploadBuffer.put(pixels, 0, bytes);
        uploadBuffer.flip();

        RenderSystem.assertOnRenderThread();
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try (PixelTransferState state = new PixelTransferState(false)) {
            // Raw bind AND restore together: do not desynchronise Minecraft's binding cache.
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, method_4624());
            GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    x,
                    y,
                    tileWidth,
                    tileHeight,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    uploadBuffer);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            uploadBuffer.clear();
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int glId() {
        return method_4624();
    }

    @Override
    public void method_4625(class_3300 resourceManager) {}

    @Override
    public void close() {
        allocated = false;
        width = 0;
        height = 0;
        if (uploadBuffer != null) {
            MemoryUtil.memFree(uploadBuffer);
            uploadBuffer = null;
        }
        method_4528();
    }
}
