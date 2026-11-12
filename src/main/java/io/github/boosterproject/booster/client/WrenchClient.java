package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchLaunch;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchModule;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchNetworking;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchWheelNetworking;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class WrenchClient {
    private static final KeyMapping WHEEL = new KeyMapping("key.createnetherite.wrench", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.createnetherite");
    private static boolean shiftWasDown;
    private WrenchClient() {}
    static KeyMapping wheelKey() { return WHEEL; }
    public static void register(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent event) -> event.register(WHEEL));
        NeoForge.EVENT_BUS.addListener(WrenchClient::tick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            if (event.getPlayer() != null) WrenchLaunch.cancel(event.getPlayer());
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.Clone event) -> WrenchLaunch.cancel(event.getOldPlayer()));
        WrenchEjectorPreview.register();
    }
    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        boolean shift = Screen.hasShiftDown(), pressed = shift && !shiftWasDown;
        shiftWasDown = shift;
        if (mc.player == null || mc.level == null || mc.screen != null || mc.isPaused()) {
            while (WHEEL.consumeClick()) {} return;
        }
        var held = mc.player.getMainHandItem();
        boolean wheelPressed = false;
        while (WHEEL.consumeClick()) wheelPressed = true;
        if (wheelPressed && WHEEL.isDown() && WrenchData.isWrench(held) && mc.player.isAlive() && !mc.player.isSpectator()) {
            var session = UUID.randomUUID();
            var wheel = new WrenchRadialScreen(session, mc.player.getInventory().selected, held);
            mc.player.stopUsingItem();
            mc.setScreen(wheel);
            if (mc.screen == wheel) PacketDistributor.sendToServer(new WrenchWheelNetworking.Action(session, WrenchWheelNetworking.OPEN, 0));
            return;
        }
        var module = WrenchData.active(held);
        if (pressed && WrenchData.has(held, WrenchModule.FAN)) PacketDistributor.sendToServer(new WrenchNetworking.Action(WrenchNetworking.TOGGLE_FAN, 0));
        if (pressed && module == WrenchModule.TANK) mc.setScreen(new WrenchFluidScreen(held));
    }
}
