package io.github.boosterproject.booster.content.kinetics.deployer;

import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetheriteDeployerBlock extends DeployerBlock {
    public NetheriteDeployerBlock(Properties properties) { super(properties); }
    @Override public BlockEntityType<? extends DeployerBlockEntity> getBlockEntityType() { return BoosterBlockEntityTypes.NETHERITE_DEPLOYER.get(); }
}
