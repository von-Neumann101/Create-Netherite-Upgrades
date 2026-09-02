package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import io.github.boosterproject.booster.content.contraptions.elevator.NetheriteElevatorPulleyBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ElevatorPulleyBlockEntity.class, remap = false)
public abstract class ElevatorPulleyBlockEntityMixin {
    @ModifyExpressionValue(
        method = "getMovementSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/contraptions/elevator/ElevatorPulleyBlockEntity;getSpeed()F"
        )
    )
    private float createnetherite$increaseSpeed(float original) {
        return (Object) this instanceof NetheriteElevatorPulleyBlockEntity
            ? original * NetheriteElevatorPulleyBlockEntity.SPEED_MULTIPLIER
            : original;
    }

    @ModifyVariable(method = "assemble", at = @At("STORE"), ordinal = 0)
    private int createnetherite$increaseAssemblyRange(int original) {
        if (!((Object) this instanceof NetheriteElevatorPulleyBlockEntity pulley)) {
            return original;
        }
        return Math.max(0, Math.min(
            NetheriteElevatorPulleyBlockEntity.getConfiguredMaxDistance(),
            pulley.getBlockPos().getY() - 1 - pulley.getLevel().getMinBuildHeight()
        ));
    }
}
