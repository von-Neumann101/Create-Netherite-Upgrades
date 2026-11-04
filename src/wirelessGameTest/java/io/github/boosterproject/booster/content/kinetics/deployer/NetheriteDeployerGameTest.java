package io.github.boosterproject.booster.content.kinetics.deployer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import io.github.boosterproject.booster.Booster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteDeployerGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_deployer");

    @GameTest(template = "wireless_empty", timeoutTicks = 200)
    public static void realBeltProcessesWholeStack(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(1, 280, 1));
        BlockPos deployerPos = controller.south().above(2), drive = controller.west(), handDrive = deployerPos.west();
        for (int i = 0; i < 3; i++) {
            var state = AllBlocks.BELT.get().defaultBlockState()
                .setValue(com.simibubi.create.content.kinetics.belt.BeltBlock.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(com.simibubi.create.content.kinetics.belt.BeltBlock.SLOPE, com.simibubi.create.content.kinetics.belt.BeltSlope.HORIZONTAL)
                .setValue(com.simibubi.create.content.kinetics.belt.BeltBlock.PART, i == 0 ? com.simibubi.create.content.kinetics.belt.BeltPart.START
                    : i == 2 ? com.simibubi.create.content.kinetics.belt.BeltPart.END : com.simibubi.create.content.kinetics.belt.BeltPart.MIDDLE);
            level.setBlock(controller.south(i), state, 3);
            var be = (com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller.south(i));
            be.setController(controller); be.beltLength = 3; be.index = i;
        }
        level.setBlock(deployerPos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState().setValue(DeployerBlock.FACING, Direction.DOWN)
            .setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, true), 3);
        for (BlockPos motor : new BlockPos[] {drive, handDrive}) {
            level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
            ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        }
        helper.runAfterDelay(3, () -> {
            ((DeployerBlockEntity) level.getBlockEntity(deployerPos)).getPlayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.ANDESITE_ALLOY.get(), 32));
            var carried = new TransportedItemStack(new ItemStack(Items.STRIPPED_OAK_LOG, 32));
            carried.beltPosition = carried.prevBeltPosition = .5F;
            ((com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller)).getInventory().addItem(carried);
        });
        helper.succeedWhen(() -> {
            var stacks = ((com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller)).getInventory().getTransportedItems();
            int output = stacks.stream().filter(s -> s.stack.is(AllBlocks.ANDESITE_CASING.asItem())).mapToInt(s -> s.stack.getCount()).sum();
            helper.assertTrue(output > 0, "moving belt really reaches the deployer's processing point");
            helper.assertTrue(output == 32 && ((DeployerBlockEntity) level.getBlockEntity(deployerPos)).getPlayer().getMainHandItem().isEmpty(),
                "one real belt action processes all thirty-two inputs and application materials");
            level.removeBlock(deployerPos, false); level.removeBlock(drive, false); level.removeBlock(handDrive, false);
            for (int i = 0; i < 3; i++) level.removeBlock(controller.south(i), false);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void nativeLogisticsAndMovementRegistrations(GameTestHelper helper) {
        var machine = machine(helper, 260, false);
        BlockPos armPos = machine.depot.south(3);
        helper.getLevel().setBlock(armPos, AllBlocks.MECHANICAL_ARM.get().defaultBlockState(), 3);
        helper.runAfterDelay(3, () -> {
            var level = helper.getLevel(); var state = level.getBlockState(machine.deployer);
            var point = com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.create(level, machine.deployer, state);
            helper.assertTrue(point != null && point.isValid(), "native and upgraded arms can select the new deployer");
            var arm = (com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity) level.getBlockEntity(armPos);
            ItemStack alloy = new ItemStack(AllItems.ANDESITE_ALLOY.get(), 4);
            helper.assertTrue(point.insert(arm, alloy.copy(), true).isEmpty()
                && ((DeployerBlockEntity) level.getBlockEntity(machine.deployer)).getPlayer().getMainHandItem().isEmpty(), "arm simulation does not consume or insert materials");
            helper.assertTrue(point.insert(arm, alloy.copy(), false).isEmpty(), "arm inserts real application materials");
            helper.assertTrue(point.extract(arm, 0, 2, false).getCount() == 2, "arm extracts real tools or materials through native capability");
            var be = (DeployerBlockEntity) level.getBlockEntity(machine.deployer);
            var saved = be.saveWithoutMetadata(level.registryAccess()); be.loadWithComponents(saved, level.registryAccess());
            helper.assertTrue(be.getPlayer().getMainHandItem().getCount() == 2, "deployer hand inventory persists through reload");
            helper.assertTrue(com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.get(state.getBlock())
                instanceof com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour, "native moving action is registered");
            helper.assertTrue(com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.REGISTRY.get(state.getBlock())
                instanceof com.simibubi.create.content.kinetics.deployer.DeployerMovingInteraction, "moving wrench mode and hand exchange interaction is registered");
            remove(helper, machine); level.removeBlock(armPos, false); helper.succeed();
        });
    }

    private record Machine(BlockPos depot, BlockPos deployer, BlockPos motor) {}
    private static Machine machine(GameTestHelper helper, int height, boolean original) {
        var level = helper.getLevel();
        BlockPos depot = helper.absolutePos(new BlockPos(1, height, 1)), deployer = depot.above(2), motor = deployer.west();
        level.setBlock(depot, AllBlocks.DEPOT.get().defaultBlockState(), 3);
        level.setBlock(deployer, (original ? AllBlocks.DEPLOYER.get() : BuiltInRegistries.BLOCK.get(ID)).defaultBlockState()
            .setValue(DeployerBlock.FACING, Direction.DOWN).setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, true), 3);
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        return new Machine(depot, deployer, motor);
    }
    private static void load(GameTestHelper helper, Machine machine, ItemStack input, ItemStack held) {
        ((DeployerBlockEntity) helper.getLevel().getBlockEntity(machine.deployer)).getPlayer().setItemInHand(InteractionHand.MAIN_HAND, held);
        var depot = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(helper.getLevel(), machine.depot, DepotBehaviour.TYPE);
        depot.setCenteredHeldItem(new TransportedItemStack(input));
    }
    private static int count(GameTestHelper helper, Machine machine, net.minecraft.world.item.Item item) {
        var inventory = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, machine.depot, null);
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) if (inventory.getStackInSlot(slot).is(item)) count += inventory.getStackInSlot(slot).getCount();
        return count;
    }
    private static void remove(GameTestHelper helper, Machine machine) {
        helper.getLevel().removeBlock(machine.deployer, false); helper.getLevel().removeBlock(machine.depot, false); helper.getLevel().removeBlock(machine.motor, false);
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void materialLimitAndOriginalIsolation(GameTestHelper helper) {
        var upgraded = machine(helper, 260, false); var original = machine(helper, 280, true);
        helper.runAfterDelay(3, () -> {
            load(helper, upgraded, new ItemStack(Items.STRIPPED_OAK_LOG, 9), new ItemStack(AllItems.ANDESITE_ALLOY.get(), 3));
            load(helper, original, new ItemStack(Items.STRIPPED_OAK_LOG, 9), new ItemStack(AllItems.ANDESITE_ALLOY.get(), 9));
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, upgraded, AllBlocks.ANDESITE_CASING.asItem()) > 0
                && count(helper, original, AllBlocks.ANDESITE_CASING.asItem()) > 0, "both machines finish their first action");
            helper.assertTrue(count(helper, upgraded, AllBlocks.ANDESITE_CASING.asItem()) == 3 && count(helper, upgraded, Items.STRIPPED_OAK_LOG) == 6,
                "batch stops at available material and keeps six raw inputs");
            helper.assertTrue(count(helper, original, AllBlocks.ANDESITE_CASING.asItem()) == 1 && count(helper, original, Items.STRIPPED_OAK_LOG) == 8,
                "original deployer still processes one input per action");
            var be = (DeployerBlockEntity) helper.getLevel().getBlockEntity(upgraded.deployer);
            var tag = be.saveWithoutMetadata(helper.getLevel().registryAccess()); be.loadWithComponents(tag, helper.getLevel().registryAccess());
            helper.assertTrue(be.getPlayer().getMainHandItem().isEmpty(), "material exhaustion survives save reload");
            remove(helper, upgraded); remove(helper, original);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void toolsAndRetainedCatalysts(GameTestHelper helper) {
        var polishing = machine(helper, 260, false); var retained = machine(helper, 280, false);
        ItemStack sandpaper = new ItemStack(AllItems.SAND_PAPER.get()); sandpaper.setDamageValue(sandpaper.getMaxDamage() - 2);
        helper.runAfterDelay(3, () -> {
            load(helper, polishing, new ItemStack(AllItems.ROSE_QUARTZ.get(), 5), sandpaper);
            load(helper, retained, new ItemStack(Items.COPPER_BLOCK, 20), new ItemStack(Items.HONEYCOMB_BLOCK));
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, polishing, AllItems.POLISHED_ROSE_QUARTZ.get()) > 0
                && count(helper, retained, Items.WAXED_COPPER_BLOCK) > 0, "polishing and keep-held recipes finish first action");
            helper.assertTrue(count(helper, polishing, AllItems.POLISHED_ROSE_QUARTZ.get()) == 2 && count(helper, polishing, AllItems.ROSE_QUARTZ.get()) == 3
                && ((DeployerBlockEntity) helper.getLevel().getBlockEntity(polishing.deployer)).getPlayer().getMainHandItem().isEmpty(),
                "two remaining tool uses process two items and preserve three raw inputs");
            helper.assertTrue(count(helper, retained, Items.WAXED_COPPER_BLOCK) == 20
                && ((DeployerBlockEntity) helper.getLevel().getBlockEntity(retained.deployer)).getPlayer().getMainHandItem().getCount() == 1,
                "keep-held catalyst processes entire stack without consuming catalyst");
            remove(helper, polishing); remove(helper, retained);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void returnedContainersAndZeroOutputRolls(GameTestHelper helper) {
        var bottles = machine(helper, 260, false); var zero = machine(helper, 280, false);
        helper.runAfterDelay(3, () -> {
            var zeroRecipe = (com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "qa_deployer_zero")).orElseThrow().value();
            zeroRecipe.getRollableResults().clear(); // Deterministic all-missed roll; JSON chance zero is invalid in Create.
            load(helper, bottles, new ItemStack(Items.CLAY_BALL, 7), new ItemStack(Items.HONEY_BOTTLE, 4));
            load(helper, zero, new ItemStack(Items.COBBLESTONE, 8), new ItemStack(Items.STICK, 8));
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, bottles, Items.BRICK) > 0, "bottle recipe completes first action");
            helper.assertTrue(count(helper, bottles, Items.BRICK) == 4 && count(helper, bottles, Items.CLAY_BALL) == 3,
                "four consumed bottles make four outputs and preserve three inputs");
            var be = (DeployerBlockEntity) helper.getLevel().getBlockEntity(bottles.deployer);
            int glass = 0;
            for (var stack : be.getPlayer().getInventory().items) if (stack.is(Items.GLASS_BOTTLE)) glass += stack.getCount();
            helper.assertTrue(glass == 4, "each consumed bottle really returns its container; count=" + glass
                + ", inventory=" + be.getPlayer().getInventory().items.stream().filter(s -> !s.isEmpty()).toList());
            helper.assertTrue(count(helper, zero, Items.COBBLESTONE) == 0 && count(helper, zero, Items.GOLD_NUGGET) == 0
                && ((DeployerBlockEntity) helper.getLevel().getBlockEntity(zero.deployer)).getPlayer().getMainHandItem().isEmpty(),
                "zero-output rolls consume exactly eight inputs and eight applications without resurrecting remainders");
            remove(helper, bottles); remove(helper, zero);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void sequencedAssemblyAdvancesOneStepPerInput(GameTestHelper helper) {
        var machine = machine(helper, 260, false);
        helper.runAfterDelay(3, () -> load(helper, machine, new ItemStack(Items.PAPER, 16), new ItemStack(Items.IRON_NUGGET, 64)));
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, machine, AllItems.INCOMPLETE_PRECISION_MECHANISM.get()) > 0, "sequenced assembly produces transitional items");
            var inventory = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, machine.depot, null);
            java.util.List<ItemStack> outputs = new java.util.ArrayList<>();
            for (int slot = 0; slot < inventory.getSlots(); slot++) if (!inventory.getStackInSlot(slot).isEmpty()) outputs.add(inventory.getStackInSlot(slot));
            for (var item : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(machine.depot).inflate(3))) outputs.add(item.getItem());
            helper.assertTrue(outputs.stream().mapToInt(ItemStack::getCount).sum() == 16 && outputs.stream().allMatch(output ->
                    output.is(AllItems.INCOMPLETE_PRECISION_MECHANISM.get()) && output.getCount() <= output.getMaxStackSize()
                    && output.get(AllDataComponents.SEQUENCED_ASSEMBLY).step() == 1),
                "sixteen unstackable transitional items each advance one step; depot overflow drops retain every item");
            helper.assertTrue(((DeployerBlockEntity) helper.getLevel().getBlockEntity(machine.deployer)).getPlayer().getMainHandItem().getCount() == 48,
                "sequence step costs one application per input");
            remove(helper, machine);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void oneActionProcessesWholeDepotStack(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ID), "netherite deployer must be registered");
        var level = helper.getLevel();
        BlockPos depotPos = helper.absolutePos(new BlockPos(1, 240, 1));
        BlockPos pos = depotPos.above(2), motor = pos.west();
        level.setBlock(depotPos, AllBlocks.DEPOT.get().defaultBlockState(), 3);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState().setValue(DeployerBlock.FACING, Direction.DOWN)
            .setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, true), 3);
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        helper.runAfterDelay(3, () -> {
            var deployer = (DeployerBlockEntity) level.getBlockEntity(pos);
            deployer.getPlayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.ANDESITE_ALLOY.get(), 32));
            var depot = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(level, depotPos, DepotBehaviour.TYPE);
            depot.setCenteredHeldItem(new TransportedItemStack(new ItemStack(Items.STRIPPED_OAK_LOG, 32)));
        });
        helper.succeedWhen(() -> {
            var depot = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(level, depotPos, DepotBehaviour.TYPE);
            helper.assertTrue(depot.getHeldItemStack().is(AllBlocks.ANDESITE_CASING.asItem()), "first action makes andesite casings");
            helper.assertTrue(depot.getHeldItemStack().getCount() == 32, "first action processes all 32 inputs");
            helper.assertTrue(((DeployerBlockEntity) level.getBlockEntity(pos)).getPlayer().getMainHandItem().isEmpty(), "all 32 application materials are consumed");
            level.removeBlock(pos, false); level.removeBlock(depotPos, false); level.removeBlock(motor, false);
        });
    }
}
