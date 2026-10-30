package io.github.boosterproject.booster.content.processing.burner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class NetheriteBlazeBurnerMovementBehaviour extends BlazeBurnerMovementBehaviour {
    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld world,
                                    ContraptionMatrices matrices, MultiBufferSource buffers) {
        if (context.state.getValue(BlazeBurnerBlock.HEAT_LEVEL) == BlazeBurnerBlock.HeatLevel.NONE) {
            return;
        }
        int count = NetheriteBlazeBurnerData.readCount(context.blockEntityData);
        PoseStack model = matrices.getModel();
        for (int index = 0; index < count; index++) {
            model.pushPose();
            if (count > 1) {
                model.translate((index % 2) * .5, .25, (index / 2) * .5);
                model.scale(.5F, .5F, .5F);
            }
            super.renderInContraption(context, world, matrices, buffers);
            model.popPose();
        }
    }
}
