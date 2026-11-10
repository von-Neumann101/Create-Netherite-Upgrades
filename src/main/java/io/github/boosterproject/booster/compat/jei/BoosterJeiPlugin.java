package io.github.boosterproject.booster.compat.jei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.MixingCategory;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.conversion.NetheriteConversionRecipe;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerData;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;

/** Loaded by JEI discovery only; common mod startup never references this class. */
@JeiPlugin
public final class BoosterJeiPlugin implements IModPlugin {
    private CreateRecipeCategory<BasinRecipe> mixing;
    private CreateRecipeCategory<NetheriteConversionRecipe> conversion;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        mixing = new CreateRecipeCategory.Builder<>(BasinRecipe.class)
            .catalyst(BoosterItems.NETHERITE_MECHANICAL_MIXER::get)
            .catalyst(AllBlocks.BASIN::get)
            .doubleItemIcon(BoosterItems.NETHERITE_MECHANICAL_MIXER.get(), AllBlocks.BASIN.get())
            .emptyBackground(177, 103)
            .build(BoosterRecipeTypes.NETHERITE_MIXING.getId(), MixingCategory::standard);
        conversion = new CreateRecipeCategory.Builder<>(NetheriteConversionRecipe.class)
            .catalyst(BoosterItems.NETHERITE_CONVERTER::get)
            .itemIcon(BoosterItems.NETHERITE_CONVERTER.get())
            .emptyBackground(177, 126)
            .build(BoosterRecipeTypes.CONVERSION.getId(), NetheriteConversionCategory::new);
        registration.addRecipeCategories(mixing, conversion);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) registerRecipesFromManager(registration, level.getRecipeManager());
    }

    public void registerRecipesFromManager(IRecipeRegistration registration, RecipeManager recipes) {
        registration.addRecipes(mixing.getRecipeType(), recipes.getAllRecipesFor(BoosterRecipeTypes.NETHERITE_MIXING.getType()));
        registration.addRecipes(conversion.getRecipeType(), recipes.getAllRecipesFor(BoosterRecipeTypes.CONVERSION.getType()));
        Component essenceInfo = Component.translatable("jei.createnetherite.essence_info");
        registration.addIngredientInfo(new ItemStack(BoosterFluids.ESSENCE_BUCKET.get()), VanillaTypes.ITEM_STACK, essenceInfo);
        registration.addIngredientInfo(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1000), NeoForgeTypes.FLUID_STACK, essenceInfo);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        mixing.registerCatalysts(registration);
        conversion.registerCatalysts(registration);
        nativeCatalysts(registration, BoosterItems.NETHERITE_MECHANICAL_PRESS.get(), "pressing", "packing", "automatic_packing");
        nativeCatalysts(registration, BoosterItems.NETHERITE_MECHANICAL_MIXER.get(), "mixing", "automatic_shapeless", "automatic_brewing");
        nativeCatalysts(registration, BoosterItems.NETHERITE_MECHANICAL_SAW.get(), "sawing", "block_cutting");
        nativeCatalysts(registration, BoosterItems.NETHERITE_MECHANICAL_CRAFTER.get(), "mechanical_crafting", "automatic_shaped");
        nativeCatalysts(registration, BoosterItems.NETHERITE_DEPLOYER.get(), "deploying");
        nativeCatalysts(registration, BoosterItems.NETHERITE_ENCASED_FAN.get(), "fan_washing", "fan_blasting", "fan_haunting", "fan_smoking");
    }

    private static void nativeCatalysts(IRecipeCatalystRegistration registration, ItemLike machine, String... paths) {
        for (String path : paths) {
            registration.addRecipeCatalyst(machine, RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("create", path)));
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Counts 1..4 share acquisition recipes; the empty shell is a separate recipe ingredient.
        registration.registerSubtypeInterpreter(BoosterItems.NETHERITE_BLAZE_BURNER.get(), new ISubtypeInterpreter<ItemStack>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                return NetheriteBlazeBurnerData.getCount(stack) == 0 ? "empty" : "filled";
            }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return getSubtypeData(stack, context).toString();
            }
        });
    }
}
