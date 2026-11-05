package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import io.github.boosterproject.booster.content.equipment.wrench.NetheriteWrenchItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractBogeyBlock.class, remap = false)
public abstract class NetheriteWrenchBogeyMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean booster$recognizeBasicWrench(ItemStack stack, Item compared, Operation<Boolean> original) {
        return original.call(stack, compared)
            || stack.getItem() instanceof NetheriteWrenchItem && compared == AllItems.WRENCH.get();
    }
}
