package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorVisual;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.BoosterPartialModels;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEjectorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = EjectorVisual.class, remap = false)
public abstract class NetheriteEjectorVisualMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), index = 0)
    private static PartialModel booster$skinTop(PartialModel original, @Local(argsOnly = true) EjectorBlockEntity ejector) {
        return ejector instanceof NetheriteEjectorBlockEntity && original == AllPartialModels.EJECTOR_TOP ? BoosterPartialModels.NETHERITE_EJECTOR_TOP : original;
    }
}
