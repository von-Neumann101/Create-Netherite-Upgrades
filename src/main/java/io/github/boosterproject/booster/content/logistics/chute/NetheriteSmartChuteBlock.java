package io.github.boosterproject.booster.content.logistics.chute;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.content.logistics.chute.SmartChuteBlock;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetheriteSmartChuteBlock extends SmartChuteBlock {
    public NetheriteSmartChuteBlock(Properties properties) { super(properties); }
    @Override public BlockEntityType<? extends ChuteBlockEntity> getBlockEntityType() { return BoosterBlockEntityTypes.NETHERITE_SMART_CHUTE.get(); }
    // Create recognizes only its own registered smart chute in its shape cache.
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (AllItems.WRENCH.isIn(player.getMainHandItem())) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof NetheriteSmartChuteBlockEntity chute)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) chute.takeItems(player);
            else serverPlayer.openMenu(chute, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
