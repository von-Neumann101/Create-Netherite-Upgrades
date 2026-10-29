package io.github.boosterproject.booster.content.kinetics.fan;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import io.github.boosterproject.booster.config.BoosterConfigs;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteFilterData;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
    private final ItemStackHandler filterInventory = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(BoosterItems.NETHERITE_FILTER.get());
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (!stack.isEmpty() && (!isItemValid(slot, stack) || stack.getCount() > 1)) {
                throw new IllegalArgumentException("The fan accepts one netherite filter");
            }
            super.setStackInSlot(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sendData();
        }
    };

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
        return NetheriteFilterData.resolve(filterInventory.getStackInSlot(0), stack, processingMode);
    }

    public int getProcessingSpeedMultiplier() {
        return BoosterConfigs.SERVER.netheriteFanProcessingSpeedMultiplier.get();
    }

    public float getWindSpeedMultiplier() {
        return BoosterConfigs.SERVER.netheriteFanWindSpeedMultiplier.get().floatValue();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        processingMode = FanProcessingMode.byName(tag.getString("Mode"));
        filterInventory.deserializeNBT(registries, tag.getCompound("Filter"));
        super.read(tag, registries, clientPacket);
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Mode", processingMode.serializedName());
        tag.put("Filter", filterInventory.serializeNBT(registries));
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null && !level.isClientSide) {
            ItemStack filter = filterInventory.extractItem(0, 1, false);
            if (!filter.isEmpty()) {
                Block.popResource(level, worldPosition, filter);
            }
        }
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
