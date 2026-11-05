package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchWheelNetworking;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** A held-key activation wheel; installation belongs exclusively to the assembly table. */
public final class WrenchRadialScreen extends Screen {
    private static final int RADIUS = 64;
    private final UUID session;
    private final int hotbar;
    private final Item wrenchType;
    private final InputConstants.Key openingKey;
    private WrenchWheelNetworking.State state;
    private boolean acknowledged, closed;

    public WrenchRadialScreen(UUID session, int hotbar, ItemStack source) {
        super(WrenchScreen.text("wheel_title"));
        this.session = session;
        this.hotbar = hotbar;
        wrenchType = source.getItem();
        openingKey = WrenchClient.wheelKey().getKey();
        state = new WrenchWheelNetworking.State(session, hotbar,
            WrenchData.modules(source).stream().map(ItemStack::copy).toList(),
            List.copyOf(WrenchData.activeSlots(source)), WrenchData.activationLimit(source), WrenchData.rightClickSlot(source));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() {
        if (!validSource() || !minecraft.isWindowActive() || !openingKeyHeld()) {
            onClose();
            return;
        }
        updateState();
    }

    private boolean validSource() {
        return minecraft.player != null && minecraft.level != null && minecraft.player.isAlive()
            && !minecraft.player.isSpectator() && minecraft.player.getInventory().selected == hotbar
            && minecraft.player.getMainHandItem().getItem() == wrenchType
            && WrenchData.isWrench(minecraft.player.getMainHandItem());
    }

    private boolean openingKeyHeld() {
        long window = minecraft.getWindow().getWindow();
        // setScreen releases all KeyMappings. Read only the opening input for the release fallback.
        return switch (openingKey.getType()) {
            case KEYSYM -> openingKey.getValue() != GLFW.GLFW_KEY_UNKNOWN && InputConstants.isKeyDown(window, openingKey.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, openingKey.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> true; // Its exact release is handled by keyReleased.
        };
    }

    private void updateState() {
        var received = WrenchWheelNetworking.clientState();
        if (received == null || !session.equals(received.session())) return;
        if (received.hotbar() != hotbar || received.activationLimit() == 0) {
            onClose();
            return;
        }
        state = received;
        acknowledged = true;
    }

    private int hoveredSlot(double mouseX, double mouseY) {
        double x = mouseX - width / 2.0, y = mouseY - height / 2.0;
        double distance = x * x + y * y;
        if (distance < 20 * 20 || distance > 88 * 88) return -1;
        double angle = Math.toDegrees(Math.atan2(x, -y));
        return (int) Math.floor((angle + 360 + 22.5) / 45) % WrenchData.SLOTS;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xA0202028, 0xC0101018);
        int centerX = width / 2, centerY = height / 2;
        int hovered = hoveredSlot(mouseX, mouseY);
        graphics.drawCenteredString(font, title, centerX, centerY - 110, 0xFFE1E3E8);
        graphics.drawCenteredString(font, WrenchScreen.text("wheel_active", state.activeSlots().size(), state.activationLimit()), centerX, centerY - 94, 0xFFD5C6AA);
        for (int slot = 0; slot < WrenchData.SLOTS; slot++) {
            double angle = Math.toRadians(slot * 45);
            int x = centerX + (int) Math.round(Math.sin(angle) * RADIUS) - 11;
            int y = centerY - (int) Math.round(Math.cos(angle) * RADIUS) - 11;
            var module = state.modules().get(slot);
            int order = state.activeSlots().indexOf(slot);
            (module.isEmpty() ? AllGuiTextures.TOOLBELT_EMPTY_SLOT
                : order >= 0 ? AllGuiTextures.TOOLBELT_SLOT : AllGuiTextures.TOOLBELT_INACTIVE_SLOT).render(graphics, x, y);
            if (!module.isEmpty()) graphics.renderItem(module, x + 3, y + 3);
            if (order >= 0) {
                AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, x - 1, y - 1);
                graphics.fill(x - 2, y - 3, x + 7, y + 7, 0xFF3F3529);
                graphics.drawString(font, Integer.toString(order + 1), x, y - 3, 0xFFFFC76A, false);
            }
            if (slot == hovered && !module.isEmpty()) graphics.renderOutline(x - 2, y - 2, 26, 26, 0xFFE1E3E8);
            if (slot == state.rightClickSlot()) {
                var badge = WrenchScreen.text("wheel_right_click");
                int halfWidth = font.width(badge) / 2;
                graphics.fill(x + 9 - halfWidth, y + 23, x + 13 + halfWidth, y + 33, 0xE0302920);
                graphics.drawCenteredString(font, badge, x + 11, y + 24, 0xFFFFC76A);
            }
        }
        graphics.drawCenteredString(font, WrenchScreen.text("wheel_hint", WrenchClient.wheelKey().getTranslatedKeyMessage()), centerX, height - 24, 0xFFE1E3E8);
        if (!acknowledged) graphics.drawCenteredString(font, WrenchScreen.text("wheel_waiting"), centerX, height - 12, 0xFFD5C6AA);
        if (hovered >= 0) {
            var module = state.modules().get(hovered);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(module.isEmpty() ? WrenchScreen.text("wheel_empty") : module.getHoverName());
            int order = state.activeSlots().indexOf(hovered);
            if (order >= 0) tooltip.add(WrenchScreen.text("wheel_order", order + 1));
            if (hovered == state.rightClickSlot()) tooltip.add(WrenchScreen.text("wheel_right_click"));
            if (!acknowledged) tooltip.add(WrenchScreen.text("wheel_waiting"));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (!validSource() || !openingKeyHeld()) {
                onClose();
                return true;
            }
            updateState();
            int slot = hoveredSlot(mouseX, mouseY);
            if (!closed && acknowledged && slot >= 0 && !state.modules().get(slot).isEmpty())
                PacketDistributor.sendToServer(new WrenchWheelNetworking.Action(session, WrenchWheelNetworking.TOGGLE, slot));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (openingKey.equals(InputConstants.getKey(keyCode, scanCode))) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (openingKey.equals(InputConstants.getKey(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (openingKey.getType() == InputConstants.Type.MOUSE && openingKey.getValue() == button) {
            onClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void onClose() {
        closeSession();
        super.onClose();
    }

    @Override public void removed() {
        closeSession();
        super.removed();
    }

    private void closeSession() {
        if (closed) return;
        closed = true;
        if (minecraft != null && minecraft.getConnection() != null)
            PacketDistributor.sendToServer(new WrenchWheelNetworking.Action(session, WrenchWheelNetworking.CLOSE, 0));
    }
}
