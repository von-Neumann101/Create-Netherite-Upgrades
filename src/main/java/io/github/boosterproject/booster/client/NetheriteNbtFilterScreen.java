package io.github.boosterproject.booster.client;

import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteNbtFilterData;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteNbtFilterMenu;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteNbtFilterNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NetheriteNbtFilterScreen extends AbstractContainerScreen<NetheriteNbtFilterMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "textures/gui/netherite_nbt_filter.png");
    private ItemStack reference = ItemStack.EMPTY;
    private List<NetheriteNbtFilterData.Field> fields = List.of();
    private int offset, selected = -1, operator, sampleButton = -1;
    private Slot draggedSource;
    private EditBox first, second;
    private Button comparison, apply, sameItem, mode;

    public NetheriteNbtFilterScreen(NetheriteNbtFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 316; imageHeight = 236;
        inventoryLabelX = 77; inventoryLabelY = 141;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.booster.nbt_filter." + key, args); }
    @Override protected void init() {
        String previousFirst = first == null ? null : first.getValue();
        String previousSecond = second == null ? null : second.getValue();
        super.init();
        sameItem = addRenderableWidget(Button.builder(text("same_item_on"), button -> send(3)).bounds(leftPos + 80, topPos + 26, 106, 18).build());
        mode = addRenderableWidget(Button.builder(text("whitelist"), button -> send(2)).bounds(leftPos + 198, topPos + 26, 106, 18).build());
        comparison = addRenderableWidget(Button.builder(text("op0"), button -> {
            if (selected >= 0 && fields.get(selected).numeric()) operator = (operator + 1) % 4;
            updateEditor();
        }).bounds(leftPos + 12, topPos + 119, 68, 18).build());
        first = new EditBox(font, leftPos + 86, topPos + 119, 100, 18, text("value")); first.setMaxLength(32767); addRenderableWidget(first);
        second = new EditBox(font, leftPos + 190, topPos + 119, 64, 18, text("upper")); second.setMaxLength(32767); addRenderableWidget(second);
        apply = addRenderableWidget(Button.builder(text("apply"), button -> send(1)).bounds(leftPos + 260, topPos + 119, 44, 18).build());
        refresh();
        if (selected >= 0 && previousFirst != null) { first.setValue(previousFirst); second.setValue(previousSecond); }
        updateEditor();
    }
    private void refresh() {
        ItemStack current = menu.ghostInventory.getStackInSlot(0);
        if (!ItemStack.isSameItemSameComponents(reference, current)) {
            reference = current.copy(); fields = menu.fields(); offset = 0; selected = -1;
            first.setValue(""); second.setValue(""); updateEditor();
        }
        sameItem.setMessage(text(NetheriteNbtFilterData.sameItem(menu.currentFilter()) ? "same_item_on" : "same_item_off"));
        mode.setMessage(text(NetheriteNbtFilterData.blacklist(menu.currentFilter()) ? "blacklist" : "whitelist"));
    }
    private void select(int index) {
        selected = index; var field = fields.get(index); var condition = NetheriteNbtFilterData.condition(menu.currentFilter(), field);
        operator = condition == null ? 0 : condition.operator();
        first.setValue(condition == null ? field.text() : condition.first());
        second.setValue(condition == null ? "" : condition.second()); updateEditor();
    }
    private void updateEditor() {
        if (first == null) return;
        boolean active = selected >= 0 && selected < fields.size();
        comparison.active = active && fields.get(selected).numeric();
        first.active = active; second.active = active && operator == 3; apply.active = active;
        comparison.setMessage(text("op" + operator));
    }
    private void send(int action) {
        PacketDistributor.sendToServer(new NetheriteNbtFilterNetworking.Action(menu.containerId, menu.referenceHash(), action,
            selected, operator, first.getValue(), second.getValue()));
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        for (int row = 0; row < 5 && offset + row < fields.size(); row++) {
            int index = offset + row, y = topPos + 54 + row * 12; var field = fields.get(index);
            boolean enabled = NetheriteNbtFilterData.condition(menu.currentFilter(), field) != null;
            if (index == selected) graphics.fill(leftPos + 14, y, leftPos + 297, y + 12, 0x66524A39);
            graphics.drawString(font, enabled ? "[x]" : "[ ]", leftPos + 15, y + 2, enabled ? 0xFFFFC76A : 0xFFBFC3CC, false);
            String name = field.path().getLast();
            if (field.path().size() > 1) name += " / " + field.path().getFirst();
            graphics.drawString(font, font.plainSubstrByWidth(name, 133), leftPos + 33, y + 2, 0xFFE1E3E8, false);
            var condition = NetheriteNbtFilterData.condition(menu.currentFilter(), field);
            String value = condition == null ? field.text() : condition.first() + (condition.operator() == 3 ? ".." + condition.second() : "");
            graphics.drawString(font, font.plainSubstrByWidth(value, 123), leftPos + 172, y + 2, 0xFFD5C6AA, false);
        }
        if (fields.isEmpty()) graphics.drawString(font, text("reference_hint"), leftPos + 18, topPos + 70, 0xFFBFC3CC, false);
        if (fields.size() > 5) {
            int thumb = Math.max(6, 60 * 5 / fields.size()); int y = topPos + 54 + (60 - thumb) * offset / (fields.size() - 5);
            graphics.fill(leftPos + 299, topPos + 54, leftPos + 303, topPos + 114, 0xFF171719);
            graphics.fill(leftPos + 299, y, leftPos + 303, y + thumb, 0xFFB5A27C);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFE1E3E8, false);
        graphics.drawString(font, text("reference"), 12, 20, 0xFFBFC3CC, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE1E3E8, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refresh(); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
        int row = fieldAt(mouseX, mouseY);
        if (row >= 0) {
            var field = fields.get(row);
            String preview = field.text(); if (preview.length() > 256) preview = preview.substring(0, 256) + "...";
            var tooltip = new java.util.ArrayList<Component>(); tooltip.add(Component.literal(field.label()));
            for (int start = 0; start < preview.length();) {
                String line = font.plainSubstrByWidth(preview.substring(start), 280);
                if (line.isEmpty()) break;
                tooltip.add(Component.literal(line)); start += line.length();
            }
            tooltip.add(text("field_hint")); graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }
    private int fieldAt(double mouseX, double mouseY) {
        if (mouseX < leftPos + 12 || mouseX >= leftPos + 298 || mouseY < topPos + 54 || mouseY >= topPos + 114) return -1;
        int row = offset + (int) (mouseY - topPos - 54) / 12; return row < fields.size() ? row : -1;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= leftPos + 12 && x < leftPos + 304 && y >= topPos + 52 && y < topPos + 116) {
            offset = Math.clamp(offset - (int) Math.signum(vertical), 0, Math.max(0, fields.size() - 5)); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (first.isFocused() || second.isFocused()) return false;
        if (hoveredSlot != null && minecraft.options.keyHotbarSlots[0].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            slotClicked(hoveredSlot, hoveredSlot.index, 0, ClickType.SWAP); return true;
        }
        return false;
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((first.isFocused() || second.isFocused()) && keyCode != 256) {
            if (keyCode == 257 || keyCode == 335) { if (apply.active) send(1); return true; }
            return first.isFocused() ? first.keyPressed(keyCode, scanCode, modifiers) : second.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        draggedSource = null; sampleButton = -1;
        int row = fieldAt(x, y);
        if (row >= 0 && button == 0) { select(row); if (x < leftPos + 32) send(0); return true; }
        Slot slot = slotAt(x, y);
        if (slot != null && slot.index == NetheriteNbtFilterMenu.REFERENCE_SLOT && (button == 0 || button == 1)) {
            stopDrag(); slotClicked(slot, slot.index, button, hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP); sampleButton = button; return true;
        }
        boolean take = button == 0 && !hasShiftDown() && slot != null && slot.hasItem()
            && slot.mayPickup(minecraft.player) && menu.getCarried().isEmpty();
        boolean handled = super.mouseClicked(x, y, button);
        if (take && !menu.getCarried().isEmpty()) draggedSource = slot;
        return handled;
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        Slot source = draggedSource; draggedSource = null;
        if (sampleButton == button) { sampleButton = -1; stopDrag(); return true; }
        Slot target = slotAt(x, y);
        if (target != null && target.index == NetheriteNbtFilterMenu.REFERENCE_SLOT && (button == 0 || button == 1)) {
            stopDrag(); slotClicked(target, target.index, button, ClickType.PICKUP);
            if (button == 0 && source != null && !menu.getCarried().isEmpty()) slotClicked(source, source.index, 0, ClickType.PICKUP);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }
    private Slot slotAt(double x, double y) { return menu.slots.stream().filter(slot -> slot.isActive() && isHovering(slot.x, slot.y, 16, 16, x, y)).findFirst().orElse(null); }
    private void stopDrag() { isQuickCrafting = false; quickCraftSlots.clear(); clearDraggingState(); }
}
