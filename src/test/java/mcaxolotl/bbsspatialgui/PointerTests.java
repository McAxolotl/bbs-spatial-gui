package mcaxolotl.bbsspatialgui;

import java.util.Arrays;

public final class PointerTests {
    public static void main(String[] args) throws Exception {
        java.util.Set<Integer> glyphHashes = new java.util.HashSet<>();
        for (int shape = 0x36001; shape <= 0x3600a; shape++) {
            var sprite = mcaxolotl.bbsspatialgui.cursor.CursorGlyphs.standard(shape);
            if (sprite.hotX() < 0
                    || sprite.hotX() >= sprite.width()
                    || sprite.hotY() < 0
                    || sprite.hotY() >= sprite.height())
                throw new AssertionError("cursor hot spot");
            glyphHashes.add(Arrays.hashCode(sprite.argb()));
        }
        if (glyphHashes.size() != 10)
            throw new AssertionError("cursor types collapsed to the same arrow");
        mcaxolotl.bbsspatialgui.cursor.CursorState.selected(
                org.lwjgl.glfw.GLFW.GLFW_HAND_CURSOR, true);
        if (mcaxolotl.bbsspatialgui.cursor.CursorState.standardShape(true)
                        != org.lwjgl.glfw.GLFW.GLFW_HAND_CURSOR
                || mcaxolotl.bbsspatialgui.cursor.CursorState.standardShape(false)
                        != org.lwjgl.glfw.GLFW.GLFW_ARROW_CURSOR)
            throw new AssertionError("cursor type leaked between BBS and vanilla");
        mcaxolotl.bbsspatialgui.cursor.CursorState.selected(
                org.lwjgl.glfw.GLFW.GLFW_IBEAM_CURSOR, false);
        if (mcaxolotl.bbsspatialgui.cursor.CursorState.standardShape(true)
                != org.lwjgl.glfw.GLFW.GLFW_ARROW_CURSOR)
            throw new AssertionError("null native handle not handled");
        if (!mcaxolotl.bbsspatialgui.capture.RecordingBridge.recordPointerOn(new Object())
                || mcaxolotl.bbsspatialgui.capture.RecordingBridge.recordPointerOn(null))
            throw new AssertionError("vanilla screen cursor excluded from recording");
        System.out.println(
                "PASS dynamic cursor types=10, vanilla recording eligibility, cursor selection and handle fallback");
    }
}
