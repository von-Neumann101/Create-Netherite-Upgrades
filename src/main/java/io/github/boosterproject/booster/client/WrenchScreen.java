package io.github.boosterproject.booster.client;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class WrenchScreen extends AbstractContainerScreen<WrenchMenu> {
    public static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "textures/gui/netherite_netherite_wrench.png");
    private final Button[] toggles = new Button[8];
    public WrenchScreen(WrenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 288; imageHeight = 286;
        titleLabelX = 12; titleLabelY = 6; inventoryLabelX = 63; inventoryLabelY = 182;
    }
    public static Component text(String key, Object... args) { return Component.translatable("wrench.createnetherite." + key, args); }
    @Override protected void init() {
        super.init();
        for (int i = 0; i < 8; i++) {
            int slot = i; var pos = WrenchMenu.POSITIONS[i];
            toggles[i] = addRenderableWidget(Button.builder(text("activate"), button -> {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, slot);
            }).bounds(leftPos + pos[0] - 6, topPos + pos[1] + 18, 28, 12).build());
        }
    }
    @Override protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 8; i++) {
            toggles[i].active = menu.getSlot(i).hasItem();
            toggles[i].setMessage(text(menu.activeSlot() == i ? "deactivate" : "activate"));
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int active = menu.activeSlot();
        if (active >= 0) {
            var pos = WrenchMenu.POSITIONS[active];
            graphics.renderOutline(leftPos + pos[0] - 2, topPos + pos[1] - 2, 20, 20, 0xFFFFC76A);
            graphics.renderItem(menu.getSlot(active).getItem(), leftPos + 136, topPos + 80);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE1E3E8, false);
        graphics.drawCenteredString(font, text("loaded", menu.loadedCount()), 144, 172, 0xFFD5C6AA);
        graphics.drawCenteredString(font, text("active", menu.activeSlot() < 0 ? 0 : 1), 144, 105, 0xFFE1E3E8);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE1E3E8, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
    }
}
