package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.NetheriteArmModels;
import io.github.boosterproject.booster.content.kinetics.mechanicalArm.NetheriteMechanicalArmBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArmVisual.class, remap = false)
public abstract class NetheriteArmVisualMixin {
    @Unique private boolean booster$netherite;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), index = 0)
    private static PartialModel booster$skinConstructor(PartialModel original, @Local(argsOnly = true) ArmBlockEntity arm) {
        return arm instanceof NetheriteMechanicalArmBlockEntity ? NetheriteArmModels.skin(original) : original;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void booster$rememberType(VisualizationContext context, ArmBlockEntity arm, float partialTick, CallbackInfo ci) {
        booster$netherite = arm instanceof NetheriteMechanicalArmBlockEntity;
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE",
        target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), index = 0)
    private PartialModel booster$skinUpdatedClaw(PartialModel original) {
        return booster$netherite ? NetheriteArmModels.skin(original) : original;
    }
}
