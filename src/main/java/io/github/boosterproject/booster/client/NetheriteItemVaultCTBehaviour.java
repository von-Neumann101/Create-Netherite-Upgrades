package io.github.boosterproject.booster.client;

import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.content.logistics.vault.ItemVaultCTBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import io.github.boosterproject.booster.Booster;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteItemVaultCTBehaviour extends ItemVaultCTBehaviour {
    private static final CTSpriteShiftEntry TOP_MEDIUM = shift("top", "medium");
    private static final CTSpriteShiftEntry TOP_LARGE = shift("top", "large");
    private static final CTSpriteShiftEntry FRONT_MEDIUM = shift("front", "medium");
    private static final CTSpriteShiftEntry FRONT_LARGE = shift("front", "large");
    private static final CTSpriteShiftEntry SIDE_MEDIUM = shift("side", "medium");
    private static final CTSpriteShiftEntry SIDE_LARGE = shift("side", "large");
    private static final CTSpriteShiftEntry BOTTOM_MEDIUM = shift("bottom", "medium");
    private static final CTSpriteShiftEntry BOTTOM_LARGE = shift("bottom", "large");

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, TextureAtlasSprite sprite) {
        Direction.Axis axis = ItemVaultBlock.getVaultBlockAxis(state);
        if (axis == null) {
            return null;
        }

        boolean large = ItemVaultBlock.isLarge(state);
        if (direction.getAxis() == axis) {
            return large ? FRONT_LARGE : FRONT_MEDIUM;
        }
        if (direction == Direction.UP) {
            return large ? TOP_LARGE : TOP_MEDIUM;
        }
        if (direction == Direction.DOWN) {
            return large ? BOTTOM_LARGE : BOTTOM_MEDIUM;
        }
        return large ? SIDE_LARGE : SIDE_MEDIUM;
    }

    private static CTSpriteShiftEntry shift(String side, String size) {
        String path = "block/item_vault/vault_" + side;
        return CTSpriteShifter.getCT(
            AllCTTypes.RECTANGLE,
            new ResourceLocation(Booster.MOD_ID, path + "_small"),
            new ResourceLocation(Booster.MOD_ID, path + "_" + size)
        );
    }
}
