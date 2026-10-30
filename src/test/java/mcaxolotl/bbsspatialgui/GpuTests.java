package mcaxolotl.bbsspatialgui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.capture.*;
import mcaxolotl.bbsspatialgui.capture.RecordingBridge;
import mcaxolotl.bbsspatialgui.mixin.FramebufferFormStateMixin;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderContext;
import mcaxolotl.bbsspatialgui.render.RenderPassScope;
import mcaxolotl.bbsspatialgui.render.ShaderClock;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.channels.*;

public final class GpuTests {
    private static class FormHooks extends FramebufferFormStateMixin {}

    public static final class Session {
        // Match upstream: a shared global recorder can be active while this world session is idle.
        public boolean isRecording() {
            return true;
        }

        public boolean isExporting() {
            return State.world;
        }

        public String getFilmId() {
            return null;
        }
    }

    public static final class State {
        public static boolean canRender = true, world, captureEnabled;
        public static int captureWidth, captureHeight;
        public static Session worldSession = new Session();

        public static void setCustomSize(boolean enabled, int width, int height) {
            captureEnabled = enabled;
            captureWidth = width;
            captureHeight = height;
        }
    }

    public static final class Recorder {
        public int textureId, counter, pboIndex;
        public int[] pbos;
        public WritableByteChannel channel;
        public final ByteArrayOutputStream frames = new ByteArrayOutputStream();

        public boolean isRecording() {
            return true;
        }

        public void recordFrame() {
            int size = 32 * 24 * 3;
            ByteBuffer guarded = MemoryUtil.memAlloc(size * 8);
            try {
                for (int i = size; i < guarded.capacity(); i++) guarded.put(i, (byte) 0x5a);
                ByteBuffer pixels = guarded.duplicate();
                pixels.limit(size);
                pixels = pixels.slice();
                // Deliberately matches upstream: no protective PixelTransferState in the recorder.
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                GL11.glGetTexImage(
                        GL11.GL_TEXTURE_2D, 0, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, pixels);
                CoreTests.check(
                        GL11.glGetError() == 0,
                        "unprotected recorder inherited bad PACK/PBO state");
                for (int i = size; i < guarded.capacity(); i++)
                    if (guarded.get(i) != (byte) 0x5a)
                        throw new AssertionError("recording readback overran nominal frame");
                byte[] data = new byte[size];
                pixels.get(data);
                frames.write(data, 0, data.length);
                counter++;
            } finally {
                MemoryUtil.memFree(guarded);
            }
        }
    }

    private static void set(String name, Object value) throws Exception {
        Field field = RecordingBridge.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void flag(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static int fbo(int texture) {
        int f = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, f);
        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        return f;
    }

    public static void main(String[] args) throws Exception {
        CoreTests.check(GLFW.glfwInit(), "GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(32, 32, "BBS Spatial GUI GPU checks", 0, 0);
        CoreTests.check(window != 0, "hidden context");
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        RenderSystem.initRenderThread();
        RenderSystem.setShaderLights(new Vector3f(1, 0, 0), new Vector3f(0, 1, 0));
        try {
            CoreTests.check(
                    mcaxolotl.bbsspatialgui.cursor.CursorState.visible(window),
                    "normal cursor unexpectedly hidden");
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
            CoreTests.check(
                    !mcaxolotl.bbsspatialgui.cursor.CursorState.visible(window),
                    "hidden cursor included");
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            var nativeCursor = mcaxolotl.bbsspatialgui.cursor.NativeCursor.capture();
            if (nativeCursor != null) {
                CoreTests.check(
                        nativeCursor.nativeImage()
                                && nativeCursor.argb().length
                                        == nativeCursor.width() * nativeCursor.height(),
                        "native cursor image layout");
                System.out.println(
                        "PASS native cursor image "
                                + nativeCursor.width()
                                + "x"
                                + nativeCursor.height()
                                + " hot="
                                + nativeCursor.hotX()
                                + ","
                                + nativeCursor.hotY());
            } else
                System.out.println(
                        "SKIP native cursor image: backend unavailable; standard types tested independently");
            var stackEntry = RenderSystem.getModelViewStack().method_23760();
            stackEntry.method_23762().rotateY(.7f);
            stackEntry.method_23761().translate(3, 4, 5);
            var originalNormal = new org.joml.Matrix3f(stackEntry.method_23762());
            var originalTop = new org.joml.Matrix4f(stackEntry.method_23761());
            var originalApplied = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix());
            GL11.glCullFace(GL11.GL_BACK);
            try (RenderPassScope scope = new RenderPassScope()) {
                GL11.glCullFace(GL11.GL_FRONT);
                stackEntry.method_23762().identity();
            }
            CoreTests.check(
                    originalNormal.equals(stackEntry.method_23762())
                            && originalTop.equals(stackEntry.method_23761())
                            && originalApplied.equals(RenderSystem.getModelViewMatrix()),
                    "scope changed normal/top/applied matrices");
            CoreTests.check(
                    GL11.glGetInteger(GL11.GL_CULL_FACE_MODE) == GL11.GL_BACK,
                    "scope lost cull mode");
            RenderSystem.getModelViewStack().method_34426();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderGameTime(12000, 0);
            float saved = RenderSystem.getShaderGameTime();
            ShaderClock.restore(saved);
            CoreTests.check(
                    RenderSystem.getShaderGameTime() == saved, "shader normalized clock lost");
            TestTexture gui = new TestTexture(), child = new TestTexture();
            gui.resize(64, 64);
            child.resize(64, 64);
            int outer = fbo(gui.glId()), inner = fbo(child.glId());
            flag(RenderContext.class, "spatialScreen", true);
            flag(RenderContext.class, "guiFramebuffer", outer);
            Method wrap =
                    FramebufferFormStateMixin.class.getDeclaredMethod(
                            "bsg$scopedForm",
                            Object.class,
                            Object.class,
                            Object.class,
                            Operation.class);
            wrap.setAccessible(true);
            FormHooks hooks = new FormHooks();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, outer);
            Operation<Void> nested =
                    a -> {
                        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, inner);
                        try {
                            Operation<Void> effect =
                                    b -> {
                                        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                                        throw new IllegalStateException(
                                                "intentional nested render failure");
                                    };
                            wrap.invoke(hooks, hooks, null, null, effect);
                        } catch (InvocationTargetException expected) {
                            CoreTests.check(
                                    expected.getCause() instanceof IllegalStateException,
                                    "unexpected nested failure");
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                        CoreTests.check(
                                GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == inner,
                                "inner scope stole outer state");
                        return null;
                    };
            wrap.invoke(hooks, hooks, null, null, nested);
            CoreTests.check(
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == outer,
                    "outer scope not restored");
            flag(RenderContext.class, "spatialScreen", false);
            int pbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pbo);
            GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, 65536, GL15.GL_STATIC_DRAW);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 512);
            byte[] tile = new byte[16 * 16 * 4];
            for (int i = 0; i < tile.length; i++) tile[i] = (byte) (i * 37);
            child.uploadTile(0, 0, 16, 16, tile);
            CoreTests.check(
                    GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING) == pbo
                            && GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH) == 512,
                    "pixel transfer leaked state");
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL15.glDeleteBuffers(pbo);
            Recorder recorder = new Recorder();
            set("isRecording", Recorder.class.getMethod("isRecording"));
            set("recordFrame", Recorder.class.getMethod("recordFrame"));
            set("canRender", State.class.getField("canRender"));
            set("worldSession", State.class.getField("worldSession"));
            for (String field : new String[] {"pbos", "pboIndex", "channel", "counter"})
                set(field, Recorder.class.getField(field));
            recorder.textureId = RecordingBridge.begin(recorder, child.glId(), 32, 24);
            for (int frame = 0; frame < 3; frame++) {
                // Change preview dimensions while the encoder's output stays exactly 32x24.
                child.resize(48 + frame * 8, 32 + frame * 8);
                int src = fbo(child.glId());
                GL11.glClearColor(frame == 0 ? 1 : 0, frame == 1 ? 1 : 0, frame == 2 ? 1 : 0, 1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                RecordingBridge.beginFrame();
                CoreTests.check(
                        RecordingBridge.skipVideo(recorder), "early recording not deferred");
                flag(BackgroundWorldPass.class, "active", true);
                CoreTests.check(
                        RecordingBridge.skipVideo(recorder) && RecordingBridge.skipAudio(),
                        "auxiliary world recorded");
                flag(BackgroundWorldPass.class, "active", false);
                CoreTests.check(
                        !RecordingBridge.skipAudio() && RecordingBridge.skipAudio(),
                        "duplicate audio frame");
                GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 64);
                GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 2);
                GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 3);
                RecordingBridge.afterFrame();
                RecordingBridge.afterFrame();
                CoreTests.check(
                        GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH) == 64
                                && GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS) == 2
                                && GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS) == 3,
                        "recording did not restore inherited PACK state");
                GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
                GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
                GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
                GL30.glDeleteFramebuffers(src);
            }
            CoreTests.check(
                    recorder.counter == 3 && recorder.frames.size() == 3 * 32 * 24 * 3,
                    "duplicate/wrong-size video output");
            File raw = File.createTempFile("bbs-spatial-gui-frames-", ".bgr");
            try (var out = new FileOutputStream(raw)) {
                recorder.frames.writeTo(out);
            }
            File movie = new File(raw.getPath() + ".mp4");
            Process encoder =
                    new ProcessBuilder(
                                    "ffmpeg",
                                    "-v",
                                    "error",
                                    "-y",
                                    "-f",
                                    "rawvideo",
                                    "-pixel_format",
                                    "bgr24",
                                    "-video_size",
                                    "32x24",
                                    "-framerate",
                                    "30",
                                    "-i",
                                    raw.getPath(),
                                    "-c:v",
                                    "libx264",
                                    "-pix_fmt",
                                    "yuv420p",
                                    movie.getPath())
                            .inheritIO()
                            .start();
            CoreTests.check(encoder.waitFor() == 0, "ffmpeg output failed");
            Process decoder =
                    new ProcessBuilder(
                                    "ffmpeg",
                                    "-v",
                                    "error",
                                    "-i",
                                    movie.getPath(),
                                    "-f",
                                    "null",
                                    "-")
                            .inheritIO()
                            .start();
            CoreTests.check(decoder.waitFor() == 0, "generated video invalid");
            raw.delete();
            movie.delete();
            recorder.pbos = new int[] {GL15.glGenBuffers(), GL15.glGenBuffers()};
            recorder.pboIndex = 1;
            ByteBuffer pending = MemoryUtil.memAlloc(12);
            for (int i = 0; i < 12; i++) pending.put((byte) (i + 1));
            pending.flip();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, recorder.pbos[0]);
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, pending, GL15.GL_STREAM_READ);
            ByteArrayOutputStream tail = new ByteArrayOutputStream();
            recorder.channel = Channels.newChannel(tail);
            RecordingBridge.beforeStop(recorder);
            RecordingBridge.beforeStop(recorder);
            CoreTests.check(tail.size() == 12, "last PBO frame missing or duplicated");
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            for (int id : recorder.pbos) GL15.glDeleteBuffers(id);
            MemoryUtil.memFree(pending);
            RecordingBridge.close(recorder);
            State.world = true;
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, outer);
            GL11.glClearColor(1, 0, 0, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            set(
                    "displaySource",
                    (java.util.function.Supplier<RecordingBridge.Source>)
                            () -> new RecordingBridge.Source(outer, 64, 64));
            set("pointerScreen", (java.util.function.BooleanSupplier) () -> false);
            Recorder screen = new Recorder();
            screen.textureId = RecordingBridge.begin(screen, child.glId(), 32, 24);
            set(
                    "setCustomSize",
                    State.class.getMethod("setCustomSize", boolean.class, int.class, int.class));
            set("editingScreen", (java.util.function.BooleanSupplier) () -> false);
            State.captureEnabled = false;
            State.captureWidth = 999;
            State.captureHeight = 999;
            RecordingBridge.beginFrame();
            CoreTests.check(
                    State.captureEnabled && State.captureWidth == 32 && State.captureHeight == 24,
                    "Esc did not hand dimensions back to F4 recording");
            RecordingBridge.skipVideo(screen);
            RecordingBridge.afterFrame();
            byte[] displayed = screen.frames.toByteArray();
            int centre = ((24 / 2) * 32 + 16) * 3;
            CoreTests.check(
                    (displayed[centre + 2] & 255) == 255 && displayed[centre] == 0,
                    "F4/F6 selected preview instead of final display");
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, outer);
            RecordingBridge.close(screen);
            CoreTests.check(
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == outer,
                    "recorder close stole GUI framebuffer");
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL30.glDeleteFramebuffers(outer);
            GL30.glDeleteFramebuffers(inner);
            gui.close();
            child.close();
            BlurShaderChecks.verify();
            CoreTests.check(GL11.glGetError() == 0, "GL error");
            System.out.println(
                    "PASS GPU: PACK guards, full matrix restoration, panel ownership vs shared recorder, Esc export lease, F4/F6 final-display source, nested exceptions, shader time, PBO/stride, fixed-size recording across preview resize, frame/audio deduplication, final PBO drain, actual MP4 encode/decode");
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }
}
