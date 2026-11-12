package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum WrenchModule {
    PUMP(() -> BoosterItems.POWERFUL_MECHANICAL_PUMP.get()),
    FAN(() -> BoosterItems.NETHERITE_ENCASED_FAN.get()),
    EJECTOR(() -> BoosterItems.NETHERITE_WEIGHTED_EJECTOR.get()),
    BURNER(() -> BoosterItems.NETHERITE_BLAZE_BURNER.get()),
    PRESS(() -> BoosterItems.NETHERITE_MECHANICAL_PRESS.get()),
    SAW(() -> BoosterItems.NETHERITE_MECHANICAL_SAW.get()),
    ENGINE(() -> BoosterItems.NETHERITE_STEAM_ENGINE.get()),
    CONVERTER(() -> BoosterItems.NETHERITE_CONVERTER.get()),
    TANK(() -> BoosterItems.NETHERITE_FLUID_TANK.get());

    private final Supplier<Item> item;
    WrenchModule(Supplier<Item> item) { this.item = item; }
    public Item item() { return item.get(); }
    public boolean usesRightClick() { return this != PUMP && this != BURNER && this != PRESS; }
    public boolean conflictsWith(WrenchModule other) {
        boolean fanSaw = this == FAN && other == SAW || this == SAW && other == FAN;
        return usesRightClick() && other.usesRightClick() && !fanSaw;
    }
    public static WrenchModule fromStack(ItemStack stack) {
        for (var module : values()) if (stack.is(module.item())) return module;
        return null;
    }
}
