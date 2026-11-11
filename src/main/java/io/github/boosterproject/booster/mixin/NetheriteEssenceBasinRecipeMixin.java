package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import io.github.boosterproject.booster.content.conversion.NetheriteMixingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasinRecipe.class, remap = false)
public abstract class NetheriteEssenceBasinRecipeMixin {
    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z", at = @At("HEAD"), cancellable = true)
    private static void booster$requireEssenceEquipment(BasinBlockEntity basin, Recipe<?> recipe, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (recipe instanceof NetheriteMixingRecipe essence && !essence.canProcessIn(basin)) cir.setReturnValue(false);
    }
}
