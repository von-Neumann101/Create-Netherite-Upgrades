package io.github.boosterproject.booster.compat.jei;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import io.github.boosterproject.booster.content.conversion.NetheriteConversionRecipe;
import io.github.boosterproject.booster.content.conversion.NetheriteConverterBlockEntity;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class NetheriteConversionCategory extends CreateRecipeCategory<NetheriteConversionRecipe> {
    public NetheriteConversionCategory(Info<NetheriteConversionRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, NetheriteConversionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 19).setBackground(getRenderedSlot(), -1, -1)
            .addIngredients(recipe.getIngredients().getFirst());
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 43).setBackground(getRenderedSlot(), -1, -1)
            .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), recipe.getEssenceAmount()))
            .setFluidRenderer(recipe.getEssenceAmount(), false, 16, 16);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 31).setBackground(getRenderedSlot(), -1, -1)
            .addItemStack(recipe.getRollableResults().getFirst().getStack());
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 79, 10).addItemStack(new ItemStack(BoosterItems.NETHERITE_CONVERTER.get()));
    }

    @Override
    public void draw(NetheriteConversionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 56, 34);
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, Component.translatable("jei.createnetherite.conversion_conditions",
            NetheriteConverterBlockEntity.PROCESSING_TICKS / 20, NetheriteConverterBlockEntity.REQUIRED_STRESS), 88, 63, 0x555555);
        graphics.drawCenteredString(font, Component.translatable(recipe.upgradesWrenchActivation()
            ? "jei.createnetherite.activation_upgrade" : "jei.createnetherite.fluid_cost", recipe.getEssenceAmount()), 88, 79, 0x555555);
    }
}
