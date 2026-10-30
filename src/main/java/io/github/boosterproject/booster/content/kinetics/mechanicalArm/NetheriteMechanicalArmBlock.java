package io.github.boosterproject.booster.content.kinetics.mechanicalArm;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetheriteMechanicalArmBlock extends ArmBlock {
    public NetheriteMechanicalArmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends ArmBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_MECHANICAL_ARM.get();
    }
}
