package mcaxolotl.bbsspatialgui.compat;

import mcaxolotl.bbsspatialgui.render.BackgroundWorldPass;
import mcaxolotl.bbsspatialgui.render.RenderContext;

import java.util.function.BooleanSupplier;

public final class WorldViewFilter implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return !RenderContext.spatialScreen()
                && (!RenderContext.bbsWorldCaptureActive() || BackgroundWorldPass.active());
    }
}
