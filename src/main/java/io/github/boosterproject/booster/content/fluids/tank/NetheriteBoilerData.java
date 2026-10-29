package io.github.boosterproject.booster.content.fluids.tank;

import com.simibubi.create.content.fluids.tank.BoilerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;

public class NetheriteBoilerData extends BoilerData {
    public static final int WATER_SUPPLY_PER_LEVEL = 12;

    @Override
    public int getMaxHeatLevelForBoilerSize(int boilerSize) {
        return boilerSize / 4;
    }

    @Override
    public int getMaxHeatLevelForWaterSupply() {
        return Mth.ceil(waterSupply) / WATER_SUPPLY_PER_LEVEL;
    }

    @Override
    public MutableComponent getHeatLevelTextComponent() {
        MutableComponent levelText = super.getHeatLevelTextComponent();
        // Create labels level 18 as the maximum; netherite boilers can exceed it.
        if (levelText.getContents() instanceof TranslatableContents contents
            && contents.getKey().equals("create.boiler.max_lvl")) {
            return Component.translatable("create.boiler.lvl", "18");
        }
        return levelText;
    }
}
