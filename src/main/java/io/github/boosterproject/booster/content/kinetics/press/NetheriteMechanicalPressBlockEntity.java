package io.github.boosterproject.booster.content.kinetics.press;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteMechanicalPressBlockEntity extends MechanicalPressBlockEntity {
    public NetheriteMechanicalPressBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_PRESS.get(), pos, state);
    }

    @Override
    public boolean canProcessInBulk() {
        return true;
    }

    @Override
    public boolean tryProcessInBasin(boolean simulate) {
        boolean processed = super.tryProcessInBasin(simulate);
        while (matchBasinRecipe(currentRecipe)) {
            applyBasinRecipe();
        }
        return processed;
    }
}
