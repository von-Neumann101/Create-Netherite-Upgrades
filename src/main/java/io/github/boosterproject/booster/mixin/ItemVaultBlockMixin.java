package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemVaultBlock.class, remap = false)
public class ItemVaultBlockMixin {
    @Redirect(
        method = "isVault",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private static boolean booster$recognizeNetheriteVault(BlockEntry<?> entry, BlockState state) {
        return entry.has(state) || state.is(BoosterBlocks.NETHERITE_ITEM_VAULT.get());
    }
}
