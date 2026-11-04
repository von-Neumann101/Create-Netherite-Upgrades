package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripRenderHandler;
import com.tterrag.registrate.util.entry.ItemEntry;
import io.github.boosterproject.booster.content.equipment.NetheriteExtendoGripItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ExtendoGripRenderHandler.class, remap = false)
public abstract class NetheriteGripRenderHandlerMixin {
    @WrapOperation(method = {"tick", "onRenderPlayerHand"}, at = @At(value = "INVOKE",
        target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean booster$renderUpgrade(ItemEntry<?> entry, ItemStack stack, Operation<Boolean> original) {
        return original.call(entry, stack) || NetheriteExtendoGripItem.isGrip(stack);
    }
}
