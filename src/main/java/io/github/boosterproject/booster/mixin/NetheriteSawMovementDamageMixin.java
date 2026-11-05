package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import io.github.boosterproject.booster.content.kinetics.saw.NetheriteSawBlock;
import io.github.boosterproject.booster.content.kinetics.saw.NetheriteSawMovementBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public abstract class NetheriteSawMovementDamageMixin {
    @ModifyArg(method = "damageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float booster$netheriteSawDamage(float damage) {
        return (Object) this instanceof NetheriteSawMovementBehaviour ? damage * NetheriteSawBlock.DAMAGE_MULTIPLIER : damage;
    }
}
