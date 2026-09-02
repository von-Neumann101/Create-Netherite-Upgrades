package io.github.boosterproject.booster.registry;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.contraptions.elevator.NetheriteElevatorPulleyBlockEntity;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlockEntity;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlockEntity;
import io.github.boosterproject.booster.content.kinetics.steamEngine.NetheriteSteamEngineBlockEntity;
import io.github.boosterproject.booster.content.logistics.vault.NetheriteItemVaultBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BoosterBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Booster.MOD_ID);

    public static final RegistryObject<BlockEntityType<PowerfulMechanicalPumpBlockEntity>> POWERFUL_MECHANICAL_PUMP =
        BLOCK_ENTITY_TYPES.register("powerful_mechanical_pump", () -> BlockEntityType.Builder
            .of(PowerfulMechanicalPumpBlockEntity::new, BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get())
            .build(null));

    public static final RegistryObject<BlockEntityType<NetheriteFluidTankBlockEntity>> NETHERITE_FLUID_TANK =
        BLOCK_ENTITY_TYPES.register("netherite_fluid_tank", () -> BlockEntityType.Builder
            .of(NetheriteFluidTankBlockEntity::new, BoosterBlocks.NETHERITE_FLUID_TANK.get())
            .build(null));

    public static final RegistryObject<BlockEntityType<NetheriteSteamEngineBlockEntity>> NETHERITE_STEAM_ENGINE =
        BLOCK_ENTITY_TYPES.register("netherite_steam_engine", () -> BlockEntityType.Builder
            .of(NetheriteSteamEngineBlockEntity::new, BoosterBlocks.NETHERITE_STEAM_ENGINE.get())
            .build(null));

    public static final RegistryObject<BlockEntityType<NetheriteItemVaultBlockEntity>> NETHERITE_ITEM_VAULT =
        BLOCK_ENTITY_TYPES.register("netherite_item_vault", () -> BlockEntityType.Builder
            .of(NetheriteItemVaultBlockEntity::new, BoosterBlocks.NETHERITE_ITEM_VAULT.get())
            .build(null));

    public static final RegistryObject<BlockEntityType<NetheriteElevatorPulleyBlockEntity>> NETHERITE_ELEVATOR_PULLEY =
        BLOCK_ENTITY_TYPES.register("netherite_elevator_pulley", () -> BlockEntityType.Builder
            .of(NetheriteElevatorPulleyBlockEntity::new, BoosterBlocks.NETHERITE_ELEVATOR_PULLEY.get())
            .build(null));

    private BoosterBlockEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
