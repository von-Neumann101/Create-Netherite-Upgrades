package io.github.boosterproject.booster.content.equipment.wrench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import java.util.IdentityHashMap;
import io.github.boosterproject.booster.mixin.WrenchFlightAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Native collision and gravity remain in control throughout the flight. */
public final class WrenchLaunch {
    public static final double RANGE = 64;
    public static final int CHARGE_TICKS = 20;
    // Entity.equals compares network IDs, shared by an integrated server and its client.
    private static final Map<Player, ItemStack> CHARGES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final int MAX_FLIGHT_TICKS = 240;
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    private WrenchLaunch() {}

    public static void register(IEventBus bus) {
        bus.addListener(WrenchLaunch::tick);
        bus.addListener(WrenchLaunch::fall);
        bus.addListener(WrenchLaunch::logout);
        bus.addListener((PlayerEvent.Clone event) -> cancel(event.getOriginal()));
        bus.addListener(WrenchLaunch::interactSpecific);
        bus.addListener(WrenchLaunch::interact);
    }

    /** Preview and launch share the same discrete vanilla drag/gravity solution. */
    public record Trajectory(Vec3 start, Vec3 target, Vec3 velocity, int ticks, List<Vec3> points) {}
    public record Preview(List<Vec3> points, Vec3 target, boolean blocked) {}

    public static Trajectory trajectory(Player player) { return trajectory(player, 1); }

    public static Trajectory trajectory(Player player, float power) {
        Vec3 start = player.position();
        power = Mth.clamp(power, 0, 1);
        if (power == 0) return new Trajectory(start, start, Vec3.ZERO, 0, List.of(start));
        Vec3 look = player.getLookAngle();
        // Aim is free: obstacles clip the preview and the real flight, not the selected target.
        Vec3 target = start.add(look.scale(RANGE * power));
        Vec3 offset = target.subtract(start);
        if (offset.lengthSqr() > RANGE * RANGE) {
            offset = offset.normalize().scale(RANGE);
            target = start.add(offset);
        }
        double horizontal = Math.hypot(offset.x, offset.z);
        int ticks = Math.max(4, Math.min(80, (int) Math.ceil((32 + horizontal / 4 + Math.max(0, offset.y) / 2) * Math.sqrt(power))));
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

    public static Preview predict(Player player, float power) {
        var trajectory = trajectory(player, power);
        List<Vec3> points = new ArrayList<>();
        points.add(trajectory.start());
        var bounds = player.getBoundingBox();
        Vec3 previous = trajectory.start();
        for (int i = 1; i < trajectory.points().size(); i++) {
            Vec3 next = trajectory.points().get(i);
            Vec3 step = next.subtract(previous);
            var entityCollisions = player.level().getEntityCollisions(player, bounds.expandTowards(step));
            Vec3 allowed = Entity.collideBoundingBox(player, step, bounds, player.level(), entityCollisions);
            Vec3 actual = previous.add(allowed);
            points.add(actual);
            if (allowed.distanceToSqr(step) > 1e-10) return new Preview(List.copyOf(points), actual, true);
            bounds = bounds.move(allowed);
            previous = actual;
        }
        return new Preview(List.copyOf(points), previous, false);
    }

    public static float powerForTime(int ticks) { return BowItem.getPowerForTime(Math.max(0, ticks)); }
    public static boolean isCharging(Player player) {
        return player.isUsingItem() && CHARGES.get(player) == player.getMainHandItem()
            && player.getUsedItemHand() == InteractionHand.MAIN_HAND && player.getUseItem() == player.getMainHandItem()
            && WrenchData.active(player) == WrenchModule.EJECTOR;
    }
    public static int chargeTicks(Player player) { return isCharging(player) ? player.getTicksUsingItem() : 0; }

    public static InteractionResultHolder<ItemStack> beginCharge(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.isAlive() || player.isSpectator()
            || player.isPassenger() || WrenchData.active(stack) != WrenchModule.EJECTOR) return InteractionResultHolder.fail(stack);
        if (!isCharging(player)) {
            player.startUsingItem(hand);
            if (player.isUsingItem() && player.getUsedItemHand() == hand) CHARGES.put(player, stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    public static void release(Player player, ItemStack stack, int remaining) {
        boolean charging = isCharging(player) && player.getUseItem() == stack;
        cancel(player);
        if (!charging || player.level().isClientSide) return;
        float power = powerForTime(stack.getUseDuration(player) - remaining);
        if (power >= .1F) start(player, power);
    }

    public static void cancel(Player player) { CHARGES.remove(player); }

    private static boolean chargeEntity(Player player, InteractionHand hand, Entity entity) {
        return WrenchData.active(player) == WrenchModule.EJECTOR && player.level() == entity.level()
            && player.canInteractWithEntity(entity, 0) && player.hasLineOfSight(entity)
            && beginCharge(player, hand).getResult().consumesAction();
    }
    private static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!chargeEntity(event.getEntity(), event.getHand(), event.getTarget())) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide)); event.setCanceled(true);
    }
    private static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!chargeEntity(event.getEntity(), event.getHand(), event.getTarget())) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide)); event.setCanceled(true);
    }

    public static boolean start(Player player) { return start(player, 1); }

    private static boolean start(Player player, float power) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator() || player.isPassenger()
            || WrenchData.active(player) != WrenchModule.EJECTOR) return false;
        Trajectory trajectory = trajectory(player, power);
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
        if (!player.isAlive() || !isCharging(player)) cancel(player);
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

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) { FLIGHTS.remove(event.getEntity().getUUID()); cancel(event.getEntity()); }

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
