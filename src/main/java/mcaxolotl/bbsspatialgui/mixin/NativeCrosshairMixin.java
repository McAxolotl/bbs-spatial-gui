package mcaxolotl.bbsspatialgui.mixin;

import net.minecraft.class_332;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.class_329", remap = false)
public interface NativeCrosshairMixin {
    @Invoker("method_1736")
    void bsg$renderCrosshair(class_332 graphics);

    @Accessor("field_2011")
    int bsg$getWidth();

    @Accessor("field_2029")
    int bsg$getHeight();

    @Accessor("field_2011")
    void bsg$setWidth(int width);

    @Accessor("field_2029")
    void bsg$setHeight(int height);
}
