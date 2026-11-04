package io.github.boosterproject.booster.mixin;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.depot.EjectorTargetHandler;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEjectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EjectorTargetHandler.class, remap = false)
public abstract class NetheriteEjectorTargetMixin {
    @Inject(method = "drawArc", at = @At("HEAD"), cancellable = true)
    private static void booster$replaceCardinalPreview(CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && AllItems.WRENCH.isIn(mc.player.getMainHandItem())
            && mc.hitResult instanceof BlockHitResult hit && mc.level.getBlockEntity(hit.getBlockPos()) instanceof NetheriteEjectorBlockEntity) ci.cancel();
    }
}
