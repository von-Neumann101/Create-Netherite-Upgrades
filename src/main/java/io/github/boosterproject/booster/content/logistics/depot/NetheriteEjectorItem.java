package io.github.boosterproject.booster.content.logistics.depot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteEjectorItem extends BlockItem {
    private static final String TARGET = "NetheriteEjectorTarget";
    public NetheriteEjectorItem(Block block, Properties properties) { super(block, properties); }

    public static boolean hasTarget(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(TARGET);
    }
    public static BlockPos selectedTarget(ItemStack stack, Level level) {
        CompoundTag target = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(TARGET);
        if (!target.getString("Dimension").equals(level.dimension().location().toString())) return null;
        int[] coordinates = target.getIntArray("Position");
        if (coordinates.length != 3) return null;
        BlockPos pos = new BlockPos(coordinates[0], coordinates[1], coordinates[2]);
        return level.isInWorldBounds(pos) ? pos : null;
    }
    private static void feedback(Player player, Level level, String key, Object... args) {
        if (player != null && !level.isClientSide) player.displayClientMessage(Component.translatable("ejector.createnetherite." + key, args), true);
    }
    public static boolean applyTarget(ItemStack stack, Level level, BlockPos pos, Player player) {
        BlockPos target = selectedTarget(stack, level);
        if (target == null || !NetheriteEjectorBlockEntity.isValidOffset(target.subtract(pos))) {
            feedback(player, level, "invalid", NetheriteEjectorBlockEntity.getMaximumRange());
            return false;
        }
        if (player == null || player.isSpectator() || !player.getAbilities().mayBuild
            || player.distanceToSqr(pos.getCenter()) > 64 || !level.mayInteract(player, pos)) return false;
        if (!(level.getBlockEntity(pos) instanceof NetheriteEjectorBlockEntity ejector)) return false;
        if (ejector.hasFlyingItems() && !ejector.getTargetPosition().equals(target)) {
            feedback(player, level, "busy");
            return false;
        }
        if (!level.isClientSide) {
            if (!ejector.setTargetOffset(target.subtract(pos))) return false;
            feedback(player, level, "applied", target.getX(), target.getY(), target.getZ());
        }
        return true;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.isSpectator() || !player.getAbilities().mayBuild) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            BlockPos target = context.getClickedPos();
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (target.equals(selectedTarget(stack, level))) {
                data.remove(TARGET);
                feedback(player, level, "cleared");
            } else {
                CompoundTag selection = new CompoundTag();
                selection.putString("Dimension", level.dimension().location().toString());
                selection.putIntArray("Position", new int[] {target.getX(), target.getY(), target.getZ()});
                data.put(TARGET, selection);
                feedback(player, level, "selected", target.getX(), target.getY(), target.getZ());
            }
            if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA); else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hasTarget(stack) && level.getBlockEntity(context.getClickedPos()) instanceof NetheriteEjectorBlockEntity)
            return applyTarget(stack, level, context.getClickedPos(), player) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.FAIL;
        if (hasTarget(stack)) {
            BlockPos source = new BlockPlaceContext(context).getClickedPos();
            BlockPos target = selectedTarget(stack, level);
            if (target == null || !NetheriteEjectorBlockEntity.isValidOffset(target.subtract(source))) {
                feedback(player, level, "invalid", NetheriteEjectorBlockEntity.getMaximumRange());
                return InteractionResult.FAIL;
            }
        }
        return super.useOn(context);
    }

    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        boolean updated = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide && hasTarget(stack)) return applyTarget(stack, level, pos, player) || updated;
        return updated;
    }
    @Override public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isShiftKeyDown();
    }
}
