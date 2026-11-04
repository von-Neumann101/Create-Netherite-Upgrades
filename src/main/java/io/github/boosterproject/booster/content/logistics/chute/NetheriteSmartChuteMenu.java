package io.github.boosterproject.booster.content.logistics.chute;

import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class NetheriteSmartChuteMenu extends AbstractContainerMenu {
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int PLAYER_X = 35, PLAYER_Y = 123;
    private final Player owner;
    private final BlockPos pos;
    private final NetheriteSmartChuteBlockEntity chute;
    private final ItemStackHandler rules;
    private final DataSlot defaultMode;
    private int syncedMode = 1;

    public NetheriteSmartChuteMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    private NetheriteSmartChuteMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof NetheriteSmartChuteBlockEntity be ? be : null, pos);
    }
    public NetheriteSmartChuteMenu(int id, Inventory inventory, NetheriteSmartChuteBlockEntity chute) {
        this(id, inventory, chute, chute.getBlockPos());
    }
    private NetheriteSmartChuteMenu(int id, Inventory inventory, NetheriteSmartChuteBlockEntity chute, BlockPos pos) {
        super(BoosterMenuTypes.NETHERITE_SMART_CHUTE.get(), id);
        owner = inventory.player;
        this.pos = pos.immutable();
        this.chute = chute;
        rules = chute == null ? new ItemStackHandler(2) : chute.getDirectionFilters();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, PLAYER_X + col * 18, PLAYER_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, PLAYER_X + col * 18, PLAYER_Y + 58));
        for (int slot = 0; slot < 2; slot++) addSlot(new SlotItemHandler(rules, slot, slot == 0 ? 48 : 166, 43) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isFake() { return true; }
        });
        defaultMode = addDataSlot(new DataSlot() {
            @Override public int get() { return !owner.level().isClientSide && NetheriteSmartChuteMenu.this.chute != null
                ? (NetheriteSmartChuteMenu.this.chute.getDefaultDirection() == Direction.UP ? 0 : 1) : syncedMode; }
            @Override public void set(int value) { syncedMode = value == 0 ? 0 : 1; }
        });
    }
    public int getDefaultMode() { return defaultMode.get(); }
    @Override public boolean stillValid(Player player) {
        return player == owner && (player.level().isClientSide || chute != null && !chute.isRemoved()
            && player.level().getBlockEntity(pos) == chute && player.distanceToSqr(pos.getCenter()) <= 64);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button < 0 || button > 1 || !stillValid(player) || player.level().isClientSide || chute == null) return false;
        chute.setDefaultDirection(NetheriteSmartChuteBlockEntity.direction(button));
        broadcastChanges();
        return true;
    }
    private void assign(int slot, ItemStack stack) {
        if (chute != null && !owner.level().isClientSide) chute.setDirectionFilter(slot, stack);
        else rules.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        broadcastChanges();
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (type == ClickType.SWAP) {
            if (button >= 0 && button < 2 && slot >= 0 && slot < slots.size() && getSlot(slot).hasItem()) assign(button, getSlot(slot).getItem());
            return;
        }
        if (slot >= PLAYER_SLOT_COUNT && slot < slots.size()) {
            if (type == ClickType.PICKUP && (button == 0 || button == 1)) assign(slot - PLAYER_SLOT_COUNT, button == 1 ? ItemStack.EMPTY : getCarried());
            else if (type == ClickType.QUICK_MOVE) quickMoveStack(player, slot);
            return;
        }
        if (slot >= slots.size() || (slot < -1 && slot != -999)) return;
        super.clicked(slot, button, type, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        if (index >= PLAYER_SLOT_COUNT) assign(index - PLAYER_SLOT_COUNT, ItemStack.EMPTY);
        else if (getSlot(index).hasItem()) {
            for (int rule = 0; rule < 2; rule++) if (rules.getStackInSlot(rule).isEmpty()) { assign(rule, getSlot(index).getItem()); break; }
        }
        return ItemStack.EMPTY;
    }
}
