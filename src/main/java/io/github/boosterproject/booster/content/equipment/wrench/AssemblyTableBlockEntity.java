package io.github.boosterproject.booster.content.equipment.wrench;

import java.util.ArrayList;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The module handler is a shared editable view; only the two input stacks own items. */
public final class AssemblyTableBlockEntity extends BlockEntity implements MenuProvider {
    private boolean loadingModules;
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override public void setSize(int size) { super.setSize(2); }
        @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? WrenchData.isWrench(stack) : stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        }
        @Override protected void onContentsChanged(int slot) { inventoryChanged(slot); }
        @Override protected void onLoad() { loadModules(); }
    };
    private final ItemStackHandler modules = new ItemStackHandler(WrenchData.SLOTS) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return WrenchData.isWrench(inventory.getStackInSlot(0)) && WrenchModule.fromStack(stack) != null;
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (!loadingModules && !stack.isEmpty() && !isItemValid(slot, stack)) return;
            super.setStackInSlot(slot, stack);
        }
        @Override protected void onContentsChanged(int slot) { saveModules(); }
    };

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.ASSEMBLY_TABLE.get(), pos, state);
    }

    public ItemStackHandler getInventory() { return inventory; }
    public ItemStackHandler getModules() { return modules; }

    void inventoryChanged(int slot) {
        if (slot == 0) loadModules();
        contentsChanged();
    }

    private void loadModules() {
        loadingModules = true;
        try {
            var wrench = inventory.getStackInSlot(0);
            var stored = WrenchData.isWrench(wrench) ? WrenchData.modules(wrench) : java.util.List.<ItemStack>of();
            for (int i = 0; i < WrenchData.SLOTS; i++)
                modules.setStackInSlot(i, i < stored.size() ? stored.get(i) : ItemStack.EMPTY);
        } finally {
            loadingModules = false;
        }
    }

    public void saveModules() {
        if (loadingModules || level != null && level.isClientSide) return;
        var wrench = inventory.getStackInSlot(0);
        if (!WrenchData.isWrench(wrench)) return;
        var stored = new ArrayList<ItemStack>(WrenchData.SLOTS);
        for (int i = 0; i < WrenchData.SLOTS; i++) stored.add(modules.getStackInSlot(i));
        WrenchData.setModules(wrench, stored);
        contentsChanged();
    }

    public boolean upgrade() {
        if (level != null && level.isClientSide) return false;
        var wrench = inventory.getStackInSlot(0);
        var template = inventory.getStackInSlot(1);
        if (!WrenchData.isWrench(wrench) || !template.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
            || !WrenchData.unlockSecondSlot(wrench)) return false;
        inventory.extractItem(1, 1, false);
        contentsChanged();
        return true;
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide && !isRemoved())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("block.createnetherite.assembly_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new WrenchMenu(id, inventory, this); }
}
