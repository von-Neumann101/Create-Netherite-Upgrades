package io.github.boosterproject.booster.compat.jei;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import io.github.boosterproject.booster.content.conversion.ConversionPlate;
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
import net.minecraft.world.item.Items;
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
            .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), NetheriteConverterBlockEntity.FLUID_PER_ITEM))
            .setFluidRenderer(NetheriteConverterBlockEntity.FLUID_PER_ITEM, false, 16, 16);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 31).setBackground(getRenderedSlot(), -1, -1)
            .addItemStack(recipe.getRollableResults().getFirst().getStack());
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 79, 10).addItemStack(new ItemStack(BoosterItems.NETHERITE_CONVERTER.get()));
        pointSource(builder, 42, new ItemStack(BoosterItems.NETHERITE_SHEET.get()));
        pointSource(builder, 108, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
    }

    private static void pointSource(IRecipeLayoutBuilder builder, int x, ItemStack stack) {
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, 100).setBackground(getRenderedSlot(), -1, -1)
            .addItemStack(stack).addRichTooltipCallback((view, tooltip) ->
                tooltip.add(Component.translatable("jei.createnetherite.point_source", ConversionPlate.remaining(stack))));
    }

    @Override
    public void draw(NetheriteConversionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 56, 34);
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, Component.translatable("jei.createnetherite.conversion_conditions",
            NetheriteConverterBlockEntity.PROCESSING_TICKS / 20, NetheriteConverterBlockEntity.REQUIRED_STRESS), 88, 63, 0x555555);
        graphics.drawCenteredString(font, Component.translatable("jei.createnetherite.value_cost", recipe.getNetheriteCost()), 88, 76, 0x555555);
        graphics.drawCenteredString(font, Component.translatable("jei.createnetherite.deposit_first"), 88, 89, 0x555555);
        graphics.drawString(font, "+16", 62, 105, 0x555555, false);
        graphics.drawString(font, "+1", 128, 105, 0x555555, false);
    }
}
