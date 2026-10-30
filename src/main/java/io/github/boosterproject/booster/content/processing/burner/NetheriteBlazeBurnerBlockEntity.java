package io.github.boosterproject.booster.content.processing.burner;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteBlazeBurnerBlockEntity extends BlazeBurnerBlockEntity implements IHaveGoggleInformation {
    private int blazeCount = 1;
    private int fuelHalfTick;

    public NetheriteBlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getBlazeCount() {
        return blazeCount;
    }

    public void setBlazeCount(int count) {
        blazeCount = NetheriteBlazeBurnerData.clampCount(count);
        if (hasLevel()) {
            notifyUpdate();
        }
    }

    protected int consumeFuelTicks() {
        int halfTicks = fuelHalfTick + blazeCount;
        fuelHalfTick = halfTicks % 2;
        return halfTicks / 2;
    }

    @Override
    public void tick() {
        int transitionDebt = 0;
        if (!level.isClientSide && !isCreative && remainingBurnTime > 0) {
            int ticks = consumeFuelTicks();
            if (activeFuel == FuelType.SPECIAL) {
                transitionDebt = Math.max(0, ticks - remainingBurnTime);
            }
            // The parent still ticks once and subtracts one fuel tick when positive.
            remainingBurnTime = Math.max(0, remainingBurnTime + 1 - ticks);
        }
        super.tick();
        if (transitionDebt > 0 && activeFuel == FuelType.NORMAL) {
            // Pay any final special-fuel fraction from the native normal-heat afterburn.
            remainingBurnTime = Math.max(0, remainingBurnTime - transitionDebt);
            updateBlockState();
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt(NetheriteBlazeBurnerData.BLAZE_COUNT, blazeCount);
        tag.putInt("FuelHalfTick", fuelHalfTick);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putInt(NetheriteBlazeBurnerData.BLAZE_COUNT, blazeCount);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        blazeCount = NetheriteBlazeBurnerData.readCount(tag);
        fuelHalfTick = Mth.clamp(tag.getInt("FuelHalfTick"), 0, 1);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("tooltip.createnetherite.netherite_blaze_burner.count", blazeCount)
            .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.createnetherite.netherite_blaze_burner.rates",
            blazeCount, blazeCount / 2F).withStyle(ChatFormatting.GRAY));
        return true;
    }
}
