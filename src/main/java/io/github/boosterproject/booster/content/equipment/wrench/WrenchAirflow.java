package io.github.boosterproject.booster.content.equipment.wrench;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.boosterproject.booster.config.BoosterConfigs;
import io.github.boosterproject.booster.mixin.WrenchFlightAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A hand-directed, one-block-wide air current with the fan's native acceleration. */
public final class WrenchAirflow {
    public static final float RPM = 256;

    private WrenchAirflow() {}

    public static float range(boolean pulling) {
        var config = AllConfigs.server().kinetics;
        float factor = Math.min(RPM / config.fanRotationArgmax.get(), 1);
        return Mth.lerp(factor, 3F, pulling ? config.fanPullDistance.get() : config.fanPushDistance.get());
    }

    public static void tick(Player player, ItemStack wrench) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()
            || player.getMainHandItem() != wrench || !WrenchData.has(wrench, WrenchModule.FAN)) return;
        boolean pulling = WrenchData.fanPull(wrench);
        double range = range(pulling);
        Vec3 origin = player.getEyePosition();
        Vec3 facing = player.getLookAngle();
        Vec3 end = origin.add(facing.scale(range));
        Vec3 flow = pulling ? facing.scale(-1) : facing;
        float speed = RPM * BoosterConfigs.SERVER.netheriteFanWindSpeedMultiplier.get().floatValue();
        for (var entity : player.level().getEntities(player, new AABB(origin, end).inflate(1),
            entity -> entity.isAlive() && !entity.isSpectator() && !AirCurrent.isPlayerCreativeFlying(entity))) {
            Vec3 center = entity.getBoundingBox().getCenter();
            Vec3 offset = center.subtract(origin);
            double along = offset.dot(facing);
            if (along < 0 || along > range) continue;
            // Include entities whose bodies, rather than just their centers, overlap the current.
            double radius = .5 + Math.min(.5, entity.getBbWidth() / 2D);
            if (offset.subtract(facing.scale(along)).lengthSqr() > radius * radius) continue;
            var obstruction = player.level().clip(new ClipContext(origin, center, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
            if (obstruction.getType() != HitResult.Type.MISS) continue;
            double distance = Math.max(.25, center.distanceTo(origin));
            float sneakModifier = entity.isShiftKeyDown() ? 4096F : 512F;
            double acceleration = speed / sneakModifier / (distance / range);
            Vec3 previous = entity.getDeltaMovement();
            Vec3 target = flow.scale(acceleration);
            entity.setDeltaMovement(previous.add(
                Mth.clamp(target.x - previous.x, -5, 5) / 8,
                Mth.clamp(target.y - previous.y, -5, 5) / 8,
                Mth.clamp(target.z - previous.z, -5, 5) / 8));
            entity.hasImpulse = true;
            entity.fallDistance = 0;
            if (entity instanceof ServerPlayer affected) {
                ((WrenchFlightAccess) affected.connection).booster$setAboveGroundTicks(0);
                WrenchNetworking.sendLaunch(affected, entity.getDeltaMovement());
            }
        }
    }
}
