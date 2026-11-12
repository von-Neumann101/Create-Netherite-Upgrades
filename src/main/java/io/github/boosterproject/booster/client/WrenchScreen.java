package io.github.boosterproject.booster.client;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class WrenchScreen extends AbstractContainerScreen<WrenchMenu> {
    public static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "textures/gui/assembly_table.png");

    public WrenchScreen(WrenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 288; imageHeight = 286;
        titleLabelX = 12; titleLabelY = 6; inventoryLabelX = 63; inventoryLabelY = 182;
    }

    public static Component text(String key, Object... args) { return Component.translatable("wrench.createnetherite." + key, args); }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        if (menu.getSlot(1).hasItem())
            com.simibubi.create.foundation.gui.AllGuiTextures.JEI_SLOT.render(graphics, leftPos + 237, topPos + 79);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE1E3E8, false);
        graphics.drawCenteredString(font, text("loaded", menu.loadedCount()), 144, 172, 0xFFD5C6AA);
        graphics.drawCenteredString(font, menu.activationLimit() == 0 ? text("insert_wrench") : text("activation_limit", menu.activationLimit()),
            144, 105, 0xFFE1E3E8);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE1E3E8, false);
        if (menu.getSlot(1).hasItem()) graphics.drawCenteredString(font, text("recover_legacy"), 246, 100, 0xFFD5C6AA);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
