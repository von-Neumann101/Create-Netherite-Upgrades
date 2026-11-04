package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import io.github.boosterproject.booster.content.logistics.chute.NetheriteSmartChuteBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChuteBlockEntity.class, remap = false)
public abstract class NetheriteSmartChuteMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;tick()V", shift = At.Shift.AFTER), cancellable = true)
    private void booster$directionalTransport(CallbackInfo ci) {
        if ((Object) this instanceof NetheriteSmartChuteBlockEntity chute) {
            chute.tickDirectionalTransport();
            ci.cancel();
        }
    }
}
