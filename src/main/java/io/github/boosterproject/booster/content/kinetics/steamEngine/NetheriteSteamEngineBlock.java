package io.github.boosterproject.booster.content.kinetics.steamEngine;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteSteamEngineBlock extends SteamEngineBlock {
    public NetheriteSteamEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction attachedDirection = getConnectedDirection(state).getOpposite();
        return level.getBlockState(pos.relative(attachedDirection)).is(BoosterBlocks.NETHERITE_FLUID_TANK.get());
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Class<SteamEngineBlockEntity> getBlockEntityClass() {
        return (Class) NetheriteSteamEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamEngineBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_STEAM_ENGINE.get();
    }
}
