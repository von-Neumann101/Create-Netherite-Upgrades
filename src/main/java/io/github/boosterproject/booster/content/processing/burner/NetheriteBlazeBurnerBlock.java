package io.github.boosterproject.booster.content.processing.burner;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public class NetheriteBlazeBurnerBlock extends BlazeBurnerBlock {
    public static final int HEAT_MULTIPLIER = 3;

    public NetheriteBlazeBurnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_BLAZE_BURNER.get();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
                                       BlockPos pos, Player player) {
        return new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get());
    }
}
