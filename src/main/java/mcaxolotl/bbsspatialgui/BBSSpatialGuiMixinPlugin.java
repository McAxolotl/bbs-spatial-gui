package mcaxolotl.bbsspatialgui;

import net.fabricmc.loader.api.FabricLoader;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class BBSSpatialGuiMixinPlugin implements IMixinConfigPlugin {
    public static volatile boolean cameraHooksVerified;

    public void onLoad(String mixinPackage) {}

    public String getRefMapperConfig() {
        return null;
    }

    private static final Set<String> VANILLA_ONLY =
            Set.of(
                    "HudDispatchMixin",
                    "CameraStateMixin",
                    "CameraPoseAccess",
                    "NativeCrosshairMixin",
                    "NativeCrosshairVisibilityMixin",
                    "WorldPassLightmapMixin",
                    "BackgroundPlayerCameraMixin",
                    "PlayerSubmissionMixin",
                    "PlayerModelMixin",
                    "GuiFramebufferRestoreMixin",
                    "GuiAlphaBlendMixin",
                    "PickerShaderBlendMixin");

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        if (VANILLA_ONLY.contains(simple)) return true;
        if ((simple.equals("VideoRecorderMixin") || simple.equals("RecordingAudioMixin"))
                && !mcaxolotl.bbsspatialgui.compat.BBSDetector.recordingSupported()) return false;
        if (targetClassName.startsWith("mchorse.")
                && !mcaxolotl.bbsspatialgui.compat.BBSDetector.hasClass(targetClassName))
            return false;
        return FabricLoader.getInstance().isModLoaded("bbs")
                && FabricLoader.getInstance().isModLoaded("spatial_gui");
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}

    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {
        if (mixin.endsWith(".SettingsOrderMixin")) {
            boolean ordered = false;
            for (var method : node.methods)
                if (method.name.equals("<init>"))
                    for (var instruction : method.instructions)
                        if (instruction instanceof MethodInsnNode call
                                && call.name.contains("bsg$orderedSettings")) ordered = true;
            System.out.println("[BBS Spatial GUI] Settings sidebar order hook verified=" + ordered);
            return;
        }
        if (!mixin.endsWith(".CameraStateMixin")) return;
        int bbs = -1, spatial = -1, bridge = -1;
        MethodInsnNode spatialCall = null;
        MethodNode cameraUpdate = null;
        for (var method : node.methods) {
            if (!method.name.equals("method_19321")) continue;
            cameraUpdate = method;
            int index = 0;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(node.name)) {
                    if (call.name.contains("$bbs$onUpdate")) bbs = index;
                    if (call.name.contains("$spatialGUI$modifyCamera")) {
                        spatial = index;
                        spatialCall = call;
                    }
                    if (call.name.contains("$bsg$observeCamera")) bridge = index;
                }
                index++;
            }
        }
        // The two camera callbacks can be applied in either order depending on mod load order.
        // Capture the panel pose right after Spatial GUI's callback and only restore the vanilla
        // pose when BBS already changed the camera before it, so both orders behave the same.
        cameraHooksVerified = bbs >= 0 && spatial >= 0 && bridge >= 0;
        if (cameraHooksVerified && cameraUpdate != null && spatialCall != null) {
            InsnList after = new InsnList();
            after.add(new VarInsnNode(Opcodes.ALOAD, 0));
            after.add(
                    new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "mcaxolotl/bbsspatialgui/camera/FilmCameraBridge",
                            "captureSpatialPose",
                            "(Lnet/minecraft/class_4184;)V",
                            false));
            cameraUpdate.instructions.insert(spatialCall, after);
            if (bbs < spatial) {
                InsnList prefix = new InsnList();
                prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                prefix.add(
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "mcaxolotl/bbsspatialgui/camera/FilmCameraBridge",
                                "beforeSpatialCamera",
                                "(Lnet/minecraft/class_4184;)V",
                                false));
                cameraUpdate.instructions.insertBefore(spatialCall, prefix);
            }
            cameraUpdate.maxStack += 2;
        }
        System.out.println(
                "[BBS Spatial GUI] Film camera hooks verified="
                        + cameraHooksVerified
                        + " order="
                        + (bbs < 0 || spatial < 0
                                ? "unknown"
                                : bbs < spatial ? "bbs-first" : "spatial-first")
                        + " bbs="
                        + bbs
                        + " spatial="
                        + spatial
                        + " bridge="
                        + bridge);
    }
}
