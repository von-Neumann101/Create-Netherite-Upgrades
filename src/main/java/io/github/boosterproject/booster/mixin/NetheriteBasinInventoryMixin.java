package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinInventory;
import io.github.boosterproject.booster.content.processing.basin.NetheriteBasinBlockEntity;
import io.github.boosterproject.booster.content.processing.basin.NetheriteBasinInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class NetheriteBasinInventoryMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(ILcom/simibubi/create/content/processing/basin/BasinBlockEntity;)Lcom/simibubi/create/content/processing/basin/BasinInventory;"))
    private static BasinInventory booster$enlargeInventory(int slots, BasinBlockEntity basin, Operation<BasinInventory> original) {
        return basin instanceof NetheriteBasinBlockEntity ? new NetheriteBasinInventory(slots * 3, basin) : original.call(slots, basin);
    }
}
