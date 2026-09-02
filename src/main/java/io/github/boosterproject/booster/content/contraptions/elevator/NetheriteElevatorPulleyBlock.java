package io.github.boosterproject.booster.content.contraptions.elevator;

import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetheriteElevatorPulleyBlock extends ElevatorPulleyBlock {
    public NetheriteElevatorPulleyBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Class<ElevatorPulleyBlockEntity> getBlockEntityClass() {
        return (Class) NetheriteElevatorPulleyBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElevatorPulleyBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_ELEVATOR_PULLEY.get();
    }
}
