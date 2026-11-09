package io.github.boosterproject.booster.content.kinetics.press;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import io.github.boosterproject.booster.content.processing.NetheriteRecipeFilter;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class NetheriteMechanicalPressBlockEntity extends MechanicalPressBlockEntity {
    private NetheriteRecipeFilter recipeFilter;

    public NetheriteMechanicalPressBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_PRESS.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        recipeFilter = new NetheriteRecipeFilter(this, new CenteredSideValueBoxTransform((state, side) ->
            side.getAxis().isHorizontal() && side.getAxis() != state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()));
        behaviours.add(recipeFilter);
    }

    @Override
    protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
        return recipeFilter.matches(recipe) && super.matchBasinRecipe(recipe);
    }

    @Override
    protected void applyBasinRecipe() {
        if (recipeFilter.matches(currentRecipe)) super.applyBasinRecipe();
    }

    @Override
    public Optional<RecipeHolder<PressingRecipe>> getRecipe(ItemStack item) {
        return super.getRecipe(item).filter(recipe -> recipeFilter.matches(recipe.value()));
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
