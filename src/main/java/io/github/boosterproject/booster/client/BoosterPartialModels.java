package io.github.boosterproject.booster.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.Booster;
import net.minecraft.resources.ResourceLocation;

public final class BoosterPartialModels {
    public static final PartialModel NETHERITE_GRIP_THIN_SHORT = PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "item/netherite_extendo_grip/thin_short"));
    public static final PartialModel NETHERITE_GRIP_WIDE_SHORT = PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "item/netherite_extendo_grip/wide_short"));
    public static final PartialModel NETHERITE_GRIP_THIN_LONG = PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "item/netherite_extendo_grip/thin_long"));
    public static final PartialModel NETHERITE_GRIP_WIDE_LONG = PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "item/netherite_extendo_grip/wide_long"));
    public static final PartialModel POWERFUL_MECHANICAL_PUMP_COG =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/powerful_mechanical_pump/cog"));
    public static final PartialModel NETHERITE_BOILER_GAUGE =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_boiler/gauge"));
    public static final PartialModel NETHERITE_BOILER_GAUGE_DIAL =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_boiler/gauge_dial"));
    public static final PartialModel NETHERITE_MIXER_HEAD =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_mechanical_mixer/head"));

    private BoosterPartialModels() {
    }

    public static final PartialModel NETHERITE_EJECTOR_TOP =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_weighted_ejector/top"));

    public static void init() {
        // Loads the class early so Flywheel sees the partial model before model baking.
    }
}
