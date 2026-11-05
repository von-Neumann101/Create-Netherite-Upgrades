package io.github.boosterproject.booster.content.kinetics.saw;

import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NetheriteSawBlock extends SawBlock {
    public static final float DAMAGE_MULTIPLIER = 1.8F;
    public NetheriteSawBlock(Properties properties) { super(properties); }
    public static float damage(float rpm) { return (float) DrillBlock.getDamage(rpm) * DAMAGE_MULTIPLIER; }
    @Override public BlockEntityType<? extends SawBlockEntity> getBlockEntityType() { return BoosterBlockEntityTypes.NETHERITE_MECHANICAL_SAW.get(); }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity instanceof ItemEntity || !new AABB(pos).deflate(.1F).intersects(entity.getBoundingBox())) return;
        withBlockEntityDo(level, pos, saw -> {
            if (saw.getSpeed() != 0) entity.hurt(CreateDamageSources.saw(level), damage(saw.getSpeed()));
        });
    }
}
