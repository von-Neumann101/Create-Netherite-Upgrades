package io.github.boosterproject.booster.client;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlockEntity;
import net.minecraft.core.Direction;

public class PowerfulMechanicalPumpVisual extends SingleAxisRotatingVisual<PowerfulMechanicalPumpBlockEntity> {
    public PowerfulMechanicalPumpVisual(VisualizationContext context, PowerfulMechanicalPumpBlockEntity blockEntity,
                                        float partialTick) {
        super(context, blockEntity, partialTick, Direction.SOUTH,
            Models.partial(BoosterPartialModels.POWERFUL_MECHANICAL_PUMP_COG));
    }
}
