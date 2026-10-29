package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteFilterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public class NetheriteFilterScreen extends AbstractContainerScreen<NetheriteFilterMenu> {
    private Slot draggedPlayerSlot;
    private int sampleClickButton = -1;

    public NetheriteFilterScreen(NetheriteFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 288;
        imageHeight = 236;
        inventoryLabelX = NetheriteFilterMenu.PLAYER_X;
        inventoryLabelY = NetheriteFilterMenu.PLAYER_Y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF17181B);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF34363C);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 20, 0xFF45484F);
        for (FanProcessingMode mode : FanProcessingMode.values()) {
            int x = leftPos + NetheriteFilterMenu.GROUP_X + mode.ordinal() * NetheriteFilterMenu.GROUP_SPACING;
            int color = 0xFF000000 | mode.color();
            graphics.fill(x - 4, topPos + 22, x + 58, topPos + 112, 0xFF25272C);
            graphics.fill(x - 4, topPos + 22, x + 58, topPos + 24, color);
            graphics.renderItem(mode.icon(), x + 8, topPos + 25);
            graphics.drawString(font, Integer.toString(mode.ordinal() + 1), x + 35, topPos + 29, 0xFFE1E3E8, false);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(mode.displayName().getString(), 60), x + 27, topPos + 43, color);
        }
        for (Slot slot : menu.slots) {
            drawSlot(graphics, leftPos + slot.x, topPos + slot.y);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF111216);
        graphics.fill(x, y, x + 17, y + 17, 0xFF73767F);
        graphics.fill(x, y, x + 16, y + 16, 0xFF22242A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE1E3E8, false);
        graphics.drawString(font, Component.translatable("gui.booster.netherite_filter.mouse_hint"), 12, 115, 0xFFBFC3CC, false);
        graphics.drawString(font, Component.translatable("gui.booster.netherite_filter.key_hint"), 12, 127, 0xFFBFC3CC, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE1E3E8, false);
    }

    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (hoveredSlot != null) {
            for (int group = 0; group < 4; group++) {
                if (minecraft.options.keyHotbarSlots[group].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                    slotClicked(hoveredSlot, hoveredSlot.index, group, ClickType.SWAP);
                    return true;
                }
            }
        }
        // Other hotbar keys and offhand swapping have no meaning in this rule editor.
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggedPlayerSlot = null;
        sampleClickButton = -1;
        Slot slot = slotAt(mouseX, mouseY);
        boolean touchscreen = minecraft.options.touchscreen().get();
        if (!touchscreen && isSampleSlot(slot) && (button == 0 || button == 1)) {
            stopSampleDrag();
            slotClicked(slot, slot.index, button, hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            sampleClickButton = button;
            return true;
        }

        boolean takeFromPlayer = !touchscreen && button == 0 && !hasShiftDown()
            && slot != null && !isSampleSlot(slot) && slot.hasItem() && slot.mayPickup(minecraft.player)
            && menu.getCarried().isEmpty()
            && !minecraft.options.keyPickItem.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(button));
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (takeFromPlayer && !menu.getCarried().isEmpty()) {
            draggedPlayerSlot = slot;
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Slot source = draggedPlayerSlot;
        draggedPlayerSlot = null;
        if (button == sampleClickButton) {
            sampleClickButton = -1;
            stopSampleDrag();
            return true;
        }

        Slot target = slotAt(mouseX, mouseY);
        if (!minecraft.options.touchscreen().get() && isSampleSlot(target) && (button == 0 || button == 1)) {
            // Vanilla skips the release after picking up a player stack. Handle the rule drop
            // ourselves and clear any pending quick-craft before it can distribute real items.
            stopSampleDrag();
            slotClicked(target, target.index, button, hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            if (button == 0 && source != null && !menu.getCarried().isEmpty()) {
                // The sample click leaves the carried stack untouched; return a one-gesture
                // inventory drag to its original slot using the normal server-owned stack.
                slotClicked(source, source.index, 0, ClickType.PICKUP);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void stopSampleDrag() {
        isQuickCrafting = false;
        quickCraftSlots.clear();
        clearDraggingState();
    }

    private Slot slotAt(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isSampleSlot(Slot slot) {
        return slot != null && slot.index >= NetheriteFilterMenu.PLAYER_SLOT_COUNT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
