package io.github.boosterproject.booster.client;

import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import io.github.boosterproject.booster.content.kinetics.fan.NetheriteFanMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class NetheriteFanScreen extends AbstractContainerScreen<NetheriteFanMenu> {
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
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF17181B);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF34363C);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 20, 0xFF45484F);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF111216);
            graphics.fill(x, y, x + 17, y + 17, 0xFF73767F);
            graphics.fill(x, y, x + 16, y + 16, 0xFF22242A);
        }
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
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                selected ? color : isHoveredOrFocused() ? 0xFFB9BDC6 : 0xFF111216);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                selected ? 0xFF454952 : 0xFF26292F);
            graphics.renderItem(mode.icon(), getX() + (width - 16) / 2, getY() + 3);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(mode.displayName().getString(), width - 4),
                getX() + width / 2, getY() + 24, selected ? color : 0xFFE1E3E8);
        }
    }
}
