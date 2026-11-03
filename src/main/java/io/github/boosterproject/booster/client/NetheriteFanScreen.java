package io.github.boosterproject.booster.client;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import io.github.boosterproject.booster.content.kinetics.fan.NetheriteFanMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NetheriteFanScreen extends AbstractContainerScreen<NetheriteFanMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
        Booster.MOD_ID, "textures/gui/netherite_fan.png");
    private static final ResourceLocation MODE_BUTTONS = ResourceLocation.fromNamespaceAndPath(
        Booster.MOD_ID, "textures/gui/netherite_fan_buttons.png");

    public NetheriteFanScreen(NetheriteFanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 288;
        imageHeight = 224;
        inventoryLabelX = NetheriteFanMenu.PLAYER_X;
        inventoryLabelY = NetheriteFanMenu.PLAYER_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        for (FanProcessingMode mode : FanProcessingMode.values()) {
            addRenderableWidget(new ModeButton(leftPos + 14 + mode.ordinal() * 66, topPos + 28, mode));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE1E3E8, false);
        graphics.drawString(font, Component.translatable("gui.booster.netherite_fan.filter"), 48, 84, 0xFFE1E3E8, false);
        graphics.drawString(font, Component.translatable("gui.booster.netherite_fan.fallback"), 12, 108, 0xFFBFC3CC, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE1E3E8, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private class ModeButton extends Button {
        private final FanProcessingMode mode;

        ModeButton(int x, int y, FanProcessingMode mode) {
            super(x, y, 62, 38, mode.displayName(), button -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode.ordinal());
                }
            }, DEFAULT_NARRATION);
            this.mode = mode;
            setTooltip(Tooltip.create(mode.displayName()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = 0xFF000000 | mode.color();
            boolean selected = menu.getProcessingMode() == mode;
            int state = selected ? 2 : isHoveredOrFocused() ? 1 : 0;
            graphics.blit(MODE_BUTTONS, getX(), getY(), mode.ordinal() * width, state * height,
                width, height, 248, 114);
            graphics.renderItem(mode.icon(), getX() + (width - 16) / 2, getY() + 3);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(mode.displayName().getString(), width - 4),
                getX() + width / 2, getY() + 24, selected ? color : 0xFFE1E3E8);
        }
    }
}
