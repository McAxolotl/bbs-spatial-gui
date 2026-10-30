package mcaxolotl.bbsspatialgui.compat;

import net.fabricmc.loader.api.FabricLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Files;

public final class BBSDetector {
    public static boolean hasClass(String name) {
        var mod = FabricLoader.getInstance().getModContainer("bbs");
        return mod.isPresent() && mod.get().findPath(name.replace('.', '/') + ".class").isPresent();
    }

    public static boolean recordingSupported() {
        if (!hasClass("mchorse.bbs_mod.film.WorldVideoExportSession")) return false;
        try {
            var path =
                    FabricLoader.getInstance()
                            .getModContainer("bbs")
                            .orElseThrow()
                            .findPath("mchorse/bbs_mod/utils/VideoRecorder.class")
                            .orElseThrow();
            ClassNode type = new ClassNode();
            new ClassReader(Files.readAllBytes(path)).accept(type, ClassReader.SKIP_CODE);
            return java.util.Set.of("pbos", "pboIndex", "channel", "counter", "textureId").stream()
                    .allMatch(
                            name ->
                                    type.fields.stream()
                                            .anyMatch(field -> field.name.equals(name)));
        } catch (java.io.IOException | java.util.NoSuchElementException e) {
            return false;
        }
    }

    public static void report() {
        var loader = FabricLoader.getInstance();
        var providers =
                loader.getAllMods().stream()
                        .filter(
                                mod ->
                                        mod.findPath("mchorse/bbs_mod/ui/framework/UIScreen.class")
                                                .isPresent())
                        .toList();
        if (providers.size() > 1)
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Multiple incompatible BBS GUI providers: "
                            + providers.stream().map(m -> m.getMetadata().getId()).toList()
                            + ". Install one BBS distribution.");
        loader.getModContainer("bbs")
                .ifPresent(
                        mod -> {
                            var metadata = mod.getMetadata();
                            String name = metadata.getName();
                            System.out.println(
                                    "[BBS Spatial GUI] BBS metadata: id="
                                            + metadata.getId()
                                            + " name="
                                            + name
                                            + " version="
                                            + metadata.getVersion()
                                            + " recordingAdapter="
                                            + recordingSupported());
                        });
    }

    private BBSDetector() {}
}
