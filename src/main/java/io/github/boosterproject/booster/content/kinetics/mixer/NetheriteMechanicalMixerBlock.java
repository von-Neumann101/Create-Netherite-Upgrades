package io.github.boosterproject.booster.content.kinetics.mixer;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetheriteMechanicalMixerBlock extends MechanicalMixerBlock {
    public NetheriteMechanicalMixerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends MechanicalMixerBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_MECHANICAL_MIXER.get();
    }
}
