package io.github.boosterproject.booster.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mirrors Create's air-current allowance without widening access for unrelated movement. */
@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public interface WrenchFlightAccess {
    @Accessor("aboveGroundTickCount") void booster$setAboveGroundTicks(int ticks);
}
