package io.github.boosterproject.booster.content.logistics.depot;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteEjectorPonderGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 220)
    public static void tutorialStructureAndUsageSequence(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var structure = new StructureTemplate();
        try (var stream = NetheriteEjectorPonderGameTest.class.getClassLoader()
                .getResourceAsStream("assets/createnetherite/ponder/ejector/tutorial.nbt")) {
            helper.assertTrue(stream != null, "actual production Ponder structure is packaged on the classpath");
            structure.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap()));
        }
        BlockPos origin = helper.absolutePos(new BlockPos(1, 310, 1));
        BlockPos source = origin.offset(1, 2, 5), first = origin.offset(4, 3, 2), second = origin.offset(5, 1, 4);
        BlockPos lever = source.south(), motor = source.west(2);
        helper.assertTrue(structure.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 3), "actual schematic places with real registered blocks");
        helper.assertTrue(level.getBlockState(source.below()).is(AllBlocks.ANDESITE_CASING.get())
            && level.getBlockEntity(source) instanceof NetheriteEjectorBlockEntity, "source is supported and uses the upgraded BE");
        level.removeBlock(source, false);
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "EjectorPonderQA"));
        var held = new ItemStack(BoosterItems.NETHERITE_WEIGHTED_EJECTOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setPos(first.getCenter()); player.setShiftKeyDown(true);
        held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(first)));
        player.setPos(source.getCenter()); player.setShiftKeyDown(false); player.setYRot(0);
        held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(source.below())));
        var ejector = (NetheriteEjectorBlockEntity) level.getBlockEntity(source);
        helper.assertTrue(ejector != null && ejector.getTargetPosition().equals(first) && player.getMainHandItem().isEmpty(),
            "tutorial's select-then-place interaction applies the diagonal elevated target using the last item");
        for (BlockPos target : new BlockPos[] {first, second}) {
            var trajectory = new NetheriteEntityLauncher(target.subtract(source));
            for (int i = 0; i <= 40; i++) {
                var point = trajectory.getGlobalPos(trajectory.getTotalFlyingTicks() * i / 40D, Direction.NORTH, source).subtract(origin.getX(), origin.getY(), origin.getZ());
                helper.assertTrue(point.x >= 0 && point.x < structure.getSize().getX() && point.y >= 0 && point.y < structure.getSize().getY()
                    && point.z >= 0 && point.z < structure.getSize().getZ(),
                    "tutorial trajectory fits its visible structure bounds: " + point);
            }
        }
        helper.runAfterDelay(15, () -> insert(helper, source, new ItemStack(Items.COPPER_INGOT)));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(count(helper, first) == 1 && !ejector.hasFlyingItems(), "first tutorial flight reaches the elevated depot without hitting its support");
            ItemStack tool = new ItemStack(BoosterItems.NETHERITE_WEIGHTED_EJECTOR.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, tool); player.setPos(second.getCenter()); player.setShiftKeyDown(true);
            tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(second)));
            player.setPos(source.getCenter()); player.setShiftKeyDown(false);
            level.getBlockState(source).useItemOn(tool, level, player, InteractionHand.MAIN_HAND, hit(source));
            helper.assertTrue(ejector.getTargetPosition().equals(second) && tool.getCount() == 1, "idle retarget applies the lower diagonal route without consuming the held machine");
            ejector.getBehaviour(ScrollValueBehaviour.TYPE).setValue(4);
            helper.assertTrue(ejector.getSpeed() == 64 && ejector.getBehaviour(ScrollValueBehaviour.TYPE).getValue() == 4,
                "tutorial quantity panel is set to four");
            insert(helper, source, new ItemStack(Items.IRON_INGOT));
            insert(helper, source, new ItemStack(Items.IRON_INGOT));
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(count(helper, source) == 2 && count(helper, second) == 0 && !ejector.hasFlyingItems(), "two items wait for the tutorial's four-item threshold");
            insert(helper, source, new ItemStack(Items.IRON_INGOT, 2));
        });
        helper.runAfterDelay(115, () -> {
            helper.assertTrue(count(helper, second) == 4 && !ejector.hasFlyingItems(), "reaching four triggers the second tutorial flight");
            var destination = level.getCapability(Capabilities.ItemHandler.BLOCK, second, null);
            helper.assertTrue(destination.extractItem(0, 4, false).getCount() == 4 && count(helper, second) == 0,
                "collect the first batch so the depot can receive the redstone demonstration");
            level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, true), 3);
            ejector.updateSignal();
            insert(helper, source, new ItemStack(Items.IRON_INGOT));
            insert(helper, source, new ItemStack(Items.IRON_INGOT, 3));
        });
        helper.runAfterDelay(145, () -> {
            helper.assertTrue(ejector.isPowered() && count(helper, source) == 4 && count(helper, second) == 0,
                "the schematic lever really pauses a full stack");
            level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, false), 3); ejector.updateSignal();
        });
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(count(helper, second) == 4 && count(helper, source) == 0, "turning off the lever resumes real item delivery");
            for (int x = 0; x < structure.getSize().getX(); x++) for (int y = 0; y < structure.getSize().getY(); y++)
                for (int z = 0; z < structure.getSize().getZ(); z++) level.removeBlock(origin.offset(x, y, z), false);
            level.removeBlock(motor, false); helper.succeed();
        });
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(pos.getCenter().add(0, .5, 0), Direction.UP, pos, false); }
    private static void insert(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        var inv = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        var be = (NetheriteEjectorBlockEntity) helper.getLevel().getBlockEntity(pos);
        ItemStack remainder = inv == null ? stack : inv.insertItem(0, stack, false);
        helper.assertTrue(remainder.isEmpty(), "tutorial input fits actual inventory; input=" + stack + ", remainder=" + remainder
            + ", state=" + be.getState() + ", speed=" + be.getSpeed() + ", powered=" + be.isPowered()
            + ", block=" + be.getBlockState() + ", contents=" + (inv == null ? "none" : inv.getStackInSlot(0)));
    }
    private static int count(GameTestHelper helper, BlockPos pos) {
        var inv = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        int count = 0;
        for (int slot = 0; slot < inv.getSlots(); slot++) count += inv.getStackInSlot(slot).getCount();
        return count;
    }
}
