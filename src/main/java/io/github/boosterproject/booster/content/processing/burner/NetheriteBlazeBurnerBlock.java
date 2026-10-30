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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
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
        return stackWithCount(state, level.getBlockEntity(pos));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        HeatLevel heat = NetheriteBlazeBurnerData.getCount(context.getItemInHand()) == 0
            ? HeatLevel.NONE : HeatLevel.SMOULDERING;
        return defaultBlockState().setValue(HEAT_LEVEL, heat)
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // The native empty-cage flint-and-steel action replaces it with a regular Create block.
        if (state.getValue(HEAT_LEVEL) == HeatLevel.NONE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
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
        for (ItemStack drop : drops) {
            if (drop.is(asItem())) {
                NetheriteBlazeBurnerData.setCount(drop, getBlazeCount(state, be));
            }
        }
        return drops;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        return new ItemRequirement(new BlazeCountRequirement(stackWithCount(state, blockEntity)));
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

    static int getBlazeCount(BlockState state, BlockEntity be) {
        if (state.getValue(HEAT_LEVEL) == HeatLevel.NONE) {
            return 0;
        }
        return be instanceof NetheriteBlazeBurnerBlockEntity burner ? burner.getBlazeCount() : 1;
    }

    private static ItemStack stackWithCount(BlockState state, BlockEntity be) {
        ItemStack stack = new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get());
        NetheriteBlazeBurnerData.setCount(stack, getBlazeCount(state, be));
        return stack;
    }

    public static float getBoilerHeat(Level level, BlockPos pos, BlockState state) {
        int count = getBlazeCount(state, level.getBlockEntity(pos));
        return NetheriteBlazeBurnerData.scaleHeat(BoilerHeater.BLAZE_BURNER.getHeat(level, pos, state), count);
    }
}
