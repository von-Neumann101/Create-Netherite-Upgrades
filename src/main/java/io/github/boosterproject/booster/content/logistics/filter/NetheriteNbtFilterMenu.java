package io.github.boosterproject.booster.content.logistics.filter;

import java.util.List;
import com.simibubi.create.foundation.gui.menu.HeldItemGhostItemMenu;
import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class NetheriteNbtFilterMenu extends HeldItemGhostItemMenu {
    public static final int REFERENCE_SLOT = 36, PLAYER_X = 77, PLAYER_Y = 153;
    public NetheriteNbtFilterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) { super(BoosterMenuTypes.NBT_FILTER.get(), id, inventory, data); }
    public NetheriteNbtFilterMenu(int id, Inventory inventory, ItemStack filter) { super(BoosterMenuTypes.NBT_FILTER.get(), id, inventory, filter); }
    @Override protected boolean allowRepeats() { return false; }
    @Override protected ItemStackHandler createGhostInventory() {
        var inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, NetheriteNbtFilterData.reference(contentHolder, player.registryAccess())); return inventory;
    }
    @Override protected void addSlots() {
        addPlayerSlots(PLAYER_X, PLAYER_Y);
        addSlot(new SlotItemHandler(ghostInventory, 0, 18, 31) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isFake() { return true; }
        });
    }
    @Override protected Slot createPlayerSlot(Inventory inventory, int slot, int x, int y) {
        return new Slot(inventory, slot, x, y) {
            @Override public boolean mayPickup(Player player) { return slot != inventory.selected; }
            @Override public boolean mayPlace(ItemStack stack) { return slot != inventory.selected; }
        };
    }
    public ItemStack currentFilter() { return player.level().isClientSide ? player.getMainHandItem() : contentHolder; }
    public List<NetheriteNbtFilterData.Field> fields() { return NetheriteNbtFilterData.fields(ghostInventory.getStackInSlot(0), player.registryAccess()); }
    public int referenceHash() {
        ItemStack reference = ghostInventory.getStackInSlot(0);
        return java.util.Objects.hash(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(reference.getItem()),
            NetheriteNbtFilterData.snapshot(reference, player.registryAccess()));
    }
    @Override public boolean stillValid(Player candidate) {
        return candidate == player && contentHolder.getItem() instanceof NetheriteFilterItem
            && (candidate.level().isClientSide ? candidate.getMainHandItem().getItem() == contentHolder.getItem() : super.stillValid(candidate));
    }
    @Override public void clicked(int slot, int button, ClickType type, Player candidate) {
        if (!stillValid(candidate) || isInSlot(slot)) return;
        if (slot == REFERENCE_SLOT) {
            if (type == ClickType.PICKUP && (button == 0 || button == 1)) setReference(button == 1 ? ItemStack.EMPTY : getCarried());
            else if (type == ClickType.QUICK_MOVE) setReference(ItemStack.EMPTY);
            return;
        }
        if (type == ClickType.SWAP) {
            if (button == 0 && slot >= 0 && slot < REFERENCE_SLOT && !getSlot(slot).getItem().isEmpty()) setReference(getSlot(slot).getItem());
            return;
        }
        if (slot >= slots.size() || (slot < -1 && slot != -999)) return;
        super.clicked(slot, button, type, candidate);
    }
    @Override public ItemStack quickMoveStack(Player candidate, int slot) {
        if (!stillValid(candidate) || slot < 0 || slot >= slots.size() || isInSlot(slot)) return ItemStack.EMPTY;
        setReference(slot == REFERENCE_SLOT ? ItemStack.EMPTY : getSlot(slot).getItem()); return ItemStack.EMPTY;
    }
    private void setReference(ItemStack sample) {
        ghostInventory.setStackInSlot(0, sample.isEmpty() ? ItemStack.EMPTY : sample.copyWithCount(1));
        if (!player.level().isClientSide) { NetheriteNbtFilterData.setReference(contentHolder, sample); changed(); }
    }
    @Override public void clearContents() {
        if (stillValid(player)) setReference(ItemStack.EMPTY);
    }
    @Override protected void saveData(ItemStack filter) { if (!player.level().isClientSide && stillValid(player)) playerInventory.setChanged(); }
    private void changed() { playerInventory.setChanged(); broadcastChanges(); }
    public void apply(NetheriteNbtFilterNetworking.Action action) {
        if (player.level().isClientSide || !stillValid(player) || action.containerId() != containerId || action.referenceHash() != referenceHash()) return;
        var fields = fields();
        switch (action.action()) {
            case 0, 1 -> {
                if (action.field() < 0 || action.field() >= fields.size()) return;
                var field = fields.get(action.field());
                if (action.action() == 0) NetheriteNbtFilterData.toggleCondition(contentHolder, field);
                else if (!NetheriteNbtFilterData.setCondition(contentHolder, field, action.operator(), action.first(), action.second())) {
                    player.displayClientMessage(Component.translatable("gui.booster.nbt_filter.invalid"), true); return;
                }
            }
            case 2 -> NetheriteNbtFilterData.toggleBlacklist(contentHolder);
            case 3 -> NetheriteNbtFilterData.toggleSameItem(contentHolder);
            case 4 -> NetheriteNbtFilterData.clearConditions(contentHolder);
            default -> { return; }
        }
        changed();
    }
}
