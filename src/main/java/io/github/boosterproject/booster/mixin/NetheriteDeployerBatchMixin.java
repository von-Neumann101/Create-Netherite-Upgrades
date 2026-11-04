package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import io.github.boosterproject.booster.content.kinetics.deployer.NetheriteDeployerBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BeltDeployerCallbacks.class, remap = false)
public abstract class NetheriteDeployerBatchMixin {
    @Inject(method = "activate", at = @At("HEAD"), cancellable = true)
    private static void booster$batchInputs(TransportedItemStack input, TransportedItemStackHandlerBehaviour handler,
                                           DeployerBlockEntity be, Recipe<?> recipe, CallbackInfo ci) {
        if (be instanceof NetheriteDeployerBlockEntity upgraded && !upgraded.isApplyingBatch()) {
            upgraded.applyBatch(input, handler);
            ci.cancel();
        }
    }
}
