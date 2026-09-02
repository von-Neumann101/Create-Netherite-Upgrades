package io.github.boosterproject.booster.registry;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.contraptions.elevator.NetheriteElevatorPulleyBlockEntity;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlockEntity;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlockEntity;
import io.github.boosterproject.booster.content.kinetics.steamEngine.NetheriteSteamEngineBlockEntity;
import io.github.boosterproject.booster.content.kinetics.press.NetheriteMechanicalPressBlockEntity;
import io.github.boosterproject.booster.content.logistics.vault.NetheriteItemVaultBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BoosterBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Booster.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerfulMechanicalPumpBlockEntity>> POWERFUL_MECHANICAL_PUMP =
        BLOCK_ENTITY_TYPES.register("powerful_mechanical_pump", () -> BlockEntityType.Builder
            .of(PowerfulMechanicalPumpBlockEntity::new, BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get())
            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetheriteFluidTankBlockEntity>> NETHERITE_FLUID_TANK =
        BLOCK_ENTITY_TYPES.register("netherite_fluid_tank", () -> BlockEntityType.Builder
            .of(NetheriteFluidTankBlockEntity::new, BoosterBlocks.NETHERITE_FLUID_TANK.get())
            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetheriteSteamEngineBlockEntity>> NETHERITE_STEAM_ENGINE =
        BLOCK_ENTITY_TYPES.register("netherite_steam_engine", () -> BlockEntityType.Builder
            .of(NetheriteSteamEngineBlockEntity::new, BoosterBlocks.NETHERITE_STEAM_ENGINE.get())
            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetheriteItemVaultBlockEntity>> NETHERITE_ITEM_VAULT =
        BLOCK_ENTITY_TYPES.register("netherite_item_vault", () -> BlockEntityType.Builder
            .of(NetheriteItemVaultBlockEntity::new, BoosterBlocks.NETHERITE_ITEM_VAULT.get())
            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetheriteElevatorPulleyBlockEntity>> NETHERITE_ELEVATOR_PULLEY =
        BLOCK_ENTITY_TYPES.register("netherite_elevator_pulley", () -> BlockEntityType.Builder
            .of(NetheriteElevatorPulleyBlockEntity::new, BoosterBlocks.NETHERITE_ELEVATOR_PULLEY.get())
            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetheriteMechanicalPressBlockEntity>> NETHERITE_MECHANICAL_PRESS =
        BLOCK_ENTITY_TYPES.register("netherite_mechanical_press", () -> BlockEntityType.Builder
            .of(NetheriteMechanicalPressBlockEntity::new, BoosterBlocks.NETHERITE_MECHANICAL_PRESS.get())
            .build(null));

    private BoosterBlockEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
        eventBus.addListener(BoosterBlockEntityTypes::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            NETHERITE_FLUID_TANK.get(),
            (tank, side) -> tank.getFluidCapability()
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            NETHERITE_ITEM_VAULT.get(),
            (vault, side) -> vault.getItemCapability()
        );
    }
}
