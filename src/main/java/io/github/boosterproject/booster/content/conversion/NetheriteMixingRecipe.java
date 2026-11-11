package io.github.boosterproject.booster.content.conversion;

import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import io.github.boosterproject.booster.content.processing.basin.NetheriteBasinBlockEntity;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;

public class NetheriteMixingRecipe extends BasinRecipe {
    public NetheriteMixingRecipe(ProcessingRecipeParams params) { super(BoosterRecipeTypes.NETHERITE_MIXING, params); }

    public boolean canProcessIn(BasinBlockEntity basin) {
        return basin instanceof NetheriteBasinBlockEntity
            && basin.getLevel().getBlockEntity(basin.getBlockPos().below()) instanceof NetheriteBlazeBurnerBlockEntity burner
            && burner.getBlazeCount() == 4;
    }
}
