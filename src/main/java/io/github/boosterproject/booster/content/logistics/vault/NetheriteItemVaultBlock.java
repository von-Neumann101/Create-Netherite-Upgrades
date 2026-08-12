package io.github.boosterproject.booster.content.logistics.vault;

import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetheriteItemVaultBlock extends ItemVaultBlock {
    public NetheriteItemVaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Class<ItemVaultBlockEntity> getBlockEntityClass() {
        return (Class) NetheriteItemVaultBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ItemVaultBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_ITEM_VAULT.get();
    }
}
