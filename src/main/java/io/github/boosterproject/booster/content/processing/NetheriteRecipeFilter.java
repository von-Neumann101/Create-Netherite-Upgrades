package io.github.boosterproject.booster.content.processing;

import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.world.item.crafting.Recipe;

/** Native recipe-output filtering shared by the Netherite Press and Mixer. */
public final class NetheriteRecipeFilter extends FilteringBehaviour {
    public NetheriteRecipeFilter(BasinOperatingBlockEntity machine, ValueBoxTransform slot) {
        super(machine, slot);
        forRecipes();
        withCallback(stack -> machine.basinChecker.scheduleUpdate());
    }

    public boolean matches(Recipe<?> recipe) {
        if (recipe == null) return false;
        // Match the same primary output as the basin's own recipe filter.
        if (recipe instanceof BasinRecipe basin && basin.getRollableResults().isEmpty()
            && !basin.getFluidResults().isEmpty()) {
            return test(basin.getFluidResults().getFirst());
        }
        return test(recipe.getResultItem(getWorld().registryAccess()));
    }
}
