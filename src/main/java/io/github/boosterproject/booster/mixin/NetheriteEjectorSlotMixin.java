package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.logistics.depot.EjectorBlock;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEjectorBlock;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.logistics.depot.EjectorBlockEntity$EjectorSlot", remap = false)
public abstract class NetheriteEjectorSlotMixin {
    @Inject(method = "angle", at = @At("HEAD"), cancellable = true)
    private void booster$nativeValueBoxDirection(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (state.getBlock() instanceof NetheriteEjectorBlock) cir.setReturnValue(AngleHelper.horizontalAngle(state.getValue(EjectorBlock.HORIZONTAL_FACING)));
    }
}
