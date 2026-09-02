package io.github.boosterproject.booster.content.kinetics.press;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlock;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetheriteMechanicalPressBlock extends MechanicalPressBlock {
    public NetheriteMechanicalPressBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Class<MechanicalPressBlockEntity> getBlockEntityClass() {
        return (Class) NetheriteMechanicalPressBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalPressBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_MECHANICAL_PRESS.get();
    }
}
