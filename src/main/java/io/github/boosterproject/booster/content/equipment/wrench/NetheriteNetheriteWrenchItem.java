package io.github.boosterproject.booster.content.equipment.wrench;

import com.simibubi.create.AllItems;
import io.github.boosterproject.booster.content.equipment.HandheldKineticPower;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class NetheriteNetheriteWrenchItem extends SwordItem {
    public NetheriteNetheriteWrenchItem(Properties properties) {
        super(Tiers.NETHERITE, properties.fireResistant().stacksTo(1)
            .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.4F))
            .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }
    @Override public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {}
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) return InteractionResult.PASS;
        var module = WrenchData.active(stack);
        if (module == WrenchModule.ENGINE) return HandheldKineticPower.begin(context);
        if (module == WrenchModule.FAN || module == WrenchModule.EJECTOR || module == WrenchModule.TANK)
            return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
        return InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return AllItems.WRENCH.get().useOn(context);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        var module = WrenchData.active(stack);
        if (module == WrenchModule.FAN) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (module == WrenchModule.EJECTOR) {
            if (!level.isClientSide) WrenchLaunch.start(player);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (module == WrenchModule.TANK) return WrenchTank.use(level, player, hand);
        return InteractionResultHolder.pass(stack);
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (player.getUsedItemHand() != InteractionHand.MAIN_HAND || player.getMainHandItem() != stack) {
            player.stopUsingItem(); return;
        }
        var module = WrenchData.active(stack);
        if (module == WrenchModule.FAN) WrenchAirflow.tick(player, stack);
        else if (module == WrenchModule.ENGINE) HandheldKineticPower.continueUsing(player, stack, getUseDuration(stack, entity) - remaining);
        else player.stopUsingItem();
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide && entity instanceof Player player) HandheldKineticPower.stop(player);
    }
}
