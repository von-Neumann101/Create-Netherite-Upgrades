package io.github.boosterproject.booster.compat.jei;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerData;
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

/** Shows the actual upgraded equipment instead of native mixing's ordinary basin and burner. */
public final class NetheriteMixingCategory extends CreateRecipeCategory<BasinRecipe> {
    public NetheriteMixingCategory(Info<BasinRecipe> info) { super(info); }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BasinRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.getIngredients().size(); i++)
            builder.addSlot(RecipeIngredientRole.INPUT, 15 + 19 * (i % 3), 18 + 19 * (i / 3))
                .setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredients().get(i));
        for (int i = 0; i < recipe.getFluidResults().size(); i++) {
            var fluid = recipe.getFluidResults().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 27 + 19 * i).setBackground(getRenderedSlot(), -1, -1)
                .addIngredient(NeoForgeTypes.FLUID_STACK, fluid).setFluidRenderer(fluid.getAmount(), false, 16, 16);
        }
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 24, 88).addItemStack(new ItemStack(BoosterItems.NETHERITE_MECHANICAL_MIXER.get()));
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 78, 88).addItemStack(new ItemStack(BoosterItems.NETHERITE_BASIN.get()));
        ItemStack burner = new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get());
        NetheriteBlazeBurnerData.setCount(burner, 4);
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 132, 88).addItemStack(burner)
            .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.createnetherite.essence_conditions")));
    }

    @Override
    public void draw(BasinRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 75, 28);
        graphics.drawCenteredString(Minecraft.getInstance().font,
            Component.translatable("jei.createnetherite.essence_conditions"), 88, 66, 0x555555);
    }
}
