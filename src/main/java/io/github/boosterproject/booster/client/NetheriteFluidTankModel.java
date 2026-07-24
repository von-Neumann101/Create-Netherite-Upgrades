package io.github.boosterproject.booster.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import io.github.boosterproject.booster.Booster;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class NetheriteFluidTankModel extends CTModel {
    private static final ModelProperty<CullData> CULL_PROPERTY = new ModelProperty<>();
    private static final CTSpriteShiftEntry SIDE = getCT("fluid_tank");
    private static final CTSpriteShiftEntry TOP = getCT("fluid_tank_top");
    private static final CTSpriteShiftEntry INNER = getCT("fluid_tank_inner");

    public static NetheriteFluidTankModel standard(BakedModel originalModel) {
        return new NetheriteFluidTankModel(originalModel);
    }

    private NetheriteFluidTankModel(BakedModel originalModel) {
        super(originalModel, new FluidTankCTBehaviour(SIDE, TOP, INNER));
    }

    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
                                      ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        CullData cullData = new CullData();
        for (Direction direction : Iterate.horizontalDirections) {
            cullData.setCulled(direction, ConnectivityHandler.isConnected(world, pos, pos.relative(direction)));
        }
        return builder.with(CULL_PROPERTY, cullData);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        if (side != null) {
            return Collections.emptyList();
        }

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            if (extraData.has(CULL_PROPERTY) && extraData.get(CULL_PROPERTY).isCulled(direction)) {
                continue;
            }
            quads.addAll(super.getQuads(state, direction, rand, extraData, renderType));
        }
        quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
        return quads;
    }

    private static CTSpriteShiftEntry getCT(String name) {
        return CTSpriteShifter.getCT(
            AllCTTypes.RECTANGLE,
            ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_fluid_tank/" + name),
            ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_fluid_tank/" + name + "_connected")
        );
    }

    private static class CullData {
        private final boolean[] culledFaces = new boolean[4];

        private CullData() {
            Arrays.fill(culledFaces, false);
        }

        private void setCulled(Direction face, boolean cull) {
            if (!face.getAxis().isVertical()) {
                culledFaces[face.get2DDataValue()] = cull;
            }
        }

        private boolean isCulled(Direction face) {
            return !face.getAxis().isVertical() && culledFaces[face.get2DDataValue()];
        }
    }
}
