package io.github.boosterproject.booster.content.processing.basin;

import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetheriteBasinBlock extends BasinBlock {
    public NetheriteBasinBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntityType<? extends BasinBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_BASIN.get();
    }
}
