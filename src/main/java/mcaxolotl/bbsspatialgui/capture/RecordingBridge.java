package mcaxolotl.bbsspatialgui.capture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import mcaxolotl.bbsspatialgui.BBSOptions;
import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderPassScope;

import net.minecraft.class_310;
import net.minecraft.class_6367;

import org.lwjgl.opengl.*;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public final class RecordingBridge {
    private static Object recorder;
    private static class_6367 target;
    private static int sourceTexture, sourceFbo;
    private static boolean screenMode, worldMode, requested, late, flushed;
    private static net.minecraft.class_276 displayFramebuffer;
    private static Method setCustomSize;
    private static Field bbsClientFramebuffer, bbsToggle;
    private static long frame, lastAudioFrame = -1, lastVideoFrame = -1;
    private static Method isRecording, recordFrame;
    private static Field worldSession, canRender, pbos, pboIndex, channel, counter;

    public record Source(int framebuffer, int width, int height) {}

    private static java.util.function.Supplier<Source> displaySource =
            () -> {
                var main =
                        displayFramebuffer != null
                                ? displayFramebuffer
                                : class_310.method_1551().method_1522();
                return new Source(main.field_1476, main.field_1480, main.field_1477);
            };
    private static java.util.function.BooleanSupplier pointerScreen =
            () -> recordPointerOn(class_310.method_1551().field_1755);
    private static java.util.function.BooleanSupplier editingScreen =
            () -> {
                var screen = class_310.method_1551().field_1755;
                return screen != null
                        && screen.getClass()
                                .getName()
                                .equals("mchorse.bbs_mod.ui.framework.UIScreen");
            };

    private static void initialize() throws ReflectiveOperationException {
        if (isRecording != null) return;
        Class<?> client = Class.forName("mchorse.bbs_mod.BBSModClient"),
                video = Class.forName("mchorse.bbs_mod.utils.VideoRecorder");
        Method recording = video.getMethod("isRecording");
        recordFrame = video.getMethod("recordFrame");
        Class<?> rendering = Class.forName("mchorse.bbs_mod.client.BBSRendering");
        canRender = rendering.getField("canRender");
        setCustomSize = rendering.getMethod("setCustomSize", boolean.class, int.class, int.class);
        bbsClientFramebuffer = field(rendering, "clientFramebuffer");
        bbsToggle = field(rendering, "toggleFramebuffer");
        worldSession = client.getDeclaredField("worldExportSession");
        worldSession.setAccessible(true);
        pbos = field(video, "pbos");
        pboIndex = field(video, "pboIndex");
        channel = field(video, "channel");
        counter = field(video, "counter");
        isRecording = recording;
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static void beginFrame() {
        frame++;
        requested = false;
        if (recorder == null || !worldMode || setCustomSize == null) return;
        if (editingScreen.getAsBoolean()) return;
        try {
            // The editor releases customSize on Esc; the still-running world recording owns it
            // again.
            setCustomSize.invoke(null, true, target.field_1480, target.field_1477);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI recording] Cannot restore world recording dimensions", e);
        }
    }

    public static int begin(Object owner, int original, int width, int height) {
        if (!BBSSpatialGui.enabled()) return original;
        try {
            initialize();
            if (!(boolean) isRecording.invoke(owner)) return original;
            close(recorder);
            recorder = owner;
            sourceTexture = original;
            flushed = false;
            Object session = worldSession.get(null);
            boolean worldRecording =
                    (boolean) session.getClass().getMethod("isExporting").invoke(session);
            worldMode = worldRecording;
            screenMode = BBSSpatialGui.recordSpatialGui() && worldRecording;
            displayFramebuffer = null;
            if (screenMode && bbsToggle != null) {
                // BBS temporarily swaps Minecraft.framebuffer. Keep the actual display, not its
                // export buffer.
                displayFramebuffer =
                        bbsToggle.getBoolean(null)
                                ? (net.minecraft.class_276) bbsClientFramebuffer.get(null)
                                : class_310.method_1551().method_1522();
                if (displayFramebuffer == null)
                    displayFramebuffer = class_310.method_1551().method_1522();
            }
            String entry =
                    worldRecording
                            ? (session.getClass().getMethod("getFilmId").invoke(session) == null
                                    ? "F4"
                                    : "F6")
                            : "panel export";
            try (RenderPassScope state = new RenderPassScope();
                    PixelTransferState pixels = new PixelTransferState(false)) {
                target = new class_6367(width, height, false, false);
                sourceFbo = GL30.glGenFramebuffers();
            }
            System.out.println(
                    "[BBS Spatial GUI recording] fixed target "
                            + width
                            + "x"
                            + height
                            + " entry="
                            + entry
                            + " source="
                            + (screenMode ? "final display" : "film texture"));
            return target.method_30277();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("[BBS Spatial GUI] Recorder adapter mismatch", e);
        }
    }

    public static boolean skipVideo(Object owner) {
        if (BackgroundWorldPass.active()) return true;
        if (owner != recorder) return false;
        if (late) return false;
        requested = true;
        return true;
    }

    public static boolean skipAudio() {
        if (BackgroundWorldPass.active()) return true;
        if (recorder == null) return false;
        if (lastAudioFrame == frame) return true;
        lastAudioFrame = frame;
        return false;
    }

    public static boolean isRecording() {
        return recorder != null;
    }

    public static boolean recordPointerOn(Object screen) {
        return screen != null;
    }

    public static void afterFrame() {
        if (recorder == null || !requested || frame == lastVideoFrame) return;
        try {
            if (!(boolean) isRecording.invoke(recorder) || !canRender.getBoolean(null)) return;
            try (RenderPassScope state = new RenderPassScope()) {
                int src, sw, sh;
                if (screenMode) {
                    Source main = displaySource.get();
                    src = main.framebuffer();
                    sw = main.width();
                    sh = main.height();
                } else {
                    // The preview can resize; only the intermediate source changes, never the
                    // recorder/PBO size.
                    GlStateManager._bindTexture(sourceTexture);
                    sw = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                    sh =
                            GL11.glGetTexLevelParameteri(
                                    GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
                    GL30.glFramebufferTexture2D(
                            GL30.GL_READ_FRAMEBUFFER,
                            GL30.GL_COLOR_ATTACHMENT0,
                            GL11.GL_TEXTURE_2D,
                            sourceTexture,
                            0);
                    src = sourceFbo;
                }
                if (sw <= 0 || sh <= 0)
                    throw new IllegalStateException(
                            "[BBS Spatial GUI recording] Empty source texture");
                copyFrame(src, sw, sh, target.field_1476, target.field_1480, target.field_1477);
                if (screenMode && BBSOptions.recordPointer() && pointerScreen.getAsBoolean()) {
                    double fit =
                            Math.min(
                                    target.field_1480 / (double) sw,
                                    target.field_1477 / (double) sh);
                    int dw = Math.max(1, (int) Math.round(sw * fit)),
                            dh = Math.max(1, (int) Math.round(sh * fit));
                    RecordingPointer.drawScreenPointer(
                            target.field_1480,
                            target.field_1477,
                            (target.field_1480 - dw) / 2,
                            (target.field_1477 - dh) / 2,
                            dw,
                            dh);
                }
                late = true;
                try (PixelTransferState pack = new PixelTransferState(true)) {
                    recordFrame.invoke(recorder);
                    lastVideoFrame = frame;
                } finally {
                    late = false;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Could not write completed recording frame", e);
        }
    }

    public static void copyFrame(int source, int sw, int sh, int output, int width, int height) {
        GlStateManager._disableScissorTest();
        RenderSystem.colorMask(true, true, true, true);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, output);
        RenderSystem.clearColor(0, 0, 0, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        double factor = Math.min(width / (double) sw, height / (double) sh);
        int dw = Math.max(1, (int) Math.round(sw * factor)),
                dh = Math.max(1, (int) Math.round(sh * factor));
        int x = (width - dw) / 2, y = (height - dh) / 2;
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source);
        GL30.glBlitFramebuffer(
                0, 0, sw, sh, x, y, x + dw, y + dh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, output);
        GlStateManager._viewport(0, 0, width, height);
    }

    public static void beforeStop(Object owner) {
        if (owner != recorder || flushed) return;
        flushed = true;
        try {
            int[] buffers = (int[]) pbos.get(owner);
            if (buffers == null || counter.getInt(owner) == 0) return;
            int last = (pboIndex.getInt(owner) + buffers.length - 1) % buffers.length;
            try (PixelTransferState transfer = new PixelTransferState(true)) {
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, buffers[last]);
                ByteBuffer pixels = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY);
                if (pixels == null)
                    throw new IllegalStateException(
                            "[BBS Spatial GUI] Failed to drain last recording PBO");
                try {
                    WritableByteChannel output = (WritableByteChannel) channel.get(owner);
                    while (pixels.hasRemaining()) output.write(pixels);
                } finally {
                    GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
                }
            }
        } catch (java.io.IOException | ReflectiveOperationException e) {
            System.err.println(
                    "[BBS Spatial GUI recording] Final frame drain failed; output may be incomplete: "
                            + e);
            // Still let the recorder close its encoder and release every PBO.
        }
    }

    public static void close(Object owner) {
        if (owner == null || owner != recorder) return;
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int deletedFbo = target == null ? -1 : target.field_1476,
                deletedTexture = target == null ? -1 : target.method_30277();
        if (target != null) target.method_1238();
        if (sourceFbo != 0) GL30.glDeleteFramebuffers(sourceFbo);
        GL30.glBindFramebuffer(
                GL30.GL_DRAW_FRAMEBUFFER, draw == deletedFbo || draw == sourceFbo ? 0 : draw);
        GL30.glBindFramebuffer(
                GL30.GL_READ_FRAMEBUFFER, read == deletedFbo || read == sourceFbo ? 0 : read);
        int restored = texture == deletedTexture ? 0 : texture;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, restored);
        GlStateManager._bindTexture(restored);
        target = null;
        sourceFbo = 0;
        recorder = null;
        displayFramebuffer = null;
        worldMode = false;
        requested = false;
        lastAudioFrame = lastVideoFrame = -1;
    }

    private RecordingBridge() {}
}
