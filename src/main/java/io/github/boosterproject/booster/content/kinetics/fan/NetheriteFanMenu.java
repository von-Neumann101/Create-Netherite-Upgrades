package io.github.boosterproject.booster.content.kinetics.fan;

import io.github.boosterproject.booster.content.logistics.filter.NetheriteFilterItem;
import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class NetheriteFanMenu extends AbstractContainerMenu {
    public static final int FILTER_X = 24;
    public static final int FILTER_Y = 80;
    public static final int PLAYER_X = 64;
    public static final int PLAYER_Y = 138;
    private static final int PLAYER_SLOTS_START = 1;
    private static final int HOTBAR_START = 28;
    private static final int SLOTS_END = 37;

    private final NetheriteEncasedFanBlockEntity fan;
    private final BlockPos blockPos;
    private final Player owner;
    private final DataSlot modeData;
    private int syncedMode;

    public NetheriteFanMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(id, inventory, extraData.readBlockPos());
    }

    private NetheriteFanMenu(int id, Inventory inventory, BlockPos blockPos) {
        this(id, inventory,
            inventory.player.level().getBlockEntity(blockPos) instanceof NetheriteEncasedFanBlockEntity fan ? fan : null,
            blockPos);
    }

    public NetheriteFanMenu(int id, Inventory inventory, NetheriteEncasedFanBlockEntity fan) {
        this(id, inventory, fan, fan.getBlockPos());
    }

    private NetheriteFanMenu(int id, Inventory inventory, NetheriteEncasedFanBlockEntity fan, BlockPos blockPos) {
        super(BoosterMenuTypes.NETHERITE_FAN.get(), id);
        this.fan = fan;
        this.blockPos = blockPos.immutable();
        owner = inventory.player;
        ItemStackHandler filters = fan == null ? new ItemStackHandler(1) : fan.getFilterInventory();
        addSlot(new SlotItemHandler(filters, 0, FILTER_X, FILTER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof NetheriteFilterItem && super.mayPlace(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, PLAYER_X + column * 18, PLAYER_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_X + column * 18, PLAYER_Y + 58));
        }
        modeData = addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return !owner.level().isClientSide && NetheriteFanMenu.this.fan != null
                    ? NetheriteFanMenu.this.fan.getProcessingMode().ordinal() : syncedMode;
            }

            @Override
            public void set(int value) {
                syncedMode = FanProcessingMode.byIndex(value).ordinal();
            }
        });
    }

    public FanProcessingMode getProcessingMode() {
        return FanProcessingMode.byIndex(modeData.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button < 0 || button >= FanProcessingMode.values().length || !stillValid(player)
            || player.level().isClientSide || fan == null) {
            return false;
        }
        fan.setProcessingMode(FanProcessingMode.byIndex(button));
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != owner) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return fan != null && !fan.isRemoved() && player.level().getBlockEntity(blockPos) == fan
            && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot source = getSlot(index);
        if (!source.hasItem() || !source.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, PLAYER_SLOTS_START, SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof NetheriteFilterItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, SLOTS_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_SLOTS_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        source.onTake(player, stack);
        return original;
    }
}
