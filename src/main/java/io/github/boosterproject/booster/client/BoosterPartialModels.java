package io.github.boosterproject.booster.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.Booster;
import net.minecraft.resources.ResourceLocation;

public final class BoosterPartialModels {
    public static final PartialModel POWERFUL_MECHANICAL_PUMP_COG =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/powerful_mechanical_pump/cog"));

    private BoosterPartialModels() {
    }

    public static void init() {
        // Loads the class early so Flywheel sees the partial model before model baking.
    }
}