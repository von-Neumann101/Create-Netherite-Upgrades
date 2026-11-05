package io.github.boosterproject.booster.content.equipment.wrench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.github.boosterproject.booster.mixin.WrenchFlightAccess;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Native collision and gravity remain in control throughout the flight. */
public final class WrenchLaunch {
    public static final double RANGE = 64;
    private static final int MAX_FLIGHT_TICKS = 240;
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    private WrenchLaunch() {}

    public static void register(IEventBus bus) {
        bus.addListener(WrenchLaunch::tick);
        bus.addListener(WrenchLaunch::fall);
        bus.addListener(WrenchLaunch::logout);
    }

    /** Preview and launch share the same discrete vanilla drag/gravity solution. */
    public record Trajectory(Vec3 start, Vec3 target, Vec3 velocity, int ticks, List<Vec3> points) {}

    public static Trajectory trajectory(Player player) {
        Vec3 start = player.position();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        var hit = player.level().clip(new ClipContext(eye, eye.add(look.scale(RANGE)), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player));
        Vec3 target = start.add(look.scale(RANGE));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            target = hit.getLocation().add(normal.scale(hit.getDirection() == Direction.UP ? .01 : .35));
        }
        Vec3 offset = target.subtract(start);
        if (offset.lengthSqr() > RANGE * RANGE) {
            offset = offset.normalize().scale(RANGE);
            target = start.add(offset);
        }
        double horizontal = Math.hypot(offset.x, offset.z);
        int ticks = Math.max(24, Math.min(80, 32 + (int) Math.ceil(horizontal / 4 + Math.max(0, offset.y) / 2)));
        double horizontalSum = (1 - Math.pow(.91, ticks)) / (1 - .91);
        double verticalSum = (1 - Math.pow(.98, ticks)) / (1 - .98);
        double vy = (offset.y + 3.92 * (ticks - verticalSum)) / verticalSum;
        Vec3 velocity = new Vec3(offset.x / horizontalSum, vy, offset.z / horizontalSum);
        List<Vec3> points = new ArrayList<>(ticks + 1);
        Vec3 current = start;
        Vec3 motion = velocity;
        points.add(current);
        for (int tick = 0; tick < ticks; tick++) {
            current = current.add(motion);
            points.add(current);
            motion = new Vec3(motion.x * .91, (motion.y - .08) * .98, motion.z * .91);
        }
        return new Trajectory(start, target, velocity, ticks, List.copyOf(points));
    }

    public static List<Vec3> preview(Player player) { return trajectory(player).points(); }

    public static boolean start(Player player) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator() || player.isPassenger()
            || WrenchData.active(player) != WrenchModule.EJECTOR) return false;
        Trajectory trajectory = trajectory(player);
        FLIGHTS.put(player.getUUID(), new Flight(player, player.level().getGameTime(), trajectory.target()));
        player.setOnGround(false);
        player.horizontalCollision = false;
        player.verticalCollision = false;
        player.verticalCollisionBelow = false;
        player.fallDistance = 0;
        player.setDeltaMovement(trajectory.velocity());
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            ((WrenchFlightAccess) serverPlayer.connection).booster$setAboveGroundTicks(0);
            WrenchNetworking.sendLaunch(serverPlayer, trajectory.velocity());
        }
        return true;
    }

    public static boolean isProtected(Player player) {
        Flight flight = FLIGHTS.get(player.getUUID());
        return flight != null && !flight.blocked && valid(player, flight);
    }

    /** Called immediately after each native move, before another packet can clear its collision flags. */
    public static void recordCollision(Player player) {
        if (player.level().isClientSide) return;
        Flight flight = FLIGHTS.get(player.getUUID());
        if (flight != null && valid(player, flight)
            && (player.horizontalCollision || player.verticalCollision && !player.onGround())) flight.blocked = true;
    }

    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        Flight flight = FLIGHTS.get(player.getUUID());
        if (flight == null) return;
        if (!valid(player, flight) || player.isInWaterOrBubble() || player.isInLava() || player.onClimbable()
            || player.isFallFlying() || player.getAbilities().flying) {
            FLIGHTS.remove(player.getUUID());
            return;
        }
        recordCollision(player);
        if (player instanceof ServerPlayer serverPlayer && !player.onGround())
            ((WrenchFlightAccess) serverPlayer.connection).booster$setAboveGroundTicks(0);
        if (player.onGround() && player.level().getGameTime() > flight.started) FLIGHTS.remove(player.getUUID());
    }

    private static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        Flight flight = FLIGHTS.remove(player.getUUID());
        if (flight == null || !valid(player, flight)) return;
        double dx = player.getX() - flight.target.x;
        double dz = player.getZ() - flight.target.z;
        // Landing on an obstacle before the selected destination is a collision too.
        boolean reachedLanding = dx * dx + dz * dz <= 25;
        if (!flight.blocked && !player.horizontalCollision && reachedLanding) event.setCanceled(true);
    }

    private static boolean valid(Player player, Flight flight) {
        long elapsed = player.level().getGameTime() - flight.started;
        return player.isAlive() && !player.isSpectator() && !player.isPassenger()
            && player == flight.owner && player.level() == flight.level && flight.dimension.equals(player.level().dimension())
            && elapsed >= 0 && elapsed <= MAX_FLIGHT_TICKS;
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) { FLIGHTS.remove(event.getEntity().getUUID()); }

    private static final class Flight {
        final Player owner;
        final Level level;
        final ResourceKey<Level> dimension;
        final long started;
        final Vec3 target;
        boolean blocked;

        Flight(Player owner, long started, Vec3 target) {
            this.owner = owner;
            this.level = owner.level();
            this.dimension = level.dimension();
            this.started = started;
            this.target = target;
        }
    }
}
