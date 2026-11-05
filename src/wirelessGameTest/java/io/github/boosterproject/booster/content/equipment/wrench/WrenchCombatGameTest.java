package io.github.boosterproject.booster.content.equipment.wrench;

import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.Booster;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchCombatGameTest {
    private static ItemStack wrench(WrenchModule module) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_netherite_wrench"));
        var stack = new ItemStack(item);
        var modules = new ArrayList<ItemStack>();
        for (int i = 0; i < 8; i++) modules.add(i == 0 ? new ItemStack(module.item()) : ItemStack.EMPTY);
        WrenchData.setModules(stack, modules);
        WrenchData.setActiveSlot(stack, 0);
        return stack;
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper helper, WrenchModule module) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "WrenchCombatQA"));
        player.setPos(helper.absolutePos(new BlockPos(1, 300, 1)).getCenter());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench(module));
        return player;
    }

    private static Cow cow(GameTestHelper helper) {
        var cow = EntityType.COW.create(helper.getLevel());
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        cow.setPos(helper.absolutePos(new BlockPos(2, 300, 1)).getCenter());
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void pumpModifiersUseMainHandOnlyAndDeactivate(GameTestHelper helper) {
        var stack = wrench(WrenchModule.PUMP);
        double[] amounts = new double[3];
        stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) amounts[0] += modifier.amount();
            if (attribute.equals(Attributes.ENTITY_INTERACTION_RANGE)) amounts[1] += modifier.amount();
            if (attribute.equals(Attributes.ATTACK_KNOCKBACK)) amounts[2] += modifier.amount();
        });
        helper.assertTrue(amounts[0] == 15 && amounts[1] == 1 && amounts[2] == 2,
            "pump adds six damage to wrench modifier nine, one reach and two knockback");
        stack.getAttributeModifiers().forEach(EquipmentSlot.OFFHAND, (attribute, modifier) -> helper.fail("wrench combat modifiers must not affect the offhand"));
        WrenchData.setActiveSlot(stack, -1);
        double[] inactiveDamage = new double[1];
        stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) inactiveDamage[0] += modifier.amount();
            helper.assertTrue(!attribute.equals(Attributes.ENTITY_INTERACTION_RANGE) && !attribute.equals(Attributes.ATTACK_KNOCKBACK),
                "deactivating pump removes its reach and knockback");
        });
        helper.assertTrue(inactiveDamage[0] == 9, "inactive wrench retains ten total base attack damage");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 50)
    public static void pressDealsOneAndHalfTotalDamageAndRootsForOneSecond(GameTestHelper helper) {
        var player = player(helper, WrenchModule.PRESS);
        var cow = cow(helper);
        var anchor = cow.position();
        cow.hurt(player.damageSources().playerAttack(player), 4);
        helper.assertTrue(cow.getHealth() == 94, "press turns four damage into six total, without a second recursive hit");
        helper.assertTrue(cow.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0 && cow.getAttributeValue(Attributes.JUMP_STRENGTH) == 0,
            "press prevents walking and jumping immediately");
        cow.knockback(2, 1, 0);
        helper.assertTrue(cow.getDeltaMovement().equals(Vec3.ZERO), "root also suppresses the attack's following knockback");
        helper.runAfterDelay(10, () -> {
            cow.setPos(anchor.add(2, 1, 0));
            cow.setDeltaMovement(0.5, 0.5, 0.5);
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(cow));
            helper.assertTrue(cow.position().distanceToSqr(anchor) < 0.000001 && cow.getDeltaMovement().equals(Vec3.ZERO),
                "root constrains actual position and velocity without replacing AI");
            helper.assertTrue(cow.isNoAi() && cow.getHealth() == 94, "root preserves existing AI flag and causes no repeated damage");
        });
        helper.runAfterDelay(21, () -> {
            helper.assertTrue(cow.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0 && cow.getAttributeValue(Attributes.JUMP_STRENGTH) > 0,
                "normal movement and jumping return after twenty ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void pressInOffhandDoesNotAmplifyAttacks(GameTestHelper helper) {
        var player = player(helper, WrenchModule.PRESS);
        player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var cow = cow(helper);
        cow.hurt(player.damageSources().playerAttack(player), 4);
        helper.assertTrue(cow.getHealth() == 96 && cow.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0,
            "offhand press neither amplifies damage nor roots targets");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void pressMultipliesTheActualDamageAfterArmor(GameTestHelper helper) {
        var player = player(helper, WrenchModule.PRESS);
        var baseline = cow(helper);
        baseline.getAttribute(Attributes.ARMOR).setBaseValue(20);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        baseline.hurt(player.damageSources().playerAttack(player), 10);
        float normalDamage = 100 - baseline.getHealth();
        var enhanced = cow(helper);
        enhanced.getAttribute(Attributes.ARMOR).setBaseValue(20);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench(WrenchModule.PRESS));
        enhanced.hurt(player.damageSources().playerAttack(player), 10);
        helper.assertTrue(Math.abs((100 - enhanced.getHealth()) - normalDamage * 1.5F) < 0.0001,
            "armor mitigation is evaluated once before press scales actual health damage");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void burnerAmplifiesPlayerFireAspectButNotAmbientFire(GameTestHelper helper) {
        var player = player(helper, WrenchModule.BURNER);
        player.getMainHandItem().enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT), 1);
        var cow = cow(helper);
        var attack = player.damageSources().playerAttack(player);
        cow.hurt(attack, 4);
        EnchantmentHelper.doPostAttackEffects(helper.getLevel(), cow, attack);
        helper.assertTrue(cow.getRemainingFireTicks() > 0, "native Fire Aspect effect ignites the attacked mob");
        cow.invulnerableTime = 0;
        float before = cow.getHealth();
        cow.hurt(cow.damageSources().onFire(), 1);
        helper.assertTrue(cow.getHealth() == before - 4, "player-attributed Fire Aspect burn deals four times its normal damage");
        cow.clearFire();
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(cow));
        cow.igniteForSeconds(4);
        cow.invulnerableTime = 0;
        before = cow.getHealth();
        cow.hurt(cow.damageSources().onFire(), 1);
        helper.assertTrue(cow.getHealth() == before - 1, "extinguished then environmental fire does not inherit player attribution");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void conversionPersistsWithoutStackingAndDropsOneTemplate(GameTestHelper helper) {
        var player = player(helper, WrenchModule.CONVERTER);
        var cow = cow(helper);
        var interaction = new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, cow, Vec3.ZERO);
        NeoForge.EVENT_BUS.post(interaction);
        helper.assertTrue(interaction.isCanceled() && interaction.getCancellationResult().consumesAction(),
            "converter intercepts entity-specific interaction before native entity menus");
        helper.assertTrue(cow.getAttributeValue(Attributes.ARMOR) == 3 && cow.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 0
            && cow.getEffect(MobEffects.DAMAGE_RESISTANCE).isInfiniteDuration(), "converted mob receives armor three and permanent resistance one");
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow));
        helper.assertTrue(cow.getAttributeValue(Attributes.ARMOR) == 3, "repeated conversion cannot stack armor");
        var saved = cow.saveWithoutId(new CompoundTag());
        var loaded = EntityType.COW.create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(loaded.getAttributeValue(Attributes.ARMOR) == 3 && loaded.hasEffect(MobEffects.DAMAGE_RESISTANCE),
            "conversion armor, resistance and marker survive entity save/reload");
        cow.removeAllEffects();
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(cow));
        helper.assertTrue(cow.hasEffect(MobEffects.DAMAGE_RESISTANCE), "permanent conversion restores resistance after effect removal");
        cow.hurt(cow.damageSources().genericKill(), Float.MAX_VALUE);
        helper.runAfterDelay(1, () -> {
            int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class, cow.getBoundingBox().inflate(2)).stream()
                .filter(item -> item.getItem().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)).mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(count == 1, "converted mob death drops exactly one vanilla netherite upgrade template");
            helper.succeed();
        });
    }
}
