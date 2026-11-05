package io.github.boosterproject.booster.content.equipment.wrench;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.redstone.diodes.ToggleLatchBlock;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.MutablePair;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteWrenchGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void mainHandAddsOnlyTwoBlockReachAndRemovalRestoresIt(GameTestHelper helper) {
        var player = player(helper, "BasicWrenchReachQA");
        try {
            double blockRange = player.blockInteractionRange();
            double entityRange = player.entityInteractionRange();
            var wrench = new ItemStack(BoosterItems.NETHERITE_WRENCH.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            player.doTick();
            helper.assertTrue(player.blockInteractionRange() == blockRange + 2
                && player.entityInteractionRange() == entityRange,
                "actual equipped main-hand attributes add exactly two block reach and no entity reach");
            player.setItemInHand(InteractionHand.OFF_HAND, wrench.copy());
            player.doTick();
            helper.assertTrue(player.blockInteractionRange() == blockRange + 2,
                "a second off-hand wrench does not add reach");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.doTick();
            helper.assertTrue(player.blockInteractionRange() == blockRange && player.entityInteractionRange() == entityRange,
                "off-hand alone restores both original ranges");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.getInventory().setItem(1, wrench);
            player.setItemSlot(EquipmentSlot.CHEST, wrench.copy());
            player.doTick();
            helper.assertTrue(player.blockInteractionRange() == blockRange,
                "inventory and non-hand equipment never grant wrench reach");
            helper.assertTrue(!WrenchData.isWrench(wrench), "basic wrench is outside modular wrench and X-wheel recognition");
            helper.succeed();
        } finally {
            close(player);
        }
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void nativeShaftRotationAndSneakPickupUseTheRealInteractionRoute(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 70, 1));
        var player = player(helper, "BasicWrenchShaftQA");
        try {
            var wrench = new ItemStack(BoosterItems.NETHERITE_WRENCH.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            level.setBlock(pos, AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
            var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
            helper.assertTrue(player.gameMode.useItemOn(player, level, wrench, InteractionHand.MAIN_HAND, hit).consumesAction()
                && level.getBlockState(pos).getValue(BlockStateProperties.AXIS) == Direction.Axis.Z,
                "server player interaction rotates the native shaft through the native wrench behavior");
            player.setShiftKeyDown(true);
            helper.assertTrue(player.gameMode.useItemOn(player, level, wrench, InteractionHand.MAIN_HAND, hit).consumesAction()
                && level.isEmptyBlock(pos), "sneak use removes the native shaft");
            helper.assertTrue(player.getInventory().countItem(AllBlocks.SHAFT.asItem()) == 1,
                "native survival pickup returns exactly one shaft to the inventory");
            helper.assertTrue(!wrench.isDamageableItem() && wrench.getDamageValue() == 0,
                "native rotation and pickup leave a tool with no durability");
            helper.succeed();
        } finally {
            level.removeBlock(pos, false);
            close(player);
        }
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void nativeLatchAndMinecartRecognitionStaySpecificToTheBasicWrench(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 90, 1));
        var player = player(helper, "BasicWrenchNativeQA");
        try {
            var wrench = new ItemStack(BoosterItems.NETHERITE_WRENCH.get());
            var modular = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
            helper.assertTrue(wrench.is(Tags.Items.TOOLS_WRENCH), "basic wrench is recognized by native common wrench tag consumers");
            helper.assertTrue(AllItems.WRENCH.isIn(wrench) && AllItems.WRENCH.isIn(AllItems.WRENCH.asStack())
                && !AllItems.WRENCH.isIn(modular) && !AllItems.SUPER_GLUE.isIn(wrench),
                "only Create's wrench entry recognizes the new basic item, and modular sword recognition stays unchanged");
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(pos, AllBlocks.POWERED_TOGGLE_LATCH.getDefaultState().setValue(ToggleLatchBlock.FACING, Direction.NORTH), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
            player.gameMode.useItemOn(player, level, wrench, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(!level.getBlockState(pos).getValue(ToggleLatchBlock.POWERING)
                && level.getBlockState(pos).getValue(ToggleLatchBlock.FACING) != Direction.NORTH,
                "native latch recognizes the wrench before its ordinary activation, so it rotates without toggling power");
            var cart = EntityType.MINECART.create(level);
            cart.setPos(pos.getCenter().add(0, 3, 0));
            player.attack(cart);
            helper.assertTrue(cart.isRemoved(), "native global wrench attack handler removes a minecart with the new tool");
            helper.succeed();
        } finally {
            level.removeBlock(pos, false);
            level.removeBlock(pos.below(), false);
            close(player);
        }
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void bothUpgradeRecipesLoadAndBasicWrenchSurvivesFireAndExplosion(GameTestHelper helper) {
        var level = helper.getLevel();
        var wrench = new ItemStack(BoosterItems.NETHERITE_WRENCH.get());
        var crafting = CraftingInput.of(2, 1, List.of(AllItems.WRENCH.asStack(), new ItemStack(BoosterItems.NETHERITE_SHEET.get())));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, crafting, level).orElseThrow();
        helper.assertTrue(recipe.value().assemble(crafting, level.registryAccess()).is(wrench.getItem()),
            "loaded shapeless recipe upgrades a Create wrench using one netherite sheet");
        var input = new SingleRecipeInput(AllItems.WRENCH.asStack());
        var conversion = level.getRecipeManager().getRecipeFor(BoosterRecipeTypes.CONVERSION.getType(), input, level).orElseThrow();
        helper.assertTrue(conversion.value().assemble(input, level.registryAccess()).is(wrench.getItem()),
            "loaded conversion recipe upgrades the original wrench to the basic tool");
        helper.assertTrue(wrench.has(DataComponents.FIRE_RESISTANT) && !wrench.isDamageableItem()
            && !wrench.has(DataComponents.MAX_DAMAGE) && wrench.getMaxStackSize() == 1,
            "basic wrench has fire resistance, no durability, and native single-tool stack size");
        var item = new ItemEntity(level, 0, 300, 0, wrench.copy());
        helper.assertTrue(!item.hurt(level.damageSources().lava(), 100) && !item.isRemoved(), "dropped wrench survives actual fire damage");
        helper.assertTrue(!item.hurt(level.damageSources().explosion(null, null), 100) && !item.isRemoved(),
            "dropped wrench survives actual explosion damage through project item protection");
        var control = new ItemEntity(level, 0, 300, 0, new ItemStack(Items.STONE));
        helper.assertTrue(control.hurt(level.damageSources().explosion(null, null), 100) && control.isRemoved(),
            "explosion control destroys an ordinary dropped item");
        item.discard();
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void movingDeployerChangesModeWithoutExchangingEitherWrench(GameTestHelper helper) {
        var player = player(helper, "MovingBasicWrenchQA");
        try {
            movingDeployerInteraction(helper, player, AllItems.WRENCH.asStack(), true);
            movingDeployerInteraction(helper, player, new ItemStack(BoosterItems.NETHERITE_WRENCH.get()), true);
            movingDeployerInteraction(helper, player, new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get()), false);
            movingDeployerInteraction(helper, player, new ItemStack(Items.DIRT), false);
            helper.succeed();
        } finally {
            close(player);
        }
    }

    private static void movingDeployerInteraction(GameTestHelper helper, ServerPlayer player, ItemStack held, boolean wrench) {
        var level = helper.getLevel();
        var contraption = new MountedContraption();
        contraption.anchor = helper.absolutePos(new BlockPos(1, 130, 1));
        contraption.bounds = new AABB(BlockPos.ZERO);
        var tag = new CompoundTag();
        tag.putString("Mode", "USE");
        var info = new StructureBlockInfo(BlockPos.ZERO, AllBlocks.DEPLOYER.getDefaultState(), tag);
        var context = new MovementContext(level, info, contraption);
        var deployer = new DeployerFakePlayer(level, player.getUUID());
        deployer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 2));
        context.temporaryData = deployer;
        contraption.getBlocks().put(BlockPos.ZERO, info);
        contraption.getActors().add(MutablePair.of(info, context));
        var entity = OrientedContraptionEntity.create(level, contraption, Direction.EAST);
        entity.setPos(contraption.anchor.getCenter());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var interaction = MovingInteractionBehaviour.REGISTRY.get(info.state().getBlock());
        helper.assertTrue(interaction != null && interaction.handlePlayerInteraction(player, InteractionHand.MAIN_HAND, BlockPos.ZERO, entity),
            "registered native moving deployer interaction handles the held item " + held);
        if (wrench) {
            helper.assertTrue(tag.getString("Mode").equals("PUNCH")
                && player.getMainHandItem() == held && deployer.getMainHandItem().is(Items.STONE)
                && deployer.getMainHandItem().getCount() == 2 && !tag.contains("HeldItem"),
                "wrench changes moving deployer mode and preserves both hands instead of swapping the wrench into its hand: " + held);
            interaction.handlePlayerInteraction(player, InteractionHand.MAIN_HAND, BlockPos.ZERO, entity);
            helper.assertTrue(tag.getString("Mode").equals("USE")
                && player.getMainHandItem() == held && deployer.getMainHandItem().is(Items.STONE),
                "second wrench interaction restores native USE mode without a hand exchange");
        } else {
            helper.assertTrue(tag.getString("Mode").equals("USE")
                && player.getMainHandItem().is(Items.STONE) && player.getMainHandItem().getCount() == 2
                && ItemStack.isSameItemSameComponents(deployer.getMainHandItem(), held) && tag.contains("HeldItem"),
                "modular sword and unrelated items keep native hand exchange without changing deployer mode");
        }
        entity.discard();
        deployer.discard();
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), name);
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 310, 1.5)));
        level.getChunkSource().addEntity(player);
        player.tick();
        return player;
    }

    private static void close(ServerPlayer player) {
        player.serverLevel().getChunkSource().removeEntity(player);
        player.discard();
    }
}
