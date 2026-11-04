package io.github.boosterproject.booster.content.kinetics.fan;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import io.github.boosterproject.booster.config.BoosterConfigs;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NetheriteEncasedFanBlockEntity extends EncasedFanBlockEntity implements MenuProvider {
    private FanProcessingMode processingMode = FanProcessingMode.WASHING;
    private ItemStack legacyFilter = ItemStack.EMPTY;
    private boolean loading;
    private final ItemStackHandler filterInventory = new ItemStackHandler(4) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isNativeFilter(stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (!stack.isEmpty() && (!isItemValid(slot, stack) || stack.getCount() > 1)) {
                throw new IllegalArgumentException("Each fan slot accepts one original Create filter");
            }
            super.setStackInSlot(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            filtersChanged();
        }
    };

    public static boolean isNativeFilter(ItemStack stack) {
        return stack.getItem() instanceof FilterItem
            && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("create");
    }

    private void filtersChanged() {
        if (loading || level == null) return;
        setChanged();
        if (!level.isClientSide) sendData();
    }

    public void returnLegacyFilter(Player player) {
        if (legacyFilter.isEmpty() || level == null || level.isClientSide) return;
        ItemStack returned = legacyFilter;
        legacyFilter = ItemStack.EMPTY;
        filtersChanged();
        player.getInventory().placeItemBackInInventory(returned);
        player.displayClientMessage(Component.translatable("gui.booster.netherite_fan.legacy_returned"), false);
    }

    public NetheriteEncasedFanBlockEntity(BlockPos pos, BlockState state) {
        this(BoosterBlockEntityTypes.NETHERITE_ENCASED_FAN.get(), pos, state);
    }

    public NetheriteEncasedFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        airCurrent = new NetheriteAirCurrent(this);
    }

    public ItemStackHandler getFilterInventory() {
        return filterInventory;
    }

    public FanProcessingMode getProcessingMode() {
        return processingMode;
    }

    public void setProcessingMode(FanProcessingMode mode) {
        if (processingMode == mode) {
            return;
        }
        processingMode = mode;
        updateAirFlow = true;
        setChanged();
        sendData();
    }

    public FanProcessingMode resolveProcessingMode(ItemStack stack) {
        for (FanProcessingMode mode : FanProcessingMode.values()) {
            ItemStack filter = filterInventory.getStackInSlot(mode.ordinal());
            if (!filter.isEmpty() && FilterItemStack.of(filter.copy()).test(level, stack)) return mode;
        }
        return processingMode;
    }

    public int getProcessingSpeedMultiplier() {
        return BoosterConfigs.SERVER.netheriteFanProcessingSpeedMultiplier.get();
    }

    public float getWindSpeedMultiplier() {
        return BoosterConfigs.SERVER.netheriteFanWindSpeedMultiplier.get().floatValue();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        loading = true;
        super.read(tag, registries, clientPacket);
        processingMode = FanProcessingMode.byName(tag.getString("Mode"));
        legacyFilter = ItemStack.parseOptional(registries, tag.getCompound("LegacyFilter"));
        for (int slot = 0; slot < 4; slot++) filterInventory.setStackInSlot(slot, ItemStack.EMPTY);
        if (tag.contains("Filters")) {
            ItemStackHandler saved = new ItemStackHandler(4);
            saved.deserializeNBT(registries, tag.getCompound("Filters"));
            for (int slot = 0; slot < Math.min(4, saved.getSlots()); slot++) filterInventory.setStackInSlot(slot, saved.getStackInSlot(slot));
        } else if (tag.contains("Filter")) {
            ItemStackHandler old = new ItemStackHandler(1);
            old.deserializeNBT(registries, tag.getCompound("Filter"));
            if (old.getSlots() > 0) legacyFilter = old.getStackInSlot(0).copy();
        }
        loading = false;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Mode", processingMode.serializedName());
        tag.put("Filters", filterInventory.serializeNBT(registries));
        tag.remove("Filter");
        if (legacyFilter.isEmpty()) tag.remove("LegacyFilter");
        else tag.put("LegacyFilter", legacyFilter.save(registries));
    }

    @Override
    public void destroy() {
        if (level != null && !level.isClientSide) {
            for (int slot = 0; slot < 4; slot++) Block.popResource(level, worldPosition, filterInventory.extractItem(slot, 1, false));
            if (!legacyFilter.isEmpty()) Block.popResource(level, worldPosition, legacyFilter);
            legacyFilter = ItemStack.EMPTY;
        }
        super.destroy();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NetheriteFanMenu(id, inventory, this);
    }
}
