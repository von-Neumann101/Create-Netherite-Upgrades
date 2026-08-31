package io.github.boosterproject.booster.content.contraptions.elevator;

import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteElevatorPulleyBlockEntity extends ElevatorPulleyBlockEntity {
    public static final int SPEED_MULTIPLIER = 2;
    public static final int RANGE_MULTIPLIER = 2;

    public NetheriteElevatorPulleyBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_ELEVATOR_PULLEY.get(), pos, state);
    }

    @Override
    protected int getExtensionRange() {
        return Math.max(0, Math.min(
                super.getExtensionRange() * RANGE_MULTIPLIER,
                getBlockPos().getY() - 1 - getLevel().getMinBuildHeight()
            ));
    }
}
