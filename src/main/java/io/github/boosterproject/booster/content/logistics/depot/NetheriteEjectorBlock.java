package io.github.boosterproject.booster.content.logistics.depot;

import com.simibubi.create.content.logistics.depot.EjectorBlock;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorTriggerPacket;
import com.simibubi.create.content.logistics.depot.SharedDepotBlockMethods;
import com.simibubi.create.foundation.item.ItemHelper;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class NetheriteEjectorBlock extends EjectorBlock {
    public NetheriteEjectorBlock(Properties properties) { super(properties); }
    @Override public BlockEntityType<? extends EjectorBlockEntity> getBlockEntityType() { return BoosterBlockEntityTypes.NETHERITE_WEIGHTED_EJECTOR.get(); }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof NetheriteEjectorItem) {
            if (!player.isShiftKeyDown() && NetheriteEjectorItem.hasTarget(stack)) {
                return NetheriteEjectorItem.applyTarget(stack, level, pos, player) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof NetheriteEjectorBlockEntity ejector && ejector.hasFlyingItems())
            return InteractionResult.FAIL;
        var before = state.getValue(HORIZONTAL_FACING);
        var result = super.onWrenched(state, context);
        var level = context.getLevel();
        if (!level.isClientSide && level.getBlockEntity(context.getClickedPos()) instanceof NetheriteEjectorBlockEntity ejector)
            ejector.rotateTarget(before, ejector.getBlockState().getValue(HORIZONTAL_FACING));
        return result;
    }

    @Override public void updateEntityAfterFallOn(BlockGetter world, Entity entity) {
        super.updateEntityAfterFallOn(world, entity);
        BlockPos pos = entity.blockPosition();
        if (!(world.getBlockEntity(pos) instanceof NetheriteEjectorBlockEntity ejector)
            || !entity.isAlive() || entity.isSuppressingBounce()) return;
        if (!ItemHelper.fromItemEntity(entity).isEmpty()) {
            SharedDepotBlockMethods.onLanded(world, entity);
            return;
        }
        if (ejector.getState() == EjectorBlockEntity.State.RETRACTING || ejector.isPowered()
            || ejector.getTargetOffset().equals(BlockPos.ZERO)) return;
        if (entity.onGround()) {
            entity.setOnGround(false);
            Vec3 center = Vec3.atCenterOf(pos).add(0, 7 / 16F, 0);
            Vec3 location = entity.position();
            entity.setDeltaMovement(0, -.125, 0);
            if (center.distanceTo(location) > .25) {
                Vec3 nudged = center.add(location).scale(.5);
                entity.setPos(nudged.x, nudged.y, nudged.z);
                return;
            }
        }
        ejector.activate();
        ejector.notifyUpdate();
        if (entity.level().isClientSide) CatnipServices.NETWORK.sendToServer(new EjectorTriggerPacket(pos));
    }
}
