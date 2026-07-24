package io.github.boosterproject.booster.content.fluids.pump;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import io.github.boosterproject.booster.config.BoosterConfigs;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

public final class BoosterPumpPressure {
    private static final Logger LOGGER = LogUtils.getLogger();

    /*
     * FluidNetwork derives transferSpeed from pressure / 2. 4096 pressure still
     * permits 2048 mB/t, well above the default 8x boost at Create's usual max
     * 256 RPM, while preventing extreme configs from causing excessive per-tick
     * drain/fill work or float overflow.
     */
    private static final float MAX_EFFECTIVE_PRESSURE = 4096.0F;
    private static final boolean DEBUG_PRESSURE =
        !FMLLoader.isProduction() && Boolean.getBoolean("createnetherite.debugPumpPressure");

    private BoosterPumpPressure() {
    }

    public static float scalePressure(SmartBlockEntity blockEntity, float basePressure, PressureWriteTarget target) {
        if (!(blockEntity instanceof PumpBlockEntity pump)) {
            return basePressure;
        }
        return scalePressure(pump, basePressure, target);
    }

    public static float scalePressure(PumpBlockEntity pump, float basePressure, PressureWriteTarget target) {
        String pumpType = pump instanceof PowerfulMechanicalPumpBlockEntity ? "powerful pump" : "vanilla pump";
        if (!(pump instanceof PowerfulMechanicalPumpBlockEntity)) {
            logPressure(pump, pumpType, target, basePressure, 1.0D, basePressure);
            return basePressure;
        }

        if (pump.getLevel() != null && pump.getLevel().isClientSide && !pump.isVirtual()) {
            return basePressure;
        }

        if (!Float.isFinite(basePressure) || basePressure <= 0) {
            logPressure(pump, pumpType, target, basePressure, 1.0D, 0);
            return 0;
        }

        double multiplier = BoosterConfigs.SERVER.powerfulPumpPressureMultiplier.get();
        if (!Double.isFinite(multiplier) || multiplier <= 0) {
            multiplier = 1.0D;
        }

        double effectivePressure = basePressure * multiplier;
        if (!Double.isFinite(effectivePressure) || effectivePressure < 0) {
            effectivePressure = MAX_EFFECTIVE_PRESSURE;
        }

        float clampedPressure = (float) Math.min(effectivePressure, MAX_EFFECTIVE_PRESSURE);
        logPressure(pump, pumpType, target, basePressure, multiplier, clampedPressure);
        return clampedPressure;
    }

    private static void logPressure(PumpBlockEntity pump, String pumpType, PressureWriteTarget target, float basePressure,
                                    double multiplier, float effectivePressure) {
        if (!DEBUG_PRESSURE || pump.getLevel() == null || pump.getLevel().isClientSide) {
            return;
        }

        LOGGER.info(
            "Pump pressure debug: type={}, target={}, pos={}, speed={}, multiplier={}, basePressure={}, effectivePressure={}",
            pumpType,
            target.logName,
            pump.getBlockPos(),
            pump.getSpeed(),
            multiplier,
            basePressure,
            effectivePressure
        );
    }

    public enum PressureWriteTarget {
        REMOTE_NETWORK("remote_network"),
        PUMP_INTERFACE("pump_interface");

        private final String logName;

        PressureWriteTarget(String logName) {
            this.logName = logName;
        }
    }
}