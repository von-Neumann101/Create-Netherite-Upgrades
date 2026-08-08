package io.github.boosterproject.booster.mixin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteBoilerData;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BoilerData.class, remap = false)
public abstract class BoilerDataMixin {
    @Redirect(
        method = "evaluate",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = false
        )
    )
    private boolean booster$recognizeMatchingSteamEngine(BlockEntry<?> entry, BlockState state,
                                                         FluidTankBlockEntity controller) {
        if (entry == AllBlocks.STEAM_ENGINE && controller instanceof NetheriteFluidTankBlockEntity) {
            return state.is(BoosterBlocks.NETHERITE_STEAM_ENGINE.get());
        }
        return entry.has(state);
    }

    @ModifyArg(
        method = "addToGoggleTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/api/stress/BlockStressValues;getCapacity(Lnet/minecraft/world/level/block/Block;)D",
            remap = false
        ),
        index = 0
    )
    private Block booster$showNetheriteSteamEngineCapacity(Block original) {
        return (Object) this instanceof NetheriteBoilerData
            ? BoosterBlocks.NETHERITE_STEAM_ENGINE.get()
            : original;
    }

    @ModifyConstant(method = "addToGoggleTooltip", constant = @Constant(doubleValue = 10.0D))
    private double booster$showNetheriteWaterConsumption(double original) {
        return (Object) this instanceof NetheriteBoilerData
            ? NetheriteBoilerData.WATER_SUPPLY_PER_LEVEL
            : original;
    }
}
