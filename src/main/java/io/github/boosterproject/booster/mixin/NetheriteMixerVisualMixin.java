package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixerVisual;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.BoosterPartialModels;
import io.github.boosterproject.booster.content.kinetics.mixer.NetheriteMechanicalMixerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = MixerVisual.class, remap = false)
public abstract class NetheriteMixerVisualMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), index = 0)
    private static PartialModel booster$skinHead(PartialModel original, @Local(argsOnly = true) MechanicalMixerBlockEntity mixer) {
        return mixer instanceof NetheriteMechanicalMixerBlockEntity && original == AllPartialModels.MECHANICAL_MIXER_HEAD
            ? BoosterPartialModels.NETHERITE_MIXER_HEAD : original;
    }
}
