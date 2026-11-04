package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.BoosterPartialModels;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEjectorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = EjectorRenderer.class, remap = false)
public abstract class NetheriteEjectorRendererMixin {
    @ModifyArg(method = "renderSafe", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), index = 0)
    private static PartialModel booster$skinTop(PartialModel original, @Local(argsOnly = true) EjectorBlockEntity ejector) {
        return ejector instanceof NetheriteEjectorBlockEntity && original == AllPartialModels.EJECTOR_TOP ? BoosterPartialModels.NETHERITE_EJECTOR_TOP : original;
    }
}
