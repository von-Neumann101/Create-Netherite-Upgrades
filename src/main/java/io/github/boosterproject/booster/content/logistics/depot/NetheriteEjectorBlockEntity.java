package io.github.boosterproject.booster.content.logistics.depot;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.depot.EjectorBlock;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.boosterproject.booster.mixin.NetheriteEjectorAccess;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NetheriteEjectorBlockEntity extends EjectorBlockEntity implements TransformableBlockEntity {
    private final NetheriteEntityLauncher vectorLauncher;

    public NetheriteEjectorBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_WEIGHTED_EJECTOR.get(), pos, state);
        vectorLauncher = new NetheriteEntityLauncher(BlockPos.ZERO.relative(getFacing()));
        ((NetheriteEjectorAccess) this).booster$setLauncher(vectorLauncher);
    }

    public static int getMaximumRange() {
        return (int) Math.min(Integer.MAX_VALUE, AllConfigs.server().kinetics.maxEjectorDistance.get() * 2L);
    }

    public static boolean isValidOffset(BlockPos offset) {
        int max = getMaximumRange();
        return !offset.equals(BlockPos.ZERO) && Math.hypot(offset.getX(), offset.getZ()) <= max
            && Math.abs((long) offset.getY()) <= max;
    }

    public boolean setTargetOffset(BlockPos offset) {
        if (!isValidOffset(offset) || level != null && !level.isInWorldBounds(worldPosition.offset(offset))) return false;
        if (offset.equals(getTargetOffset())) return true;
        if (hasFlyingItems()) return false;
        updateTargetOffset(offset);
        return true;
    }

    private void updateTargetOffset(BlockPos offset) {
        vectorLauncher.setOffset(offset);
        var access = (NetheriteEjectorAccess) this;
        access.booster$setEarlyTarget(null);
        access.booster$setEarlyTargetTime(0);
        access.booster$setScanCooldown(0);
        var flying = access.booster$getLaunchedItems();
        access.booster$setTrackedItem(flying.isEmpty() ? null : flying.getFirst().getValue());
        setChanged();
        if (level != null && !level.isClientSide) sendData();
    }

    public boolean hasFlyingItems() { return !((NetheriteEjectorAccess) this).booster$getLaunchedItems().isEmpty(); }
    public BlockPos getTargetOffset() { return vectorLauncher.getOffset(); }
    public NetheriteEntityLauncher getVectorLauncher() { return vectorLauncher; }
    public boolean isPowered() { return ((NetheriteEjectorAccess) this).booster$isPowered(); }

    @Override protected Direction getFacing() { return getBlockState().getValue(EjectorBlock.HORIZONTAL_FACING); }
    @Override public BlockPos getTargetPosition() { return worldPosition.offset(vectorLauncher.getOffset()); }
    @Override public float getWindUpSpeed() { return super.getWindUpSpeed() * 8; }

    @Override public void deployElytra(Player player) {
        BlockPos offset = getTargetOffset();
        if (offset.getX() != 0 || offset.getZ() != 0)
            player.setYRot((float) Math.toDegrees(Math.atan2(-offset.getX(), offset.getZ())));
        super.deployElytra(player);
    }

    @Override public void dropFlyingItems() {
        var flying = ((NetheriteEjectorAccess) this).booster$getLaunchedItems();
        for (var entry : flying) {
            Vec3 position = getLaunchedItemLocation(entry.getFirst());
            // Construct at the flight position so the entity's bounding box moves with it.
            var item = new ItemEntity(level, position.x, position.y, position.z, entry.getValue());
            item.setDeltaMovement(getLaunchedItemMotion(entry.getFirst()));
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        flying.clear();
    }

    // Native 2D placement packets cannot represent this machine's target and must not edit it.
    @Override public void setTarget(int horizontalDistance, int verticalDistance) {}

    @Override public DirectBeltInputBehaviour getTargetOpenInv() {
        var hit = ((NetheriteEjectorAccess) this).booster$getEarlyTarget();
        return BlockEntityBehaviour.get(level, hit == null ? getTargetPosition() : hit.getSecond(), DirectBeltInputBehaviour.TYPE);
    }

    public void rotateTarget(Direction before, Direction after) {
        BlockPos offset = getTargetOffset();
        for (int turns = 0; before != after && turns < 4; turns++) {
            before = before.getClockWise();
            offset = new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
        }
        setTargetOffset(offset);
    }

    @Override public void transform(BlockEntity blockEntity, StructureTransform transform) {
        Vec3 offset = transform.applyWithoutOffsetUncentered(Vec3.atLowerCornerOf(getTargetOffset()));
        BlockPos transformed = new BlockPos((int) Math.round(offset.x), (int) Math.round(offset.y), (int) Math.round(offset.z));
        if (isValidOffset(transformed)) updateTargetOffset(transformed);
    }

    private void writeTarget(CompoundTag tag) {
        BlockPos offset = getTargetOffset();
        tag.putIntArray("TargetOffset", new int[] {offset.getX(), offset.getY(), offset.getZ()});
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        writeTarget(tag);
    }
    @Override public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        writeTarget(tag);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        int[] stored = tag.getIntArray("TargetOffset");
        BlockPos offset = stored.length == 3 ? new BlockPos(stored[0], stored[1], stored[2])
            : BlockPos.ZERO.relative(getFacing(), Math.max(1, vectorLauncher.getHorizontalDistance())).above(vectorLauncher.getVerticalDistance());
        int max = getMaximumRange();
        double horizontal = Math.hypot(offset.getX(), offset.getZ());
        if (horizontal > max) offset = new BlockPos((int) (offset.getX() * max / horizontal), offset.getY(), (int) (offset.getZ() * max / horizontal));
        offset = new BlockPos(offset.getX(), Math.max(-max, Math.min(max, offset.getY())), offset.getZ());
        vectorLauncher.setOffset(offset);
    }
}
