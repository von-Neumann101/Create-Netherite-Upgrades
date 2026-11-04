package io.github.boosterproject.booster.content.equipment;

import io.github.boosterproject.booster.Booster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class NetheriteNetheriteItem extends Item {
    public static final ResourceLocation TEETH_ADVANCEMENT = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "are_your_teeth_ok");
    private static final String CHEW_STARTED = "CreateNetheriteChewStarted";

    public NetheriteNetheriteItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isUsingItem()) {
            player.startUsingItem(hand);
            if (!level.isClientSide && player.isUsingItem()) player.getPersistentData().putLong(CHEW_STARTED, level.getGameTime());
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) { return stack; }

    public static void keepChewing(LivingEntityUseItemEvent.Tick event) {
        // Keep the native 32-tick eating motion cycling without completing or restarting use.
        if (event.getItem().getItem() instanceof NetheriteNetheriteItem && event.getDuration() == 1)
            event.setDuration(33);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player) || !player.isUsingItem() || !player.getPersistentData().contains(CHEW_STARTED)) return;
        if (level.getGameTime() - player.getPersistentData().getLong(CHEW_STARTED) <= 200) return;
        var advancement = player.server.getAdvancements().get(TEETH_ADVANCEMENT);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "chew");
            player.getPersistentData().remove(CHEW_STARTED);
        }
    }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide) entity.getPersistentData().remove(CHEW_STARTED);
    }
}
