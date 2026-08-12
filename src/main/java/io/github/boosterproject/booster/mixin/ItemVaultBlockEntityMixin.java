package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import io.github.boosterproject.booster.content.logistics.vault.NetheriteItemVaultBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemVaultBlockEntity.class, remap = false)
public class ItemVaultBlockEntityMixin {
    @Redirect(
        method = "initCapability",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntityEntry;get()Ljava/lang/Object;"
        )
    )
    private Object booster$useNetheriteVaultType(BlockEntityEntry<?> entry) {
        return (Object) this instanceof NetheriteItemVaultBlockEntity
            ? BoosterBlockEntityTypes.NETHERITE_ITEM_VAULT.get()
            : entry.get();
    }
}
