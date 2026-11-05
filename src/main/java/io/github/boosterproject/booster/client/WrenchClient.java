package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchLaunch;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchModule;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.DustParticleOptions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public final class WrenchClient {
    private static final KeyMapping WHEEL = new KeyMapping("key.createnetherite.wrench", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.createnetherite");
    private static boolean shiftWasDown;
    private WrenchClient() {}
    public static void register(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent event) -> event.register(WHEEL));
        NeoForge.EVENT_BUS.addListener(WrenchClient::tick);
    }
    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        boolean shift = Screen.hasShiftDown(), pressed = shift && !shiftWasDown;
        shiftWasDown = shift;
        if (mc.player == null || mc.level == null || mc.screen != null || mc.isPaused()) {
            while (WHEEL.consumeClick()) {} return;
        }
        var held = mc.player.getMainHandItem();
        while (WHEEL.consumeClick()) if (WrenchData.isWrench(held)) PacketDistributor.sendToServer(new WrenchNetworking.Action(WrenchNetworking.OPEN, 0));
        var module = WrenchData.active(held);
        if (pressed && module == WrenchModule.FAN) PacketDistributor.sendToServer(new WrenchNetworking.Action(WrenchNetworking.TOGGLE_FAN, 0));
        if (pressed && module == WrenchModule.TANK) mc.setScreen(new WrenchFluidScreen(held));
        if (shift && module == WrenchModule.EJECTOR && mc.level.getGameTime() % 2 == 0) {
            var dust = new DustParticleOptions(new Vector3f(0.7F, 0.9F, 0.6F), 0.7F);
            for (var p : WrenchLaunch.preview(mc.player)) mc.level.addParticle(dust, p.x, p.y, p.z, 0, 0, 0);
        }
    }
}
