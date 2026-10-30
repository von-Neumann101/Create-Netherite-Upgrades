package io.github.boosterproject.booster.content.processing.burner;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import java.util.List;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class NetheriteBlazeBurnerBlock extends BlazeBurnerBlock {
    public NetheriteBlazeBurnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_BLAZE_BURNER.get();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level,
                                       BlockPos pos, Player player) {
        return stackWithCount(level.getBlockEntity(pos));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HEAT_LEVEL, HeatLevel.SMOULDERING)
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof NetheriteBlazeBurnerBlockEntity burner) {
            burner.setBlazeCount(NetheriteBlazeBurnerData.getCount(stack));
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof NetheriteBlazeBurnerBlockEntity burner) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    NetheriteBlazeBurnerData.setCount(drop, burner.getBlazeCount());
                }
            }
        }
        return drops;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        return new ItemRequirement(new BlazeCountRequirement(stackWithCount(blockEntity)));
    }

    public static class BlazeCountRequirement extends ItemRequirement.StackRequirement {
        public BlazeCountRequirement(ItemStack stack) {
            super(stack, ItemRequirement.ItemUseType.CONSUME);
        }

        @Override
        public boolean matches(ItemStack other) {
            return super.matches(other)
                && NetheriteBlazeBurnerData.getCount(stack) == NetheriteBlazeBurnerData.getCount(other);
        }
    }

    private static ItemStack stackWithCount(BlockEntity be) {
        ItemStack stack = new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get());
        if (be instanceof NetheriteBlazeBurnerBlockEntity burner) {
            NetheriteBlazeBurnerData.setCount(stack, burner.getBlazeCount());
        }
        return stack;
    }

    public static float getBoilerHeat(Level level, BlockPos pos, BlockState state) {
        int count = level.getBlockEntity(pos) instanceof NetheriteBlazeBurnerBlockEntity burner
            ? burner.getBlazeCount() : 1;
        return NetheriteBlazeBurnerData.scaleHeat(BoilerHeater.BLAZE_BURNER.getHeat(level, pos, state), count);
    }
}
