package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.logistics.chute.NetheriteSmartChuteMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public class NetheriteSmartChuteScreen extends AbstractContainerScreen<NetheriteSmartChuteMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
        Booster.MOD_ID, "textures/gui/netherite_smart_chute.png");

    private Slot draggedPlayerSlot;
    private int sampleClickButton = -1;

    public NetheriteSmartChuteScreen(NetheriteSmartChuteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 230;
        imageHeight = 206;
        inventoryLabelX = NetheriteSmartChuteMenu.PLAYER_X;
        inventoryLabelY = NetheriteSmartChuteMenu.PLAYER_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        for (int mode = 0; mode < 2; mode++) {
            final int chosen = mode;
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(label(mode == 0 ? "up" : "down"), button -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, chosen);
            }).bounds(leftPos + (mode == 0 ? 106 : 160), topPos + 79, mode == 0 ? 50 : 58, 20).build());
        }
    }

    private static Component label(String name) {
        return Component.translatable("gui.createnetherite.smart_chute." + name);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE1E3E8, false);
        graphics.drawCenteredString(font, label("up").copy().append(" [1]"), 56, 26, 0xFF75D4DA);
        graphics.drawCenteredString(font, label("down").copy().append(" [2]"), 174, 26, 0xFFE8AA61);
        graphics.drawString(font, label("hint"), 12, 66, 0xFFBFC3CC, false);
        graphics.drawString(font, label("fallback"), 12, 84, 0xFFE1E3E8, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE1E3E8, false);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int mode = 0;
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.Button button) button.active = mode++ != menu.getDefaultMode();
        }
    }

    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (hoveredSlot != null) {
            for (int group = 0; group < 2; group++) {
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
        return slot != null && slot.index >= NetheriteSmartChuteMenu.PLAYER_SLOT_COUNT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
