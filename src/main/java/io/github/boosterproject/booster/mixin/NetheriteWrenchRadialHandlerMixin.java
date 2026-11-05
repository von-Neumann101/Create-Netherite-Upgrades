package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchHandler;
import io.github.boosterproject.booster.content.equipment.wrench.NetheriteWrenchItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RadialWrenchHandler.class, remap = false)
public abstract class NetheriteWrenchRadialHandlerMixin {
    @ModifyExpressionValue(method = "onKeyInput", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private static Item booster$recognizeBasicWrench(Item heldItem) {
        return heldItem instanceof NetheriteWrenchItem ? AllItems.WRENCH.get() : heldItem;
    }
}
