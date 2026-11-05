package io.github.boosterproject.booster.content.conversion;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class NetheriteConversionRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public NetheriteConversionRecipe(ProcessingRecipeParams params) { super(BoosterRecipeTypes.CONVERSION, params); }
    @Override protected int getMaxInputCount() { return 1; }
    @Override protected int getMaxOutputCount() { return 1; }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredients.size() == 1 && ingredients.getFirst().test(input.item()); }
}
