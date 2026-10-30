package mcaxolotl.bbsspatialgui.mixin;

import net.minecraft.class_5498;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.class_315", remap = false)
public interface NativePerspectiveAccess {
    @Accessor("field_26677")
    class_5498 bsg$getStoredPerspective();
}
