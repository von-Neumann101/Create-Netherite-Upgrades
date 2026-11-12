package io.github.boosterproject.booster.content.conversion;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteConverterPonderGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 950)
    public static void tutorialLayoutSupportsPipeSupplyMachineAndBothWrenchUpgrades(GameTestHelper helper) throws Exception {
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
        var source = ConverterInteractionGameTest.installSupply(helper, machinePos, 2000);
        var depot = (DepotBlockEntity) level.getBlockEntity(depotPos);
        var motorPos = machinePos.west(3);
        level.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(motorPos)).generatedSpeed.setValue(64);
        var recipeInput = CraftingInput.of(2, 1, List.of(AllItems.WRENCH.asStack(), new ItemStack(BoosterItems.NETHERITE_SHEET.get())));
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, recipeInput, level).orElseThrow().value()
            .assemble(recipeInput, level.registryAccess()).is(BoosterItems.NETHERITE_WRENCH.get()), "the shown unordered crafting recipe makes the basic wrench");
        int[] stage = {0};
        long[] started = {-1};
        helper.onEachTick(() -> {
            long now = level.getGameTime();
            if (stage[0] == 0 && machine.getEssenceTank().getFluidAmount() == 2000) {
                helper.assertTrue(machine.getOrCreateNetwork().getActualStressOf(machine) == 2048, "tutorial shaft supplies the fixed stress");
                depot.setHeldItem(AllBlocks.MECHANICAL_PRESS.asStack()); stage[0] = 1;
            }
            if (stage[0] > 0 && machine.getProgress() == 1 && started[0] < 0) started[0] = now;
            if (stage[0] == 1 && depot.getHeldItem().is(BoosterItems.NETHERITE_MECHANICAL_PRESS.get())) {
                helper.assertTrue(now - started[0] >= 99 && machine.getEssenceTank().getFluidAmount() == 1000, "machine conversion uses1000mB in five seconds");
                depot.setHeldItem(AllItems.WRENCH.asStack()); stage[0] = 2; started[0] = -1;
            } else if (stage[0] == 2 && depot.getHeldItem().is(BoosterItems.NETHERITE_WRENCH.get())) {
                helper.assertTrue(now - started[0] >= 99 && machine.getEssenceTank().isEmpty(), "basic wrench conversion also uses1000mB");
                var supply = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, source, null);
                helper.assertTrue(supply.fill(new net.neoforged.neoforge.fluids.FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 4000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 4000, "four-bucket advanced supply enters the source tank");
                stage[0] = 3; started[0] = -1;
            } else if (stage[0] == 3 && depot.getHeldItem().is(BoosterItems.NETHERITE_NETHERITE_WRENCH.get())) {
                helper.assertTrue(now - started[0] >= 99 && machine.getEssenceTank().isEmpty(), "advanced wrench consumes exactly4000mB after a full five seconds");
                helper.assertTrue(io.github.boosterproject.booster.content.equipment.wrench.WrenchData.activationLimit(depot.getHeldItem()) == 1, "crafting advanced wrench retains its initial single active slot");
                var supply = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, source, null);
                helper.assertTrue(supply.fill(new net.neoforged.neoforge.fluids.FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 4000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 4000, "four more buckets supply the active-slot upgrade");
                stage[0] = 4; started[0] = -1;
            } else if (stage[0] == 4 && io.github.boosterproject.booster.content.equipment.wrench.WrenchData.activationLimit(depot.getHeldItem()) == 2) {
                helper.assertTrue(now - started[0] >= 99 && machine.getEssenceTank().isEmpty(), "second active slot consumes another4000mB in five seconds");
                for (int x = 0; x < structure.getSize().getX(); x++) for (int y = 0; y < structure.getSize().getY(); y++)
                    for (int z = 0; z < structure.getSize().getZ(); z++) level.removeBlock(origin.offset(x, y, z), false);
                helper.succeed();
            }
        });
    }
}
