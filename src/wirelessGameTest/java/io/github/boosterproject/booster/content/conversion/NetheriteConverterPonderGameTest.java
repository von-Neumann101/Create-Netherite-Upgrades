package io.github.boosterproject.booster.content.conversion;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.List;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteConverterPonderGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 430)
    public static void tutorialLayoutSupportsValueMachineAndBothWrenchUpgrades(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var structure = new StructureTemplate();
        try (var stream = NetheriteConverterPonderGameTest.class.getClassLoader()
                .getResourceAsStream("assets/createnetherite/ponder/converter/tutorial.nbt")) {
            helper.assertTrue(stream != null, "converter production Ponder structure exists");
            structure.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap()));
        }
        var origin = helper.absolutePos(new BlockPos(1, 170, 1));
        helper.assertTrue(structure.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 3), "production structure places successfully");
        var machinePos = origin.offset(3, 3, 3);
        var depotPos = machinePos.below(2);
        var shaft = level.getBlockState(machinePos.west());
        helper.assertTrue(level.getBlockState(machinePos).is(BoosterBlocks.NETHERITE_CONVERTER.get())
            && level.getBlockState(depotPos).is(AllBlocks.DEPOT.get()) && level.isEmptyBlock(machinePos.below())
            && shaft.is(AllBlocks.SHAFT.get()) && shaft.getValue(BlockStateProperties.AXIS) == Direction.Axis.X
            && level.getBlockState(machinePos.west(2).below()).is(AllBlocks.ANDESITE_CASING.get())
            && level.getBlockState(origin.offset(5, 1, 2)).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE),
            "tutorial has the two-block processing gap, aligned side shaft, support and crafting table");
        var machine = (NetheriteConverterBlockEntity) level.getBlockEntity(machinePos);
        var depot = (DepotBlockEntity) level.getBlockEntity(depotPos);
        var motorPos = machinePos.west(3);
        level.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(motorPos)).generatedSpeed.setValue(64);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ConverterPonderQA"));
        player.setPos(machinePos.getCenter().add(0, 0, 2));
        var recipeInput = CraftingInput.of(2, 1, List.of(AllItems.WRENCH.asStack(), new ItemStack(BoosterItems.NETHERITE_SHEET.get())));
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, recipeInput, level).orElseThrow().value()
            .assemble(recipeInput, level.registryAccess()).is(BoosterItems.NETHERITE_WRENCH.get()), "the shown unordered crafting recipe makes the basic wrench");
        int[] stage = {0};
        long[] started = {-1}, waiting = {-1};
        helper.runAfterDelay(5, () -> {
            deposit(helper, player, machinePos, new ItemStack(BoosterItems.NETHERITE_SHEET.get()));
            helper.assertTrue(machine.getNetheriteValue() == 16, "shown plate right-click immediately gives sixteen points");
            deposit(helper, player, machinePos, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            helper.assertTrue(machine.getNetheriteValue() == 17 && machine.getPlateInventory().getStackInSlot(0).isEmpty(), "shown template adds one point without leaving an extractable catalyst");
            deposit(helper, player, machinePos, new ItemStack(BoosterFluids.ESSENCE_BUCKET.get()));
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && machine.getEssenceTank().getFluidAmount() == 1000,
                "actual bucket use fills the tutorial converter and returns its container");
            helper.assertTrue(((KineticBlockEntity) level.getBlockEntity(machinePos.west())).getSpeed() == 64 && machine.getSpeed() == 64
                && machine.getOrCreateNetwork().getActualStressOf(machine) == 2048, "tutorial shaft really powers the converter at its stated stress");
            depot.setHeldItem(AllBlocks.MECHANICAL_PRESS.asStack());
            stage[0] = 1;
        });
        helper.onEachTick(() -> {
            long now = level.getGameTime();
            if (stage[0] >= 1 && stage[0] <= 3 && machine.getProgress() == 1 && started[0] < 0) started[0] = now;
            if (stage[0] == 1 && depot.getHeldItem().is(BoosterItems.NETHERITE_MECHANICAL_PRESS.get())) {
                helper.assertTrue(now - started[0] >= 99 && machine.getNetheriteValue() == 16 && machine.getEssenceTank().getFluidAmount() == 750,
                    "normal machine example really takes five seconds, one point and 250mB");
                depot.setHeldItem(AllItems.WRENCH.asStack()); started[0] = -1; stage[0] = 2;
            } else if (stage[0] == 2 && depot.getHeldItem().is(BoosterItems.NETHERITE_WRENCH.get())) {
                helper.assertTrue(now - started[0] >= 99 && machine.getNetheriteValue() == 15 && machine.getEssenceTank().getFluidAmount() == 500,
                    "original wrench converts to the basic tier for one point and 250mB");
                waiting[0] = now; stage[0] = 3; started[0] = -1;
            } else if (stage[0] == 3 && now - waiting[0] >= 10) {
                helper.assertTrue(machine.getProgress() == 0 && depot.getHeldItem().is(BoosterItems.NETHERITE_WRENCH.get()), "fifteen points cannot accidentally start the advanced tier");
                deposit(helper, player, machinePos, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                stage[0] = 4; started[0] = now;
            } else if (stage[0] == 4 && depot.getHeldItem().is(BoosterItems.NETHERITE_NETHERITE_WRENCH.get())) {
                helper.assertTrue(now - started[0] >= 100 && machine.getNetheriteValue() == 0 && machine.getEssenceTank().getFluidAmount() == 250,
                    "basic wrench upgrades to advanced tier for sixteen points and 250mB after another five seconds");
                for (int x = 0; x < structure.getSize().getX(); x++) for (int y = 0; y < structure.getSize().getY(); y++)
                    for (int z = 0; z < structure.getSize().getZ(); z++) level.removeBlock(origin.offset(x, y, z), false);
                helper.succeed();
            }
        });
    }

    private static void deposit(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player, BlockPos pos, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false));
    }
}
