package io.github.boosterproject.booster.content.kinetics.crafter;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.kinetics.crafter.CrafterHelper;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import io.github.boosterproject.booster.Booster;
import net.createmod.catnip.math.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public class NetheriteCrafterGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_mechanical_crafter");

    @GameTest(template = "wireless_empty", timeoutTicks = 30)
    public static void stackLimitsAndNativeStress(GameTestHelper h) {
        Block block = upgraded();
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(ID), "Netherite Mechanical Crafter is registered");
        var m = machine(h, false, 1);
        var inv = m.crafters[0].getInventory();
        h.assertTrue(inv.getSlotLimit(0) == 64, "each cell accepts a normal full stack");
        h.assertTrue(inv.insertItem(0, new ItemStack(Items.EGG, 64), true).getCount() == 48, "sixteen-stack items retain their normal limit");
        h.assertTrue(inv.insertItem(0, new ItemStack(Items.WATER_BUCKET, 2), true).getCount() == 1, "nonstackable items remain single");
        h.assertTrue(inv.getItem(0).isEmpty(), "simulated insertion consumes nothing");
        h.assertTrue(block.asItem().getDefaultInstance().has(DataComponents.FIRE_RESISTANT) && block.getExplosionResistance() >= 1200, "Netherite protection");
        h.assertTrue(BlockStressValues.getImpact(block) == BlockStressValues.getImpact(AllBlocks.MECHANICAL_CRAFTER.get()), "native stress impact");
        var crafting = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
            new ItemStack(AllBlocks.MECHANICAL_CRAFTER.get()), new ItemStack(io.github.boosterproject.booster.registry.BoosterItems.NETHERITE_SHEET.get())));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, crafting, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(crafting, h.getLevel().registryAccess()).is(block.asItem()), "sheet upgrade crafting recipe");
        var conversion = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(AllBlocks.MECHANICAL_CRAFTER.get()));
        var upgrade = h.getLevel().getRecipeManager().getRecipeFor(io.github.boosterproject.booster.registry.BoosterRecipeTypes.CONVERSION.getType(), conversion, h.getLevel()).orElseThrow().value();
        h.assertTrue(upgrade.assemble(conversion, h.getLevel().registryAccess()).is(block.asItem()), "converter upgrade recipe");
        var armType = io.github.boosterproject.booster.registry.BoosterArmInteractionPointTypes.NETHERITE_MECHANICAL_CRAFTER.get();
        h.assertTrue(armType.canCreatePoint(h.getLevel(), m.crafters[0].getBlockPos(), m.crafters[0].getBlockState()), "native arm can insert and extract at the upgraded cell");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 180)
    public static void fullStackProduces256PlanksInOneCycle(GameTestHelper h) {
        var m = machine(h, false, 1);
        h.runAfterDelay(4, () -> insert(h, m.crafters[0], new ItemStack(Items.OAK_LOG, 64)));
        h.succeedWhen(() -> {
            h.assertTrue(count(m.chest, Items.OAK_PLANKS) == 256, "all 64 recipes complete within one native cycle");
            h.assertTrue(m.crafters[0].getInventory().getItem(0).isEmpty(), "all ingredients consumed");
            legalStacks(h, m.chest);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 210)
    public static void limitingIngredientLeavesExcessInOriginalCell(GameTestHelper h) {
        var m = machine(h, false, 2);
        h.runAfterDelay(4, () -> {
            insert(h, m.crafters[0], new ItemStack(Items.OAK_PLANKS, 64));
            insert(h, m.crafters[1], new ItemStack(Items.OAK_PLANKS, 7));
        });
        h.runAfterDelay(8, () -> {
            for (int i = 0; i < m.crafters.length; i++) m.crafters[i] = reload(h, m.crafters[i]);
        });
        h.succeedWhen(() -> {
            h.assertTrue(count(m.chest, Items.STICK) == 28, "only seven complete stick recipes after reloading the running grid");
            h.assertTrue(m.crafters[0].getInventory().getItem(0).getCount() == 57 && m.crafters[1].getInventory().getItem(0).isEmpty(), "excess stays in its source cell");
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 180)
    public static void everyHoneyBottleReturnsItsContainer(GameTestHelper h) {
        var m = machine(h, false, 1);
        h.runAfterDelay(4, () -> insert(h, m.crafters[0], new ItemStack(Items.HONEY_BOTTLE, 16)));
        h.succeedWhen(() -> {
            h.assertTrue(count(m.chest, Items.SUGAR) == 48 && count(m.chest, Items.GLASS_BOTTLE) == 16, "each copy produces sugar and returns one bottle");
            legalStacks(h, m.chest);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 260)
    public static void blockedOutputAndSaveReloadPreserveEntireBatch(GameTestHelper h) {
        var m = machine(h, false, 1);
        for (int i = 0; i < m.chest.getContainerSize(); i++) m.chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        h.runAfterDelay(4, () -> insert(h, m.crafters[0], new ItemStack(Items.OAK_LOG, 64)));
        h.runAfterDelay(100, () -> {
            var tag = m.crafters[0].saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(tag.getString("Phase").equals("INSERTING"), "batch waits at a blocked chest");
            m.crafters[0] = reload(h, m.crafters[0]);
            m.chest.clearContent();
        });
        h.succeedWhen(() -> h.assertTrue(count(m.chest, Items.OAK_PLANKS) == 256, "saved blocked products are delivered once after space opens"));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 180)
    public static void ordinaryCrafterStillAcceptsOnlyOneItem(GameTestHelper h) {
        var m = machine(h, true, 1);
        h.runAfterDelay(4, () -> {
            var leftover = m.crafters[0].getInventory().insertItem(0, new ItemStack(Items.OAK_LOG, 64), false);
            h.assertTrue(leftover.getCount() == 63 && m.crafters[0].getInventory().getSlotLimit(0) == 1, "ordinary inventory stays single-item");
        });
        h.succeedWhen(() -> h.assertTrue(count(m.chest, Items.OAK_PLANKS) == 4, "ordinary recipe produces one result"));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 300)
    public static void nativeMechanicalRecipeLargerThanThreeCellsBatches(GameTestHelper h) {
        var m = machine(h, false, 4);
        h.runAfterDelay(4, () -> {
            int[] counts = {12, 8, 5, 3};
            for (int i = 0; i < counts.length; i++) insert(h, m.crafters[i], new ItemStack(Items.DIAMOND, counts[i]));
        });
        h.succeedWhen(() -> {
            h.assertTrue(count(m.chest, Items.EMERALD) == 9, "four-cell Create-only recipe produces three copies");
            int[] excess = {9, 5, 2, 0};
            for (int i = 0; i < excess.length; i++) h.assertTrue(m.crafters[i].getInventory().getItem(0).getCount() == excess[i], "mechanical recipe leaves exact source excess");
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 210)
    public static void connectedInputsShareFullStackCells(GameTestHelper h) {
        var m = machine(h, false, 2);
        var level = h.getLevel();
        BlockPos top = m.crafters[0].getBlockPos(), bottom = m.crafters[1].getBlockPos();
        h.assertTrue(ConnectedInputHandler.shouldConnect(level, top, Direction.SOUTH, Direction.DOWN), "back-side wrench connection accepts upgraded cells");
        ConnectedInputHandler.toggleConnection(level, top, bottom);
        h.assertTrue(CrafterHelper.areCraftersConnected(level, top, bottom), "native shared-inventory group is connected");
        h.runAfterDelay(4, () -> {
            var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, bottom, Direction.SOUTH);
            h.assertTrue(cap != null && cap.getSlots() == 2, "either cell exposes both shared slots");
            for (int i = 0; i < 2; i++) {
                ItemStack remainder = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(cap, new ItemStack(Items.OAK_PLANKS, 64), false);
                h.assertTrue(remainder.isEmpty(), "shared automation distributes a complete stack");
            }
        });
        h.succeedWhen(() -> h.assertTrue(count(m.chest, Items.STICK) == 256, "two full shared cells craft 64 stick recipes"));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 210)
    public static void mixedChainConsumesOneCopyAndRetainsUpgradedExcess(GameTestHelper h) {
        var m = machine(h, false, 2);
        var level = h.getLevel();
        BlockPos top = m.crafters[0].getBlockPos();
        var state = m.crafters[0].getBlockState();
        level.setBlockAndUpdate(top, AllBlocks.MECHANICAL_CRAFTER.getDefaultState()
            .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING))
            .setValue(MechanicalCrafterBlock.POINTING, state.getValue(MechanicalCrafterBlock.POINTING)));
        var ordinary = (MechanicalCrafterBlockEntity) level.getBlockEntity(top);
        h.runAfterDelay(4, () -> {
            insert(h, m.crafters[1], new ItemStack(Items.OAK_PLANKS, 64));
            insert(h, ordinary, new ItemStack(Items.OAK_PLANKS));
        });
        h.succeedWhen(() -> {
            h.assertTrue(count(m.chest, Items.STICK) == 4, "ordinary cell limits mixed chain to one copy");
            h.assertTrue(m.crafters[1].getInventory().getItem(0).getCount() == 63, "upgraded cell keeps all unused ingredients in mixed chain");
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 210)
    public static void redstoneStartsGridWithIntentionalEmptyCells(GameTestHelper h) {
        var m = machine(h, false, 2);
        // Leave the bottom slot empty; native redstone-start allows an intentional empty recipe cell.
        h.runAfterDelay(4, () -> insert(h, m.crafters[0], new ItemStack(Items.OAK_LOG, 64)));
        h.runAfterDelay(7, () -> m.crafters[0].checkCompletedRecipe(true));
        h.succeedWhen(() -> h.assertTrue(count(m.chest, Items.OAK_PLANKS) == 256, "redstone starts an incomplete grid and empty cells do not limit copies"));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 210)
    public static void nativeSlotCoverDoesNotLimitAutomaticBatch(GameTestHelper h) {
        var m = machine(h, false, 2);
        var covered = m.crafters[1];
        var tag = covered.saveWithFullMetadata(h.getLevel().registryAccess());
        tag.putBoolean("Cover", true);
        covered.loadWithComponents(tag, h.getLevel().registryAccess());
        h.runAfterDelay(4, () -> insert(h, m.crafters[0], new ItemStack(Items.OAK_LOG, 64)));
        h.succeedWhen(() -> h.assertTrue(count(m.chest, Items.OAK_PLANKS) == 256, "slot-covered cell counts as empty space and does not reduce automatic batch size"));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 70)
    public static void breakingRunningCellReturnsReservedAndUnusedIngredients(GameTestHelper h) {
        var m = machine(h, false, 2);
        h.runAfterDelay(4, () -> {
            insert(h, m.crafters[0], new ItemStack(Items.OAK_PLANKS, 64));
            insert(h, m.crafters[1], new ItemStack(Items.STONE, 7));
        });
        h.runAfterDelay(7, () -> {
            BlockPos pos = m.crafters[0].getBlockPos();
            h.getLevel().destroyBlock(pos, true);
            var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(4));
            int planks = drops.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(s -> s.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum();
            int stone = drops.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(s -> s.is(Items.STONE)).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(planks == 64 && stone == 7, "breaking returns all in-flight and reserved materials exactly once; planks=" + planks + ", stone=" + stone);
            h.succeed();
        });
    }

    private static Block upgraded() { return BuiltInRegistries.BLOCK.getOptional(ID).orElse(AllBlocks.MECHANICAL_CRAFTER.get()); }

    private static MechanicalCrafterBlockEntity reload(GameTestHelper h, MechanicalCrafterBlockEntity previous) {
        var level = h.getLevel();
        var tag = previous.saveWithFullMetadata(level.registryAccess());
        BlockPos pos = previous.getBlockPos();
        var state = previous.getBlockState();
        // A placed KineticBlockEntity is marked wasMoved: loading into that same instance clears speed.
        // Chunk loading creates a fresh instance instead and initializes its saved kinetic network.
        previous.onChunkUnloaded();
        level.removeBlockEntity(pos);
        var restored = (MechanicalCrafterBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, state, tag, level.registryAccess());
        h.assertTrue(restored != null, "saved crafter block entity loads");
        level.setBlockEntity(restored);
        return restored;
    }

    private record Machine(MechanicalCrafterBlockEntity[] crafters, ChestBlockEntity chest) {}

    // Index zero is the upper cell; the lowest cell points into the chest to its east.
    private static Machine machine(GameTestHelper h, boolean ordinary, int height) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(1, 1, 1));
        base = new BlockPos(base.getX(), 260, base.getZ());
        var crafters = new MechanicalCrafterBlockEntity[height];
        for (int i = 0; i < height; i++) {
            BlockPos pos = base.above(height - 1 - i);
            Direction target = i == height - 1 ? Direction.EAST : Direction.DOWN;
            BlockState nativeState = AllBlocks.MECHANICAL_CRAFTER.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.NORTH);
            Pointing pointing = java.util.Arrays.stream(Pointing.values()).filter(p -> MechanicalCrafterBlock.getTargetDirection(nativeState.setValue(MechanicalCrafterBlock.POINTING, p)) == target).findFirst().orElseThrow();
            level.setBlockAndUpdate(pos, (ordinary ? AllBlocks.MECHANICAL_CRAFTER.get() : upgraded()).defaultBlockState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.NORTH).setValue(MechanicalCrafterBlock.POINTING, pointing));
            crafters[i] = (MechanicalCrafterBlockEntity) level.getBlockEntity(pos);
        }
        level.setBlockAndUpdate(base.east(), Blocks.CHEST.defaultBlockState());
        // Crafters have shaftless gears: a shaft cannot power their back directly.
        level.setBlockAndUpdate(base.west(), AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z));
        level.setBlockAndUpdate(base.west().south(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.NORTH));
        var motor = (CreativeMotorBlockEntity) level.getBlockEntity(base.west().south());
        motor.generatedSpeed.setValue(64);
        motor.updateGeneratedRotation();
        return new Machine(crafters, (ChestBlockEntity) level.getBlockEntity(base.east()));
    }

    private static void insert(GameTestHelper h, MechanicalCrafterBlockEntity be, ItemStack stack) {
        var capability = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), Direction.SOUTH);
        h.assertTrue(capability != null && capability.insertItem(0, stack, false).isEmpty(), "capability accepts the whole offered stack");
    }
    private static int count(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) count += chest.getItem(i).getCount();
        return count;
    }
    private static void legalStacks(GameTestHelper h, ChestBlockEntity chest) {
        for (int i = 0; i < chest.getContainerSize(); i++) h.assertTrue(chest.getItem(i).getCount() <= chest.getItem(i).getMaxStackSize(), "outputs obey normal stack limits");
    }
}
