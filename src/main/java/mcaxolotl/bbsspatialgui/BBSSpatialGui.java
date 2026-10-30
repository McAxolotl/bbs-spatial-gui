package mcaxolotl.bbsspatialgui;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.BBSClientReadyEvent;
import mchorse.bbs_mod.api.client.events.RegisterClientSettingsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class BBSSpatialGui implements BBSAddonMod {
    private static volatile boolean enabled = true;

    public BBSSpatialGui() {
        mcaxolotl.bbsspatialgui.compat.BBSDetector.report();
    }

    @Subscribe
    public void registerSettings(RegisterClientSettingsEvent event) {
        event.register(Icons.FULLSCREEN, BBSOptions.ID, BBSOptions::register);
    }

    @Subscribe
    public void registerLanguage(RegisterL10nEvent event) {
        mchorse.bbs_mod.BBSMod.getProvider()
                .register(
                        new mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack(
                                BBSOptions.ID, "assets/" + BBSOptions.ID, BBSSpatialGui.class));
        event.l10n.register(
                language -> {
                    String path = "lang/" + language + ".json";
                    return BBSSpatialGui.class.getResource("/assets/" + BBSOptions.ID + "/" + path)
                                    == null
                            ? java.util.List.of()
                            : java.util.List.of(
                                    new mchorse.bbs_mod.resources.Link(BBSOptions.ID, path));
                });
    }

    @Subscribe
    public void onReady(BBSClientReadyEvent event) {
        enabled = BBSOptions.enabled(enabled);
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    if (client.field_1755 != null
                            && client.field_1755
                                    .getClass()
                                    .getName()
                                    .equals("mchorse.bbs_mod.ui.framework.UIScreen")) return;
                    var renderer = org.tastytrash.spatialGUI.client.SpatialGUIClient.renderer();
                    if (renderer == null || !renderer.isClosing())
                        enabled = BBSOptions.enabled(enabled);
                });
        System.out.println("[BBS Spatial GUI] BBS-FS settings registered; enabled=" + enabled);
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean debugLogging() {
        return BBSOptions.debug(false);
    }

    public static boolean recordSpatialGui() {
        return BBSOptions.recording(true);
    }
}
