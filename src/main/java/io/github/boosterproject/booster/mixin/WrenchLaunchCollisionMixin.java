package io.github.boosterproject.booster.mixin;

import io.github.boosterproject.booster.content.equipment.wrench.WrenchLaunch;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, remap = false)
public abstract class WrenchLaunchCollisionMixin {
    @Inject(method = "move", at = @At("TAIL"))
    private void booster$rememberLaunchCollision(MoverType type, Vec3 movement, CallbackInfo ci) {
        if ((Object) this instanceof Player player) WrenchLaunch.recordCollision(player);
    }
}
