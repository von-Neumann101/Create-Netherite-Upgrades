package io.github.boosterproject.booster.registry;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlockEntity;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
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

    private BoosterBlockEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}