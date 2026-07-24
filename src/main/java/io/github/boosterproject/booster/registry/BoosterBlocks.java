package io.github.boosterproject.booster.registry;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlock;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BoosterBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Booster.MOD_ID);

    public static final DeferredBlock<PowerfulMechanicalPumpBlock> POWERFUL_MECHANICAL_PUMP =
        BLOCKS.register("powerful_mechanical_pump", () -> new PowerfulMechanicalPumpBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                .mapColor(MapColor.STONE)
        ));

    public static final DeferredBlock<NetheriteFluidTankBlock> NETHERITE_FLUID_TANK =
        BLOCKS.register("netherite_fluid_tank", () -> new NetheriteFluidTankBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                .noOcclusion()
                .isRedstoneConductor((state, getter, pos) -> true)
        ));

    private BoosterBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}