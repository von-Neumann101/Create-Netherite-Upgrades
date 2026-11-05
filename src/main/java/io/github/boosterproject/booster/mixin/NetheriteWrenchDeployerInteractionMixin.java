package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.deployer.DeployerMovingInteraction;
import io.github.boosterproject.booster.content.equipment.wrench.NetheriteWrenchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DeployerMovingInteraction.class, remap = false)
public abstract class NetheriteWrenchDeployerInteractionMixin {
    @WrapOperation(method = "handlePlayerInteraction", at = @At(value = "INVOKE",
        target = "Ljava/lang/Object;equals(Ljava/lang/Object;)Z"))
    private boolean booster$recognizeBasicWrench(Object heldItem, Object compared, Operation<Boolean> original) {
        return original.call(heldItem, compared)
            || heldItem instanceof NetheriteWrenchItem && compared == AllItems.WRENCH.get();
    }
}
