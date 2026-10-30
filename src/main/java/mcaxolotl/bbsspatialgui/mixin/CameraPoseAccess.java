package mcaxolotl.bbsspatialgui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.class_4184", remap = false)
public interface CameraPoseAccess {
    @Invoker("method_19325")
    void bsg$setRotation(float yaw, float pitch);

    @Invoker("method_19327")
    void bsg$setPosition(double x, double y, double z);
}
