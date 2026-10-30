package mcaxolotl.bbsspatialgui;

import mcaxolotl.bbsspatialgui.render.ScreenPolicy;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.SettingsManager;
import mchorse.bbs_mod.settings.ui.UIValueFactory;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.nio.file.Files;
import java.util.Comparator;

public final class BBSSettingsTests {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("bbs-addon-settings-");
        try {
            var file = directory.resolve("bbs_spatial_gui.json").toFile();
            var builder = new SettingsBuilder(Icons.FULLSCREEN, BBSOptions.ID, file);
            BBSOptions.register(builder);
            var settings = builder.getConfig();
            if (settings.categories.size() != 3
                    || !BBSOptions.enabled(false)
                    || !BBSOptions.recording(false)
                    || BBSOptions.debug(true))
                throw new AssertionError("native categories or defaults incorrect");
            if (!UIValueFactory.getTitleKey(settings).equals("bbs_spatial_gui.config.title"))
                throw new AssertionError("module tooltip key incorrect");
            if (!UIValueFactory.getValueLabelKey(settings.get("general", "enabled"))
                    .equals("bbs_spatial_gui.config.general.enabled"))
                throw new AssertionError("native option label key incorrect");
            ((ValueBoolean) settings.get("general", "enabled")).set(false);
            ((ValueBoolean) settings.get("recording", "recordSpatialGui")).set(false);
            if (!settings.save(file)) throw new AssertionError("native settings save failed");
            var reload = new SettingsBuilder(Icons.FULLSCREEN, BBSOptions.ID, file);
            BBSOptions.register(reload);
            if (!new SettingsManager().load(reload.getConfig(), file)
                    || BBSOptions.enabled(true)
                    || BBSOptions.recording(true))
                throw new AssertionError("native settings did not restore saved values");
            ((ValueBoolean) reload.getConfig().get("general", "enabled")).reset();
            if (!BBSOptions.enabled(false))
                throw new AssertionError("reset changed factory default");
            var pointer = (ValueBoolean) reload.getConfig().get("recording", "recordMousePointer");
            var nativeCursor =
                    (ValueBoolean) reload.getConfig().get("recording", "useNativeCursor");
            pointer.set(false);
            nativeCursor.set(false);
            if (BBSOptions.recordPointer() || BBSOptions.nativeCursor())
                throw new AssertionError("recording cursor controls not bound");
            pointer.reset();
            nativeCursor.reset();
            if (!BBSOptions.recordPointer() || !BBSOptions.nativeCursor())
                throw new AssertionError("recording cursor defaults changed");
            reload.getConfig().save(file);
            var saved =
                    com.google.gson.JsonParser.parseString(Files.readString(file.toPath()))
                            .getAsJsonObject();
            if (!saved.getAsJsonObject("general").get("enabled").getAsBoolean())
                throw new AssertionError("native save schema mismatch");
            // Wait for the real BBS settings writer to finish its debounce before deleting the
            // fixture.
            var pending =
                    mchorse.bbs_mod.settings.SettingsThread.class.getDeclaredField("instance");
            pending.setAccessible(true);
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (pending.get(null) != null && System.nanoTime() < deadline) Thread.sleep(25);
            if (pending.get(null) != null) throw new AssertionError("native save did not finish");
            var provider = new mchorse.bbs_mod.resources.AssetProvider();
            provider.register(
                    new mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack(
                            BBSOptions.ID, "assets/" + BBSOptions.ID, BBSOptions.class));
            for (String language : new String[] {"en_us", "zh_cn"}) {
                var link =
                        new mchorse.bbs_mod.resources.Link(
                                BBSOptions.ID, "lang/" + language + ".json");
                try (var input = provider.getAsset(link);
                        var reader =
                                new java.io.InputStreamReader(
                                        input, java.nio.charset.StandardCharsets.UTF_8)) {
                    var translations =
                            com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    for (var category : settings.categories.values()) {
                        if (!translations.has(UIValueFactory.getCategoryTitleKey(category)))
                            throw new AssertionError("missing category title " + language);
                    }
                    if (!translations.has(UIValueFactory.getTitleKey(settings))
                            || !translations.has("bbs_spatial_gui.config.general.enabled"))
                        throw new AssertionError(
                                "native translation resource unavailable " + language);
                }
            }
            String screen = "mchorse.bbs_mod.ui.framework.UIScreen";
            for (boolean allScreens : new boolean[] {false, true}) {
                boolean disabled =
                        ScreenPolicy.excluded(screen, false)
                                ? false
                                : ScreenPolicy.included(screen, false, allScreens);
                if (disabled)
                    throw new AssertionError(
                            "Spatial whitelist/all-screens bypassed the BBS off switch");
                if (ScreenPolicy.excluded(screen, true)
                        || !ScreenPolicy.included(screen, true, allScreens))
                    throw new AssertionError("enabled BBS screen was not selected");
            }
            if (ScreenPolicy.excluded("net.minecraft.class_481", false))
                throw new AssertionError("vanilla selection changed");
            var base =
                    new mchorse.bbs_mod.settings.Settings(
                            Icons.GEAR, "bbs", directory.resolve("base.json").toFile());
            var otherA =
                    new mchorse.bbs_mod.settings.Settings(
                            Icons.GEAR, "other_a", directory.resolve("a.json").toFile());
            var otherB =
                    new mchorse.bbs_mod.settings.Settings(
                            Icons.GEAR, "other_b", directory.resolve("b.json").toFile());
            var source =
                    new java.util.ArrayList<>(java.util.List.of(otherA, settings, base, otherB));
            var ordered = mcaxolotl.bbsspatialgui.ui.BBSSettingsOrder.arrange(source);
            if (!ordered.equals(java.util.List.of(otherA, base, settings, otherB)))
                throw new AssertionError("bridge button is not directly below BBS");
            if (!source.equals(java.util.List.of(otherA, settings, base, otherB)))
                throw new AssertionError("ordering changed the native registry collection");
            if (!mcaxolotl.bbsspatialgui.ui.BBSSettingsOrder.arrange(
                            java.util.List.of(otherA, settings, otherB))
                    .equals(java.util.List.of(otherA, settings, otherB)))
                throw new AssertionError("missing base reorders unrelated modules");
            System.out.println(
                    "PASS BBS-FS settings: native registration, labels, defaults, JSON save/reload, reset, recording binding, native languages and disabled-screen veto");
        } finally {
            try (var files = Files.walk(directory)) {
                for (var path : files.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
