package io.github.boosterproject.booster.registry;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.conversion.NetheriteConversionRecipe;
import io.github.boosterproject.booster.content.conversion.NetheriteMixingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public enum BoosterRecipeTypes implements IRecipeTypeInfo {
    CONVERSION("netherite_conversion", NetheriteConversionRecipe::new),
    NETHERITE_MIXING("netherite_mixing", NetheriteMixingRecipe::new);

    private final ResourceLocation id;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializer;
    private final DeferredHolder<RecipeType<?>, RecipeType<?>> type;

    BoosterRecipeTypes(String name, StandardProcessingRecipe.Factory<?> factory) {
        id = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, name);
        serializer = Registers.SERIALIZERS.register(name, () -> new StandardProcessingRecipe.Serializer<>(factory));
        type = Registers.TYPES.register(name, () -> RecipeType.simple(id));
    }

    @Override public ResourceLocation getId() { return id; }
    @Override @SuppressWarnings("unchecked") public <T extends RecipeSerializer<?>> T getSerializer() { return (T) serializer.get(); }
    @Override @SuppressWarnings("unchecked") public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() { return (RecipeType<R>) type.get(); }
    public static void register(IEventBus bus) { Registers.SERIALIZERS.register(bus); Registers.TYPES.register(bus); }

    private static class Registers {
        static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Booster.MOD_ID);
        static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Booster.MOD_ID);
    }
}
