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
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchEjectorAimGameTest {
    @GameTest(template = "wireless_empty")
    public static void equalNetworkIdsDoNotMixClientAndServerCharge(GameTestHelper helper) {
        var serverPlayer = player(helper, 360);
        var clientPeer = player(helper, 360);
        clientPeer.setId(serverPlayer.getId());
        helper.assertTrue(serverPlayer != clientPeer && serverPlayer.equals(clientPeer),
            "separate logical players can share the network entity ID and vanilla equality");
        serverPlayer.setShiftKeyDown(true); clientPeer.setShiftKeyDown(true);
        serverPlayer.getMainHandItem().use(helper.getLevel(), serverPlayer, InteractionHand.MAIN_HAND);
        clientPeer.getMainHandItem().use(helper.getLevel(), clientPeer, InteractionHand.MAIN_HAND);
        helper.assertTrue(WrenchLaunch.isCharging(serverPlayer) && WrenchLaunch.isCharging(clientPeer),
            "logical client and server retain independent charges despite equal entity IDs");
        serverPlayer.stopUsingItem();
        helper.assertTrue(WrenchLaunch.isCharging(clientPeer), "cancelling one logical side cannot erase the other's charge");
        clientPeer.stopUsingItem(); helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void chargeChangesRangeAndViewChangesArc(GameTestHelper helper) {
        var player = player(helper, 360);
        float half = WrenchLaunch.powerForTime(10), full = WrenchLaunch.powerForTime(20);
        var shortArc = WrenchLaunch.trajectory(player, half);
        var longArc = WrenchLaunch.trajectory(player, full);
        helper.assertTrue(half > .1 && half < full && full == 1 && WrenchLaunch.powerForTime(40) == 1,
            "bow charge increases to full power at twenty ticks and stops increasing");
        helper.assertTrue(shortArc.target().distanceTo(player.position()) < longArc.target().distanceTo(player.position())
            && longArc.target().distanceTo(player.position()) <= 64.00001, "charge controls range up to sixty-four blocks");
        player.setYRot(-90); player.setXRot(-30);
        var aimed = WrenchLaunch.trajectory(player, half);
        helper.assertTrue(aimed.target().x > player.getX() && aimed.target().y > player.getY()
            && Math.abs(aimed.target().z - player.getZ()) < .001 && !aimed.velocity().equals(shortArc.velocity()),
            "current view changes both horizontal direction and vertical arc");
        var preview = WrenchLaunch.predict(player, half);
        helper.assertTrue(!preview.blocked() && preview.target().distanceTo(aimed.target()) < 1e-6
            && preview.points().getLast().distanceTo(aimed.target()) < 1e-6,
            "unobstructed preview ends at the same discrete trajectory target used by launch");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void previewUsesPlayerBodyCollisionWithoutSnappingAim(GameTestHelper helper) {
        var player = player(helper, 230);
        BlockPos wall = player.blockPosition().offset(0, 0, 8);
        for (int x = -1; x <= 1; x++) for (int y = 0; y < 14; y++)
            helper.getLevel().setBlock(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 3);
        var desired = WrenchLaunch.trajectory(player, 1);
        var clipped = WrenchLaunch.predict(player, 1);
        helper.assertTrue(clipped.blocked() && clipped.target().z < wall.getZ()
            && clipped.target().z <= wall.getZ() - player.getBbWidth() / 2.0 + .001,
            "preview body contact: blocked=" + clipped.blocked() + ", point=" + clipped.target()
                + ", wall=" + wall + ", desired=" + desired.target() + ", body=" + player.getBoundingBox());
        helper.assertTrue(desired.target().distanceTo(player.position()) > 63.99,
            "nearby obstacles do not turn free aim into a fixed short-range target");
        for (int x = -1; x <= 1; x++) for (int y = 0; y < 14; y++) helper.getLevel().removeBlock(wall.offset(x, y, 0), false);
        helper.assertTrue(!WrenchLaunch.predict(player, 1).blocked(), "moving the obstacle away immediately restores the complete preview");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void previewIncludesHardEntityCollision(GameTestHelper helper) {
        var player = player(helper, 250);
        var boat = EntityType.BOAT.create(helper.getLevel());
        boat.setNoGravity(true); boat.setPos(player.position().add(0, 2, 5)); helper.getLevel().addFreshEntity(boat);
        helper.assertTrue(WrenchLaunch.predict(player, 1).blocked(), "hard entity collision clips preview just as it clips real player movement");
        boat.discard(); helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void releaseUsesFinalViewAndReplacingWrenchCancelsCharge(GameTestHelper helper) {
        var player = player(helper, 360);
        var wrench = player.getMainHandItem();
        player.setShiftKeyDown(true);
        wrench.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int i = 0; i < 20; i++) player.doTick();
        player.setYRot(-90); player.setXRot(-20);
        var expected = WrenchLaunch.trajectory(player, 1).velocity();
        player.releaseUsingItem();
        helper.assertTrue(player.getDeltaMovement().distanceTo(expected) < 1e-7 && !WrenchLaunch.isCharging(player),
            "release launches once along the final view, not the view at charge start");
        WrenchLaunch.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        player.setDeltaMovement(Vec3.ZERO);
        wrench.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int i = 0; i < 10; i++) player.doTick();
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench.copy());
        player.doTick();
        helper.assertTrue(!WrenchLaunch.isCharging(player) && !player.isUsingItem(),
            "replacing even an identical wrench cancels the original item's charge");
        Vec3 beforeCancelledRelease = player.getDeltaMovement();
        player.releaseUsingItem();
        helper.assertTrue(player.getDeltaMovement().equals(beforeCancelledRelease), "cancelled release adds no impulse to the native current movement");
        player.discard(); WrenchLaunch.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void shiftEntityInteractionStartsChargeBeforeNativeGui(GameTestHelper helper) {
        var player = player(helper, 260);
        var villager = EntityType.VILLAGER.create(helper.getLevel());
        villager.setPos(player.position().add(0, 0, 2)); villager.setNoAi(true);
        helper.getLevel().addFreshEntity(villager);
        player.setShiftKeyDown(true);
        var event = new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, villager, Vec3.ZERO);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled() && WrenchLaunch.isCharging(player) && !WrenchLaunch.isProtected(player),
            "Shift entity use begins charge before villager interaction, without immediate launch");
        WrenchData.setActiveSlot(player.getMainHandItem(), -1);
        player.doTick();
        player.releaseUsingItem();
        helper.assertTrue(!WrenchLaunch.isCharging(player) && !WrenchLaunch.isProtected(player), "deactivating the module cancels the pending release");
        villager.discard(); helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, int height) {
        var profile = new GameProfile(UUID.randomUUID(), "WrenchAimQA");
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        player.setPos(helper.absoluteVec(new Vec3(1.5, height, 1.5))); player.setYRot(0); player.setXRot(0);
        var stack = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(stack, List.of(new ItemStack(WrenchModule.EJECTOR.item()))); WrenchData.setActiveSlot(stack, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }
}
