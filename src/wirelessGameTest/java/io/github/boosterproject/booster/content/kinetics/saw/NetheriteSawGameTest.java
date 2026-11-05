package io.github.boosterproject.booster.content.kinetics.saw;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteSawGameTest {
    private record Machine(BlockPos pos, BlockPos motor, SawBlockEntity be) {}

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void netheriteSawIsRegisteredWithNativeLogisticsAndRecipes(GameTestHelper helper) {
        var machine = machine(helper, 105, false, Direction.UP, 64);
        var level = helper.getLevel();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(machine.be instanceof NetheriteSawBlockEntity && machine.be.getSpeed() == 64, "registered entity receives real motor rotation");
            helper.assertTrue(BlockStressValues.getImpact(BoosterBlocks.NETHERITE_MECHANICAL_SAW.get()) == BlockStressValues.getImpact(AllBlocks.MECHANICAL_SAW.get()), "same RPM uses original saw stress impact");
            var input = level.getCapability(Capabilities.ItemHandler.BLOCK, machine.pos, Direction.UP);
            helper.assertTrue(input != null && input.getSlotLimit(0) == Item.ABSOLUTE_MAX_STACK_SIZE && level.getCapability(Capabilities.ItemHandler.BLOCK, machine.pos, Direction.DOWN) == null,
                "native bulk input capability uses absolute slot limit and blocked bottom face; slotLimit=" + (input == null ? -1 : input.getSlotLimit(0)));
            helper.assertTrue(!input.isItemValid(1, new ItemStack(Items.STONE)), "output slots refuse automation input");
            helper.assertTrue(BoosterItems.NETHERITE_MECHANICAL_SAW.get().getDefaultInstance().has(DataComponents.FIRE_RESISTANT) && level.getBlockState(machine.pos).getBlock().getExplosionResistance() >= 1200, "fire and explosion resistance");
            helper.assertTrue(MovementBehaviour.REGISTRY.get(BoosterBlocks.NETHERITE_MECHANICAL_SAW.get()) instanceof NetheriteSawMovementBehaviour, "upgraded movement behavior is registered");
            var crafting = CraftingInput.of(2, 1, List.of(new ItemStack(BoosterItems.NETHERITE_SHEET.get()), new ItemStack(AllBlocks.MECHANICAL_SAW.get())));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, crafting, level).orElseThrow();
            helper.assertTrue(recipe.value().assemble(crafting, level.registryAccess()).is(BoosterItems.NETHERITE_MECHANICAL_SAW.get()), "sheet upgrade recipe produces new saw");
            var conversion = new SingleRecipeInput(new ItemStack(AllBlocks.MECHANICAL_SAW.get()));
            helper.assertTrue(level.getRecipeManager().getRecipeFor(BoosterRecipeTypes.CONVERSION.getType(), conversion, level).orElseThrow().value().assemble(conversion, level.registryAccess()).is(BoosterItems.NETHERITE_MECHANICAL_SAW.get()), "real conversion recipe produces new saw");
            remove(helper, machine); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 120)
    public static void fullStackCutsInOneItemCycleAndOriginalIsUnchanged(GameTestHelper helper) {
        var bulk = machine(helper, 125, false, Direction.UP, 64);
        var single = machine(helper, 145, false, Direction.UP, 64);
        var original = machine(helper, 165, true, Direction.UP, 64);
        boolean[] loaded = {false};
        helper.runAfterDelay(3, () -> {
            filter(bulk.be, Items.DIAMOND); filter(single.be, Items.DIAMOND); filter(original.be, Items.DIAMOND);
            helper.assertTrue(insert(helper, bulk, new ItemStack(Items.STONE, 64)).isEmpty(), "real input handler accepts all 64 items");
            helper.assertTrue(insert(helper, single, new ItemStack(Items.STONE)).isEmpty(), "single-item comparison is inserted");
            ItemStack remainder = insert(helper, original, new ItemStack(Items.STONE, 64));
            helper.assertTrue(original.be.inventory.getSlotLimit(0) == (AllConfigs.server().recipes.bulkCutting.get() ? Item.ABSOLUTE_MAX_STACK_SIZE : 1) && remainder.getCount() == 64 - original.be.inventory.getStackInSlot(0).getCount(), "original bulk configuration remains unchanged");
            helper.assertTrue(bulk.be.inventory.recipeDuration == single.be.inventory.recipeDuration && bulk.be.inventory.recipeDuration == 100,
                "64 inputs have the same native recipe duration as one; bulk=" + bulk.be.inventory.recipeDuration + ", single=" + single.be.inventory.recipeDuration);
            loaded[0] = true;
        });
        helper.onEachTick(() -> {
            if (!loaded[0] || !bulk.be.inventory.appliedRecipe) return;
            helper.assertTrue(single.be.inventory.appliedRecipe && count(bulk.be.inventory, Items.DIAMOND) == 128 && count(single.be.inventory, Items.DIAMOND) == 2 && count(bulk.be.inventory, Items.STONE) == 0, "one completion rolls and consumes all 64 inputs");
            remove(helper, bulk, single, original); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 120)
    public static void largeOutputKeepsAllStacksBeyondNativeThirtyOneSlots(GameTestHelper helper) {
        var machine = machine(helper, 120, false, Direction.UP, 64);
        helper.runAfterDelay(3, () -> { filter(machine.be, Items.DIAMOND); insert(helper, machine, new ItemStack(Items.AMETHYST_SHARD, 64)); });
        helper.onEachTick(() -> {
            if (!machine.be.inventory.appliedRecipe) return;
            helper.assertTrue(count(machine.be.inventory, Items.DIAMOND) == 4096 && machine.be.inventory.getSlots() >= 65, "4096 products retain all 64 output stacks");
            var saved = machine.be.saveWithoutMetadata(helper.getLevel().registryAccess()); machine.be.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(count(machine.be.inventory, Items.DIAMOND) == 4096, "expanded output inventory survives native save/reload");
            remove(helper, machine); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 120)
    public static void zeroChanceOutputStillReturnsEveryContainer(GameTestHelper helper) {
        var machine = machine(helper, 125, false, Direction.UP, 64);
        var recipe = (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "qa_saw_zero")).orElseThrow().value();
        var originalResult = recipe.getRollableResults().getFirst();
        // Create's JSON codec requires positive chance. Its public runtime API permits a deterministic missed roll.
        recipe.getRollableResults().set(0, new ProcessingOutput(new ItemStack(Items.SUGAR), 0));
        helper.runAfterDelay(3, () -> { filter(machine.be, Items.SUGAR); insert(helper, machine, new ItemStack(Items.HONEY_BOTTLE, 16)); });
        helper.onEachTick(() -> {
            if (!machine.be.inventory.appliedRecipe) return;
            helper.assertTrue(count(machine.be.inventory, Items.SUGAR) == 0 && count(machine.be.inventory, Items.GLASS_BOTTLE) == 16 && count(machine.be.inventory, Items.HONEY_BOTTLE) == 0,
                "zero-chance rolls produce nothing but return every consumed bottle; sugar=" + count(machine.be.inventory, Items.SUGAR) + ", empty=" + count(machine.be.inventory, Items.GLASS_BOTTLE) + ", input=" + count(machine.be.inventory, Items.HONEY_BOTTLE));
            recipe.getRollableResults().set(0, originalResult); remove(helper, machine); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 140)
    public static void nativeStonecuttingFilterAndSequenceAdvanceOncePerInput(GameTestHelper helper) {
        var stone = machine(helper, 125, false, Direction.UP, 64);
        var sequence = machine(helper, 145, false, Direction.UP, 64);
        boolean[] loaded = {false}, seen = {false, false};
        helper.runAfterDelay(3, () -> {
            filter(stone.be, Items.COBBLESTONE_SLAB); insert(helper, stone, new ItemStack(Items.COBBLESTONE, 12));
            insert(helper, sequence, new ItemStack(Items.PRISMARINE_SHARD, 12)); loaded[0] = true;
        });
        helper.onEachTick(() -> {
            if (!loaded[0]) return;
            if (!seen[0] && stone.be.inventory.appliedRecipe) {
                helper.assertTrue(count(stone.be.inventory, Items.COBBLESTONE_SLAB) == 24, "native result filter chooses slab stonecutting for the full stack"); seen[0] = true;
            }
            if (!seen[1] && sequence.be.inventory.appliedRecipe) {
                int progressed = 0;
                for (int i = 1; i < sequence.be.inventory.getSlots(); i++) {
                    var stack = sequence.be.inventory.getStackInSlot(i); if (stack.isEmpty()) continue;
                    var progress = stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
                    helper.assertTrue(progress != null && progress.step() == 1, "each sequence result advances exactly one cutting step"); progressed += stack.getCount();
                }
                helper.assertTrue(progressed == 12 && count(sequence.be.inventory, Items.EMERALD) == 0, "bulk cutting never finishes future sequence steps"); seen[1] = true;
            }
            if (seen[0] && seen[1]) { remove(helper, stone, sequence); helper.succeed(); }
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 50)
    public static void fixedAndMovingDamageMultiplyOnlyUpgradedSaw(GameTestHelper helper) {
        var upgraded = machine(helper, 125, false, Direction.UP, 64);
        var original = machine(helper, 145, true, Direction.UP, 64);
        helper.runAfterDelay(3, () -> {
            var level = helper.getLevel();
            var a = cow(helper, upgraded.pos.getCenter()); var b = cow(helper, original.pos.getCenter());
            level.getBlockState(upgraded.pos).entityInside(level, upgraded.pos, a); level.getBlockState(original.pos).entityInside(level, original.pos, b);
            float base = (float) DrillBlock.getDamage(64);
            helper.assertTrue(Math.abs((100 - a.getHealth()) - base * 1.8F) < .0001 && Math.abs((100 - b.getHealth()) - base) < .0001, "real fixed collision scales only upgraded saw damage by 1.8");
            a.discard(); b.discard();
            BlockPos target = helper.absolutePos(new BlockPos(1, 185, 1));
            var mobile = context(helper, target, true); var normal = context(helper, target.above(20), false);
            var hit = cow(helper, target.getCenter()); var reference = cow(helper, target.above(20).getCenter());
            new NetheriteSawMovementBehaviour().damageEntities(mobile, target, level); new SawMovementBehaviour().damageEntities(normal, target.above(20), level);
            helper.assertTrue(Math.abs((100 - hit.getHealth()) - (100 - reference.getHealth()) * 1.8F) < .0001 && hit.getDeltaMovement().lengthSqr() > 0,
                "real moving action scales damage and retains native throwing; upgraded=" + (100 - hit.getHealth()) + ", original=" + (100 - reference.getHealth()) + ", motion=" + hit.getDeltaMovement());
            var item = new ItemEntity(level, target.getX() + .5, target.getY() + .5, target.getZ() + .5, new ItemStack(Items.DIAMOND)); level.addFreshEntity(item);
            var passenger = cow(helper, target.getCenter()); passenger.startRiding(mobile.contraption.entity, true);
            helper.assertTrue(passenger.isPassenger(), "moving exclusion test has a real same-vehicle passenger");
            new NetheriteSawMovementBehaviour().damageEntities(mobile, target, level);
            helper.assertTrue(item.isAlive() && item.getItem().getCount() == 1 && passenger.getHealth() == 100, "moving saw preserves item and same-vehicle passenger exclusions");
            hit.discard(); reference.discard(); passenger.discard(); item.discard(); remove(helper, upgraded, original); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 200)
    public static void motorDrivenSawFellsWholeTreeFasterThanOriginal(GameTestHelper helper) {
        var upgraded = machine(helper, 135, false, Direction.EAST, 64); var original = machine(helper, 155, true, Direction.EAST, 64);
        for (var machine : List.of(upgraded, original)) for (int y = 0; y < 3; y++) helper.getLevel().setBlock(machine.pos.east().above(y), Blocks.OAK_LOG.defaultBlockState(), 3);
        long[] times = {-1, -1}; long started = helper.getLevel().getGameTime();
        helper.onEachTick(() -> {
            var level = helper.getLevel();
            if (times[0] < 0 && level.getBlockState(upgraded.pos.east()).isAir()) times[0] = level.getGameTime() - started;
            if (times[1] < 0 && level.getBlockState(original.pos.east()).isAir()) times[1] = level.getGameTime() - started;
            if (times[0] < 0 || times[1] < 0) return;
            if (!level.getBlockState(upgraded.pos.east().above()).isAir() || !level.getBlockState(upgraded.pos.east().above(2)).isAir() || !level.getBlockState(original.pos.east().above()).isAir() || !level.getBlockState(original.pos.east().above(2)).isAir()) return;
            helper.assertTrue(times[0] < times[1], "same real 64RPM fells upgraded tree sooner: " + times[0] + " < " + times[1]);
            remove(helper, upgraded, original); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void waitingInputPersistsAndIsReturnedWhenBlockBreaks(GameTestHelper helper) {
        var machine = machine(helper, 125, false, Direction.UP, 0); var level = helper.getLevel();
        helper.runAfterDelay(3, () -> {
            var stack = new ItemStack(Items.STONE, 12); stack.set(DataComponents.CUSTOM_NAME, Component.literal("Saw payload QA")); filter(machine.be, Items.DIAMOND);
            helper.assertTrue(insert(helper, machine, stack).isEmpty(), "stopped saw accepts and retains real input");
            var saved = machine.be.saveWithoutMetadata(level.registryAccess()); machine.be.loadWithComponents(saved, level.registryAccess());
            helper.assertTrue(ItemStack.matches(machine.be.inventory.getStackInSlot(0), stack) && machine.be.getBehaviour(FilteringBehaviour.TYPE).test(new ItemStack(Items.DIAMOND)), "save reload preserves item components and filtering");
            level.destroyBlock(machine.pos, true); int materials = 0, saws = 0;
            for (var drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(machine.pos).inflate(2))) {
                if (drop.getItem().is(Items.STONE)) { materials += drop.getItem().getCount(); helper.assertTrue(drop.getItem().getHoverName().getString().equals("Saw payload QA"), "returned input keeps components"); }
                if (drop.getItem().is(BoosterItems.NETHERITE_MECHANICAL_SAW.get())) saws += drop.getItem().getCount();
            }
            helper.assertTrue(materials == 12 && saws == 1, "breaking the saw returns exactly its twelve inputs and one machine");
            level.removeBlock(machine.motor, false); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 20)
    public static void assemblyNeverCollectsBlocksInFrontOfSawBlade(GameTestHelper helper) {
        for (var facing : Direction.values()) {
            var upgraded = BoosterBlocks.NETHERITE_MECHANICAL_SAW.get().defaultBlockState().setValue(SawBlock.FACING, facing);
            var original = AllBlocks.MECHANICAL_SAW.getDefaultState().setValue(SawBlock.FACING, facing);
            for (var checkedFace : Direction.values()) {
                helper.assertTrue(BlockMovementChecks.isNotSupportive(upgraded, checkedFace) == (facing == checkedFace),
                    "upgraded saw stops assembly collection only in front of blade: " + facing + "/" + checkedFace);
                helper.assertTrue(BlockMovementChecks.isNotSupportive(original, checkedFace) == (facing == checkedFace), "original saw support rule remains unchanged");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void mechanicalArmCanFeedOnlyRunningUpwardSaw(GameTestHelper helper) {
        var running = machine(helper, 105, false, Direction.UP, 64);
        var stopped = machine(helper, 125, false, Direction.UP, 0);
        var horizontal = machine(helper, 145, false, Direction.EAST, 64);
        var armPos = running.pos.south(3); var level = helper.getLevel();
        level.setBlock(armPos, AllBlocks.MECHANICAL_ARM.getDefaultState(), 3);
        helper.runAfterDelay(3, () -> {
            var arm = (ArmBlockEntity) level.getBlockEntity(armPos);
            var point = ArmInteractionPoint.create(level, running.pos, level.getBlockState(running.pos));
            helper.assertTrue(point != null && point.isValid(), "real arm registry creates a point for the running upward upgraded saw");
            helper.assertTrue(ArmInteractionPoint.create(level, stopped.pos, level.getBlockState(stopped.pos)) == null
                && ArmInteractionPoint.create(level, horizontal.pos, level.getBlockState(horizontal.pos)) == null,
                "stopped and horizontal saws are not arm processing targets");
            filter(running.be, Items.DIAMOND); var input = new ItemStack(Items.STONE, 12);
            helper.assertTrue(point.insert(arm, input.copy(), true).isEmpty() && running.be.inventory.isEmpty(), "arm insertion simulation preserves saw inventory");
            helper.assertTrue(point.insert(arm, input.copy(), false).isEmpty() && running.be.inventory.getStackInSlot(0).getCount() == 12,
                "real mechanical arm point inserts the full processing stack");
            remove(helper, running, stopped, horizontal); level.removeBlock(armPos, false); helper.succeed();
        });
    }

    private static Machine machine(GameTestHelper helper, int y, boolean original, Direction facing, int rpm) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(1, y, 1));
        var block = original ? AllBlocks.MECHANICAL_SAW.get() : BoosterBlocks.NETHERITE_MECHANICAL_SAW.get();
        var state = block.defaultBlockState().setValue(SawBlock.FACING, facing).setValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE, false); level.setBlock(pos, state, 3);
        var axis = block.getRotationAxis(state);
        Direction motorFacing = axis == Direction.Axis.X ? Direction.EAST : axis == Direction.Axis.Z ? Direction.SOUTH : Direction.UP;
        var motor = pos.relative(motorFacing.getOpposite()); level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, motorFacing), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(rpm);
        return new Machine(pos, motor, (SawBlockEntity) level.getBlockEntity(pos));
    }
    private static ItemStack insert(GameTestHelper helper, Machine machine, ItemStack stack) { return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.pos, Direction.UP).insertItem(0, stack, false); }
    private static void filter(SawBlockEntity saw, Item item) { saw.getBehaviour(FilteringBehaviour.TYPE).setFilter(new ItemStack(item)); }
    private static int count(IItemHandler inventory, Item item) { int count = 0; for (int i = 0; i < inventory.getSlots(); i++) if (inventory.getStackInSlot(i).is(item)) count += inventory.getStackInSlot(i).getCount(); return count; }
    private static void remove(GameTestHelper helper, Machine... machines) { for (var machine : machines) { helper.getLevel().removeBlock(machine.pos, false); helper.getLevel().removeBlock(machine.motor, false); } }
    private static Cow cow(GameTestHelper helper, Vec3 position) { var cow = EntityType.COW.create(helper.getLevel()); cow.setNoAi(true); cow.setNoGravity(true); cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100); cow.setPos(position); helper.getLevel().addFreshEntity(cow); return cow; }
    private static MovementContext context(GameTestHelper helper, BlockPos pos, boolean upgraded) {
        var contraption = new MountedContraption();
        contraption.anchor = pos;
        contraption.bounds = new AABB(BlockPos.ZERO);
        contraption.getSeats().add(BlockPos.ZERO);
        var entity = OrientedContraptionEntity.create(helper.getLevel(), contraption, Direction.EAST);
        entity.setPos(pos.getCenter());
        // Represents an already running contraption after its native ten-tick assembly damage grace period.
        entity.nonDamageTicks = 0;
        var state = (upgraded ? BoosterBlocks.NETHERITE_MECHANICAL_SAW.get() : AllBlocks.MECHANICAL_SAW.get()).defaultBlockState().setValue(SawBlock.FACING, Direction.EAST);
        var context = new MovementContext(helper.getLevel(), new StructureBlockInfo(BlockPos.ZERO, state, new CompoundTag()), contraption); context.position = pos.getCenter(); context.motion = context.relativeMotion = new Vec3(.2, 0, 0); return context;
    }
}
