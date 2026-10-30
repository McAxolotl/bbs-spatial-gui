package mcaxolotl.bbsspatialgui.render;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;

import java.lang.reflect.Method;
import java.util.Map;

public final class FilmCurveState {
    private static Method getController, getCurrent, getContext, getCurveValues;
    private static long previousSample;
    private static int samples;
    private static boolean failed;

    public static void sample() {
        if (!BBSSpatialGui.debugLogging() || !RenderContext.currentSpatialBBSScreen()) return;
        long now = System.nanoTime();
        if (samples >= 40 || now - previousSample < 500_000_000L) return;
        try {
            if (getController == null) {
                getController =
                        Class.forName("mchorse.bbs_mod.BBSModClient")
                                .getMethod("getCameraController");
                Class<?> controller =
                        Class.forName("mchorse.bbs_mod.camera.controller.CameraController");
                getCurrent = controller.getMethod("getCurrent");
                Class<?> runner =
                        Class.forName("mchorse.bbs_mod.camera.controller.RunnerCameraController");
                getContext = runner.getMethod("getContext");
                Class<?> curve = Class.forName("mchorse.bbs_mod.camera.clips.misc.CurveClip");
                getCurveValues =
                        curve.getMethod(
                                "getValues",
                                Class.forName("mchorse.bbs_mod.utils.clips.ClipContext"));
            }
            Object controller = getController.invoke(null);
            Object current = getCurrent.invoke(controller);
            if (current == null
                    || !current.getClass()
                            .getName()
                            .equals("mchorse.bbs_mod.camera.controller.RunnerCameraController"))
                return;
            Object context = getContext.invoke(current);
            Object raw = getCurveValues.invoke(null, context);
            previousSample = now;
            samples++;
            if (raw instanceof Map<?, ?> values) {
                Object sun = first(values, "sun_rotation", "curve.sun_rotation");
                Object horizontal =
                        first(values, "sun_horizontal_rotation", "curve.sun_horizontal_rotation");
                Object brightness = first(values, "brightness", "curve.brightness");
                System.out.println(
                        "[BBS Spatial GUI curve] sun_rotation="
                                + sun
                                + " sun_horizontal_rotation="
                                + horizontal
                                + " brightness="
                                + brightness);
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!failed) {
                System.err.println("[BBS Spatial GUI] Curve state unavailable: " + e);
                failed = true;
            }
        }
    }

    private static Object first(Map<?, ?> map, String a, String b) {
        return map.containsKey(a) ? map.get(a) : map.get(b);
    }

    private FilmCurveState() {}
}
