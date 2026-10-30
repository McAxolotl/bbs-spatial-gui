package mcaxolotl.bbsspatialgui.camera;

import mcaxolotl.bbsspatialgui.BBSSpatialGui;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_4184;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CameraState {
    private static Method getController, getCurrent, getYaw, getPitch;
    private static Field panel;
    private static Method isFlying;
    private static long previousSample;
    private static int samples;
    private static boolean reported;

    public static void sample(class_4184 camera) {
        if (!BBSSpatialGui.debugLogging() || !RenderContext.currentSpatialBBSScreen()) return;
        long now = System.nanoTime();
        if (samples >= 60 || now - previousSample < 1_000_000_000L) return;
        try {
            if (getController == null) {
                getController =
                        Class.forName("mchorse.bbs_mod.BBSModClient")
                                .getMethod("getCameraController");
                Class<?> type = Class.forName("mchorse.bbs_mod.camera.controller.CameraController");
                getCurrent = type.getMethod("getCurrent");
                getYaw = type.getMethod("getYaw");
                getPitch = type.getMethod("getPitch");
                Class<?> runner =
                        Class.forName("mchorse.bbs_mod.camera.controller.RunnerCameraController");
                panel = runner.getDeclaredField("panel");
                panel.setAccessible(true);
                isFlying =
                        Class.forName("mchorse.bbs_mod.ui.film.UIFilmPanel").getMethod("isFlying");
            }
            Object controller = getController.invoke(null);
            Object current = getCurrent.invoke(controller);
            if (current == null
                    || !current.getClass()
                            .getName()
                            .equals("mchorse.bbs_mod.camera.controller.RunnerCameraController"))
                return;
            boolean flying = (boolean) isFlying.invoke(panel.get(current));
            previousSample = now;
            samples++;
            System.out.println(
                    "[BBS Spatial GUI camera] flying="
                            + flying
                            + " bbsYaw="
                            + getYaw.invoke(controller)
                            + " bbsPitch="
                            + getPitch.invoke(controller)
                            + " renderYaw="
                            + camera.method_19330()
                            + " renderPitch="
                            + camera.method_19329()
                            + " separated="
                            + FilmCameraBridge.active()
                            + " panelYaw="
                            + FilmCameraBridge.panelYaw()
                            + " panelPitch="
                            + FilmCameraBridge.panelPitch());
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!reported) {
                System.err.println("[BBS Spatial GUI] Cannot sample BBS camera state: " + e);
                reported = true;
            }
        }
    }

    private CameraState() {}
}
