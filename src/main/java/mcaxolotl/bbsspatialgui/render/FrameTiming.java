package mcaxolotl.bbsspatialgui.render;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;

public final class FrameTiming {
    private static Object screen;
    private static long start, previousStart, interval, uiStart, ui, background;
    private static int frame, sessions;
    private static boolean active;

    public static void beginFrame() {
        active = false;
        if (!BBSSpatialGui.debugLogging() || !RenderContext.currentSpatialBBSScreen()) {
            screen = null;
            previousStart = 0;
            return;
        }
        Object current = net.minecraft.class_310.method_1551().field_1755;
        if (screen != current) {
            screen = current;
            frame = 0;
            previousStart = 0;
            sessions++;
        }
        if (frame >= 24 || sessions > 12) return;
        active = true;
        start = System.nanoTime();
        interval = previousStart == 0 ? 0 : start - previousStart;
        previousStart = start;
        ui = background = uiStart = 0;
    }

    public static void beforeUi() {
        if (active) uiStart = System.nanoTime();
    }

    public static void afterUi() {
        if (active && uiStart != 0) {
            ui += System.nanoTime() - uiStart;
            uiStart = 0;
        }
    }

    public static void background(long nanos) {
        if (active) background += nanos;
    }

    public static void endFrame() {
        if (!active) return;
        long elapsed = System.nanoTime() - start;
        active = false;
        System.out.println(
                "[BBS Spatial GUI timing] frame="
                        + frame++
                        + " intervalMs="
                        + interval / 1_000_000.0
                        + " renderMs="
                        + elapsed / 1_000_000.0
                        + " backgroundMs="
                        + background / 1_000_000.0
                        + " uiMs="
                        + ui / 1_000_000.0);
    }

    private FrameTiming() {}
}
