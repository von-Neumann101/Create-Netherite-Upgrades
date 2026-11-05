package io.github.boosterproject.booster.mixin;

import com.simibubi.create.AllItems;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import io.github.boosterproject.booster.content.equipment.wrench.NetheriteWrenchItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemProviderEntry.class, remap = false)
public abstract class NetheriteWrenchRecognitionMixin {
    @Inject(method = "isIn", at = @At("RETURN"), cancellable = true)
    private void booster$recognizeBasicWrench(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof NetheriteWrenchItem && (Object) this == AllItems.WRENCH) {
            cir.setReturnValue(true);
        }
    }
}
