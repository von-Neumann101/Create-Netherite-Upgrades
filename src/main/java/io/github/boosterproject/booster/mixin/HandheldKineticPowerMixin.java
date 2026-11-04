package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.github.boosterproject.booster.content.equipment.HandheldKineticPower;
import java.lang.ref.WeakReference;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class HandheldKineticPowerMixin implements HandheldKineticPower.Target {
    @Unique private float booster$handSpeed;
    @Unique private long booster$powerExpiry;
    @Unique private WeakReference<Player> booster$operator = new WeakReference<>(null);

    @Override public boolean booster$isDrivenBy(Player player) { return booster$operator.get() == player; }

    @Override public boolean booster$claimPower(Player player, float speed) {
        var owner = booster$operator.get();
        if (booster$handSpeed != 0 && owner != null && owner != player) return false;
        var be = (KineticBlockEntity) (Object) this;
        float previous = be.getSpeed();
        booster$operator = new WeakReference<>(player);
        booster$handSpeed = speed;
        booster$refreshPower();
        if (previous != speed) { be.onSpeedChanged(previous); be.sendData(); }
        return true;
    }
    @Override public void booster$refreshPower() {
        booster$powerExpiry = ((KineticBlockEntity) (Object) this).getLevel().getGameTime() + 2;
    }
    @Override public void booster$stopPower() {
        if (booster$handSpeed == 0) { booster$operator.clear(); return; }
        var be = (KineticBlockEntity) (Object) this;
        float previous = be.getSpeed();
        booster$handSpeed = 0;
        booster$operator.clear();
        be.onSpeedChanged(previous);
        be.sendData();
    }

    @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)
    private void booster$directSpeed(CallbackInfoReturnable<Float> cir) {
        var be = (KineticBlockEntity) (Object) this;
        if (booster$handSpeed != 0 && be.getTheoreticalSpeed() == 0)
            cir.setReturnValue(be.getLevel() != null && be.getLevel().tickRateManager().isFrozen() ? 0 : booster$handSpeed);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void booster$expirePower(CallbackInfo ci) {
        var be = (KineticBlockEntity) (Object) this;
        if (be.getLevel().isClientSide || booster$handSpeed == 0) return;
        var player = booster$operator.get();
        if (be.getLevel().getGameTime() >= booster$powerExpiry || player == null || !HandheldKineticPower.isStillDriving(player, be))
            booster$stopPower();
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void booster$syncPower(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (clientPacket) tag.putFloat("NetheriteGripSpeed", booster$handSpeed);
    }
    @Inject(method = "read", at = @At("RETURN"))
    private void booster$readPower(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        booster$handSpeed = clientPacket ? tag.getFloat("NetheriteGripSpeed") : 0;
        if (!clientPacket) booster$operator.clear();
    }
}
