package io.github.boosterproject.booster.content.logistics.chute;

import java.util.List;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.utility.BlockHelper;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NetheriteSmartChuteBlockEntity extends SmartChuteBlockEntity implements MenuProvider {
    private final int[] remainingTicks = new int[2];
    private boolean loading;
    private Direction defaultDirection = Direction.DOWN;
    private final ItemStackHandler directionFilters = new ItemStackHandler(2) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) { changed(); }
    };
    private final ItemStackHandler transport = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return accepts(stack) && route(stack) == direction(slot);
        }
        @Override protected void onContentsChanged(int slot) {
            if (getStackInSlot(slot).isEmpty()) remainingTicks[slot] = 0;
            else if (remainingTicks[slot] <= 0) remainingTicks[slot] = 5;
            changed();
        }
    };

    public NetheriteSmartChuteBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_SMART_CHUTE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        for (BlockEntityBehaviour behaviour : behaviours) {
            if (behaviour instanceof FilteringBehaviour filter) filter.showCountWhen(() -> true);
            if (behaviour instanceof DirectBeltInputBehaviour input) input.onlyInsertWhen(side -> canActivate());
        }
    }
    public static Direction direction(int slot) { return slot == 0 ? Direction.UP : Direction.DOWN; }
    public ItemStackHandler getDirectionFilters() { return directionFilters; }
    public ItemStackHandler getTransportInventory() { return transport; }
    public Direction getDefaultDirection() { return defaultDirection; }
    public void setDefaultDirection(Direction value) {
        if (value != Direction.UP && value != Direction.DOWN) return;
        defaultDirection = value;
        changed();
    }
    public void setDirectionFilter(int slot, ItemStack sample) {
        if (slot < 0 || slot > 1) return;
        directionFilters.setStackInSlot(slot, sample.isEmpty() ? ItemStack.EMPTY : sample.copyWithCount(1));
    }
    public Direction route(ItemStack stack) {
        for (int slot = 0; slot < 2; slot++) {
            ItemStack sample = directionFilters.getStackInSlot(slot);
            if (!sample.isEmpty() && FilterItemStack.of(sample.copy()).test(level, stack)) return direction(slot);
        }
        return defaultDirection;
    }
    private boolean accepts(ItemStack stack) {
        return level != null && canActivate() && !stack.isEmpty() && getBehaviour(FilteringBehaviour.TYPE).test(stack);
    }
    @Override protected boolean canAcceptItem(ItemStack stack) {
        return accepts(stack) && transport.insertItem(route(stack) == Direction.UP ? 0 : 1, stack, true).isEmpty();
    }
    // Native upward connections check receiver motion. Both our channels remain independently available.
    @Override public float getItemMotion() { return .2F; }
    @Override public ItemStack getItem() {
        return transport.getStackInSlot(0).isEmpty() ? transport.getStackInSlot(1) : transport.getStackInSlot(0);
    }
    @Override public void setItem(ItemStack stack) { setItem(stack, 0); }
    @Override public void setItem(ItemStack stack, float insertionPos) {
        if (stack.isEmpty()) { transport.setStackInSlot(0, ItemStack.EMPTY); transport.setStackInSlot(1, ItemStack.EMPTY); return; }
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(transport, stack.copy(), false);
        if (!remainder.isEmpty() && level != null && !level.isClientSide) Block.popResource(level, worldPosition, remainder);
    }
    /** Runs after SmartBlockEntity's behaviour tick, replacing the original single-channel transport. */
    public void tickDirectionalTransport() {
        if (level == null || level.isClientSide || !canActivate()) return;
        for (int slot = 0; slot < 2; slot++) {
            if (!transport.getStackInSlot(slot).isEmpty() && --remainingTicks[slot] <= 0) output(slot);
        }
        for (int slot = 0; slot < 2; slot++) if (transport.getStackInSlot(slot).isEmpty()) pull(slot);
    }
    private void output(int slot) {
        Direction direction = direction(slot);
        BlockPos target = worldPosition.relative(direction);
        var neighbour = level.getBlockEntity(target);
        if (neighbour instanceof ChuteBlockEntity chute && !(chute instanceof NetheriteSmartChuteBlockEntity)
            && (chute.getItemMotion() > 0) != (direction == Direction.UP)) return;
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, target, direction.getOpposite());
        ItemStack held = transport.getStackInSlot(slot);
        if (inventory != null) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, held.copy(), false);
            if (remainder.getCount() != held.getCount()) transport.setStackInSlot(slot, remainder);
            return;
        }
        BlockState state = level.getBlockState(target);
        if (BlockHelper.hasBlockSolidSide(state, level, target, direction.getOpposite())) return;
        double y = worldPosition.getY() + (direction == Direction.UP ? 1.05 : -.05);
        ItemEntity dropped = new ItemEntity(level, worldPosition.getX() + .5, y, worldPosition.getZ() + .5, held.copy());
        dropped.setDefaultPickUpDelay();
        dropped.setDeltaMovement(0, direction.getStepY() * .25, 0);
        level.addFreshEntity(dropped);
        transport.setStackInSlot(slot, ItemStack.EMPTY);
    }
    private void pull(int slot) {
        Direction sourceSide = direction(slot).getOpposite();
        BlockPos sourcePos = worldPosition.relative(sourceSide);
        // Chutes push their own traffic. Pulling here would bypass native direction and redstone pause.
        if (level.getBlockEntity(sourcePos) instanceof ChuteBlockEntity) return;
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, sourcePos, sourceSide.getOpposite());
        if (inventory != null) {
            java.util.function.Predicate<ItemStack> predicate = stack -> accepts(stack) && route(stack) == direction(slot);
            int amount = getExtractionAmount();
            ExtractionCountMode mode = getExtractionMode();
            if (mode == ExtractionCountMode.EXACTLY && ItemHelper.extract(inventory, predicate, mode, amount, true).isEmpty()) return;
            ItemStack extracted = ItemHelper.extract(inventory, predicate, mode, amount, false);
            if (!extracted.isEmpty()) transport.setStackInSlot(slot, extracted);
            return;
        }
        if (slot == 0) {
            var belt = BlockEntityBehaviour.get(level, sourcePos, TransportedItemStackHandlerBehaviour.TYPE);
            if (belt != null) {
                belt.handleCenteredProcessingOnAllItems(.5F, carried -> {
                    int count = pickupCount(slot, carried.stack);
                    if (count == 0) return TransportedResult.doNothing();
                    transport.setStackInSlot(slot, carried.stack.copyWithCount(count));
                    var remainder = carried.copy();
                    remainder.stack.shrink(count);
                    return remainder.stack.isEmpty() ? TransportedResult.removeItem() : TransportedResult.convertTo(remainder);
                });
                return;
            }
        }
        if (!level.getBlockState(sourcePos).getCollisionShape(level, sourcePos).isEmpty()) return;
        AABB area = new AABB(sourcePos).deflate(.1, 0, .1);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            int count = pickupCount(slot, entity.getItem());
            if (count == 0) continue;
            transport.setStackInSlot(slot, entity.getItem().copyWithCount(count));
            ItemStack remainder = entity.getItem().copy();
            remainder.shrink(count);
            if (remainder.isEmpty()) entity.discard(); else entity.setItem(remainder);
            break;
        }
    }
    private int pickupCount(int slot, ItemStack stack) {
        if (!transport.getStackInSlot(slot).isEmpty() || !accepts(stack) || route(stack) != direction(slot)) return 0;
        int amount = getExtractionAmount();
        if (getExtractionMode() == ExtractionCountMode.EXACTLY && stack.getCount() < amount) return 0;
        return Math.min(stack.getCount(), amount);
    }
    private void changed() {
        if (loading || level == null) return;
        setChanged();
        if (!level.isClientSide) sendData();
    }
    @Override public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("DirectionFilters", directionFilters.serializeNBT(registries));
        tag.put("Transport", transport.serializeNBT(registries));
        tag.putIntArray("TransportTicks", remainingTicks);
        tag.putString("DefaultDirection", defaultDirection.getSerializedName());
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        loading = true;
        super.read(tag, registries, clientPacket);
        if (tag.contains("DirectionFilters")) directionFilters.deserializeNBT(registries, tag.getCompound("DirectionFilters"));
        if (tag.contains("Transport")) transport.deserializeNBT(registries, tag.getCompound("Transport"));
        int[] ticks = tag.getIntArray("TransportTicks");
        for (int slot = 0; slot < 2; slot++) {
            ItemStack sample = directionFilters.getStackInSlot(slot);
            if (!sample.isEmpty()) directionFilters.setStackInSlot(slot, sample.copyWithCount(1));
            remainingTicks[slot] = ticks.length == 2 ? Math.max(0, Math.min(5, ticks[slot])) : 5;
        }
        defaultDirection = "up".equals(tag.getString("DefaultDirection")) ? Direction.UP : Direction.DOWN;
        loading = false;
    }
    public void takeItems(Player player) {
        for (int slot = 0; slot < 2; slot++) player.getInventory().placeItemBackInInventory(transport.extractItem(slot, 64, false));
    }
    @Override public void clearContent() {
        transport.setStackInSlot(0, ItemStack.EMPTY);
        transport.setStackInSlot(1, ItemStack.EMPTY);
        setDirectionFilter(0, ItemStack.EMPTY);
        setDirectionFilter(1, ItemStack.EMPTY);
        super.clearContent();
    }
    @Override public void destroy() {
        if (level != null && !level.isClientSide) {
            for (int slot = 0; slot < 2; slot++) Block.popResource(level, worldPosition, transport.extractItem(slot, 64, false));
        }
        super.destroy();
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(getDisplayName());
        for (int slot = 0; slot < 2; slot++) tooltip.add(Component.translatable("tooltip.createnetherite.smart_chute.buffer",
            Component.translatable("gui.createnetherite.smart_chute." + direction(slot).getSerializedName()), transport.getStackInSlot(slot).getCount()));
        return true;
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NetheriteSmartChuteMenu(id, inventory, this);
    }
}
