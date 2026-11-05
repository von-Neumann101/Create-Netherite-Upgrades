package io.github.boosterproject.booster.content.equipment.wrench;

import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchSawGameTest {
    private static ServerPlayer player(GameTestHelper helper, String name) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), name);
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(1, 280, 1)).getBottomCenter());
        player.setYRot(0); player.setXRot(0);
        // Actual server tick consumes native spawn protection; FakePlayer.tick() is intentionally empty.
        for (int i = 0; i < 60; i++) player.tick();
        return player;
    }

    private static ItemStack equip(ServerPlayer player) {
        var stack = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(stack, List.of(new ItemStack(BoosterItems.NETHERITE_MECHANICAL_SAW.get())));
        WrenchData.setActiveSlot(stack, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    private static Cow cow(GameTestHelper helper, Vec3 position) {
        var cow = EntityType.COW.create(helper.getLevel());
        cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(position);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void continuousSawUsesMachineDamageNativeIntervalAndRelease(GameTestHelper helper) {
        var player = player(helper, "SawHoldQA");
        var stack = equip(player);
        var cow = cow(helper, player.position().add(0, .4, 2));
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        stack.getItem().onUseTick(helper.getLevel(), player, stack, 71999);
        helper.assertTrue(cow.getHealth() == 82 && cow.getLastHurtByMob() == player,
            "held saw uses eighteen machine damage and preserves player attribution");
        for (int i = 0; i < 9; i++) {
            cow.tick();
            stack.getItem().onUseTick(helper.getLevel(), player, stack, 71998 - i);
        }
        helper.assertTrue(cow.getHealth() == 82, "continuing right click never clears the native hurt interval");
        cow.tick();
        stack.getItem().onUseTick(helper.getLevel(), player, stack, 71988);
        helper.assertTrue(cow.getHealth() == 64, "held saw damages again after the native interval");
        player.releaseUsingItem(); cow.invulnerableTime = 0;
        WrenchSaw.tick(player, stack);
        helper.assertTrue(cow.getHealth() == 64, "release stops damage even after target immunity has expired");
        cow.discard(); helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void sawChoosesNearestForwardTargetAndRespectsWallAndReach(GameTestHelper helper) {
        var player = player(helper, "SawAimQA"); var stack = equip(player);
        var near = cow(helper, player.position().add(0, .4, 2));
        var far = cow(helper, player.position().add(0, .4, 2.8));
        var behind = cow(helper, player.position().add(0, .4, -2));
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        WrenchSaw.tick(player, stack);
        helper.assertTrue(near.getHealth() == 82 && far.getHealth() == 100 && behind.getHealth() == 100,
            "saw hits only nearest target on the forward sight ray");
        near.setHealth(100); near.invulnerableTime = 0;
        var wall = BlockPos.containing(player.position()).offset(0, 1, 1);
        helper.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        WrenchSaw.tick(player, stack);
        helper.assertTrue(near.getHealth() == 100 && far.getHealth() == 100, "solid wall obstructs saw damage");
        helper.getLevel().removeBlock(wall, false);
        near.setPos(player.position().add(0, .4, 4)); far.setPos(player.position().add(0, .4, 5));
        WrenchSaw.tick(player, stack);
        helper.assertTrue(near.getHealth() == 100 && far.getHealth() == 100 && behind.getHealth() == 100,
            "out of reach and behind targets remain untouched");
        near.discard(); far.discard(); behind.discard(); helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void offhandInactiveAndSwitchedSawCannotDamage(GameTestHelper helper) {
        var player = player(helper, "SawStateQA"); var stack = equip(player);
        var cow = cow(helper, player.position().add(0, .4, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, stack); player.startUsingItem(InteractionHand.OFF_HAND);
        WrenchSaw.tick(player, stack);
        helper.assertTrue(cow.getHealth() == 100, "offhand saw has no continuous damage");
        player.stopUsingItem(); player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack); player.startUsingItem(InteractionHand.MAIN_HAND);
        WrenchData.setActiveSlot(stack, -1); WrenchSaw.tick(player, stack);
        helper.assertTrue(cow.getHealth() == 100, "stored but inactive saw cannot hurt");
        WrenchData.setActiveSlot(stack, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        WrenchSaw.tick(player, stack);
        helper.assertTrue(cow.getHealth() == 100, "old source stack cannot damage after swapping the held wrench");
        cow.discard(); helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void sawStartsBeforeEntityGuiAndOnBlockUse(GameTestHelper helper) {
        var player = player(helper, "SawInteractQA"); var stack = equip(player);
        var villager = EntityType.VILLAGER.create(helper.getLevel());
        villager.setPos(player.position().add(0, 0, 2)); villager.setNoAi(true); villager.setNoGravity(true);
        helper.getLevel().addFreshEntity(villager);
        var specific = new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, villager, Vec3.ZERO);
        NeoForge.EVENT_BUS.post(specific);
        helper.assertTrue(specific.isCanceled() && specific.getCancellationResult().consumesAction() && player.isUsingItem(),
            "specific entity interaction starts the saw before a villager opens its GUI");
        player.releaseUsingItem();
        var general = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, villager);
        NeoForge.EVENT_BUS.post(general);
        helper.assertTrue(general.isCanceled() && player.isUsingItem(), "generic entity interaction also starts continuous use");
        player.releaseUsingItem();
        var pos = BlockPos.containing(player.position()).offset(1, 0, 0);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false));
        helper.assertTrue(stack.getItem().onItemUseFirst(stack, context).consumesAction() && player.isUsingItem(),
            "right click on a block starts saw use before native block menus");
        player.releaseUsingItem(); villager.discard(); helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void playerOwnedSawHonorsDisabledPvp(GameTestHelper helper) {
        var player = player(helper, "SawPvpOwnerQA"); var stack = equip(player);
        var victim = player(helper, "SawPvpVictimQA"); victim.setPos(player.position().add(0, .4, 2));
        helper.getLevel().addFreshEntity(victim);
        boolean previous = helper.getLevel().getServer().isPvpAllowed();
        try {
            helper.getLevel().getServer().setPvpAllowed(true);
            stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            WrenchSaw.tick(player, stack);
            helper.assertTrue(victim.getHealth() < 20, "positive control: normal PvP accepts player-owned saw damage");
            victim.setHealth(20); victim.invulnerableTime = 0;
            helper.getLevel().getServer().setPvpAllowed(false);
            WrenchSaw.tick(player, stack);
            helper.assertTrue(victim.getHealth() == 20, "disabled server PvP rejects saw damage through normal player attribution");
        } finally {
            helper.getLevel().getServer().setPvpAllowed(previous); victim.discard();
        }
        helper.succeed();
    }
}
