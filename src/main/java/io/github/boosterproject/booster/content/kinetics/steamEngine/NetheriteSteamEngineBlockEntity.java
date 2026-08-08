package io.github.boosterproject.booster.content.kinetics.steamEngine;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteSteamEngineBlockEntity extends SteamEngineBlockEntity {
    public NetheriteSteamEngineBlockEntity(BlockPos pos, BlockState state) {
        this(BoosterBlockEntityTypes.NETHERITE_STEAM_ENGINE.get(), pos, state);
    }

    public NetheriteSteamEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isValid() {
        Level level = getLevel();
        if (level == null) {
            return false;
        }

        Direction attachedDirection = SteamEngineBlock.getConnectedDirection(getBlockState()).getOpposite();
        return level.getBlockState(getBlockPos().relative(attachedDirection))
            .is(BoosterBlocks.NETHERITE_FLUID_TANK.get());
    }
}
