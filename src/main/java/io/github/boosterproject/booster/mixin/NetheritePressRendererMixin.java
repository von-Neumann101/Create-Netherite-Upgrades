package io.github.boosterproject.booster.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import io.github.boosterproject.booster.content.kinetics.press.NetheriteMechanicalPressBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalPressRenderer.class, remap = false)
public abstract class NetheritePressRendererMixin {
    @Inject(method = "renderSafe(Lcom/simibubi/create/content/kinetics/press/MechanicalPressBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"))
    private void booster$renderFilter(MechanicalPressBlockEntity press, float partialTicks, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (press instanceof NetheriteMechanicalPressBlockEntity) {
            FilteringRenderer.renderOnBlockEntity(press, partialTicks, pose, buffers, light, overlay);
        }
    }
}
