package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class NetheriteBlazeBurnerAnimationMixin {
    @ModifyReturnValue(method = "shouldTickAnimation", at = @At("RETURN"))
    private boolean booster$animateRenderer(boolean original) {
        return original || (Object) this instanceof NetheriteBlazeBurnerBlockEntity;
    }
}
