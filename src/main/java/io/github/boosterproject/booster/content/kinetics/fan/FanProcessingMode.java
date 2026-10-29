package io.github.boosterproject.booster.content.kinetics.fan;

import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum FanProcessingMode {
    WASHING("washing", 0xFF579EE8),
    BLASTING("blasting", 0xFFE68B3D),
    HAUNTING("haunting", 0xFF51CBC4),
    SMOKING("smoking", 0xFFD66B59);

    private final String name;
    private final int color;

    FanProcessingMode(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String serializedName() { return name; }

    public Component displayName() {
        return Component.translatable("processing.createnetherite." + name);
    }

    public int color() { return color; }

    public ItemStack icon() {
        return new ItemStack(switch (this) {
            case WASHING -> Items.WATER_BUCKET;
            case BLASTING -> Items.LAVA_BUCKET;
            case HAUNTING -> Items.SOUL_CAMPFIRE;
            case SMOKING -> Items.CAMPFIRE;
        });
    }

    public FanProcessingType processingType() {
        return switch (this) {
            case WASHING -> AllFanProcessingTypes.SPLASHING;
            case BLASTING -> AllFanProcessingTypes.BLASTING;
            case HAUNTING -> AllFanProcessingTypes.HAUNTING;
            case SMOKING -> AllFanProcessingTypes.SMOKING;
        };
    }

    public static FanProcessingMode byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : WASHING;
    }

    public static FanProcessingMode byName(String name) {
        for (FanProcessingMode mode : values()) {
            if (mode.name.equals(name)) return mode;
        }
        return WASHING;
    }
}
