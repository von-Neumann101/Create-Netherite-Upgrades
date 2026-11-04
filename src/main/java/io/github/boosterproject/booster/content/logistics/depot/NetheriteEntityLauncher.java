package io.github.boosterproject.booster.content.logistics.depot;

import com.simibubi.create.content.logistics.depot.EntityLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Uses Create's launch dynamics, with a full horizontal direction instead of a cardinal axis. */
public class NetheriteEntityLauncher extends EntityLauncher {
    private BlockPos offset;

    public NetheriteEntityLauncher(BlockPos offset) {
        super((int) Math.ceil(Math.hypot(offset.getX(), offset.getZ())), offset.getY());
        this.offset = offset.immutable();
    }

    public BlockPos getOffset() { return offset; }

    public void setOffset(BlockPos value) {
        offset = value.immutable();
        super.set((int) Math.ceil(Math.hypot(value.getX(), value.getZ())), value.getY());
    }

    @Override public void clamp(int max) {
        // The inherited BE calls this with Create's original limit while reading its scalar fields.
        super.clamp((int) Math.min(Integer.MAX_VALUE, Math.max(0L, max * 2L)));
    }

    @Override public Vec3 getGlobalPos(double time, Direction ignored, BlockPos origin) {
        double progress = Mth.clamp(time / getTotalFlyingTicks(), 0, 1);
        double correction = progress * progress * progress;
        double horizontal = x(time) + (getHorizontalDistance() - x(time)) * correction;
        double scale = getHorizontalDistance() == 0 ? 0 : horizontal / getHorizontalDistance();
        double vertical = y(time) + (offset.getY() - y(time)) * correction;
        return Vec3.atCenterOf(origin).add(offset.getX() * scale, vertical, offset.getZ() * scale);
    }

    @Override public Vec3 getGlobalVelocity(double time, Direction ignored, BlockPos origin) {
        double before = Math.max(0, time - .05);
        double after = time + .05;
        return getGlobalPos(after, ignored, origin).subtract(getGlobalPos(before, ignored, origin)).scale(1 / (after - before));
    }

    @Override public void applyMotion(Entity entity, Direction ignored) {
        double horizontal = getHorizontalDistance() == 0 ? 0 : dx(0) * .91 / getHorizontalDistance();
        entity.setDeltaMovement(offset.getX() * horizontal, dy(0) * .98, offset.getZ() * horizontal);
    }
}
