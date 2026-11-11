package io.github.boosterproject.booster.content.conversion;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public class NetheriteConverterBlock extends HorizontalKineticBlock implements IBE<NetheriteConverterBlockEntity> {
    public NetheriteConverterBlock(Properties properties) { super(properties); }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) { return face.getAxis() == getRotationAxis(state); }
    @Override public Class<NetheriteConverterBlockEntity> getBlockEntityClass() { return NetheriteConverterBlockEntity.class; }
    @Override public BlockEntityType<? extends NetheriteConverterBlockEntity> getBlockEntityType() { return BoosterBlockEntityTypes.NETHERITE_CONVERTER.get(); }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator() || !player.getAbilities().mayBuild) return ItemInteractionResult.FAIL;
        var be = getBlockEntity(level, pos);
        if (be == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.isEmpty()) {
            return ItemInteractionResult.CONSUME;
        }
        // Consume fluid-container clicks so item-use fallback cannot pour a bucket beside the machine.
        // Fluid capabilities remain available to pipes; manual containers do not fill or drain it.
        if (FluidUtil.getFluidHandler(stack).isPresent()) return ItemInteractionResult.CONSUME;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

}
