package io.github.boosterproject.booster.content.kinetics.mixer;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteMechanicalMixerBlockEntity extends MechanicalMixerBlockEntity {
    private static final Object NETHERITE_RECIPES = new Object();
    public NetheriteMechanicalMixerBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_MIXER.get(), pos, state);
    }

    @Override protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
        return recipe.value().getType() == BoosterRecipeTypes.NETHERITE_MIXING.getType() || super.matchStaticFilters(recipe);
    }

    @Override protected Object getRecipeCacheKey() { return NETHERITE_RECIPES; }

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
