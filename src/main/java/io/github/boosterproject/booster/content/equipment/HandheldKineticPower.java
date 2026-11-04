package io.github.boosterproject.booster.content.equipment;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Direct hand power for one working block; no torque network or persistent generator is created. */
public final class HandheldKineticPower {
    public static final float RPM = 16;
    private static final String TARGET = "CreateNetheriteHandPower";
    private HandheldKineticPower() {}

    public interface Target {
        boolean booster$claimPower(Player player, float speed);
        boolean booster$isDrivenBy(Player player);
        void booster$refreshPower();
        void booster$stopPower();
    }

    public static float capacityPerRpm() { return (float) BlockStressValues.getImpact(BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get()); }

    private static KineticBlockEntity selected(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(TARGET);
        if (!tag.getString("Dimension").equals(player.level().dimension().location().toString())) return null;
        BlockPos pos = BlockPos.of(tag.getLong("Position"));
        return player.level().hasChunkAt(pos) && player.level().getBlockEntity(pos) instanceof KineticBlockEntity be ? be : null;
    }

    private static boolean canDrive(Player player, KineticBlockEntity be) {
        return !player.isSpectator() && player.isAlive() && player.getAbilities().mayBuild && !be.isRemoved()
            && !(be instanceof GeneratingKineticBlockEntity) && be.getTheoreticalSpeed() == 0 && !be.isSource()
            && be.calculateStressApplied() <= capacityPerRpm() && player.canInteractWithBlock(be.getBlockPos(), 0)
            && player.level().mayInteract(player, be.getBlockPos());
    }

    public static boolean isStillDriving(Player player, KineticBlockEntity be) {
        if (player.level() != be.getLevel() || !player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND
            || !NetheriteExtendoGripItem.isGrip(player.getMainHandItem()) || selected(player) != be || !canDrive(player, be)) return false;
        var eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && ((BlockHitResult) hit).getBlockPos().equals(be.getBlockPos());
    }

    public static InteractionResult begin(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        if (context.getHand() != InteractionHand.MAIN_HAND || player == null
            || !(level.getBlockEntity(context.getClickedPos()) instanceof KineticBlockEntity be)) return InteractionResult.PASS;
        if (!canDrive(player, be)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("grip.createnetherite.cannot_power", capacityPerRpm() * RPM), true);
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            stop(player);
            var target = (Target) be;
            if (!target.booster$claimPower(player, 0)) return InteractionResult.FAIL;
            NetheriteExtendoGripItem.consumeAction(player, context.getItemInHand(), EquipmentSlot.MAINHAND);
            if (context.getItemInHand().isEmpty()) return InteractionResult.FAIL;
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", level.dimension().location().toString());
            tag.putLong("Position", be.getBlockPos().asLong());
            player.getPersistentData().put(TARGET, tag);
            target.booster$claimPower(player, player.isShiftKeyDown() ? -RPM : RPM);
        }
        player.startUsingItem(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    public static void continueUsing(Player player, ItemStack stack, int elapsed) {
        var be = selected(player);
        if (be == null || !isStillDriving(player, be) || !((Target) be).booster$isDrivenBy(player)) {
            stop(player); player.stopUsingItem(); return;
        }
        if (elapsed > 0 && elapsed % 20 == 0) NetheriteExtendoGripItem.consumeAction(player, stack, EquipmentSlot.MAINHAND);
        if (stack.isEmpty()) { stop(player); player.stopUsingItem(); return; }
        ((Target) be).booster$refreshPower();
    }

    public static void stop(Player player) {
        var be = selected(player);
        player.getPersistentData().remove(TARGET);
        if (be instanceof Target target && target.booster$isDrivenBy(player)) target.booster$stopPower();
    }
}
