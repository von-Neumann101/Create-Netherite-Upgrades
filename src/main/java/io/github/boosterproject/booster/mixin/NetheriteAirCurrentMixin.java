package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import io.github.boosterproject.booster.content.kinetics.fan.NetheriteEncasedFanBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AirCurrent.class, remap = false)
public abstract class NetheriteAirCurrentMixin {
    @Shadow
    @Final
    public IAirCurrentSource source;

    @ModifyExpressionValue(
        method = "tickAffectedEntities",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/fan/AirCurrent;getTypeAt(F)Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;"
        )
    )
    private FanProcessingType booster$selectItemProcessing(FanProcessingType original, @Local Entity entity) {
        if (original != null && source instanceof NetheriteEncasedFanBlockEntity fan
            && entity instanceof ItemEntity item) {
            return fan.resolveProcessingMode(item.getItem()).processingType();
        }
        return original;
    }

    @WrapOperation(
        method = "tickAffectedEntities",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessing;applyProcessing(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z"
        )
    )
    private boolean booster$advanceItemProcessing(ItemEntity entity, FanProcessingType type,
                                                 Operation<Boolean> original) {
        int steps = source instanceof NetheriteEncasedFanBlockEntity fan ? fan.getProcessingSpeedMultiplier() : 1;
        for (int step = 0; step < steps; step++) {
            boolean processed = original.call(entity, type);
            if (processed || !entity.isAlive()) {
                return processed;
            }
        }
        return false;
    }

    @ModifyExpressionValue(
        method = "tickAffectedEntities",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/fan/IAirCurrentSource;getSpeed()F")
    )
    private float booster$scaleWindSpeed(float speed) {
        return source instanceof NetheriteEncasedFanBlockEntity fan ? speed * fan.getWindSpeedMultiplier() : speed;
    }
}
