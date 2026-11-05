package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class WrenchMenu extends AbstractContainerMenu {
    public static final int[][] POSITIONS = {{136,20},{178,38},{196,80},{178,122},{136,140},{94,122},{76,80},{94,38}};
    private static final int MODULE_START = 2, PLAYER_START = 10;
    private final Player viewer;
    private final BlockPos pos;
    private final ResourceKey<Level> dimension;
    private final AssemblyTableBlockEntity table;
    private final ItemStackHandler inputs;
    private final ItemStackHandler modules;

    public WrenchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, null, data.readBlockPos());
    }

    public WrenchMenu(int id, Inventory inventory, AssemblyTableBlockEntity table) {
        this(id, inventory, table, table.getBlockPos());
    }

    private WrenchMenu(int id, Inventory inventory, AssemblyTableBlockEntity table, BlockPos pos) {
        super(BoosterMenuTypes.NETHERITE_WRENCH.get(), id);
        viewer = inventory.player;
        this.pos = pos.immutable();
        dimension = viewer.level().dimension();
        this.table = table;
        inputs = table != null ? table.getInventory() : new ItemStackHandler(2) {
            @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 ? WrenchData.isWrench(stack) : stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            }
        };
        modules = table != null ? table.getModules() : new ItemStackHandler(WrenchData.SLOTS) {
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return WrenchData.isWrench(inputs.getStackInSlot(0)) && WrenchModule.fromStack(stack) != null;
            }
        };
        addInputSlot(0, 136, 80);
        addInputSlot(1, 238, 80);
        for (int i = 0; i < WrenchData.SLOTS; i++) {
            addSlot(new SlotItemHandler(modules, i, POSITIONS[i][0], POSITIONS[i][1]) {
                @Override public void setChanged() {
                    super.setChanged();
                    if (WrenchMenu.this.table != null && stillValid(viewer)) WrenchMenu.this.table.saveModules();
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 63 + col * 18, 194 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 63 + col * 18, 252));
    }

    private void addInputSlot(int index, int x, int y) {
        addSlot(new SlotItemHandler(inputs, index, x, y) {
            @Override public void setChanged() {
                super.setChanged();
                if (table != null && stillValid(viewer)) table.inventoryChanged(index);
            }
        });
    }

    public int activationLimit() { return WrenchData.isWrench(inputs.getStackInSlot(0)) ? WrenchData.activationLimit(inputs.getStackInSlot(0)) : 0; }
    public boolean canUpgrade() { return activationLimit() == 1 && inputs.getStackInSlot(1).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE); }
    public int loadedCount() {
        int count = 0;
        for (int i = 0; i < WrenchData.SLOTS; i++) if (!modules.getStackInSlot(i).isEmpty()) count++;
        return count;
    }

    @Override public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true;
        return player == viewer && table != null && !table.isRemoved() && table.getLevel() == player.level()
            && dimension.equals(player.level().dimension()) && player.level().getBlockEntity(pos) == table
            && player.level().getBlockState(pos).is(BoosterBlocks.ASSEMBLY_TABLE.get()) && player.distanceToSqr(pos.getCenter()) <= 64;
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || !stillValid(player) || player.level().isClientSide || table == null || !table.upgrade()) return false;
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || slot >= slots.size() || slot < -1 && slot != -999) return;
        if (slot < 0 && (type == ClickType.SWAP || type == ClickType.QUICK_CRAFT && getQuickcraftHeader(button) == 1)) return;
        super.clicked(slot, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var original = stack.copy();
        boolean moved;
        if (index < PLAYER_START) moved = moveItemStackTo(stack, PLAYER_START, slots.size(), true);
        else if (WrenchData.isWrench(stack)) moved = moveItemStackTo(stack, 0, 1, false);
        else if (stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) moved = moveItemStackTo(stack, 1, MODULE_START, false);
        else {
            moved = false;
            if (WrenchModule.fromStack(stack) != null) {
                while (!stack.isEmpty()) {
                    int before = stack.getCount();
                    if (!moveItemStackTo(stack, MODULE_START, PLAYER_START, false) || stack.getCount() >= before) break;
                    moved = true;
                }
            }
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
