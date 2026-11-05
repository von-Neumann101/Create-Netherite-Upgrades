package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.Booster;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;

/** Passive effects use both active slots; conversion uses the current right-click module. */
public final class WrenchCombat {
    private static final ResourceLocation PUMP_DAMAGE = id("wrench_pump_damage");
    private static final ResourceLocation PUMP_REACH = id("wrench_pump_reach");
    private static final ResourceLocation PUMP_KNOCKBACK = id("wrench_pump_knockback");
    private static final ResourceLocation ROOT = id("wrench_press_root");
    private static final ResourceLocation CONVERTED_ARMOR = id("wrench_converted_armor");
    private static final String CONVERTED = "CreateNetheriteWrenchConverted";
    private static final String TEMPLATE_DROPPED = "CreateNetheriteWrenchTemplateDropped";
    private static final Map<LivingEntity, Root> ROOTS = new WeakHashMap<>();
    private static final Map<LivingEntity, FireOwner> FIRE_OWNERS = new WeakHashMap<>();

    private WrenchCombat() {}

    public static void register(IEventBus bus) {
        bus.addListener(WrenchCombat::attributes);
        bus.addListener(WrenchCombat::damage);
        bus.addListener(WrenchCombat::afterDamage);
        bus.addListener(WrenchCombat::beforeTick);
        bus.addListener(WrenchCombat::afterTick);
        bus.addListener(WrenchCombat::knockback);
        bus.addListener(WrenchCombat::interactSpecific);
        bus.addListener(WrenchCombat::interact);
        bus.addListener(WrenchCombat::drops);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, path);
    }

    private static void attributes(ItemAttributeModifierEvent event) {
        if (!WrenchData.has(event.getItemStack(), WrenchModule.PUMP)) return;
        add(event, ATTACK_DAMAGE, PUMP_DAMAGE, 6);
        add(event, ENTITY_INTERACTION_RANGE, PUMP_REACH, 1);
        add(event, ATTACK_KNOCKBACK, PUMP_KNOCKBACK, 2);
    }

    private static void add(ItemAttributeModifierEvent event, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        event.addModifier(attribute, new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    private static Player meleePlayer(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof Player player
            && source.getDirectEntity() == player ? player : null;
    }

    private static void damage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide || event.getNewDamage() <= 0) return;
        Player attacker = meleePlayer(event.getSource());
        if (attacker != null && WrenchData.has(attacker, WrenchModule.PRESS)) {
            // Scale the one final damage operation, never recursively hurt the entity a second time.
            event.setNewDamage(event.getNewDamage() * 1.5F);
        }
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && playerCausedFire(event.getEntity(), event.getSource())) {
            event.setNewDamage(event.getNewDamage() * 4);
        }
    }

    private static boolean playerCausedFire(LivingEntity target, DamageSource source) {
        if (source.getEntity() instanceof Player player && WrenchData.has(player, WrenchModule.BURNER)) return true;
        // Vanilla ON_FIRE ticks lose their attacker. Only a known player ignition supplies that attribution.
        if (!source.is(DamageTypes.ON_FIRE)) return false;
        FireOwner attribution = FIRE_OWNERS.get(target);
        if (attribution == null || !attribution.valid(target)) return false;
        Player owner = attribution.owner.get();
        return owner != null && owner.isAlive() && !owner.isRemoved() && owner.level() == target.level()
            && WrenchData.has(owner, WrenchModule.BURNER);
    }

    private static void afterDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getNewDamage() <= 0 || !target.isAlive()) return;
        Player melee = meleePlayer(event.getSource());
        if (melee != null && WrenchData.has(melee, WrenchModule.PRESS)) root(target);
        if (!(event.getSource().getEntity() instanceof Player player) || !WrenchData.has(player, WrenchModule.BURNER)) return;
        if (melee != null) {
            var fireAspect = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT);
            int level = EnchantmentHelper.getItemEnchantmentLevel(fireAspect, player.getMainHandItem());
            if (level > 0) FIRE_OWNERS.put(target, new FireOwner(player, target.level().getGameTime() + level * 80L + 2));
        } else if (event.getSource().getDirectEntity() instanceof Projectile projectile && projectile.isOnFire()
            && target.getRemainingFireTicks() > 0) {
            FIRE_OWNERS.put(target, new FireOwner(player, target.level().getGameTime() + target.getRemainingFireTicks() + 2L));
        }
    }

    private static void root(LivingEntity target) {
        ROOTS.put(target, new Root(target.level(), target.level().getGameTime() + 20, target.position()));
        stopAttribute(target, MOVEMENT_SPEED);
        stopAttribute(target, JUMP_STRENGTH);
        target.setDeltaMovement(Vec3.ZERO);
        syncVelocity(target);
    }

    private static void stopAttribute(LivingEntity target, Holder<Attribute> attribute) {
        var value = target.getAttribute(attribute);
        if (value != null && !value.hasModifier(ROOT)) {
            value.addTransientModifier(new AttributeModifier(ROOT, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void releaseRoot(LivingEntity target) {
        ROOTS.remove(target);
        for (var attribute : java.util.List.of(MOVEMENT_SPEED, JUMP_STRENGTH)) {
            var value = target.getAttribute(attribute);
            if (value != null) value.removeModifier(ROOT);
        }
    }

    private static Root currentRoot(LivingEntity target) {
        Root root = ROOTS.get(target);
        if (root != null && (root.level != target.level() || !target.isAlive() || target.level().getGameTime() >= root.until)) {
            releaseRoot(target);
            return null;
        }
        return root;
    }

    private static void beforeTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide) return;
        if (currentRoot(target) != null) target.setDeltaMovement(Vec3.ZERO);
        updateFireOwner(target);
    }

    private static void afterTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide) return;
        Root root = currentRoot(target);
        if (root != null) {
            if (target.position().distanceToSqr(root.position) > 0.000001) {
                // Keep vanilla AI, damage and effect clocks running; constrain only translation.
                target.teleportTo(root.position.x, root.position.y, root.position.z);
            }
            target.setDeltaMovement(Vec3.ZERO);
            syncVelocity(target);
        }
        updateFireOwner(target);
        if (target.isAlive() && target.getPersistentData().getBoolean(CONVERTED)) applyConversion(target);
    }

    private static void syncVelocity(LivingEntity entity) {
        entity.hasImpulse = true;
        if (entity instanceof ServerPlayer player && player.connection != null) {
            player.connection.send(new ClientboundSetEntityMotionPacket(entity));
        }
    }

    private static void knockback(LivingKnockBackEvent event) {
        if (!event.getEntity().level().isClientSide && currentRoot(event.getEntity()) != null) event.setCanceled(true);
    }

    private static void updateFireOwner(LivingEntity target) {
        FireOwner owner = FIRE_OWNERS.get(target);
        if (owner == null) return;
        if (!owner.valid(target)) FIRE_OWNERS.remove(target);
        else owner.remaining = target.getRemainingFireTicks();
    }

    private static boolean canConvert(Player player, InteractionHand hand, Entity target) {
        return hand == InteractionHand.MAIN_HAND && !player.isSpectator() && player.isAlive()
            && WrenchData.active(player) == WrenchModule.CONVERTER && target instanceof LivingEntity living && living.isAlive()
            && !target.isRemoved() && player.level() == target.level() && player.canInteractWithEntity(target, 0) && player.hasLineOfSight(target);
    }

    private static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!canConvert(event.getEntity(), event.getHand(), event.getTarget())) return;
        convert(event.getEntity(), (LivingEntity) event.getTarget());
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    private static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!canConvert(event.getEntity(), event.getHand(), event.getTarget())) return;
        convert(event.getEntity(), (LivingEntity) event.getTarget());
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    private static void convert(Player player, LivingEntity target) {
        if (player.level().isClientSide) return;
        target.getPersistentData().putBoolean(CONVERTED, true);
        applyConversion(target);
    }

    private static void applyConversion(LivingEntity target) {
        var armor = target.getAttribute(ARMOR);
        if (armor != null && !armor.hasModifier(CONVERTED_ARMOR)) {
            armor.addPermanentModifier(new AttributeModifier(CONVERTED_ARMOR, 3, AttributeModifier.Operation.ADD_VALUE));
        }
        if (!target.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0));
        }
    }

    private static void drops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.getPersistentData().getBoolean(CONVERTED)
            || entity.getPersistentData().getBoolean(TEMPLATE_DROPPED)) return;
        entity.getPersistentData().putBoolean(TEMPLATE_DROPPED, true);
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
            new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
    }

    private record Root(Level level, long until, Vec3 position) {}

    private static final class FireOwner {
        final WeakReference<Player> owner;
        final long until;
        int remaining = -1;

        FireOwner(Player owner, long until) {
            this.owner = new WeakReference<>(owner);
            this.until = until;
        }

        boolean valid(LivingEntity target) {
            int current = target.getRemainingFireTicks();
            return current > 0 && target.level().getGameTime() <= until && (remaining < 0 || current <= remaining);
        }
    }
}
