package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class WrenchMenu extends AbstractContainerMenu {
    public static final int[][] POSITIONS = {{136,20},{178,38},{196,80},{178,122},{136,140},{94,122},{76,80},{94,38}};
    private final Inventory inventory;
    private final ItemStack wrench;
    private final int sourceSlot;
    private final ItemStackHandler modules;
    private int clientActive = -1;
    private boolean loading = true;

    public WrenchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, inventory.getItem(data.readVarInt()));
    }
    public WrenchMenu(int id, Inventory inventory, ItemStack wrench) {
        super(BoosterMenuTypes.NETHERITE_WRENCH.get(), id);
        this.inventory = inventory; this.wrench = wrench; this.sourceSlot = inventory.selected;
        modules = new ItemStackHandler(WrenchData.SLOTS) {
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return WrenchModule.fromStack(stack) != null; }
            @Override protected void onContentsChanged(int slot) { save(); }
        };
        var stored = WrenchData.modules(wrench);
        for (int i = 0; i < WrenchData.SLOTS; i++) {
            modules.setStackInSlot(i, stored.get(i));
            addSlot(new SlotItemHandler(modules, i, POSITIONS[i][0], POSITIONS[i][1]));
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(col + row * 9 + 9, 63 + col * 18, 194 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(col, 63 + col * 18, 252);
        addDataSlot(new DataSlot() {
            @Override public int get() { return inventory.player.level().isClientSide ? clientActive : WrenchData.activeSlot(wrench); }
            @Override public void set(int value) { clientActive = value; }
        });
        loading = false;
    }
    private void addPlayerSlot(int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return index != sourceSlot; }
            @Override public boolean mayPlace(ItemStack stack) { return index != sourceSlot; }
        });
    }
    private void save() {
        if (loading || inventory.player.level().isClientSide || !stillValid(inventory.player)) return;
        java.util.List<ItemStack> contents = new java.util.ArrayList<>();
        for (int i = 0; i < WrenchData.SLOTS; i++) contents.add(modules.getStackInSlot(i));
        WrenchData.setModules(wrench, contents); inventory.setChanged();
    }
    public int activeSlot() { return inventory.player.level().isClientSide ? clientActive : WrenchData.activeSlot(wrench); }
    public int loadedCount() {
        int count = 0; for (int i = 0; i < WrenchData.SLOTS; i++) if (!modules.getStackInSlot(i).isEmpty()) count++;
        return count;
    }
    @Override public boolean stillValid(Player player) {
        return player == inventory.player && WrenchData.isWrench(player.getMainHandItem())
            && inventory.selected == sourceSlot && (player.level().isClientSide || player.getItemInHand(InteractionHand.MAIN_HAND) == wrench);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || button < 0 || button >= WrenchData.SLOTS || modules.getStackInSlot(button).isEmpty()) return false;
        if (!player.level().isClientSide) {
            WrenchData.setActiveSlot(wrench, activeSlot() == button ? -1 : button);
            inventory.setChanged(); broadcastChanges();
        }
        return true;
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || (type == ClickType.SWAP && (button == sourceSlot || button == 40))) return;
        if (slot >= 0 && slot < slots.size() && slots.get(slot).container == inventory && slots.get(slot).getContainerSlot() == sourceSlot) return;
        super.clicked(slot, button, type, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        boolean moved = index < WrenchData.SLOTS ? moveItemStackTo(stack, WrenchData.SLOTS, slots.size(), true)
            : WrenchModule.fromStack(stack) != null && moveItemStackTo(stack, 0, WrenchData.SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
