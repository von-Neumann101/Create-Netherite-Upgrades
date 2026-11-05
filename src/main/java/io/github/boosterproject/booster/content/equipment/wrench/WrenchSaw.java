package io.github.boosterproject.booster.content.equipment.wrench;

import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import io.github.boosterproject.booster.content.kinetics.saw.NetheriteSawBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** The hand-held saw uses the machine's damage and vanilla hurt interval. */
public final class WrenchSaw {
    public static final float RPM = 256;
    private WrenchSaw() {}

    public static void register(IEventBus bus) {
        bus.addListener(WrenchSaw::interactSpecific);
        bus.addListener(WrenchSaw::interact);
    }

    private static boolean canStart(Player player, InteractionHand hand, net.minecraft.world.entity.Entity target) {
        return hand == InteractionHand.MAIN_HAND && player.isAlive() && !player.isSpectator()
            && WrenchData.active(player) == WrenchModule.SAW && target instanceof LivingEntity living && living.isAlive()
            && target.level() == player.level() && player.canInteractWithEntity(target, 0) && player.hasLineOfSight(target);
    }

    private static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!canStart(event.getEntity(), event.getHand(), event.getTarget())) return;
        event.getEntity().startUsingItem(InteractionHand.MAIN_HAND);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    private static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!canStart(event.getEntity(), event.getHand(), event.getTarget())) return;
        event.getEntity().startUsingItem(InteractionHand.MAIN_HAND);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    public static void tick(Player player, ItemStack wrench) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()
            || !player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND
            || player.getMainHandItem() != wrench || player.getUseItem() != wrench
            || WrenchData.active(wrench) != WrenchModule.SAW) return;
        var start = player.getEyePosition();
        var direction = player.getViewVector(1);
        double range = player.entityInteractionRange();
        var end = start.add(direction.scale(range));
        var wall = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() == HitResult.Type.BLOCK) end = wall.getLocation();
        double distance = start.distanceToSqr(end);
        if (distance <= 0) return;
        var hit = ProjectileUtil.getEntityHitResult(player, start, end,
            player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1),
            target -> target instanceof LivingEntity living && living.isAlive() && target.isPickable() && !target.isSpectator(), distance);
        if (hit == null) return;
        var source = new DamageSource(CreateDamageSources.saw(player.level()).typeHolder(), player);
        // Do not reset invulnerableTime: repeated ticks have the machine saw's normal cadence.
        hit.getEntity().hurt(source, NetheriteSawBlock.damage(RPM));
    }
}
