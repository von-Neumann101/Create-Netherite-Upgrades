package io.github.boosterproject.booster.content.kinetics.portable;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PortableStressDeviceBlock extends DirectionalKineticBlock implements IBE<PortableStressDeviceBlockEntity> {
    private static final VoxelShape SHAPE = box(2, 2, 2, 14, 14, 14);
    public PortableStressDeviceBlock(Properties properties) { super(properties); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction output = context.getClickedFace().getOpposite();
        BlockPos target = context.getClickedPos().relative(output);
        BlockState state = context.getLevel().getBlockState(target);
        return state.getBlock() instanceof IRotate rotate && rotate.hasShaftTowards(context.getLevel(), target, state, output.getOpposite())
            ? defaultBlockState().setValue(FACING, output) : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel && stack.getItem() instanceof PortableStressDeviceItem)
            withBlockEntityDo(level, pos, be -> be.preparePlacement(stack));
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (level instanceof ServerLevel) withBlockEntityDo(level, pos, PortableStressDeviceBlockEntity::commitPlacement);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) withBlockEntityDo(level, pos, PortableStressDeviceBlockEntity::retire);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) { return ItemStack.EMPTY; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(FACING).getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) { return face == state.getValue(FACING); }
    @Override public boolean hideStressImpact() { return true; }
    @Override public Class<PortableStressDeviceBlockEntity> getBlockEntityClass() { return PortableStressDeviceBlockEntity.class; }
    @Override public BlockEntityType<? extends PortableStressDeviceBlockEntity> getBlockEntityType() { return BoosterBlockEntityTypes.NETHERITE_PORTABLE_STRESS_DEVICE.get(); }
}
