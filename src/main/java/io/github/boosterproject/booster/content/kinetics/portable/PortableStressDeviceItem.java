package io.github.boosterproject.booster.content.kinetics.portable;

import com.simibubi.create.content.kinetics.base.IRotate;
import java.util.UUID;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PortableStressDeviceItem extends BlockItem {
    private static final String KEY = "PortableStress";
    private final int helperId;
    private static long clientTime;

    public PortableStressDeviceItem(Block block, Properties properties) {
        super(block, properties);
        helperId = PlacementHelpers.register(new PlacementHelper());
    }
    private static CompoundTag state(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY); }
    public static UUID id(ItemStack stack) { var tag = state(stack); return tag.hasUUID("Id") ? tag.getUUID("Id") : null; }
    public static UUID ensureId(ItemStack stack) {
        UUID id = id(stack);
        if (id != null) return id;
        id = UUID.randomUUID();
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var data = new CompoundTag(); data.putUUID("Id", id); root.put(KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return id;
    }
    public static void sync(ItemStack stack, MinecraftServer server) {
        UUID id = id(stack);
        if (id == null) return;
        var entry = PortableStressData.get(server).entry(id);
        var next = new CompoundTag(); next.putUUID("Id", id);
        if (entry != null) {
            if (entry.deployment() != null) {
                next.putLong("ExpiresAt", entry.deployment().expiresAt());
                next.putLong("ReadyAt", entry.deployment().expiresAt() + PortableStressData.COOLDOWN_TICKS);
            } else next.putLong("ReadyAt", entry.readyAt());
        }
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!next.equals(root.getCompound(KEY))) { root.put(KEY, next); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root)); }
    }
    private static boolean available(ItemStack stack, Level level) {
        UUID id = id(stack);
        if (level instanceof ServerLevel serverLevel) return id == null || PortableStressData.get(serverLevel.getServer()).available(id, PortableStressData.now(serverLevel.getServer()));
        return level.getGameTime() >= state(stack).getLong("ReadyAt");
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Enter NeoForge's normal snapshot/event transaction before placing, even on
        // machines whose own block interaction would otherwise consume right-click.
        return stack.useOn(context);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || player.isSpectator() || !player.getAbilities().mayBuild || !available(stack, context.getLevel())) return InteractionResult.FAIL;
        var helper = PlacementHelpers.get(helperId);
        var hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), false);
        var offset = helper.getOffset(player, context.getLevel(), context.getLevel().getBlockState(context.getClickedPos()), context.getClickedPos(), hit, stack);
        if (!offset.isSuccessful()) return InteractionResult.FAIL;
        var difference = offset.getBlockPos().subtract(context.getClickedPos());
        Direction side = Direction.fromDelta(difference.getX(), difference.getY(), difference.getZ());
        if (side == null) return InteractionResult.FAIL;
        var placementHit = new BlockHitResult(context.getClickedPos().getCenter().add(side.getStepX() * .5, side.getStepY() * .5, side.getStepZ() * .5), side, context.getClickedPos(), false);
        // Reuse the preview offset without its consumable-item placement shortcut.
        return place(new BlockPlaceContext(player, context.getHand(), stack, placementHit));
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator() || !player.getAbilities().mayBuild
            || !available(context.getItemInHand(), context.getLevel()) || !context.getLevel().mayInteract(player, context.getClickedPos())
            || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        int count = stack.getCount();
        InteractionResult result = super.place(context);
        if (result.consumesAction()) stack.setCount(count);
        return result;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || !player.getAbilities().mayBuild
            || getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS) return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel serverLevel) {
            UUID id = id(stack);
            var entry = id == null ? null : PortableStressData.get(serverLevel.getServer()).entry(id);
            if (entry == null || entry.deployment() == null) return InteractionResultHolder.pass(stack);
            PortableStressService.recall(serverLevel.getServer(), id);
            sync(stack, serverLevel.getServer());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel) sync(stack, serverLevel.getServer());
    }
    public static void setClientTime(long now) { clientTime = now; }
    public static long remainingTicks(ItemStack stack, long now) {
        var data = state(stack);
        long expiry = data.getLong("ExpiresAt");
        return Math.max(0, (now < expiry ? expiry : data.getLong("ReadyAt")) - now);
    }
    @Override public boolean isBarVisible(ItemStack stack) { return remainingTicks(stack, clientTime) > 0; }
    @Override public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13F * remainingTicks(stack, clientTime) / PortableStressData.RUN_TICKS), 0, 13);
    }
    @Override public int getBarColor(ItemStack stack) { return clientTime < state(stack).getLong("ExpiresAt") ? 0x53D769 : 0x5599FF; }

    private final class PlacementHelper implements IPlacementHelper {
        @Override public Predicate<ItemStack> getItemPredicate() { return stack -> stack.getItem() == PortableStressDeviceItem.this; }
        @Override public Predicate<BlockState> getStatePredicate() { return state -> state.getBlock() instanceof IRotate; }
        @Override public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult hit) {
            ItemStack stack = player.getMainHandItem().getItem() == PortableStressDeviceItem.this ? player.getMainHandItem() : player.getOffhandItem();
            return getOffset(player, level, state, pos, hit, stack);
        }
        @Override public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult hit, ItemStack stack) {
            if (player.isSpectator() || !player.getAbilities().mayBuild || !available(stack, level)
                || !(state.getBlock() instanceof IRotate rotate)) return PlacementOffset.fail();
            for (Direction side : IPlacementHelper.orderedByDistance(pos, hit.getLocation(), direction -> rotate.hasShaftTowards(level, pos, state, direction))) {
                BlockPos target = pos.relative(side);
                if (!level.getBlockState(target).canBeReplaced()) continue;
                var output = getBlock().defaultBlockState().setValue(PortableStressDeviceBlock.FACING, side.getOpposite());
                return PlacementOffset.success(target, ignored -> output).withGhostState(output);
            }
            return PlacementOffset.fail();
        }
    }
}
