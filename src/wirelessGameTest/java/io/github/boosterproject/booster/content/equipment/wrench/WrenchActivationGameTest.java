package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchActivationGameTest {
    @GameTest(template = "wireless_empty")
    public static void wheelSessionRejectsReplacedSourceAndStaleRequests(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "WheelQA"));
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(wrench, List.of(new ItemStack(WrenchModule.FAN.item()), new ItemStack(WrenchModule.TANK.item())));
        player.getInventory().selected = 0; player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var nonce = UUID.randomUUID();
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.TOGGLE, 0));
        helper.assertTrue(WrenchData.activeSlots(wrench).isEmpty(), "toggle requires OPEN acknowledgement");
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.OPEN, 0));
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(UUID.randomUUID(), WrenchWheelNetworking.TOGGLE, 0));
        helper.assertTrue(WrenchData.activeSlots(wrench).isEmpty(), "stale nonce cannot mutate active session");
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.TOGGLE, 0));
        helper.assertTrue(WrenchData.activeSlots(wrench).equals(List.of(0)), "matching session toggles actual source");
        var copy = wrench.copy(); player.setItemInHand(InteractionHand.MAIN_HAND, copy);
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.TOGGLE, 1));
        helper.assertTrue(WrenchData.activeSlots(copy).equals(List.of(0)) && WrenchData.activeSlots(wrench).equals(List.of(0)), "identical replacement is never mutated by old wheel");
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.OPEN, 0));
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.CLOSE, 0));
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.TOGGLE, 1));
        helper.assertTrue(WrenchData.activeSlots(copy).equals(List.of(0)), "release closes without hover activation and rejects later clicks");
        WrenchNetworking.handle(player, new WrenchNetworking.Action(WrenchNetworking.OPEN, 0));
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "legacy packet cannot open handheld installation UI");
        nonce = UUID.randomUUID();
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.OPEN, 0));
        player.getInventory().selected = 1;
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.TOGGLE, 1));
        player.getInventory().selected = 0;
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.TOGGLE, 1));
        helper.assertTrue(WrenchData.activeSlots(copy).equals(List.of(0)), "switching hotbar invalidates the session even after returning");
        nonce = UUID.randomUUID();
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.OPEN, 0));
        player.containerMenu = net.minecraft.world.inventory.ChestMenu.threeRows(8, player.getInventory());
        WrenchWheelNetworking.handle(player, new WrenchWheelNetworking.Action(nonce, WrenchWheelNetworking.TOGGLE, 1));
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(WrenchData.activeSlots(copy).equals(List.of(0)), "container UI cannot toggle held wrench through an old wheel");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void wheelSnapshotCodecRetainsModulesAndOrder(GameTestHelper helper) {
        var module = new ItemStack(WrenchModule.TANK.item());
        module.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Stored module"));
        var modules = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(8, ItemStack.EMPTY));
        modules.set(0, module); modules.set(7, new ItemStack(WrenchModule.PUMP.item()));
        var nonce = UUID.randomUUID();
        var state = new WrenchWheelNetworking.State(nonce, 3, modules, List.of(7, 0), 2, 0);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            WrenchWheelNetworking.State.CODEC.encode(buffer, state);
            var decoded = WrenchWheelNetworking.State.CODEC.decode(buffer);
            helper.assertTrue(decoded.session().equals(nonce) && decoded.hotbar() == 3 && decoded.activationLimit() == 2
                && decoded.activeSlots().equals(List.of(7, 0)) && decoded.rightClickSlot() == 0
                && ItemStack.matches(module, decoded.modules().getFirst()) && decoded.modules().get(1).isEmpty(),
                "authoritative eight-slot snapshot preserves components, empties, order and driver");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void passiveModulesCoexistAndDuplicateEffectsDoNotStack(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DualPassiveQA"));
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(wrench, List.of(new ItemStack(WrenchModule.PUMP.item()), new ItemStack(WrenchModule.PRESS.item()), new ItemStack(WrenchModule.PUMP.item())));
        WrenchData.unlockSecondSlot(wrench); WrenchData.toggleSlot(wrench, 0); WrenchData.toggleSlot(wrench, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        double[] damage = {0};
        wrench.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) damage[0] += modifier.amount();
        });
        helper.assertTrue(damage[0] == 15, "pump damage persists while press is newer active module");
        var cow = EntityType.COW.create(helper.getLevel()); cow.setNoAi(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
        cow.hurt(player.damageSources().playerAttack(player), 4);
        helper.assertTrue(cow.getHealth() == 94 && cow.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "press damage and root apply alongside pump");
        WrenchData.toggleSlot(wrench, 1); WrenchData.toggleSlot(wrench, 2);
        damage[0] = 0;
        wrench.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) damage[0] += modifier.amount();
        });
        helper.assertTrue(damage[0] == 15, "two active pumps add the passive attribute only once");
        helper.succeed();
    }
    @GameTest(template = "wireless_empty")
    public static void activationUsesToggleOrderAndRightClickPriority(GameTestHelper helper) {
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(wrench, List.of(new ItemStack(WrenchModule.FAN.item()),
            new ItemStack(WrenchModule.PUMP.item()), new ItemStack(WrenchModule.TANK.item())));
        helper.assertTrue(WrenchData.activationLimit(wrench) == 1, "one active slot initially");
        WrenchData.toggleSlot(wrench, 0); WrenchData.toggleSlot(wrench, 1);
        helper.assertTrue(WrenchData.activeSlots(wrench).equals(List.of(1)), "single-slot activation replaces previous");
        WrenchData.toggleSlot(wrench, 1);
        helper.assertTrue(WrenchData.activeSlots(wrench).isEmpty(), "click active disables it");
        helper.assertTrue(WrenchData.unlockSecondSlot(wrench) && !WrenchData.unlockSecondSlot(wrench), "upgrade occurs only once");
        WrenchData.toggleSlot(wrench, 0); WrenchData.toggleSlot(wrench, 1);
        helper.assertTrue(WrenchData.activeSlots(wrench).equals(List.of(0, 1)) && WrenchData.active(wrench) == WrenchModule.FAN
            && WrenchData.has(wrench, WrenchModule.PUMP), "passive pump coexists without stealing fan right click");
        WrenchData.toggleSlot(wrench, 2);
        helper.assertTrue(WrenchData.activeSlots(wrench).equals(List.of(1, 2)) && WrenchData.active(wrench) == WrenchModule.TANK,
            "third activation evicts oldest, latest right click wins");
        WrenchData.toggleSlot(wrench, 1); WrenchData.toggleSlot(wrench, 1);
        helper.assertTrue(WrenchData.activeSlots(wrench).equals(List.of(2, 1)) && WrenchData.rightClickSlot(wrench) == 2,
            "reactivation becomes newest but passive never overrides right click");
        helper.assertTrue(!WrenchData.toggleSlot(wrench, 7) && !WrenchData.toggleSlot(wrench, -1), "empty or invalid regions do nothing");
        var saved = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) wrench.save(helper.getLevel().registryAccess()));
        helper.assertTrue(WrenchData.activationLimit(saved) == 2 && WrenchData.activeSlots(saved).equals(List.of(2, 1)), "upgrade and activation order survive serialization");
        var modules = WrenchData.modules(saved); modules.set(2, ItemStack.EMPTY); WrenchData.setModules(saved, modules);
        helper.assertTrue(WrenchData.activeSlots(saved).equals(List.of(1)), "removing a module clears only its activation");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void legacyActivationAndUnrelatedComponentsSurvive(GameTestHelper helper) {
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(wrench, List.of(new ItemStack(WrenchModule.PUMP.item()), new ItemStack(WrenchModule.PUMP.item())));
        var root = new CompoundTag(); var legacy = new CompoundTag(); legacy.putInt("Active", 1); legacy.putBoolean("Pull", true);
        root.put("NetheriteWrench", legacy); root.putString("OtherMod", "keep");
        wrench.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        helper.assertTrue(WrenchData.activeSlots(wrench).equals(List.of(1)) && WrenchData.has(wrench, WrenchModule.PUMP), "old Active int remains usable");
        WrenchData.toggleSlot(wrench, 1);
        helper.assertTrue(WrenchData.activeSlots(wrench).isEmpty(), "new empty array does not fall back to legacy Active");
        WrenchData.unlockSecondSlot(wrench); WrenchData.toggleSlot(wrench, 0); WrenchData.toggleSlot(wrench, 1);
        helper.assertTrue(WrenchData.activeSlots(wrench).equals(List.of(0, 1)) && WrenchData.fanPull(wrench)
            && wrench.get(DataComponents.CUSTOM_DATA).copyTag().getString("OtherMod").equals("keep"), "duplicate stored modules and unrelated custom state preserved");
        helper.succeed();
    }
}
