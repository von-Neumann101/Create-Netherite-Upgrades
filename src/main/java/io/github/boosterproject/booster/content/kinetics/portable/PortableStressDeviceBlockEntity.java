package io.github.boosterproject.booster.content.kinetics.portable;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PortableStressDeviceBlockEntity extends GeneratingKineticBlockEntity {
    public static final float RPM = 64;
    private UUID itemId;
    private UUID deploymentToken;
    private long expiresAt;
    private ItemStack pendingPlacement;
    private boolean reconcilingNetwork;
    private boolean networkInitialized;
    public PortableStressDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_PORTABLE_STRESS_DEVICE.get(), pos, state);
    }
    public void preparePlacement(ItemStack stack) {
        pendingPlacement = stack;
        if (!level.captureBlockSnapshots) commitPlacement();
    }
    public void commitPlacement() {
        // NeoForge calls onPlace after accepting the captured placement and restoring
        // the held stack's components. A canceled placement never arms the device.
        if (pendingPlacement == null || !(level instanceof ServerLevel serverLevel)) return;
        ItemStack stack = pendingPlacement;
        pendingPlacement = null;
        UUID id = PortableStressDeviceItem.ensureId(stack);
        var deployment = PortableStressData.get(serverLevel.getServer()).deploy(id, level.dimension(), worldPosition,
            PortableStressData.now(serverLevel.getServer()));
        if (deployment == null) { level.removeBlock(worldPosition, false); return; }
        bind(id, deployment);
        PortableStressDeviceItem.sync(stack, serverLevel.getServer());
    }
    public void bind(UUID id, PortableStressData.Deployment deployment) {
        itemId = id;
        deploymentToken = deployment.token();
        expiresAt = deployment.expiresAt();
        setChanged();
        updateGeneratedRotation();
        sendData();
    }
    public boolean matches(UUID id, UUID token) { return id.equals(itemId) && token.equals(deploymentToken); }
    private boolean active() {
        if (itemId == null || deploymentToken == null || level == null) return false;
        if (level instanceof ServerLevel serverLevel) return PortableStressData.get(serverLevel.getServer())
            .active(itemId, deploymentToken, level.dimension(), worldPosition, PortableStressData.now(serverLevel.getServer()));
        return level.getGameTime() < expiresAt;
    }
    @Override public void initialize() {
        networkInitialized = true;
        if (level instanceof ServerLevel && !active()) {
            // Reconcile the saved contribution before native removal. Skipping this
            // leaves the source's old capacity in a restored network's unloaded total.
            reconcilingNetwork = true;
            try {
                super.initialize();
                level.removeBlock(worldPosition, false);
            } finally { reconcilingNetwork = false; }
            return;
        }
        super.initialize();
        if (!level.isClientSide) updateGeneratedRotation();
    }
    @Override public void tick() {
        if (level instanceof ServerLevel && !active()) {
            if (!networkInitialized) initialize();
            else level.removeBlock(worldPosition, false);
            return;
        }
        super.tick();
    }
    @Override public void remove() {
        boolean previous = reconcilingNetwork;
        reconcilingNetwork = true;
        try {
            // FULL chunks can be recalled/unloaded before their block entities tick.
            // Native removal still needs their saved contribution registered first.
            if (level instanceof ServerLevel && !networkInitialized) {
                networkInitialized = true;
                super.initialize();
            }
            super.remove();
        } finally { reconcilingNetwork = previous; }
    }
    @Override public void onChunkUnloaded() {
        if (level instanceof ServerLevel) {
            // Unlike a permanent generator, this source must not leave cached power
            // in the loaded part of the network while its deadline continues ticking.
            remove();
            setSpeed(0);
            setNetwork(null);
            source = null;
            updateSpeed = true;
        }
        super.onChunkUnloaded();
    }
    public void retire() {
        if (level instanceof ServerLevel serverLevel && itemId != null && deploymentToken != null)
            PortableStressData.get(serverLevel.getServer()).finish(itemId, deploymentToken, PortableStressData.now(serverLevel.getServer()));
    }
    @Override public float getGeneratedSpeed() {
        return (reconcilingNetwork && itemId != null && deploymentToken != null || active())
            ? convertToDirection(RPM, getBlockState().getValue(PortableStressDeviceBlock.FACING)) : 0;
    }
    @Override public float calculateAddedStressCapacity() { return lastCapacityProvided = 2048 / RPM; }
    @Override public float calculateStressApplied() { return lastStressApplied = 0; }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (itemId != null) tag.putUUID("PortableItem", itemId);
        if (deploymentToken != null) tag.putUUID("Deployment", deploymentToken);
        tag.putLong("ExpiresAt", expiresAt);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        itemId = tag.hasUUID("PortableItem") ? tag.getUUID("PortableItem") : null;
        deploymentToken = tag.hasUUID("Deployment") ? tag.getUUID("Deployment") : null;
        expiresAt = tag.getLong("ExpiresAt");
    }
}
