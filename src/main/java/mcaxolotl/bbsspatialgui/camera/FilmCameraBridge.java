package mcaxolotl.bbsspatialgui.camera;

import mcaxolotl.bbsspatialgui.BBSSpatialGuiMixinPlugin;
import mcaxolotl.bbsspatialgui.mixin.CameraPoseAccess;
import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.ModelBlockPreview;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import net.minecraft.class_310;
import net.minecraft.class_4184;

import org.joml.Vector3d;

import java.lang.reflect.Method;

public final class FilmCameraBridge {
    public record Pose(double x, double y, double z, float yaw, float pitch) {}

    private static Method getController, getCurrent, getPosition, getYaw, getPitch;
    private static boolean active, reportedFailure;
    private static float panelYaw, panelPitch;
    private static Pose vanillaPose, spatialPose;
    private static boolean nativeDimensions;

    public static void beginFrame() {
        spatialPose = null;
        nativeDimensions = false;
    }

    public static void beginCamera() {
        nativeDimensions = RenderContext.currentSpatialBBSScreen();
    }

    public static void endCamera() {
        nativeDimensions = false;
    }

    public static boolean nativeDimensions() {
        return nativeDimensions;
    }

    public static void finishFrame() {
        if (active && spatialPose != null && RenderContext.currentSpatialBBSScreen()) {
            // UI previews needed the BBS pose earlier in the frame. Once composition
            // finishes, expose the actually displayed pose to screen lifecycle code.
            apply(class_310.method_1551().field_1773.method_19418(), spatialPose);
        }
    }

    private static Pose snapshot(class_4184 camera) {
        var p = camera.method_19326();
        return new Pose(
                p.field_1352,
                p.field_1351,
                p.field_1350,
                camera.method_19330(),
                camera.method_19329());
    }

    private static void apply(class_4184 camera, Pose pose) {
        var access = (CameraPoseAccess) camera;
        access.bsg$setPosition(pose.x(), pose.y(), pose.z());
        access.bsg$setRotation(pose.yaw(), pose.pitch());
    }

    // Installed at the head of the camera update by CameraStateMixin. The snapshot must happen
    // before any other mod's camera callback, whichever order they end up in.
    public static void captureVanillaPose(class_4184 camera) {
        if (camera == class_310.method_1551().field_1773.method_19418()
                && nativeDimensions
                && !BackgroundWorldPass.active()) vanillaPose = snapshot(camera);
    }

    // Installed immediately after Spatial GUI's camera callback by BBSSpatialGuiMixinPlugin.
    // Capturing here keeps the displayed panel pose correct no matter whether BBS runs before or
    // after Spatial GUI.
    public static void captureSpatialPose(class_4184 camera) {
        if (camera == class_310.method_1551().field_1773.method_19418()
                && nativeDimensions
                && !BackgroundWorldPass.active()) {
            spatialPose = snapshot(camera);
            panelYaw = camera.method_19330();
            panelPitch = camera.method_19329();
        }
    }

    public static void beforeSpatialCamera(class_4184 camera) {
        if (camera == class_310.method_1551().field_1773.method_19418()
                && nativeDimensions
                && vanillaPose != null
                && !BackgroundWorldPass.active()) apply(camera, vanillaPose);
    }

    public static boolean replaySpatialCamera(class_4184 camera) {
        if (!BackgroundWorldPass.active()
                || spatialPose == null
                || !BBSSpatialGuiMixinPlugin.cameraHooksVerified) return false;
        apply(camera, spatialPose);
        return true;
    }

    public static Pose afterSpatialCamera(class_4184 camera) {
        if (BackgroundWorldPass.active()) return null;
        active = false;
        if (!BBSSpatialGuiMixinPlugin.cameraHooksVerified
                || !RenderContext.currentSpatialBBSScreen()) return null;
        try {
            if (getController == null) {
                getController =
                        Class.forName("mchorse.bbs_mod.BBSModClient")
                                .getMethod("getCameraController");
                Class<?> type = Class.forName("mchorse.bbs_mod.camera.controller.CameraController");
                getCurrent = type.getMethod("getCurrent");
                getPosition = type.getMethod("getPosition");
                getYaw = type.getMethod("getYaw");
                getPitch = type.getMethod("getPitch");
            }
            Object controller = getController.invoke(null);
            Object current = getCurrent.invoke(controller);
            if (current == null) return null;
            String name = current.getClass().getName();
            boolean film = name.equals("mchorse.bbs_mod.camera.controller.RunnerCameraController");
            boolean modelBlock =
                    ModelBlockPreview.active()
                            && (name.equals(
                                            "mchorse.bbs_mod.ui.model_blocks.camera.OrbitModelBlockCameraController")
                                    || name.equals(
                                            "mchorse.bbs_mod.ui.model_blocks.camera.ImmersiveModelBlockCameraController"));
            if (!film && !modelBlock) return null;
            Vector3d position = (Vector3d) getPosition.invoke(controller);
            float yaw = ((Number) getYaw.invoke(controller)).floatValue();
            float pitch = ((Number) getPitch.invoke(controller)).floatValue();
            active = true;
            return new Pose(position.x, position.y, position.z, yaw, pitch);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!reportedFailure) {
                System.err.println("[BBS Spatial GUI] Film camera separation disabled: " + e);
                reportedFailure = true;
            }
            return null;
        }
    }

    public static boolean active() {
        return active && RenderContext.currentSpatialBBSScreen();
    }

    public static float panelYaw() {
        return panelYaw;
    }

    public static float panelPitch() {
        return panelPitch;
    }

    public static void updatePanelPose(float yaw, float pitch) {
        panelYaw = yaw;
        panelPitch = pitch;
    }

    private FilmCameraBridge() {}
}
