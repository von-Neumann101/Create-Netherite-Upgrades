package io.github.boosterproject.booster.content.processing.burner;

import com.simibubi.create.AllTags.AllEntityTags;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
    public Component getName(ItemStack stack) {
        return NetheriteBlazeBurnerData.getCount(stack) == 0
            ? Component.translatable("item.createnetherite.empty_netherite_blaze_burner")
            : super.getName(stack);
    }

}
