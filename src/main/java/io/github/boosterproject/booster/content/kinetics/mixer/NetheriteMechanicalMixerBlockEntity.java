package io.github.boosterproject.booster.content.kinetics.mixer;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteMechanicalMixerBlockEntity extends MechanicalMixerBlockEntity {
    public NetheriteMechanicalMixerBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_MIXER.get(), pos, state);
    }

    @Override
    protected void applyBasinRecipe() {
        if (currentRecipe == null) return;
        getBasin().ifPresent(basin -> {
            int processed = NetheriteMixerBatching.process(() -> matchBasinRecipe(currentRecipe), () -> {
                if (!BasinRecipe.apply(basin, currentRecipe)) return false;
                getProcessedRecipeTrigger().ifPresent(this::award);
                return true;
            });
            if (processed > 0) {
                basin.inputTank.sendDataImmediately();
                basin.notifyChangeOfContents();
            }
        });
        // The native mixer tick retracts after this call. A completed batch does
        // not restart the single-recipe continuation timer at runningTicks = 20.
    }
}
