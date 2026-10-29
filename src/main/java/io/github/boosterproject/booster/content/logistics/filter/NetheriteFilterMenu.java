package io.github.boosterproject.booster.content.logistics.filter;

import com.simibubi.create.foundation.gui.menu.HeldItemGhostItemMenu;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class NetheriteFilterMenu extends HeldItemGhostItemMenu {
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int GROUP_X = 18;
    public static final int GROUP_SPACING = 66;
    public static final int GROUP_Y = 56;
    public static final int PLAYER_X = 64;
    public static final int PLAYER_Y = 153;

    public NetheriteFilterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        super(BoosterMenuTypes.NETHERITE_FILTER.get(), id, inventory, extraData);
    }

    public NetheriteFilterMenu(int id, Inventory inventory, ItemStack filter) {
        super(BoosterMenuTypes.NETHERITE_FILTER.get(), id, inventory, filter);
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        return NetheriteFilterData.inventory(contentHolder);
    }

    @Override
    protected boolean allowRepeats() {
        return false;
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(PLAYER_X, PLAYER_Y);
        for (int group = 0; group < NetheriteFilterData.GROUP_COUNT; group++) {
            for (int localSlot = 0; localSlot < NetheriteFilterData.GROUP_SIZE; localSlot++) {
                int slot = group * NetheriteFilterData.GROUP_SIZE + localSlot;
                addSlot(new SlotItemHandler(ghostInventory, slot,
                    GROUP_X + group * GROUP_SPACING + localSlot % 3 * 18,
                    GROUP_Y + localSlot / 3 * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean isFake() {
                        return true;
                    }
                });
            }
        }
    }

    @Override
    protected Slot createPlayerSlot(Inventory inventory, int slot, int x, int y) {
        return new Slot(inventory, slot, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return slot != inventory.selected;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return slot != inventory.selected;
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return player == playerInventory.player && contentHolder.getItem() instanceof NetheriteFilterItem
            && (player.level().isClientSide
                ? player.getMainHandItem().getItem() == contentHolder.getItem()
                : super.stillValid(player));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!stillValid(player)) {
            return;
        }

        // SWAP is used only to assign a server-owned source stack to one of the four groups.
        // Never let it reach vanilla's hotbar/offhand swapping code.
        if (clickType == ClickType.SWAP) {
            if (button >= 0 && button < NetheriteFilterData.GROUP_COUNT && slotId >= 0 && slotId < slots.size()) {
                ItemStack sample = getSlot(slotId).getItem();
                if (!sample.isEmpty()
                    && NetheriteFilterData.assignToGroup(ghostInventory, FanProcessingMode.byIndex(button), sample)) {
                    rulesChanged();
                }
            }
            return;
        }

        if (slotId >= PLAYER_SLOT_COUNT && slotId < slots.size()) {
            int sampleSlot = slotId - PLAYER_SLOT_COUNT;
            if (clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
                ItemStack sample = button == 1 ? ItemStack.EMPTY : getCarried();
                if (NetheriteFilterData.assign(ghostInventory, sampleSlot, sample)) {
                    rulesChanged();
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                quickMoveStack(player, slotId);
            }
            // Ghost slots cannot clone, throw, collect or drag real items.
            return;
        }

        if (slotId >= slots.size() || (slotId < -1 && slotId != -999) || isInSlot(slotId)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        if (!stillValid(player) || slotId < 0 || slotId >= slots.size() || isInSlot(slotId)) {
            return ItemStack.EMPTY;
        }
        if (slotId >= PLAYER_SLOT_COUNT) {
            if (NetheriteFilterData.assign(ghostInventory, slotId - PLAYER_SLOT_COUNT, ItemStack.EMPTY)) {
                rulesChanged();
            }
            return ItemStack.EMPTY;
        }

        ItemStack sample = getSlot(slotId).getItem();
        if (sample.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < ghostInventory.getSlots(); slot++) {
            ItemStack existing = ghostInventory.getStackInSlot(slot);
            if (!existing.isEmpty() && existing.is(sample.getItem())) {
                return ItemStack.EMPTY;
            }
        }
        for (int slot = 0; slot < ghostInventory.getSlots(); slot++) {
            if (ghostInventory.getStackInSlot(slot).isEmpty()) {
                if (NetheriteFilterData.assign(ghostInventory, slot, sample)) {
                    rulesChanged();
                }
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clearContents() {
        if (stillValid(player)) {
            super.clearContents();
            rulesChanged();
        }
    }

    private void rulesChanged() {
        saveData(contentHolder);
        broadcastChanges();
    }

    @Override
    protected void saveData(ItemStack filter) {
        if (!player.level().isClientSide && stillValid(player)) {
            NetheriteFilterData.save(filter, ghostInventory);
            playerInventory.setChanged();
        }
    }
}
