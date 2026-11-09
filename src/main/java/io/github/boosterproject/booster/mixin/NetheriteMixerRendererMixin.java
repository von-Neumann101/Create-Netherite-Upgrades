package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.BoosterPartialModels;
import io.github.boosterproject.booster.content.kinetics.mixer.NetheriteMechanicalMixerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalMixerRenderer.class, remap = false)
public abstract class NetheriteMixerRendererMixin {
    @Inject(method = "renderSafe(Lcom/simibubi/create/content/kinetics/mixer/MechanicalMixerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"))
    private void booster$renderFilter(MechanicalMixerBlockEntity mixer, float partialTicks, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (mixer instanceof NetheriteMechanicalMixerBlockEntity) {
            FilteringRenderer.renderOnBlockEntity(mixer, partialTicks, pose, buffers, light, overlay);
        }
    }

    @ModifyArg(method = "renderSafe", at = @At(value = "INVOKE",
        target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), index = 0)
    private static PartialModel booster$skinHead(PartialModel original, @Local(argsOnly = true) MechanicalMixerBlockEntity mixer) {
        return mixer instanceof NetheriteMechanicalMixerBlockEntity && original == AllPartialModels.MECHANICAL_MIXER_HEAD
            ? BoosterPartialModels.NETHERITE_MIXER_HEAD : original;
    }
}
