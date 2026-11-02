package io.github.boosterproject.booster.content.redstone.wireless;

import java.util.UUID;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WirelessTerminalBlockEntity extends BlockEntity {
    private UUID terminalId;
    private UUID owner;
    private String ownerName = "";
    private String name = "";
    private boolean shared;

    public WirelessTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_WIRELESS_TERMINAL.get(), pos, state);
    }

    public UUID terminalId() { return terminalId; }

    public void placedBy(ServerPlayer player) {
        terminalId = UUID.randomUUID();
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        name = "";
        shared = false;
        register();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        register();
    }

    private void register() {
        if (!(level instanceof ServerLevel serverLevel) || owner == null || terminalId == null) return;
        WirelessTerminalData data = WirelessTerminalData.get(serverLevel.getServer());
        var stored = data.get(terminalId);
        if (stored != null && !stored.matches(level.dimension(), worldPosition)) {
            // A cloned block must not take over the original terminal's identity.
            terminalId = UUID.randomUUID();
            shared = false;
            stored = null;
        }
        if (stored == null) {
            data.register(new WirelessTerminalData.Terminal(terminalId, owner, ownerName, name,
                level.dimension(), worldPosition, shared));
        } else {
            owner = stored.owner(); ownerName = stored.ownerName(); name = stored.name(); shared = stored.shared();
        }
        var active = data.activation(terminalId);
        updatePower(active != null && active.powered(serverLevel.getServer().overworld().getGameTime()));
        setChanged();
    }

    public void updatePower(boolean powered) {
        if (level == null || level.isClientSide || getBlockState().getValue(WirelessTerminalBlock.POWERED) == powered) return;
        level.setBlock(worldPosition, getBlockState().setValue(WirelessTerminalBlock.POWERED, powered), 3);
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        level.updateNeighborsAt(worldPosition.relative(WirelessTerminalBlock.getAttachedDirection(getBlockState()).getOpposite()), getBlockState().getBlock());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (terminalId != null) tag.putUUID("TerminalId", terminalId);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.putString("Name", name);
        tag.putBoolean("Shared", shared);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        terminalId = tag.hasUUID("TerminalId") ? tag.getUUID("TerminalId") : null;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        name = tag.getString("Name");
        shared = tag.getBoolean("Shared");
    }
}
