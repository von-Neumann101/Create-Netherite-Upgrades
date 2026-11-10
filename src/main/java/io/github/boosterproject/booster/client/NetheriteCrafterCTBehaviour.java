package io.github.boosterproject.booster.client;

import com.simibubi.create.content.kinetics.crafter.CrafterCTBehaviour;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import io.github.boosterproject.booster.Booster;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteCrafterCTBehaviour extends CrafterCTBehaviour {
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_crafter_side");
    private static final ResourceLocation CONNECTED = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_crafter_side_connected");
    private static final CTSpriteShiftEntry VERTICAL = CTSpriteShifter.getCT(AllCTTypes.VERTICAL, SIDE, CONNECTED);
    private static final CTSpriteShiftEntry HORIZONTAL = CTSpriteShifter.getCT(AllCTTypes.HORIZONTAL, SIDE, CONNECTED);

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, TextureAtlasSprite sprite) {
        Direction facing = state.getValue(MechanicalCrafterBlock.HORIZONTAL_FACING);
        if (facing.getAxis() == direction.getAxis()) return null;
        return direction.getAxis().isVertical() && facing.getAxis() != Direction.Axis.X ? HORIZONTAL : VERTICAL;
    }
}
