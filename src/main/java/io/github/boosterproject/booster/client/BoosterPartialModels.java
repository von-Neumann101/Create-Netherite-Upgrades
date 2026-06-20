package io.github.boosterproject.booster.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.Booster;
import net.minecraft.resources.ResourceLocation;

public final class BoosterPartialModels {
    public static final PartialModel POWERFUL_MECHANICAL_PUMP_COG =
        PartialModel.of(new ResourceLocation(Booster.MOD_ID, "block/powerful_mechanical_pump/cog"));

    private BoosterPartialModels() {
    }
}
