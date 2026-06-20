package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;

public class PowerfulMechanicalPumpRenderer extends KineticBlockEntityRenderer<PowerfulMechanicalPumpBlockEntity> {
    public PowerfulMechanicalPumpRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PowerfulMechanicalPumpBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        BlockState state = getRenderedBlockState(be);
        RenderType type = getRenderType(be, state);
        VertexConsumer vertexConsumer = buffer.getBuffer(type);
        standardKineticRotationTransform(getRotatedModel(be, state), be, light)
            .renderInto(ms, vertexConsumer);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PowerfulMechanicalPumpBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(BoosterPartialModels.POWERFUL_MECHANICAL_PUMP_COG, state);
    }
}
