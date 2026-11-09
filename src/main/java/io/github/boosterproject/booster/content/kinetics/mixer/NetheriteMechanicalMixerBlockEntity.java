package io.github.boosterproject.booster.content.kinetics.mixer;

import java.util.List;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import io.github.boosterproject.booster.content.processing.NetheriteRecipeFilter;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteMechanicalMixerBlockEntity extends MechanicalMixerBlockEntity {
    private static final Object NETHERITE_RECIPES = new Object();
    private NetheriteRecipeFilter recipeFilter;
    public NetheriteMechanicalMixerBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_MIXER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        recipeFilter = new NetheriteRecipeFilter(this,
            new CenteredSideValueBoxTransform((state, side) -> side.getAxis().isHorizontal()));
        behaviours.add(recipeFilter);
    }

    @Override
    protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
        return recipeFilter.matches(recipe) && super.matchBasinRecipe(recipe);
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
