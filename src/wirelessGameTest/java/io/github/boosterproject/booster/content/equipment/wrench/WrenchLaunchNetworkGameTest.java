package io.github.boosterproject.booster.content.equipment.wrench;

import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises the real server packet entry points without creating a graphical client. */
@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchLaunchNetworkGameTest {
    private WrenchLaunchNetworkGameTest() {}

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void onlyChargedShiftUseReleaseSendsFullLaunchToMainHandPlayer(GameTestHelper helper) {
        var viewer = viewer(helper, "WrenchUsePacketQA");
        try {
            equip(viewer.player, InteractionHand.MAIN_HAND);
            equip(viewer.player, InteractionHand.OFF_HAND);
            viewer.listener.handleUseItem(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, 0, 0));
            helper.assertTrue(viewer.launches().isEmpty() && !viewer.player.isUsingItem(),
                "offhand item-use packet cannot charge or launch an active main-hand ejector");

            viewer.listener.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 1, 0, 0));
            helper.assertTrue(viewer.launches().isEmpty() && !viewer.player.isUsingItem(),
                "ordinary main-hand right-click cannot charge or launch the ejector");
            viewer.player.setShiftKeyDown(true);
            viewer.listener.handleUseItem(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 2, 0, 0));
            helper.assertTrue(viewer.launches().isEmpty() && !viewer.player.isUsingItem(),
                "Shift plus offhand right-click still cannot charge or launch");

            beginCharge(helper, viewer, 3);
            chargeFor(helper, viewer, 20);
            var expected = WrenchLaunch.trajectory(viewer.player).velocity();
            release(viewer);
            helper.assertTrue(viewer.launches().size() == 1,
                "real RELEASE_USE_ITEM after twenty server use ticks sends exactly one launch custom payload");
            helper.assertTrue(!viewer.player.isUsingItem(), "native release clears the charging use state");
            Vec3 sent = viewer.launches().getFirst().velocity();
            helper.assertTrue(sent.distanceTo(expected) < 1e-9 && sent.z > 3.9,
                "custom payload retains the complete launch velocity above the vanilla 3.9 motion-packet clamp");
            helper.assertTrue(viewer.player.getDeltaMovement().distanceTo(sent) < 1e-9 && !viewer.player.onGround(),
                "server owns the same airborne launch impulse that it sends to its client");

            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                WrenchNetworking.Launch.CODEC.encode(buffer, viewer.launches().getFirst());
                var decoded = WrenchNetworking.Launch.CODEC.decode(buffer);
                helper.assertTrue(decoded.velocity().equals(sent), "registered launch codec round-trips all velocity doubles exactly");
            } finally {
                buffer.release();
            }
            helper.succeed();
        } finally {
            viewer.close();
        }
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void releaseBeforeMinimumChargeDoesNotLaunch(GameTestHelper helper) {
        var viewer = viewer(helper, "WrenchShortChargeQA");
        try {
            equip(viewer.player, InteractionHand.MAIN_HAND);
            beginCharge(helper, viewer, 0);
            chargeFor(helper, viewer, 2);
            release(viewer);
            helper.assertTrue(viewer.launches().isEmpty() && !viewer.player.isUsingItem(),
                "native release after two use ticks is below bow power 0.1 and cannot launch");
            helper.succeed();
        } finally {
            viewer.close();
        }
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void clientLaunchPositionsSurviveNativeServerMovementChecks(GameTestHelper helper) {
        var viewer = viewer(helper, "WrenchMovePacketQA");
        final WrenchLaunch.Trajectory trajectory;
        final Vec3 start;
        try {
            equip(viewer.player, InteractionHand.MAIN_HAND);
            beginCharge(helper, viewer, 0);
            chargeFor(helper, viewer, 20);
            start = viewer.player.position();
            trajectory = WrenchLaunch.trajectory(viewer.player);
            release(viewer);
            helper.assertTrue(viewer.launches().size() == 1,
                "full charge and real release send the launch needed by the simulated client");
        } catch (RuntimeException | Error failure) {
            viewer.close();
            throw failure;
        }
        Vec3[] clientPosition = {start};
        Vec3[] clientMotion = {viewer.launches().getFirst().velocity()};
        int[] ticks = {0};
        helper.onEachTick(() -> {
            try {
                // These are the real server entity/connection ticks. Do not call travel directly
                // or override isControlledByLocalInstance: both hid the packet path in earlier tests.
                viewer.player.tick();
                viewer.listener.tick();
                clientPosition[0] = clientPosition[0].add(clientMotion[0]);
                viewer.listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(
                    clientPosition[0].x, clientPosition[0].y, clientPosition[0].z, false));
                helper.assertTrue(viewer.corrections().isEmpty(),
                    "native handleMovePlayer accepts launch position at client tick " + (ticks[0] + 1));
                helper.assertTrue(viewer.player.position().distanceTo(clientPosition[0]) < 1e-7,
                    "server follows the client's launch movement at tick " + (ticks[0] + 1));
                // Reproduce the no-input, airborne vanilla step from the received payload.
                // This deliberately does not claim to execute LocalPlayer or a real network channel.
                Vec3 velocity = clientMotion[0];
                clientMotion[0] = new Vec3(velocity.x * .91, (velocity.y - .08) * .98, velocity.z * .91);
                if (++ticks[0] < trajectory.ticks()) return;
                helper.assertTrue(viewer.player.position().distanceTo(trajectory.target()) < 1e-6
                    && viewer.player.position().subtract(start).horizontalDistance() > 60,
                    "packet-driven server player completes the unobstructed full-range trajectory");
                viewer.close();
                helper.succeed();
            } catch (RuntimeException | Error failure) {
                viewer.close();
                throw failure;
            }
        });
    }

    private static void beginCharge(GameTestHelper helper, Viewer viewer, int sequence) {
        viewer.player.setShiftKeyDown(true);
        viewer.listener.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence, 0, 0));
        helper.assertTrue(viewer.launches().isEmpty(), "Shift right-click starts charging without sending a launch");
        helper.assertTrue(viewer.player.isUsingItem() && viewer.player.getUsedItemHand() == InteractionHand.MAIN_HAND
            && viewer.player.getTicksUsingItem() == 0,
            "real use packet starts the native main-hand use state at zero elapsed server ticks");
    }

    private static void chargeFor(GameTestHelper helper, Viewer viewer, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            viewer.player.tick();
            viewer.listener.tick();
            helper.assertTrue(viewer.player.isUsingItem() && viewer.launches().isEmpty(),
                "holding Shift right-click keeps charging without launch at server tick " + (tick + 1));
        }
        helper.assertTrue(viewer.player.getTicksUsingItem() == ticks,
            "elapsed charge comes from native remaining use time consumed by real server ticks");
    }

    private static void release(Viewer viewer) {
        viewer.listener.handlePlayerAction(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN, 0));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void ordinaryTooFastMovementStillProducesCorrection(GameTestHelper helper) {
        var viewer = viewer(helper, "WrenchMoveControlQA");
        try {
            Vec3 start = viewer.player.position();
            viewer.listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(start.x + 12, start.y, start.z, false));
            helper.assertTrue(viewer.corrections().size() == 1 && viewer.player.position().distanceTo(start) < 1e-7,
                "unlaunched survival player still hits vanilla moved-too-quickly correction; movement checks really ran");
            helper.succeed();
        } finally {
            viewer.close();
        }
    }

    private static Viewer viewer(GameTestHelper helper, String name) {
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), name);
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        List<Packet<?>> packets = new ArrayList<>();
        var listener = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) { packets.add(packet); }
        };
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.getAbilities().flying = false;
        // Above build height keeps other test fixtures out of this 64-block flight corridor.
        player.setPos(helper.absoluteVec(new Vec3(1.5, 360, 1.5)));
        player.setYRot(0);
        player.setXRot(0);
        level.getChunkSource().addEntity(player);
        player.tick();
        listener.tick();
        packets.clear();
        return new Viewer(player, listener, packets);
    }

    private static void equip(ServerPlayer player, InteractionHand hand) {
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        WrenchData.setModules(wrench, List.of(new ItemStack(WrenchModule.EJECTOR.item())));
        WrenchData.setActiveSlot(wrench, 0);
        player.setItemInHand(hand, wrench);
    }

    private record Viewer(ServerPlayer player, ServerGamePacketListenerImpl listener, List<Packet<?>> packets) {
        List<WrenchNetworking.Launch> launches() {
            return packets.stream().filter(ClientboundCustomPayloadPacket.class::isInstance)
                .map(ClientboundCustomPayloadPacket.class::cast).map(ClientboundCustomPayloadPacket::payload)
                .filter(WrenchNetworking.Launch.class::isInstance).map(WrenchNetworking.Launch.class::cast).toList();
        }
        List<ClientboundPlayerPositionPacket> corrections() {
            return packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance)
                .map(ClientboundPlayerPositionPacket.class::cast).toList();
        }
        void close() {
            player.serverLevel().getChunkSource().removeEntity(player);
            player.discard();
            WrenchLaunch.tick(new PlayerTickEvent.Post(player));
        }
    }
}
