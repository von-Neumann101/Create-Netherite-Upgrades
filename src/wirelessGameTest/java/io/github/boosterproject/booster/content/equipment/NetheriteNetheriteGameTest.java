package io.github.boosterproject.booster.content.equipment;

import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.Booster;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteNetheriteGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_netherite");

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper helper, String name) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var server = helper.getLevel().getServer();
        var player = new net.minecraft.server.level.ServerPlayer(server, helper.getLevel(), profile,
            net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            };
        player.setPos(helper.absolutePos(new BlockPos(1, 300, 1)).getCenter());
        player.setNoGravity(true);
        player.getFoodData().setFoodLevel(20);
        return player;
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 320)
    public static void continuousChewingAwardsOnlyAfterTenSeconds(GameTestHelper helper) {
        var player = player(helper, "LongChewQA");
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ID), 16);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Chew components QA"));
        var original = stack.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var advancement = helper.getLevel().getServer().getAdvancements().get(NetheriteNetheriteItem.TEETH_ADVANCEMENT);
        helper.assertTrue(advancement != null && advancement.value().display().orElseThrow().getType() == net.minecraft.advancements.AdvancementType.CHALLENGE,
            "real advancement uses the purple challenge frame");
        float saturation = player.getFoodData().getSaturationLevel(), health = player.getHealth();
        long started = helper.getLevel().getGameTime();
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.onEachTick(() -> {
            player.doTick(); // Real Player/LivingEntity loop; NeoForge intentionally rejects FakePlayer advancements.
            long elapsed = helper.getLevel().getGameTime() - started;
            helper.assertTrue(player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUseItemRemainingTicks() <= 32,
                "native eating cycle keeps renewing without finishing");
            helper.assertTrue(ItemStack.matches(stack, original), "chewing preserves count and all item components");
            boolean completed = player.getAdvancements().getOrStartProgress(advancement).isDone();
            if (elapsed <= 200) helper.assertTrue(!completed, "exactly ten seconds or less cannot award the challenge");
            if (elapsed == 201) helper.assertTrue(completed, "challenge is awarded after 201 continuous ticks; data=" + player.getPersistentData()
                + ", remaining=" + player.getUseItemRemainingTicks() + ", progress=" + player.getAdvancements().getOrStartProgress(advancement));
            if (elapsed >= 260) {
                helper.assertTrue(completed && player.getFoodData().getFoodLevel() == 20
                    && player.getFoodData().getSaturationLevel() == saturation && player.getHealth() == health,
                    "continued chewing after award neither eats nor changes food, saturation or health");
                player.releaseUsingItem(); helper.succeed();
            }
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 420)
    public static void interruptedChewingRestartsAndOffhandWorks(GameTestHelper helper) {
        var player = player(helper, "RestartChewQA");
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ID), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var advancement = helper.getLevel().getServer().getAdvancements().get(NetheriteNetheriteItem.TEETH_ADVANCEMENT);
        long started = helper.getLevel().getGameTime();
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.onEachTick(() -> {
            player.doTick();
            long elapsed = helper.getLevel().getGameTime() - started;
            boolean complete = player.getAdvancements().getOrStartProgress(advancement).isDone();
            if (elapsed == 150) {
                player.releaseUsingItem();
                helper.assertTrue(!player.isUsingItem() && !complete, "release before ten seconds grants nothing");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, stack);
                stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
                helper.assertTrue(player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND, "offhand starts a new continuous use");
            }
            if (elapsed <= 350) helper.assertTrue(!complete, "separate short holds do not accumulate or award at the second hold's 200-tick boundary");
            if (elapsed >= 351) {
                helper.assertTrue(complete && stack.getCount() == 3, "offhand receives the challenge after its own full continuous duration");
                player.releaseUsingItem(); helper.succeed();
            }
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void realRecipeAndUseEventBoundaries(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(ID).orElseThrow();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
            new ItemStack(net.minecraft.world.item.Items.NETHERITE_INGOT), new ItemStack(io.github.boosterproject.booster.registry.BoosterItems.NETHERITE_SHEET.get())));
        var crafting = (net.minecraft.world.item.crafting.CraftingRecipe) recipe.value();
        helper.assertTrue(crafting.matches(input, helper.getLevel()) && crafting.assemble(input, helper.getLevel().registryAccess()).is(BuiltInRegistries.ITEM.get(ID)),
            "real loaded shapeless recipe uses an ingot and a netherite sheet");
        var player = player(helper, "ChewEventQA"); var stack = new ItemStack(BuiltInRegistries.ITEM.get(ID));
        helper.assertTrue(net.neoforged.neoforge.event.EventHooks.onItemUseTick(player, stack, 1) == 33, "last eating tick is renewed through real NeoForge event bus");
        helper.assertTrue(net.neoforged.neoforge.event.EventHooks.onItemUseTick(player, stack, 0) == 0
            && net.neoforged.neoforge.event.EventHooks.onItemUseTick(player, stack, -1) == -1, "non-positive stop durations remain respected");
        helper.assertTrue(net.neoforged.neoforge.event.EventHooks.onItemUseTick(player, new ItemStack(net.minecraft.world.item.Items.APPLE), 1) == 1,
            "ordinary food retains original completion timing");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void startsChewingAtFullHungerWithoutConsumption(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ID), "netherite netherite must be registered");
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "NetheriteChewQA"));
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ID), 12);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult().consumesAction() && player.isUsingItem() && stack.getUseAnimation() == UseAnim.EAT,
            "full-hunger player starts native eating animation");
        helper.assertTrue(stack.getItem().finishUsingItem(stack, helper.getLevel(), player) == stack && stack.getCount() == 12,
            "even completion returns the same uneaten stack");
        player.releaseUsingItem();
        helper.assertTrue(!player.isUsingItem(), "release stops chewing");
        helper.succeed();
    }
}
