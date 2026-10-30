package io.github.boosterproject.booster.content.kinetics.mechanicalArm;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.createmod.catnip.nbt.NBTHelper;

public final class NetheriteArmBehavior {
    public static final float MOVEMENT_MULTIPLIER = 3;
    public static final double STRESS_MULTIPLIER = 1.5;
    public static final int RANGE_MULTIPLIER = 3;
    private NetheriteArmBehavior() {}

    public static float movementSpeed(float vanillaLimit, float rpm) {
        return Math.min(vanillaLimit, Math.abs(rpm)) * MOVEMENT_MULTIPLIER;
    }

    public static double stressImpact(double vanillaImpact) {
        return vanillaImpact * STRESS_MULTIPLIER;
    }

    public static int workRange(int vanillaRange) {
        // Native initialization adds one to this radius before checking loaded chunks.
        return (int) Math.min(Integer.MAX_VALUE - 1L, (long) vanillaRange * RANGE_MULTIPLIER);
    }

    public static boolean isWithinRange(BlockPos relativeTarget, int range) {
        return relativeTarget.closerThan(BlockPos.ZERO, range);
    }

    public static boolean isWithinRange(CompoundTag pointTag, int range) {
        return isWithinRange(NBTHelper.readBlockPos(pointTag, "Pos"), range);
    }
}
