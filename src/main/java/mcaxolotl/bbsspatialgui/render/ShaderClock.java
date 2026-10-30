package mcaxolotl.bbsspatialgui.render;

import com.mojang.blaze3d.systems.RenderSystem;

public final class ShaderClock {
    public static void restore(float fraction) {
        RenderSystem.setShaderGameTime(0L, fraction * 24000.0f);
    }

    private ShaderClock() {}
}
