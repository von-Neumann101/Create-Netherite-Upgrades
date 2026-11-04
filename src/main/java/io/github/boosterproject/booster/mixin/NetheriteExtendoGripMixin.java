package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import io.github.boosterproject.booster.content.equipment.NetheriteExtendoGripItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ExtendoGripItem.class, remap = false)
public abstract class NetheriteExtendoGripMixin {
    @WrapOperation(method = {"holdingExtendoGripIncreasesRange", "findAndDamageExtendoGrip", "isHoldingExtendoGrip"},
        at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean booster$recognizeUpgrade(ItemEntry<?> entry, ItemStack stack, Operation<Boolean> original) {
        return original.call(entry, stack) || NetheriteExtendoGripItem.isGrip(stack);
    }

    @Inject(method = "holdingExtendoGripIncreasesRange", at = @At("RETURN"))
    private static void booster$extraReach(EntityTickEvent.Pre event, CallbackInfo ci) {
        if (event.getEntity() instanceof Player player) NetheriteExtendoGripItem.updateExtraRange(player);
    }
}
