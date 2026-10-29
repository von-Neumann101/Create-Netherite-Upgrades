package io.github.boosterproject.booster.content.kinetics.fan;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class NetheriteAirCurrent extends AirCurrent {
    private final NetheriteEncasedFanBlockEntity fan;

    public NetheriteAirCurrent(NetheriteEncasedFanBlockEntity fan) {
        super(fan);
        this.fan = fan;
    }

    @Nullable
    @Override
    public FanProcessingType getTypeAt(float offset) {
        return maxDistance > 0 && offset >= 0 && offset <= maxDistance
            ? fan.getProcessingMode().processingType() : null;
    }

    @Override
    public void tickAffectedHandlers() {
        if (source.getSpeed() == 0) {
            return;
        }
        for (Pair<TransportedItemStackHandlerBehaviour, FanProcessingType> pair : affectedItemHandlers) {
            TransportedItemStackHandlerBehaviour handler = pair.getKey();
            Level world = handler.getWorld();
            handler.handleProcessingOnAllItems(transported -> {
                FanProcessingType processingType = fan.resolveProcessingMode(transported.stack).processingType();
                if (world.isClientSide) {
                    processingType.spawnProcessingParticles(world, handler.getWorldPositionOf(transported));
                    return TransportedResult.doNothing();
                }
                for (int step = 0; step < fan.getProcessingSpeedMultiplier(); step++) {
                    TransportedResult result = FanProcessing.applyProcessing(transported, world, processingType);
                    if (!result.doesNothing()) {
                        fan.award(AllAdvancements.FAN_PROCESSING);
                        return result;
                    }
                }
                return TransportedResult.doNothing();
            });
        }
    }
}
