package io.github.boosterproject.booster.registry;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.BlazeBurnerType;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import io.github.boosterproject.booster.Booster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BoosterArmInteractionPointTypes {
    private static final DeferredRegister<ArmInteractionPointType> TYPES =
        DeferredRegister.create(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, Booster.MOD_ID);

    public static final DeferredHolder<ArmInteractionPointType, BlazeBurnerType> NETHERITE_BLAZE_BURNER =
        TYPES.register("netherite_blaze_burner", () -> new BlazeBurnerType() {
            @Override
            public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
                return state.is(BoosterBlocks.NETHERITE_BLAZE_BURNER.get());
            }
        });

    private BoosterArmInteractionPointTypes() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
