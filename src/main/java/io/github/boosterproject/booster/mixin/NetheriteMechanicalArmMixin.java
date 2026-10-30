package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import io.github.boosterproject.booster.content.kinetics.mechanicalArm.NetheriteArmBehavior;
import io.github.boosterproject.booster.content.kinetics.mechanicalArm.NetheriteMechanicalArmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ArmBlockEntity.class, remap = false)
public abstract class NetheriteMechanicalArmMixin {
    @WrapOperation(method = "tickMovementProgress", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"))
    private float booster$movementSpeed(float limit, float magnitude, Operation<Float> original) {
        return (Object) this instanceof NetheriteMechanicalArmBlockEntity
            ? NetheriteArmBehavior.movementSpeed(limit, magnitude) : original.call(limit, magnitude);
    }

    @ModifyExpressionValue(method = "initInteractionPoints", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;getRange()I"))
    private int booster$loadingRange(int original) {
        return (Object) this instanceof NetheriteMechanicalArmBlockEntity
            ? NetheriteArmBehavior.workRange(original) : original;
    }

    @WrapOperation(method = "initInteractionPoints", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;deserialize(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;"))
    private ArmInteractionPoint booster$limitTarget(CompoundTag tag, Level level, BlockPos anchor,
                                                   Operation<ArmInteractionPoint> original) {
        if ((Object) this instanceof NetheriteMechanicalArmBlockEntity arm
            && !NetheriteArmBehavior.isWithinRange(tag, arm.getWorkingRange())) {
            return null;
        }
        return original.call(tag, level, anchor);
    }
}
