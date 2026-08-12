package io.github.boosterproject.booster.content.logistics.vault;

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NetheriteItemVaultBlockEntity extends ItemVaultBlockEntity {
    public static final int CAPACITY_MULTIPLIER = 16;

    public NetheriteItemVaultBlockEntity(BlockPos pos, BlockState state) {
        this(BoosterBlockEntityTypes.NETHERITE_ITEM_VAULT.get(), pos, state);
    }

    public NetheriteItemVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new ItemStackHandler(inventory.getSlots() * CAPACITY_MULTIPLIER) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                updateComparators();
                level.blockEntityChanged(worldPosition);
            }
        };
    }

    public IItemHandler getItemCapability() {
        getInvId();
        return itemCapability == null ? null : itemCapability.getCapability();
    }
}
