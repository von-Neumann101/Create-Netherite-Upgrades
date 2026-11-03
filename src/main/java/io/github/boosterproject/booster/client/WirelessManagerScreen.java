package io.github.boosterproject.booster.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.redstone.wireless.WirelessActionPayload;
import io.github.boosterproject.booster.content.redstone.wireless.WirelessManagerMenu;
import io.github.boosterproject.booster.content.redstone.wireless.WirelessPagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class WirelessManagerScreen extends AbstractContainerScreen<WirelessManagerMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
        Booster.MOD_ID, "textures/gui/netherite_wireless_manager.png");

    private final List<Button> rows = new ArrayList<>();
    private final List<Button> edits = new ArrayList<>();
    private EditBox name;
    private Button sharing, save, previous, next, activate;
    private UUID editing;
    private boolean shared;

    public WirelessManagerScreen(WirelessManagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 316;
        imageHeight = 236;
    }

    private static Component text(String key) { return Component.translatable("gui.createnetherite.wireless." + key); }

    @Override
    protected void init() {
        super.init();
        rows.clear(); edits.clear();
        for (int i = 0; i < WirelessPagePayload.PAGE_SIZE; i++) {
            final int index = i;
            rows.add(addRenderableWidget(new TerminalRow(leftPos + 8, topPos + 32 + i * 23, index)));
            edits.add(addRenderableWidget(Button.builder(text("edit"), button -> edit(index))
                .bounds(leftPos + 262, topPos + 32 + i * 23, 46, 22).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> page(-1)).bounds(leftPos + 8, topPos + 152, 24, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> page(1)).bounds(leftPos + 36, topPos + 152, 24, 20).build());
        addRenderableWidget(Button.builder(text("select_page"), button -> send(WirelessActionPayload.SELECT_PAGE, WirelessActionPayload.NO_TARGET))
            .bounds(leftPos + 132, topPos + 152, 84, 20).build());
        addRenderableWidget(Button.builder(text("clear"), button -> send(WirelessActionPayload.CLEAR, WirelessActionPayload.NO_TARGET))
            .bounds(leftPos + 220, topPos + 152, 88, 20).build());
        String previousName = name == null ? "" : name.getValue();
        name = addRenderableWidget(new EditBox(font, leftPos + 8, topPos + 180, 158, 18, text("name")));
        name.setMaxLength(48); name.setHint(text("name")); name.setValue(previousName);
        sharing = addRenderableWidget(Button.builder(text("private"), button -> { shared = !shared; updateWidgets(); })
            .bounds(leftPos + 172, topPos + 179, 76, 20).build());
        save = addRenderableWidget(Button.builder(text("save"), button -> {
            if (editing != null) PacketDistributor.sendToServer(new WirelessActionPayload(menu.containerId, WirelessActionPayload.SAVE,
                editing, name.getValue(), shared, menu.snapshot().page()));
        }).bounds(leftPos + 254, topPos + 179, 54, 20).build());
        activate = addRenderableWidget(Button.builder(text("activate"), button -> send(WirelessActionPayload.ACTIVATE, WirelessActionPayload.NO_TARGET))
            .bounds(leftPos + 8, topPos + 207, 140, 20).build());
        updateWidgets();
    }

    private WirelessPagePayload.Entry entry(int index) {
        var entries = menu.snapshot().entries();
        return index < entries.size() ? entries.get(index) : null;
    }

    private void edit(int index) {
        var entry = entry(index);
        if (entry == null || !entry.owned()) return;
        editing = entry.id(); shared = entry.shared(); name.setValue(entry.name());
        setFocused(name); name.setFocused(true);
        updateWidgets();
    }

    private void page(int delta) {
        editing = null;
        name.setValue("");
        PacketDistributor.sendToServer(new WirelessActionPayload(menu.containerId, WirelessActionPayload.PAGE,
            WirelessActionPayload.NO_TARGET, "", false, menu.snapshot().page() + delta));
    }

    private void send(int action, UUID id) {
        PacketDistributor.sendToServer(new WirelessActionPayload(menu.containerId, action, id, "", false, menu.snapshot().page()));
    }

    @Override protected void containerTick() { super.containerTick(); updateWidgets(); }

    private void updateWidgets() {
        for (int i = 0; i < rows.size(); i++) {
            var entry = entry(i);
            rows.get(i).visible = entry != null;
            edits.get(i).visible = entry != null;
            edits.get(i).active = entry != null && entry.owned();
        }
        boolean canEdit = editing != null && menu.snapshot().entries().stream().anyMatch(e -> e.id().equals(editing) && e.owned());
        name.active = canEdit; sharing.active = canEdit; save.active = canEdit;
        sharing.setMessage(text(shared ? "public" : "private"));
        previous.active = menu.snapshot().page() > 0;
        next.active = menu.snapshot().page() + 1 < menu.snapshot().pages();
        activate.active = menu.snapshot().selectedCount() > 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFE1E3E8, false);
        graphics.drawString(font, text("visible"), 8, 21, 0xFFBFC3CC, false);
        graphics.drawString(font, (menu.snapshot().page() + 1) + " / " + menu.snapshot().pages(), 68, 158, 0xFFE1E3E8, false);
        graphics.drawString(font, Component.translatable("gui.createnetherite.wireless.selected", menu.snapshot().selectedCount()), 158, 213, 0xFFE1E3E8, false);
        if (menu.snapshot().entries().isEmpty()) graphics.drawString(font, text("empty"), 14, 64, 0xFFBFC3CC, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key != 256 && name.isFocused()) {
            name.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private class TerminalRow extends Button {
        private final int index;

        TerminalRow(int x, int y, int index) {
            super(x, y, 250, 22, Component.empty(), button -> {
                var entry = entry(index);
                if (entry != null) send(WirelessActionPayload.SELECT, entry.id());
            }, DEFAULT_NARRATION);
            this.index = index;
        }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setMessage(Component.empty());
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            var entry = entry(index);
            if (entry == null) return;
            String title = (entry.selected() ? "[x] " : "[ ] ") + entry.displayName().getString();
            String position = entry.dimension() + "  " + entry.pos().getX() + ", " + entry.pos().getY() + ", " + entry.pos().getZ();
            graphics.drawString(font, font.plainSubstrByWidth(title, width - 8), getX() + 4, getY() + 2, 0xFFFFFF, false);
            graphics.drawString(font, font.plainSubstrByWidth(position, width - 8), getX() + 4, getY() + 12, 0xBFC3CC, false);
            setTooltip(Tooltip.create(entry.displayName().copy().append("\n" + position + "\n").append(text(entry.shared() ? "public" : "private"))));
            setMessage(Component.literal(title));
        }
    }
}
