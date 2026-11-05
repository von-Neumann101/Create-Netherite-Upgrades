package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.Booster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchInventoryGameTest {
    @GameTest(template = "wireless_empty")
    public static void wrenchIsRegistered(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_netherite_wrench");
        helper.assertTrue(BuiltInRegistries.ITEM.get(id) != Items.AIR, "new modular wrench is registered");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void storedComponentsSurviveSaveAndSwordIsUnbreakable(GameTestHelper helper) {
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        var module = new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get());
        module.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved module"));
        WrenchData.setModules(wrench, java.util.List.of(module)); WrenchData.setActiveSlot(wrench, 0);
        WrenchData.setFanPull(wrench, true);
        var saved = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) wrench.save(helper.getLevel().registryAccess()));
        helper.assertTrue(ItemStack.matches(wrench, saved) && ItemStack.matches(module, WrenchData.modules(saved).getFirst()), "item serialization preserves module item components and active state");
        helper.assertTrue(WrenchData.active(saved) == WrenchModule.BURNER && WrenchData.fanPull(saved), "stack state persists without a global player cache");
        var copied = WrenchData.modules(saved); copied.getFirst().shrink(1);
        helper.assertTrue(WrenchData.modules(saved).getFirst().getCount() == 1, "callers cannot mutate saved module components by aliasing");
        var profile = new GameProfile(UUID.randomUUID(), "WrenchSwordQA");
        var server = helper.getLevel().getServer();
        var player = new net.minecraft.server.level.ServerPlayer(server, helper.getLevel(), profile,
            net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        player.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(1, 300, 1)).getCenter());
        player.setNoGravity(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, saved);
        player.doTick();
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 10) < 0.001
            && Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 1.6) < 0.001, "base sword damage ten and attack speed 1.6");
        saved.hurtAndBreak(10000, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(saved.getCount() == 1 && saved.getDamageValue() == 0 && !saved.isBarVisible(), "wrench has no durability loss or bar");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void serverActionsValidateHeldModuleAndFluidIndex(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "WrenchPacketQA"));
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(wrench, java.util.List.of(new ItemStack(WrenchModule.FAN.item()), new ItemStack(WrenchModule.TANK.item())));
        WrenchData.setActiveSlot(wrench, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        WrenchNetworking.handle(player, new WrenchNetworking.Action(WrenchNetworking.TOGGLE_FAN, 0));
        helper.assertTrue(WrenchData.fanPull(wrench), "server toggles the held active fan");
        WrenchData.setActiveSlot(wrench, 1);
        var water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
        var lava = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 1000);
        WrenchData.setFluids(wrench, java.util.List.of(water, lava), helper.getLevel().registryAccess());
        WrenchNetworking.handle(player, new WrenchNetworking.Action(WrenchNetworking.TOGGLE_FAN, 0));
        helper.assertTrue(WrenchData.fanPull(wrench), "wrong active module cannot change fan mode");
        WrenchNetworking.handle(player, new WrenchNetworking.Action(WrenchNetworking.SELECT_FLUID, 1));
        helper.assertTrue(WrenchData.selectedFluid(wrench) == 1, "valid fluid selection succeeds");
        WrenchNetworking.handle(player, new WrenchNetworking.Action(WrenchNetworking.SELECT_FLUID, Integer.MAX_VALUE));
        WrenchNetworking.handle(player, new WrenchNetworking.Action(WrenchNetworking.SELECT_FLUID, -1));
        helper.assertTrue(WrenchData.selectedFluid(wrench) == 1, "out of range packet indices leave selection unchanged");
        WrenchData.setActiveSlot(wrench, -1);
        WrenchNetworking.handle(player, new WrenchNetworking.Action(WrenchNetworking.SELECT_FLUID, 0));
        helper.assertTrue(WrenchData.selectedFluid(wrench) == 1 && WrenchData.totalFluid(wrench, helper.getLevel().registryAccess()) == 2000,
            "inactive tank cannot change selection, and removing module preserves fluids");
        var reloaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) wrench.save(helper.getLevel().registryAccess()));
        helper.assertTrue(WrenchData.totalFluid(reloaded, helper.getLevel().registryAccess()) == 2000 && WrenchData.selectedFluid(reloaded) == 1,
            "mixed fluid state persists with item serialization");
        helper.succeed();
    }
}
