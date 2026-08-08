package io.github.boosterproject.booster.content.fluids.tank;

import com.simibubi.create.content.fluids.tank.BoilerData;
import net.minecraft.util.Mth;

public class NetheriteBoilerData extends BoilerData {
    public static final int WATER_SUPPLY_PER_LEVEL = 12;

    @Override
    public int getMaxHeatLevelForWaterSupply() {
        return (int) Math.min(18, Mth.ceil(waterSupply) / WATER_SUPPLY_PER_LEVEL);
    }
}
