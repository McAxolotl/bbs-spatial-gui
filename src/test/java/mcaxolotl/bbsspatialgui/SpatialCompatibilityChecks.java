package mcaxolotl.bbsspatialgui;

import mcaxolotl.bbsspatialgui.ui.SpatialUiScale;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Guards the Spatial GUI integration against upstream API drift: checks the installed Spatial GUI
 * version against the dependency range declared in fabric.mod.json, then verifies every mixin
 * injection point and reflected member the addon relies on against the actual upstream classes.
 */
public final class SpatialCompatibilityChecks {
    private static final String SPATIAL_ROOT = "org.tastytrash.spatialGUI.";
    private static final Path MOD_JSON =
            Path.of(System.getProperty("bsg.modJson", "src/main/resources/fabric.mod.json"));
    private static final Path MIXIN_JSON =
            Path.of(
                    System.getProperty(
                            "bsg.mixinsJson", "src/main/resources/bbs_spatial_gui.mixins.json"));

    private static Class<?> type(String simpleName) throws Exception {
        return Class.forName(
                SPATIAL_ROOT + simpleName.replace('/', '.'),
                false,
                SpatialCompatibilityChecks.class.getClassLoader());
    }

    private static void method(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            owner.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    owner.getName()
                            + "."
                            + name
                            + " is missing; the addon was built for a newer API than "
                            + "the installed jar");
        }
    }

    private static void anyMethod(Class<?> owner, String name) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (var method : current.getDeclaredMethods())
                if (method.getName().equals(name)) return;
        }
        throw new AssertionError(owner.getName() + " has no method named " + name);
    }

    private static void field(Class<?> owner, String name) {
        try {
            owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(owner.getName() + " has no field named " + name);
        }
    }

    private static String installedVersion() throws Exception {
        var source =
                type("SpatialGUI").getProtectionDomain().getCodeSource().getLocation();
        try (var zip = new ZipFile(Path.of(source.toURI()).toFile())) {
            var entry = zip.getEntry("fabric.mod.json");
            if (entry == null) throw new AssertionError("Spatial GUI jar ships no fabric.mod.json");
            try (InputStream stream = zip.getInputStream(entry)) {
                var metadata = stream.readAllBytes();
                var json =
                        com.google.gson.JsonParser.parseString(
                                        new String(metadata, StandardCharsets.UTF_8))
                                .getAsJsonObject();
                return json.get("version").getAsString();
            }
        }
    }

    private static String declaredSpatialRange() throws Exception {
        var json =
                com.google.gson.JsonParser.parseString(Files.readString(MOD_JSON))
                        .getAsJsonObject();
        return json.getAsJsonObject("depends").get("spatial_gui").getAsString();
    }

    /** Collects @Mixin targets and @Inject/@Redirect/@At points from one compiled mixin class. */
    private static final class MixinScan extends ClassVisitor {
        private final Set<String> targets = new LinkedHashSet<>();
        private final Set<String> injectedMethods = new LinkedHashSet<>();
        private final Set<String> atTargets = new LinkedHashSet<>();
        private final Set<String> shadowFields = new LinkedHashSet<>();

        MixinScan() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) return null;
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitArray(String name) {
                    if (!name.equals("targets")) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String name, Object value) {
                            targets.add(String.valueOf(value).replace('.', '/'));
                        }
                    };
                }
            };
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(
                int access, String name, String descriptor, String signature, Object value) {
            final String fieldName = name;
            return new org.objectweb.asm.FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    if (annotation.equals("Lorg/spongepowered/asm/mixin/Shadow;")) shadowFields.add(fieldName);
                    return null;
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    String kind = annotation.substring(1, annotation.length() - 1);
                    if (!kind.startsWith("org/spongepowered/asm/mixin/injection/")) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitArray(String element) {
                            if (!element.equals("method")) return null;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String ignored, Object value) {
                                    injectedMethods.add(String.valueOf(value));
                                }
                            };
                        }

                        @Override
                        public void visit(String element, Object value) {
                            if (element.equals("method")) injectedMethods.add(String.valueOf(value));
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String element, String desc) {
                            if (!element.equals("at") || !desc.contains("/At;")) return null;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String atElement, Object value) {
                                    if (atElement.equals("target"))
                                        atTargets.add(String.valueOf(value));
                                }
                            };
                        }
                    };
                }
            };
        }
    }

    private static void checkMember(Class<?> owner, String member, String context) {
        // Member syntax: name, name(descriptor), field:Descriptor or <init>.
        String name = member;
        boolean isField = name.indexOf(':') >= 0;
        int cut = name.length();
        int paren = name.indexOf('(');
        if (paren >= 0) cut = Math.min(cut, paren);
        int colon = name.indexOf(':');
        if (colon >= 0) cut = Math.min(cut, colon);
        name = name.substring(0, cut);
        if (name.equals("<init>") || name.equals("<clinit>") || name.isEmpty()) return;
        if (name.equals("*")) return;
        if (isField) {
            for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
                for (var field : current.getDeclaredFields())
                    if (field.getName().equals(name)) return;
            }
            throw new AssertionError(
                    "Spatial GUI " + owner.getSimpleName() + " has no field named " + name
                            + " (used by " + context + ")");
        }
        anyMethod(owner, name);
    }

    private static void checkMixinHooks() throws Exception {
        var clientMixins = new ArrayList<String>();
        var json =
                com.google.gson.JsonParser.parseString(Files.readString(MIXIN_JSON))
                        .getAsJsonObject();
        json.getAsJsonArray("client").forEach(element -> clientMixins.add(element.getAsString()));

        int checked = 0;
        for (String simple : clientMixins) {
            String className = "mcaxolotl.bbsspatialgui.mixin." + simple;
            try (InputStream stream =
                    SpatialCompatibilityChecks.class.getResourceAsStream("/" + className.replace('.', '/') + ".class")) {
                if (stream == null) throw new AssertionError("mixin class missing: " + className);
                var scan = new MixinScan();
                new ClassReader(stream.readAllBytes()).accept(scan, ClassReader.SKIP_CODE);
                if (scan.targets.isEmpty()) continue;
                for (String target : scan.targets) {
                    boolean spatial = target.startsWith("org/tastytrash/");
                    boolean bbs = target.startsWith("mchorse/bbs_mod/");
                    if (!spatial && !bbs) continue;
                    Class<?> owner;
                    try {
                        owner =
                                Class.forName(target.replace('/', '.'), false,
                                        SpatialCompatibilityChecks.class.getClassLoader());
                    } catch (ClassNotFoundException e) {
                        if (bbs) continue;
                        throw e;
                    }
                    for (String injected : scan.injectedMethods)
                        checkMember(owner, injected, simple);
                    for (String shadow : scan.shadowFields) {
                        // Mixin resolves shadows against the target class itself; an inherited
                        // field that only exists in a superclass makes the mixin fail to apply.
                        boolean found = false;
                        if (spatial || bbs) {
                            for (var field : owner.getDeclaredFields())
                                if (field.getName().equals(shadow)) { found = true; break; }
                        }
                        if (!found)
                            throw new AssertionError(
                                    "shadow field " + shadow + " is missing on " + target
                                            + " (used by " + simple + ")");
                    }
                    checked++;
                }
                for (String at : scan.atTargets) {
                    int end = at.indexOf(';');
                    if (end < 0) continue;
                    String atOwner = at.substring(1, end);
                    if (!(atOwner.startsWith("org/tastytrash/")
                            || atOwner.startsWith("net/minecraft/")
                            || atOwner.startsWith("java/"))) continue;
                    Class<?> atType =
                            Class.forName(atOwner.replace('/', '.'), false,
                                    SpatialCompatibilityChecks.class.getClassLoader());
                    checkMember(atType, at.substring(end + 1), simple);
                }
            }
        }
        if (checked == 0)
            throw new AssertionError("no Spatial GUI mixin targets were verified");
        System.out.println(
                "Mixin hooks verified against Spatial GUI and BBS targets: " + checked);
    }

    private static void checkApiSurface() throws Exception {
        Class<?> api = type("api/SpatialGUIAPI");
        for (String name :
                new String[] {
                    "isActive", "isCrosshairModeActive", "isClosingScreen", "isOpeningScreen"
                })
            method(api, name);

        Class<?> client = type("client/SpatialGUIClient");
        method(client, "renderer");
        method(client, "isEnabled");
        method(client, "getEffectiveFirstPersonMode");
        method(client, "getWasThirdPersonCamera");
        method(client, "getOriginalCameraType");
        method(client, "shouldHookScreen", net.minecraft.class_437.class);

        Class<?> renderer = type("render/SpatialGUIRenderer");
        for (String name :
                new String[] {
                    "renderInWorldPost",
                    "getCameraStartPos",
                    "getCameraStartYRot",
                    "getCameraStartXRot",
                    "getScreenTexture",
                    "isClosing"
                })
            anyMethod(renderer, name);
        method(
                renderer,
                "capturePerspectiveState",
                org.joml.Matrix4f.class,
                net.minecraft.class_8251.class,
                net.minecraft.class_4587.class);
        field(renderer, "skipWindowOverride");

        Class<?> screenTexture = type("render/ScreenTexture");
        anyMethod(screenTexture, "getTarget");

        Class<?> camera = type("util/CameraUtil");
        method(camera, "yaw");
        method(camera, "pitch");
        method(camera, "getCurrentFov");

        Class<?> renderUtil = type("util/RenderUtil");
        anyMethod(renderUtil, "buildCameraRayDirection");

        Class<?> invoker = type("mixin/accessor/GameRendererInvoker");
        method(
                invoker,
                "spatialGUI$getFov",
                net.minecraft.class_4184.class,
                float.class,
                boolean.class);

        Class<?> config = type("config/ConfigManager");
        method(config, "config");
        anyMethod(type("config/SpatialGUIConfig"), "getEffectiveGuiScale");
    }

    public static void main(String[] args) throws Exception {
        String installed = installedVersion();
        String declared = declaredSpatialRange();
        CoreTests.check(
                VersionPredicate.parse(declared).test(Version.parse(installed)),
                "installed Spatial GUI "
                        + installed
                        + " is outside the declared dependency range "
                        + declared);
        CoreTests.check(SpatialUiScale.toIntegerScale(0.5f) == 1, "BBS UI scale 0.5 clamps to 1");
        CoreTests.check(SpatialUiScale.toIntegerScale(1.0f) == 1, "BBS UI scale 1.0 rounds to 1");
        CoreTests.check(SpatialUiScale.toIntegerScale(1.49f) == 1, "BBS UI scale 1.49 rounds to 1");
        CoreTests.check(SpatialUiScale.toIntegerScale(1.51f) == 2, "BBS UI scale 1.51 rounds to 2");
        CoreTests.check(SpatialUiScale.toIntegerScale(4.0f) == 4, "BBS UI scale 4.0 rounds to 4");
        CoreTests.check(SpatialUiScale.toIntegerScale(9.0f) == 6, "BBS UI scale 9 clamps to 6");
        System.out.println("PASS BBS interface scale bridge: fractional scale rounds into 1-6");
        checkApiSurface();
        checkMixinHooks();
        System.out.println(
                "PASS Spatial GUI compatibility: installed="
                        + installed
                        + " declared="
                        + declared
                        + " API, mixin targets and reflection hooks verified");
    }

    private SpatialCompatibilityChecks() {}
}
