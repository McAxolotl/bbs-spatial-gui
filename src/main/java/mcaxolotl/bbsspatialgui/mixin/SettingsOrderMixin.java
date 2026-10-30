package mcaxolotl.bbsspatialgui.mixin;

import mcaxolotl.bbsspatialgui.ui.BBSSettingsOrder;

import mchorse.bbs_mod.settings.Settings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Map;

@Mixin(targets = "mchorse.bbs_mod.settings.ui.UISettingsOverlayPanel", remap = false)
public abstract class SettingsOrderMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"),
            require = 0,
            remap = false)
    private Collection<Settings> bsg$orderedSettings(Map<String, Settings> modules) {
        return BBSSettingsOrder.arrange(modules.values());
    }
}
