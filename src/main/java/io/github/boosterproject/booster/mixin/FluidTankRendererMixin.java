package io.github.boosterproject.booster.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.BoosterPartialModels;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FluidTankRenderer.class, remap = false)
public class FluidTankRendererMixin {
    @Redirect(
        method = "renderAsBoiler",
        at = @At(
            value = "INVOKE",
            target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"
        )
    )
    private SuperByteBuffer booster$useNetheriteGauge(PartialModel model, BlockState state) {
        if (state.is(BoosterBlocks.NETHERITE_FLUID_TANK.get())) {
            model = model == AllPartialModels.BOILER_GAUGE
                ? BoosterPartialModels.NETHERITE_BOILER_GAUGE
                : BoosterPartialModels.NETHERITE_BOILER_GAUGE_DIAL;
        }
        return CachedBuffers.partial(model, state);
    }
}
