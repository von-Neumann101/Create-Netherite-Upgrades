package io.github.boosterproject.booster.content.equipment;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.boosterproject.booster.Booster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class NetheriteExtendoGripItem extends ExtendoGripItem {
    private static final ResourceLocation EXTRA_RANGE = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "extendo_extra_range");

    public NetheriteExtendoGripItem(Properties properties) { super(properties); }

    public static boolean isGrip(ItemStack stack) { return stack.getItem() instanceof NetheriteExtendoGripItem; }

    public static void updateExtraRange(Player player) {
        int count = (isGrip(player.getMainHandItem()) ? 1 : 0) + (isGrip(player.getOffhandItem()) ? 1 : 0);
        var range = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var previous = range.getModifier(EXTRA_RANGE);
        if (previous != null && previous.amount() == count * 3) return;
        range.removeModifier(EXTRA_RANGE);
        if (count > 0) range.addTransientModifier(new AttributeModifier(EXTRA_RANGE, count * 3, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void consumeAction(Player player, ItemStack stack, EquipmentSlot slot) {
        if (!player.level().isClientSide && !BacktankUtil.canAbsorbDamage(player, AllConfigs.server().equipment.maxExtendoGripActions.get()))
            stack.hurtAndBreak(1, player, slot);
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return HandheldKineticPower.begin(context);
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && entity instanceof Player player) HandheldKineticPower.continueUsing(player, stack, getUseDuration(stack, entity) - remaining);
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide && entity instanceof Player player) HandheldKineticPower.stop(player);
    }
}
