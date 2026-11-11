package io.github.boosterproject.booster.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import io.github.boosterproject.booster.content.kinetics.portable.PortableStressDeviceBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public final class PortableStressDeviceRenderer extends KineticBlockEntityRenderer<PortableStressDeviceBlockEntity> {
    public PortableStressDeviceRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected SuperByteBuffer getRotatedModel(PortableStressDeviceBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state);
    }
}
