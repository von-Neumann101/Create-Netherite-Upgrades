package io.github.boosterproject.booster.content.kinetics.saw;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteSawBlockEntity extends SawBlockEntity {
    public NetheriteSawBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_SAW.get(), pos, state);
        inventory.withSlotLimit(false);
    }
    @Override public void start(ItemStack inserted) {
        // Native start reads the real inventory for recipes; this argument only scales batch time.
        super.start(inserted.isEmpty() ? inserted : inserted.copyWithCount(1));
    }
    @Override protected float getBreakSpeed() { return super.getBreakSpeed() * 2; }
}
