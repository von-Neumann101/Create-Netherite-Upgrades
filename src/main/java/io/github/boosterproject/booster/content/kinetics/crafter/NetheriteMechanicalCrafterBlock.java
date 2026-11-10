package io.github.boosterproject.booster.content.kinetics.crafter;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteMechanicalCrafterBlock extends MechanicalCrafterBlock {
    public NetheriteMechanicalCrafterBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntityType<? extends MechanicalCrafterBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_MECHANICAL_CRAFTER.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof NetheriteMechanicalCrafterBlockEntity crafter)
            crafter.removing = true;
        super.onRemove(state, level, pos, next, moving);
    }
}
