package mcaxolotl.bbsspatialgui;

import net.fabricmc.loader.api.FabricLoader;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class BBSSpatialGuiMixinPlugin implements IMixinConfigPlugin {
    public static volatile boolean cameraOrderVerified;

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
        for (var method : node.methods) {
            if (!method.name.equals("method_19321")) continue;
            int index = 0;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(node.name)) {
                    if (call.name.contains("$bbs$onUpdate")) bbs = index;
                    if (call.name.contains("$spatialGUI$modifyCamera")) spatial = index;
                    if (call.name.contains("$bsg$observeCamera")) bridge = index;
                }
                index++;
            }
        }
        cameraOrderVerified = bbs >= 0 && spatial > bbs && bridge > spatial;
        if (cameraOrderVerified) {
            for (var method : node.methods) {
                if (!method.name.equals("method_19321")) continue;
                // Snapshot vanilla pose before BBS, restore it before Spatial computes
                // its animation. Existing callback arguments stay on the operand stack.
                for (var instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode call)
                            || !call.owner.equals(node.name)) continue;
                    String hook =
                            call.name.contains("$bbs$onUpdate")
                                    ? "beforeBBSCamera"
                                    : call.name.contains("$spatialGUI$modifyCamera")
                                            ? "beforeSpatialCamera"
                                            : null;
                    if (hook == null) continue;
                    InsnList prefix = new InsnList();
                    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    prefix.add(
                            new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "mcaxolotl/bbsspatialgui/camera/FilmCameraBridge",
                                    hook,
                                    "(Lnet/minecraft/class_4184;)V",
                                    false));
                    method.instructions.insertBefore(call, prefix);
                }
                method.maxStack += 1;
            }
        }
        System.out.println(
                "[BBS Spatial GUI] Film camera callback order verified="
                        + cameraOrderVerified
                        + " bbs="
                        + bbs
                        + " spatial="
                        + spatial
                        + " bridge="
                        + bridge);
    }
}
