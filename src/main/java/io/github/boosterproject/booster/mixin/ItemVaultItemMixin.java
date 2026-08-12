package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.logistics.vault.ItemVaultItem;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemVaultItem.class, remap = false)
public class ItemVaultItemMixin {
    @Redirect(
        method = "tryMultiPlace",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntityEntry;get()Ljava/lang/Object;"
        )
    )
    private Object booster$useNetheriteVaultType(BlockEntityEntry<?> entry) {
        ItemVaultItem item = (ItemVaultItem) (Object) this;
        return item.getBlock() == BoosterBlocks.NETHERITE_ITEM_VAULT.get()
            ? BoosterBlockEntityTypes.NETHERITE_ITEM_VAULT.get()
            : entry.get();
    }
}
