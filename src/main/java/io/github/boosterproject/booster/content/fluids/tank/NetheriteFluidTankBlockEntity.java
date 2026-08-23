package io.github.boosterproject.booster.content.fluids.tank;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import io.github.boosterproject.booster.config.BoosterConfigs;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class NetheriteFluidTankBlockEntity extends FluidTankBlockEntity {
    public static final int DEFAULT_CAPACITY_MULTIPLIER = 16;

    public NetheriteFluidTankBlockEntity(BlockPos pos, BlockState state) {
        this(BoosterBlockEntityTypes.NETHERITE_FLUID_TANK.get(), pos, state);
    }

    public NetheriteFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        boiler = new NetheriteBoilerData();
    }

    public static int getNetheriteCapacityMultiplierValue() {
        return safeCapacity(FluidTankBlockEntity.getCapacityMultiplier(), getConfiguredCapacityMultiplier());
    }

    public int getNetheriteCapacityMultiplier() {
        return getNetheriteCapacityMultiplierValue();
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    @Override
    protected SmartFluidTank createInventory() {
        return new SmartFluidTank(getNetheriteCapacityMultiplier(), this::onFluidStackChanged);
    }

    @Override
    public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(getCapacityForBlocks(blocks));
        drainOverflow();
        forceFluidLevelUpdate = true;
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag tankContent = compound.contains("TankContent")
            ? compound.getCompound("TankContent").copy()
            : null;

        super.read(compound, registries, clientPacket);

        if (!isController()) {
            return;
        }

        tankInventory.setCapacity(getCapacityForBlocks(getTotalTankSize()));
        if (tankContent != null) {
            tankInventory.readFromNBT(registries, tankContent);
        }
        drainOverflow();
        forceFluidLevelUpdate = true;

        if (clientPacket || compound.contains("ForceFluidLevel")) {
            setFluidLevel(LerpedFloat.linear().startWithValue(getFillState()));
        }
    }

    @Override
    public int getTankSize(int tank) {
        return getNetheriteCapacityMultiplier();
    }

    private int getCapacityForBlocks(int blocks) {
        return safeCapacity(getNetheriteCapacityMultiplier(), Math.max(1, blocks));
    }

    public static int getConfiguredCapacityMultiplier() {
        return Math.max(1, BoosterConfigs.SERVER.netheriteFluidTankCapacityMultiplier.get());
    }

    private static int safeCapacity(int base, int multiplier) {
        long capacity = (long) base * multiplier;
        if (capacity > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) capacity);
    }

    private void drainOverflow() {
        int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
        if (overflow > 0) {
            tankInventory.drain(overflow, FluidAction.EXECUTE);
        }
    }
}
