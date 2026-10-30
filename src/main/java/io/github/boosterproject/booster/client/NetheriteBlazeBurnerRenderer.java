package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class NetheriteBlazeBurnerRenderer extends BlazeBurnerRenderer {
    public NetheriteBlazeBurnerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(BlazeBurnerBlockEntity be, float partialTick, PoseStack poses,
                              MultiBufferSource buffers, int light, int overlay) {
        int count = be instanceof NetheriteBlazeBurnerBlockEntity burner ? burner.getBlazeCount() : 1;
        for (int index = 0; index < count; index++) {
            poses.pushPose();
            if (count > 1) {
                poses.translate((index % 2) * .5, .25, (index / 2) * .5);
                poses.scale(.5F, .5F, .5F);
            }
            super.renderSafe(be, partialTick, poses, buffers, light, overlay);
            poses.popPose();
        }
    }
}
