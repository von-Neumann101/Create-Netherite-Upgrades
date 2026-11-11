package io.github.boosterproject.booster.content.processing.basin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinMovementBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.impl.unpacking.BasinUnpackingHandler;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.List;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public class NetheriteBasinGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_basin");

    @GameTest(template = "wireless_empty", timeoutTicks = 25)
    public static void capacityTriplesAndFluidCapacityStaysNative(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(ID), "Netherite Basin is registered");
        var basin = basin(h, false, 1);
        var original = basin(h, true, 5);
        h.assertTrue(basin.getInputInventory().getSlots() == 27 && basin.getOutputInventory().getSlots() == 27, "both input and product buffers have27 slots");
        h.assertTrue(items(h, basin).getSlots() == 54 && items(h, original).getSlots() == 18, "native combined capability exposes both inventories");
        h.assertTrue(basin.getInvs().get(true) == basin.getInputInventory() && basin.getInvs().get(false) == basin.getOutputInventory(), "native inventory references use the enlarged buffers");
        var a = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, basin.getBlockPos(), null);
        var b = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, original.getBlockPos(), null);
        h.assertTrue(a != null && b != null && a.getTanks() == b.getTanks(), "fluid tank count remains native");
        for (int tank = 0; tank < a.getTanks(); tank++) h.assertTrue(a.getTankCapacity(tank) == b.getTankCapacity(tank), "fluid capacity remains native");
        h.assertTrue(upgraded().getExplosionResistance() >= 1200 && upgraded().asItem().getDefaultInstance().has(DataComponents.FIRE_RESISTANT), "Netherite fire and blast protection");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 25)
    public static void repeatedStacksFillAll27InputCellsAndSimulationDoesNotConsume(GameTestHelper h) {
        var basin = basin(h, false, 1);
        var cap = items(h, basin);
        h.assertTrue(ItemHandlerHelper.insertItemStacked(cap, new ItemStack(Items.STONE, 64), true).isEmpty() && count(cap, Items.STONE) == 0, "simulated insertion is mutation-free");
        for (int i = 0; i < 27; i++) h.assertTrue(ItemHandlerHelper.insertItemStacked(cap, new ItemStack(Items.STONE, 64), false).isEmpty(), "same item can fill every input cell: " + i);
        h.assertTrue(count(cap, Items.STONE) == 1728, "full input holds1728 ordinary items");
        h.assertTrue(ItemHandlerHelper.insertItemStacked(cap, new ItemStack(Items.STONE), false).getCount() == 1, "full input refuses overflow into product slots");
        h.assertTrue(cap.insertItem(27, new ItemStack(Items.DIAMOND), false).getCount() == 1, "automation cannot insert into output buffer");
        var original = basin(h, true, 5);
        var nativeCap = items(h, original);
        h.assertTrue(ItemHandlerHelper.insertItemStacked(nativeCap, new ItemStack(Items.STONE, 64), false).isEmpty()
            && ItemHandlerHelper.insertItemStacked(nativeCap, new ItemStack(Items.STONE, 64), false).getCount() == 64, "ordinary basin retains its same-item uniqueness rule");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 25)
    public static void normalStackLimitsAndIndependentComponentsAreRetained(GameTestHelper h) {
        var basin = basin(h, false, 1);
        h.assertTrue(basin.inputInventory.insertItem(0, new ItemStack(Items.EGG, 64), false).getCount() == 48, "eggs retain max stack16");
        h.assertTrue(basin.inputInventory.insertItem(1, new ItemStack(Items.WATER_BUCKET, 2), false).getCount() == 1, "buckets retain max stack1");
        ItemStack named = new ItemStack(Items.STONE, 64);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("reference"));
        h.assertTrue(basin.inputInventory.insertItem(2, named, false).isEmpty()
            && basin.inputInventory.insertItem(3, new ItemStack(Items.STONE, 64), false).isEmpty(), "different components remain separate stacks");
        h.assertTrue(basin.inputInventory.getItem(2).has(DataComponents.CUSTOM_NAME) && !basin.inputInventory.getItem(3).has(DataComponents.CUSTOM_NAME), "component data is retained");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 25)
    public static void productBufferStores27StacksAndHonorsInsertionLocks(GameTestHelper h) {
        var basin = basin(h, false, 1);
        for (int i = 0; i < 27; i++) {
            h.assertTrue(basin.acceptOutputs(List.of(new ItemStack(Items.DIAMOND, 64)), List.of(), true), "processing can simulate room in each product cell");
            h.assertTrue(count(basin.getOutputInventory(), Items.DIAMOND) == i * 64, "simulation does not alter output");
            h.assertTrue(basin.acceptOutputs(List.of(new ItemStack(Items.DIAMOND, 64)), List.of(), false), "processing fills product cell " + i);
        }
        h.assertTrue(count(basin.getOutputInventory(), Items.DIAMOND) == 1728, "product storage also triples");
        h.assertTrue(!basin.acceptOutputs(List.of(new ItemStack(Items.DIAMOND)), List.of(), true), "full products block further processing");
        h.assertTrue(basin.getOutputInventory().insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1, "processing restores output insertion lock");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 25)
    public static void highSlotsPersistAndAllStoredItemsDropOnRemoval(GameTestHelper h) {
        var basin = basin(h, false, 1);
        for (int slot = 0; slot < 27; slot++) {
            basin.inputInventory.setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            h.assertTrue(basin.acceptOutputs(List.of(new ItemStack(Items.DIAMOND, 64)), List.of(), false), "store product stack");
        }
        var tag = basin.saveWithFullMetadata(h.getLevel().registryAccess());
        basin.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(count(basin.inputInventory, Items.STONE) == 1728 && count(basin.getOutputInventory(), Items.DIAMOND) == 1728, "all high-index cells persist");
        BlockPos pos = basin.getBlockPos();
        h.getLevel().destroyBlock(pos, true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.STONE)).mapToInt(e -> e.getItem().getCount()).sum() == 1728
            && drops.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum() == 1728, "removal returns all input and product stacks once");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 25)
    public static void upgradeRecipesAndNativeAutomationAreRegistered(GameTestHelper h) {
        var basin = basin(h, false, 1);
        var level = h.getLevel();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(AllBlocks.BASIN.get()), new ItemStack(BoosterItems.NETHERITE_SHEET.get())));
        var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow().value();
        h.assertTrue(recipe.assemble(input, level.registryAccess()).is(upgraded().asItem()), "basin and sheet crafting upgrade");
        var conversion = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(AllBlocks.BASIN.get()));
        h.assertTrue(level.getRecipeManager().getRecipeFor(BoosterRecipeTypes.CONVERSION.getType(), conversion, level).orElseThrow().value().assemble(conversion, level.registryAccess()).is(upgraded().asItem()), "converter acquisition route");
        h.assertTrue(UnpackingHandler.REGISTRY.get(upgraded()) == BasinUnpackingHandler.INSTANCE, "native basin package handler is registered");
        h.assertTrue(BasinUnpackingHandler.INSTANCE.unpack(level, basin.getBlockPos(), basin.getBlockState(), Direction.UP,
            new java.util.ArrayList<>(List.of(new ItemStack(Items.STONE, 64), new ItemStack(Items.STONE, 64))), null, true) && count(items(h, basin), Items.STONE) == 0, "package simulation does not consume or insert items");
        h.assertTrue(BasinUnpackingHandler.INSTANCE.unpack(level, basin.getBlockPos(), basin.getBlockState(), Direction.UP,
            new java.util.ArrayList<>(List.of(new ItemStack(Items.STONE, 64), new ItemStack(Items.STONE, 64))), null, false) && count(items(h, basin), Items.STONE) == 128, "package inserts repeated stacks");
        h.assertTrue(basin.inputInventory.insertItem(2, new ItemStack(Items.STONE, 64), false).isEmpty(), "normal multistack insertion still works after package mode resets");
        var context = new com.simibubi.create.content.contraptions.behaviour.MovementContext(level,
            new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(basin.getBlockPos(), basin.getBlockState(), basin.saveWithFullMetadata(level.registryAccess())), null);
        basin.inputInventory.setStackInSlot(26, new ItemStack(Items.EMERALD, 64));
        context.blockEntityData = basin.saveWithFullMetadata(level.registryAccess());
        var movement = MovementBehaviour.REGISTRY.get(upgraded());
        h.assertTrue(movement instanceof BasinMovementBehaviour, "native basin movement behavior is registered");
        var carried = ((BasinMovementBehaviour) movement).getOrReadInventory(context);
        h.assertTrue(carried.get("InputItems").getSlots() == 27 && carried.get("OutputItems").getSlots() == 27
            && carried.get("InputItems").getStackInSlot(26).getCount() == 64, "movement reads all27 saved input and product cells");
        h.assertTrue(new com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.BasinType().canCreatePoint(level, basin.getBlockPos(), basin.getBlockState()), "native mechanical arm recognizes the upgrade");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 380)
    public static void allFourOperatorsUseHighSlotsAndRespectBasinFilter(GameTestHelper h) {
        var basins = new BasinBlockEntity[4];
        var mixers = new boolean[] {false, true, false, true};
        for (int i = 0; i < 4; i++) {
            basins[i] = operator(h, mixers[i], i >= 2, 150 + 25 * i);
            basins[i].getFilter().setFilter(new ItemStack(Items.BEDROCK));
            if (mixers[i]) {
                basins[i].inputInventory.setStackInSlot(24, new ItemStack(Items.LAPIS_LAZULI));
                basins[i].inputInventory.setStackInSlot(25, new ItemStack(Items.EGG));
                basins[i].inputInventory.setStackInSlot(26, new ItemStack(Items.SUGAR));
            } else basins[i].inputInventory.setStackInSlot(26, new ItemStack(Items.LAPIS_LAZULI, 9));
        }
        h.runAfterDelay(100, () -> {
            for (int i = 0; i < 4; i++) {
                h.assertTrue(basins[i].getOutputInventory().isEmpty() && !basins[i].inputInventory.isEmpty(), "basin recipe filter blocks operator " + i);
                basins[i].getFilter().setFilter(ItemStack.EMPTY);
            }
        });
        h.succeedWhen(() -> {
            for (int i = 0; i < 4; i++) {
                h.assertTrue(count(basins[i].getOutputInventory(), mixers[i] ? Items.DIAMOND : Items.LAPIS_BLOCK) == 1, "native processing consumes new high-index slots for operator " + i);
                h.assertTrue(basins[i].inputInventory.isEmpty(), "operator consumes exactly its ingredients " + i);
            }
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 70)
    public static void sideOutputStillFeedsARegularBasin(GameTestHelper h) {
        var source = basin(h, false, 1);
        var level = h.getLevel();
        BlockPos targetPos = source.getBlockPos().east().below();
        level.setBlockAndUpdate(targetPos, AllBlocks.BASIN.getDefaultState());
        var target = (BasinBlockEntity) level.getBlockEntity(targetPos);
        h.runAfterDelay(4, () -> {
            if (source.getBlockState().getValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING) == Direction.EAST) {
                source.onWrenched(Direction.EAST);
                h.assertTrue(source.getBlockState().getValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING) == Direction.DOWN, "wrench can disable an automatically selected outlet");
            }
            source.onWrenched(Direction.EAST);
            h.assertTrue(source.getBlockState().getValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING) == Direction.EAST, "native wrench selects the valid outlet");
            h.assertTrue(source.acceptOutputs(List.of(new ItemStack(Items.DIAMOND, 64)), List.of(), false), "native side outlet accepts product stack");
        });
        h.succeedWhen(() -> h.assertTrue(count(target.inputInventory, Items.DIAMOND) == 64 && source.getOutputInventory().isEmpty(), "side outlet delivers the complete stack to an ordinary basin"));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 25)
    public static void multipleProductsReserveSpaceTogetherBeforeConsumingInputs(GameTestHelper h) {
        var basin = basin(h, false, 1);
        var output = basin.getOutputInventory();
        for (int i = 0; i < 25; i++) output.setStackInSlot(i, new ItemStack(Items.STONE, 64));
        output.setStackInSlot(25, new ItemStack(Items.DIAMOND, 63));
        basin.inputInventory.setStackInSlot(26, new ItemStack(Items.EMERALD));
        var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "qa_basin_double_product")).orElseThrow().value();
        h.assertTrue(!com.simibubi.create.content.processing.basin.BasinRecipe.match(basin, recipe), "65 free diamond positions cannot hold128 recipe products");
        h.assertTrue(!com.simibubi.create.content.processing.basin.BasinRecipe.apply(basin, recipe), "full collective output prevents ingredient consumption");
        h.assertTrue(count(basin.inputInventory, Items.EMERALD) == 1 && count(output, Items.DIAMOND) == 63, "blocked recipe preserves both ingredient and existing products");
        output.setStackInSlot(24, ItemStack.EMPTY);
        h.assertTrue(com.simibubi.create.content.processing.basin.BasinRecipe.match(basin, recipe)
            && com.simibubi.create.content.processing.basin.BasinRecipe.apply(basin, recipe), "enough collective space permits the real recipe");
        h.assertTrue(basin.inputInventory.isEmpty() && count(output, Items.DIAMOND) == 191, "successful execution adds all128 products exactly once");
        h.succeed();
    }

    private static BasinBlockEntity operator(GameTestHelper h, boolean mixer, boolean upgradedMachine, int height) {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, height, 1));
        level.setBlockAndUpdate(pos.below(2), upgraded().defaultBlockState());
        Block machine = mixer
            ? (upgradedMachine ? BoosterBlocks.NETHERITE_MECHANICAL_MIXER.get() : AllBlocks.MECHANICAL_MIXER.get())
            : (upgradedMachine ? BoosterBlocks.NETHERITE_MECHANICAL_PRESS.get() : AllBlocks.MECHANICAL_PRESS.get());
        var state = machine.defaultBlockState();
        if (!mixer) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, state);
        BlockPos motor;
        Direction facing;
        if (mixer) {
            level.setBlockAndUpdate(pos.west(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
            level.setBlockAndUpdate(pos.west().above(), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
            motor = pos.west().above(2); facing = Direction.DOWN;
        } else { motor = pos.west(); facing = Direction.EAST; }
        level.setBlockAndUpdate(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, facing));
        ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        h.runAfterDelay(4, () -> h.assertTrue(Math.abs(((KineticBlockEntity) level.getBlockEntity(pos)).getSpeed()) >= 64, "operator has actual motor power"));
        return (BasinBlockEntity) level.getBlockEntity(pos.below(2));
    }

    private static Block upgraded() { return BuiltInRegistries.BLOCK.getOptional(ID).orElse(AllBlocks.BASIN.get()); }
    private static BasinBlockEntity basin(GameTestHelper h, boolean original, int offset) {
        BlockPos pos = h.absolutePos(new BlockPos(offset, 240, 1));
        h.getLevel().setBlockAndUpdate(pos, (original ? AllBlocks.BASIN.get() : upgraded()).defaultBlockState());
        return (BasinBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static IItemHandler items(GameTestHelper h, BasinBlockEntity basin) {
        var cap = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(), Direction.UP);
        h.assertTrue(cap != null, "item capability exists"); return cap;
    }
    private static int count(IItemHandler inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.getSlots(); i++) if (inv.getStackInSlot(i).is(item)) count += inv.getStackInSlot(i).getCount();
        return count;
    }
}
