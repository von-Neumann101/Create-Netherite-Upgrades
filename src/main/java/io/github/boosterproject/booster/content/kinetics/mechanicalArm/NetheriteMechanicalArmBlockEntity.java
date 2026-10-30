package io.github.boosterproject.booster.content.kinetics.mechanicalArm;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NetheriteMechanicalArmBlockEntity extends ArmBlockEntity {
    public NetheriteMechanicalArmBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_ARM.get(), pos, state);
    }

    public int getWorkingRange() {
        return NetheriteArmBehavior.workRange(ArmBlockEntity.getRange());
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(getWorkingRange());
    }
}
