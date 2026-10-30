package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.BoosterPartialModels;
import io.github.boosterproject.booster.content.kinetics.mixer.NetheriteMechanicalMixerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = MechanicalMixerRenderer.class, remap = false)
public abstract class NetheriteMixerRendererMixin {
    @ModifyArg(method = "renderSafe", at = @At(value = "INVOKE",
        target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), index = 0)
    private static PartialModel booster$skinHead(PartialModel original, @Local(argsOnly = true) MechanicalMixerBlockEntity mixer) {
        return mixer instanceof NetheriteMechanicalMixerBlockEntity && original == AllPartialModels.MECHANICAL_MIXER_HEAD
            ? BoosterPartialModels.NETHERITE_MIXER_HEAD : original;
    }
}
