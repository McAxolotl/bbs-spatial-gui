package mcaxolotl.bbsspatialgui.render;

import com.mojang.blaze3d.systems.RenderSystem;

public final class FilmCurveSnapshot {
    private static boolean playerWorld() {
        // World ticks read the same getter as renderSky. Allowing a film value
        // outside capture feeds it back into ClientWorld.tickTime's stored clock.
        // The integrated server also calls BBSRendering: leave that thread alone.
        return RenderSystem.isOnRenderThread()
                && (BackgroundWorldPass.active()
                        || (RenderContext.bbsCustomSizeActive()
                                && !RenderContext.bbsWorldCaptureActive()));
    }

    public static Long time(Long value) {
        return playerWorld() ? null : value;
    }

    public static Double brightness(Double value) {
        return playerWorld() ? null : value;
    }

    public static Double weather(Double value) {
        return playerWorld() ? null : value;
    }

    public static Float sunRotation(Float value) {
        return playerWorld() ? 0.0f : value;
    }

    public static Integer skyColor(Integer value) {
        return playerWorld() ? null : value;
    }

    private FilmCurveSnapshot() {}
}
