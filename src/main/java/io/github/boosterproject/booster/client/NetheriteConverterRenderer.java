package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.boosterproject.booster.content.conversion.NetheriteConverterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public class NetheriteConverterRenderer implements BlockEntityRenderer<NetheriteConverterBlockEntity> {
    public NetheriteConverterRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(NetheriteConverterBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var plate = be.getPlateInventory().getStackInSlot(0);
        if (plate.isEmpty()) return;
        pose.pushPose();
        pose.translate(.5, 1.015, .5);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(.45F, .45F, .45F);
        Minecraft.getInstance().getItemRenderer().renderStatic(plate, ItemDisplayContext.FIXED, light, overlay, pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }
}
