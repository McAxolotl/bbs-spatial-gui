package mcaxolotl.bbsspatialgui.capture;

import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

public final class ZeroedPixels {
    private static Constructor<?> constructor;

    public static Object allocate(int width, int height) {
        int bytes = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        ByteBuffer buffer = MemoryUtil.memCalloc(bytes);
        try {
            if (constructor == null) {
                constructor =
                        Class.forName("mchorse.bbs_mod.utils.resources.Pixels")
                                .getConstructor(ByteBuffer.class, int.class, int.class);
            }
            buffer.position(bytes);
            return constructor.newInstance(buffer, width, height);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            MemoryUtil.memFree(buffer);
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Cannot allocate zeroed GUI pixels", e);
        }
    }

    private ZeroedPixels() {}
}
