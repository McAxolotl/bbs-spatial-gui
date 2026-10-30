package mcaxolotl.bbsspatialgui;

import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.capture.RecordingBridge;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

/**
 * Optional local integration fixture: actual upstream methods, no encoder process or live game
 * required.
 */
public final class UpstreamRecorderChecks {
    private static void set(Class<?> type, Object owner, String name, Object value)
            throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void bridge(String name, Object value) throws Exception {
        set(RecordingBridge.class, null, name, value);
    }

    private static void verifyBlue(byte[] bytes) {
        for (int i = 0; i < bytes.length; i += 3)
            if ((bytes[i] & 255) != 255 || bytes[i + 1] != 0 || bytes[i + 2] != 0)
                throw new AssertionError("upstream pixels differ from blue BGR fixture");
    }

    private static void dirtyPack(int external) {
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, external);
        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 32);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 2);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 3);
    }

    private static void verifyPack(int external) {
        CoreTests.check(
                GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING) == external
                        && GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH) == 32
                        && GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS) == 2
                        && GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS) == 3,
                "upstream call lost caller's PACK state");
        CoreTests.check(GL11.glGetError() == 0, "upstream recording caused GL error");
    }

    public static void main(String[] args) throws Exception {
        CoreTests.check(GLFW.glfwInit(), "GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(32, 32, "Actual BBS recorder integration", 0, 0);
        CoreTests.check(window != 0, "context");
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        RenderSystem.initRenderThread();
        RenderSystem.setShaderLights(new Vector3f(1, 0, 0), new Vector3f(0, 1, 0));
        try {
            Class<?> video = Class.forName("mchorse.bbs_mod.utils.VideoRecorder");
            bridge("isRecording", video.getMethod("isRecording"));
            bridge("canRender", GpuTests.State.class.getField("canRender"));
            bridge("worldSession", GpuTests.State.class.getField("worldSession"));
            for (String field : new String[] {"pbos", "pboIndex", "channel", "counter"}) {
                var f = video.getDeclaredField(field);
                f.setAccessible(true);
                bridge(field, f);
            }
            TestTexture image = new TestTexture();
            image.resize(16, 16);
            int source = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, source);
            GL30.glFramebufferTexture2D(
                    GL30.GL_FRAMEBUFFER,
                    GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D,
                    image.glId(),
                    0);
            GL11.glClearColor(0, 0, 1, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            int external = GL15.glGenBuffers();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, external);
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, 16384, GL15.GL_STREAM_READ);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            Object recorder = video.getConstructor().newInstance();
            set(video, recorder, "recording", true);
            int[] ring = {GL15.glGenBuffers(), GL15.glGenBuffers()};
            for (int id : ring) {
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, id);
                GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, 16 * 16 * 3, GL15.GL_STREAM_READ);
            }
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            set(video, recorder, "pbos", ring);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            set(video, recorder, "channel", Channels.newChannel(output));
            Method pbo = video.getDeclaredMethod("recordFramePBO");
            pbo.setAccessible(true);
            bridge("recordFrame", pbo);
            set(
                    video,
                    recorder,
                    "textureId",
                    RecordingBridge.begin(recorder, image.glId(), 16, 16));
            for (int i = 0; i < 3; i++) {
                set(video, recorder, "counter", i);
                dirtyPack(external);
                RecordingBridge.beginFrame();
                RecordingBridge.skipVideo(recorder);
                RecordingBridge.afterFrame();
                verifyPack(external);
                set(video, recorder, "counter", i + 1);
            }
            RecordingBridge.beforeStop(recorder);
            verifyPack(external);
            CoreTests.check(output.size() == 3 * 16 * 16 * 3, "actual PBO frame count/tail");
            verifyBlue(output.toByteArray());
            RecordingBridge.close(recorder);
            for (int id : ring) GL15.glDeleteBuffers(id);
            recorder = video.getConstructor().newInstance();
            set(video, recorder, "recording", true);
            output = new ByteArrayOutputStream();
            set(video, recorder, "channel", Channels.newChannel(output));
            Method direct = video.getDeclaredMethod("recordFrameDirect");
            direct.setAccessible(true);
            bridge("recordFrame", direct);
            ByteBuffer guarded = MemoryUtil.memAlloc(16 * 16 * 3 * 8);
            for (int i = 0; i < guarded.capacity(); i++) guarded.put(i, (byte) 0x7f);
            ByteBuffer exact = guarded.duplicate();
            exact.limit(16 * 16 * 3);
            exact = exact.slice();
            set(video, recorder, "buffer", exact);
            set(
                    video,
                    recorder,
                    "textureId",
                    RecordingBridge.begin(recorder, image.glId(), 16, 16));
            dirtyPack(external);
            RecordingBridge.beginFrame();
            RecordingBridge.skipVideo(recorder);
            RecordingBridge.afterFrame();
            verifyPack(external);
            for (int i = 16 * 16 * 3; i < guarded.capacity(); i++)
                CoreTests.check(
                        guarded.get(i) == (byte) 0x7f,
                        "actual synchronous recording overran frame allocation");
            CoreTests.check(output.size() == 16 * 16 * 3, "actual direct frame size");
            verifyBlue(output.toByteArray());
            RecordingBridge.close(recorder);
            set(video, recorder, "buffer", null);
            MemoryUtil.memFree(guarded);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            GL15.glDeleteBuffers(external);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL30.glDeleteFramebuffers(source);
            image.close();
            System.out.println(
                    "PASS actual BBS PBO/direct recorder: dirty PACK/PBO isolated and restored, exact blue frames, tail drain, native canaries intact");
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }
}
