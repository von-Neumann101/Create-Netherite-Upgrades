package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import io.github.boosterproject.booster.content.kinetics.saw.NetheriteSawBlockEntity;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class NetheriteSawOutputMixin {
    @ModifyExpressionValue(method = "applyRecipe", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/recipe/ProcessingInventory;getSlots()I"))
    private int booster$fitAllBatchOutputs(int slots, @Local(ordinal = 0) List<ItemStack> results) {
        if ((Object) this instanceof NetheriteSawBlockEntity saw && results.size() + 1 > slots) {
            // Native applyRecipe clears the inventory before writing the completed result list.
            // Grow only at the first output write, retaining native export and save behavior.
            saw.inventory.setSize(results.size() + 1);
            return saw.inventory.getSlots();
        }
        return slots;
    }
}
