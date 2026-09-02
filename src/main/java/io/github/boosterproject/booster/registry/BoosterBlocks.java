package io.github.boosterproject.booster.registry;

import com.simibubi.create.AllBlocks;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.contraptions.elevator.NetheriteElevatorPulleyBlock;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlock;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlock;
import io.github.boosterproject.booster.content.kinetics.steamEngine.NetheriteSteamEngineBlock;
import io.github.boosterproject.booster.content.kinetics.press.NetheriteMechanicalPressBlock;
import io.github.boosterproject.booster.content.logistics.vault.NetheriteItemVaultBlock;
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

    public static final DeferredBlock<NetheriteSteamEngineBlock> NETHERITE_STEAM_ENGINE =
        BLOCKS.register("netherite_steam_engine", () -> new NetheriteSteamEngineBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                .noOcclusion()
        ));

    public static final DeferredBlock<NetheriteItemVaultBlock> NETHERITE_ITEM_VAULT =
        BLOCKS.register("netherite_item_vault", () -> new NetheriteItemVaultBlock(
            BlockBehaviour.Properties.ofFullCopy(AllBlocks.ITEM_VAULT.get())
                .explosionResistance(1200.0F)
        ));

    public static final DeferredBlock<NetheriteElevatorPulleyBlock> NETHERITE_ELEVATOR_PULLEY =
        BLOCKS.register("netherite_elevator_pulley", () -> new NetheriteElevatorPulleyBlock(
            BlockBehaviour.Properties.ofFullCopy(AllBlocks.ELEVATOR_PULLEY.get())
        ));

    public static final DeferredBlock<NetheriteMechanicalPressBlock> NETHERITE_MECHANICAL_PRESS =
        BLOCKS.register("netherite_mechanical_press", () -> new NetheriteMechanicalPressBlock(
            BlockBehaviour.Properties.ofFullCopy(AllBlocks.MECHANICAL_PRESS.get())
        ));

    private BoosterBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
