package io.github.boosterproject.booster.content.processing.burner;

import com.simibubi.create.AllTags.AllEntityTags;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class NetheriteBlazeBurnerItem extends BlockItem {
    public NetheriteBlazeBurnerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack held, Player player, LivingEntity entity,
                                                  InteractionHand hand) {
        if (!AllEntityTags.BLAZE_BURNER_CAPTURABLE.matches(entity)) {
            return InteractionResult.PASS;
        }
        ItemStack filled = NetheriteBlazeBurnerData.captureOne(held);
        if (filled.isEmpty()) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable(
                    "message.createnetherite.netherite_blaze_burner.full"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide) {
            ItemStack inventoryResult = NetheriteBlazeBurnerData.finishCapture(
                held, player.getItemInHand(hand), filled, player.isCreative());
            if (!inventoryResult.isEmpty()) {
                player.getInventory().placeItemBackInInventory(inventoryResult);
            }
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.BLAZE_HURT,
                SoundSource.HOSTILE, .25F, .75F);
            entity.discard();
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int count = NetheriteBlazeBurnerData.getCount(stack);
        tooltip.add(Component.translatable("tooltip.createnetherite.netherite_blaze_burner.count", count)
            .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.createnetherite.netherite_blaze_burner.rates", count, count / 2F)
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.createnetherite.netherite_blaze_burner.capture")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
