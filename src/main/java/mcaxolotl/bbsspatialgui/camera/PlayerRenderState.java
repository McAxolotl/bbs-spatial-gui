package mcaxolotl.bbsspatialgui.camera;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_1297;
import net.minecraft.class_310;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

import java.util.HashMap;
import java.util.Map;

public final class PlayerRenderState {
    private static final Map<String, Long> last = new HashMap<>();
    private static int samples;

    public static void sample(class_1297 entity, String stage, boolean result) {
        var client = class_310.method_1551();
        if (!BBSSpatialGui.debugLogging()
                || samples >= 120
                || entity != client.field_1724
                || !RenderContext.currentSpatialBBSScreen()) return;
        String phase =
                BackgroundWorldPass.active()
                        ? "background"
                        : RenderContext.bbsWorldCaptureActive() ? "preview" : "world";
        String key = stage + phase;
        long now = System.nanoTime();
        if (now - last.getOrDefault(key, 0L) < 1_000_000_000L) return;
        last.put(key, now);
        samples++;
        var camera = client.field_1773.method_19418();
        var p = camera.method_19326();
        System.out.println(
                "[BBS Spatial GUI player] stage="
                        + stage
                        + " pass="
                        + phase
                        + " result="
                        + result
                        + " firstPerson="
                        + SpatialGUIClient.getEffectiveFirstPersonMode()
                        + " detached="
                        + camera.method_19333()
                        + " camera="
                        + p
                        + " player="
                        + entity.method_19538()
                        + " fbo="
                        + GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
    }

    private PlayerRenderState() {}
}
