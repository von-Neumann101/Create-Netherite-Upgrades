package io.github.boosterproject.booster.content.conversion;

import io.github.boosterproject.booster.Booster;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteConverterGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void registeredMachineFluidAndRecipes(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id("netherite_converter")), "converter block must be registered");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id("netherite_converter")), "converter item must be registered");
        helper.assertTrue(BuiltInRegistries.FLUID.containsKey(id("nether_essence"))
            && BuiltInRegistries.ITEM.containsKey(id("nether_essence_bucket")), "essence and bucket must be registered");
        for (String recipe : new String[] {"netherite_converter", "mixing/nether_essence", "conversion/netherite_deployer", "conversion/empty_netherite_blaze_burner"})
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id(recipe)).isPresent(), "production recipe is loaded: " + recipe);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 250)
    public static void fixedStressAndDurationAtDifferentSpeeds(GameTestHelper helper) {
        Fixture slow = fixture(helper, 230, 16), fast = fixture(helper, 238, 256);
        Fixture[] fixtures = {slow, fast};
        long[] starts = {-1, -1}; boolean[] finished = {false, false};
        helper.runAfterDelay(5, () -> {
            for (Fixture f : fixtures) { supply(f, 4000); put(f, new ItemStack(AllBlocks.MECHANICAL_PRESS.get())); }
        });
        helper.onEachTick(() -> {
            for (int i = 0; i < fixtures.length; i++) {
                Fixture f = fixtures[i];
                if (f.be.getProgress() == 1 && starts[i] == -1) starts[i] = helper.getLevel().getGameTime();
                if (starts[i] == -1 || finished[i]) continue;
                helper.assertTrue(f.be.getOrCreateNetwork().getActualStressOf(f.be) == 2048, "actual network consumes 2048SU at " + f.rpm + " RPM");
                long elapsed = helper.getLevel().getGameTime() - starts[i];
                if (elapsed == 98) helper.assertTrue(count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 0
                    && f.be.getEssenceTank().getFluidAmount() == 4000 && ConversionPlate.remaining(f.be.getPlateInventory().getStackInSlot(0)) == 16,
                    "nothing is consumed or converted before 100 processing ticks");
                if (count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 1) {
                    helper.assertTrue(elapsed >= 99 && elapsed <= 100, "conversion takes 100 ticks at " + f.rpm + " RPM; elapsed=" + elapsed);
                    helper.assertTrue(f.be.getEssenceTank().getFluidAmount() == 3750 && ConversionPlate.remaining(f.be.getPlateInventory().getStackInSlot(0)) == 15,
                        "one output costs exactly 250mB and one of sixteen sheet uses");
                    finished[i] = true;
                }
            }
            if (finished[0] && finished[1]) { cleanup(helper, slow); cleanup(helper, fast); helper.succeed(); }
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 350)
    public static void missingResourcesAndPowerLossRestart(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        int[] stage = {0}; long[] stopped = {-1}, restarted = {-1};
        helper.runAfterDelay(5, () -> put(f, new ItemStack(AllBlocks.MECHANICAL_PRESS.get())));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(f.be.getProgress() == 0, "missing plate and fluid do not start progress");
            supply(f, 249);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(f.be.getProgress() == 0 && f.be.getEssenceTank().getFluidAmount() == 249, "249mB cannot start or be partially consumed");
            f.be.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1), FluidAction.EXECUTE);
        });
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (stage[0] == 0 && f.be.getProgress() >= 30) { f.motor.generatedSpeed.setValue(0); stopped[0] = now; stage[0] = 1; }
            if (stage[0] == 1 && now - stopped[0] >= 10) {
                helper.assertTrue(f.be.getProgress() == 0 && f.be.getEssenceTank().getFluidAmount() == 250
                    && ConversionPlate.remaining(f.be.getPlateInventory().getStackInSlot(0)) == 16, "power loss clears elapsed work without consuming resources");
                f.motor.generatedSpeed.setValue(256); stage[0] = 2;
            }
            if (stage[0] == 2 && f.be.getProgress() == 1) { restarted[0] = now; stage[0] = 3; }
            if (stage[0] == 3 && count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 1) {
                helper.assertTrue(now - restarted[0] >= 99 && f.be.getEssenceTank().isEmpty(), "restored faster power still needs a fresh full 100 ticks");
                cleanup(helper, f); helper.succeed();
            }
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 1900)
    public static void oneSheetConvertsSixteenItems(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        helper.runAfterDelay(5, () -> { supply(f, 4000); put(f, new ItemStack(AllBlocks.MECHANICAL_PRESS.get(), 17)); });
        helper.runAfterDelay(1800, () -> {
            helper.assertTrue(count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 16
                && count(f.inventory(), AllBlocks.MECHANICAL_PRESS.asItem()) == 1, "one sheet upgrades exactly sixteen actual depot items");
            helper.assertTrue(f.be.getPlateInventory().getStackInSlot(0).isEmpty() && f.be.getEssenceTank().isEmpty() && f.be.getProgress() == 0,
                "the sheet and 4000mB are exhausted; the seventeenth item waits");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 240)
    public static void componentsPlateReloadAndDrops(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        var input = new ItemStack(AllBlocks.FLUID_TANK.get());
        input.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved tank"));
        CompoundTag tank = new CompoundTag(); tank.putString("id", "create:fluid_tank"); tank.putInt("Size", 1); tank.putInt("Height", 1);
        CompoundTag fluid = new CompoundTag(); fluid.putString("id", "minecraft:water"); fluid.putInt("amount", 4000);
        CompoundTag content = new CompoundTag(); content.put("Fluid", fluid); tank.put("TankContent", content);
        input.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tank));
        helper.runAfterDelay(5, () -> { supply(f, 4000); put(f, input); });
        helper.onEachTick(() -> {
            var output = find(f.inventory(), BoosterItems.NETHERITE_FLUID_TANK.get());
            if (output.isEmpty()) return;
            var stored = output.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
            helper.assertTrue(output.getHoverName().getString().equals("Preserved tank") && stored.getString("id").equals("createnetherite:netherite_fluid_tank")
                && stored.getCompound("TankContent").getCompound("Fluid").getInt("amount") == 4000, "output keeps name and tank contents while updating the BE type");
            var saved = f.be.saveWithoutMetadata(helper.getLevel().registryAccess());
            f.be.getPlateInventory().extractItem(0, 1, false);
            f.be.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(ConversionPlate.remaining(f.be.getPlateInventory().getStackInSlot(0)) == 15 && f.be.getEssenceTank().getFluidAmount() == 3750,
                "reload preserves remaining uses and fluid");
            helper.getLevel().destroyBlock(f.pos, true);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(f.pos).inflate(2));
            helper.assertTrue(drops.stream().filter(e -> e.getItem().is(BoosterItems.NETHERITE_SHEET.get())).mapToInt(e -> e.getItem().getCount()).sum() == 1
                && drops.stream().anyMatch(e -> ConversionPlate.remaining(e.getItem()) == 15), "break drops exactly the original partially used plate");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 70)
    public static void plateSlotAndBucketInteraction(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ConverterSlotQA"));
        helper.assertTrue(f.be.getPlateInventory().insertItem(0, new ItemStack(Items.DIAMOND), false).is(Items.DIAMOND), "slot rejects non-sheets");
        helper.assertTrue(f.be.getPlateInventory().insertItem(0, new ItemStack(BoosterItems.NETHERITE_SHEET.get(), 2), false).getCount() == 1, "slot accepts only one sheet");
        helper.assertTrue(f.be.getEssenceTank().fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE) == 0, "fluid port rejects lava");
        player.setPos(f.pos.getCenter().add(0, 0, 2)); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BoosterFluids.ESSENCE_BUCKET.get()));
        helper.getLevel().getBlockState(f.pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(f.pos.getCenter(), Direction.SOUTH, f.pos, false));
        helper.assertTrue(f.be.getEssenceTank().getFluidAmount() == 1000 && player.getMainHandItem().is(Items.BUCKET), "real bucket interaction fills the machine and returns its container");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); player.setShiftKeyDown(true);
        helper.getLevel().getBlockState(f.pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(f.pos.getCenter(), Direction.SOUTH, f.pos, false));
        helper.assertTrue(f.be.getPlateInventory().getStackInSlot(0).isEmpty() && player.getInventory().items.stream().anyMatch(s -> s.is(BoosterItems.NETHERITE_SHEET.get())), "sneak empty-hand retrieves the actual sheet");
        cleanup(helper, f); helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 400)
    public static void essenceOnlyMixesInNetheriteMixer(GameTestHelper helper) {
        BlockPos upgraded = mixer(helper, 270, BoosterBlocks.NETHERITE_MECHANICAL_MIXER.get());
        BlockPos regular = mixer(helper, 280, AllBlocks.MECHANICAL_MIXER.get());
        helper.runAfterDelay(5, () -> {
            for (BlockPos pos : List.of(upgraded, regular)) {
                helper.assertTrue(Math.abs(((com.simibubi.create.content.kinetics.base.KineticBlockEntity) helper.getLevel().getBlockEntity(pos)).getSpeed()) == 64,
                    "the real mixer fixture must receive power through a cogwheel");
                var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos.below(2), null);
                ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.BLAZE_POWDER), false);
                ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.NETHER_WART), false);
                helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos.below(2), null).fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
            }
        });
        helper.runAfterDelay(350, () -> {
            var nether = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, upgraded.below(2), null);
            var original = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, regular.below(2), null);
            helper.assertTrue(amount(nether, BoosterFluids.NETHER_ESSENCE.get()) == 1000 && amount(nether, Fluids.LAVA) == 0,
                "actual netherite mixer produces exactly 1000mB essence from the lava recipe");
            helper.assertTrue(amount(original, BoosterFluids.NETHER_ESSENCE.get()) == 0 && amount(original, Fluids.LAVA) == 1000,
                "normal mixer cannot consume or produce the exclusive essence recipe");
            for (BlockPos pos : List.of(upgraded, regular)) {
                for (int dy = -2; dy <= 0; dy++) helper.getLevel().removeBlock(pos.above(dy), false);
                for (int dy = 0; dy <= 2; dy++) helper.getLevel().removeBlock(pos.west().above(dy), false);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void exactCraftingAndAllConversionMappings(GameTestHelper helper) {
        var input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.NETHER_STAR), new ItemStack(Items.NETHERITE_INGOT),
            new ItemStack(Items.DRAGON_BREATH), new ItemStack(Blocks.ENCHANTING_TABLE), new ItemStack(Items.DRAGON_BREATH),
            new ItemStack(Blocks.OBSIDIAN), new ItemStack(Blocks.DIAMOND_BLOCK), new ItemStack(Blocks.CRYING_OBSIDIAN)));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent() && recipe.get().value().assemble(input, helper.getLevel().registryAccess()).is(BoosterItems.NETHERITE_CONVERTER.get()), "specified 3x3 layout crafts the converter");
        helper.assertTrue(recipe.get().value().getRemainingItems(input).stream().filter(s -> s.is(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum() == 2,
            "dragon breath crafting returns its two glass bottles");
        String[] machines = {"powerful_mechanical_pump","netherite_fluid_tank","netherite_steam_engine","netherite_item_vault","netherite_elevator_pulley",
            "netherite_mechanical_press","netherite_blaze_burner","netherite_encased_fan","netherite_mechanical_arm","netherite_mechanical_mixer",
            "netherite_wireless_terminal","netherite_smart_chute","netherite_weighted_ejector","netherite_deployer","empty_netherite_blaze_burner"};
        for (String machine : machines) {
            var conversion = helper.getLevel().getRecipeManager().byKey(id("conversion/" + machine)).orElseThrow().value();
            var result = conversion.getResultItem(helper.getLevel().registryAccess());
            helper.assertTrue(result.getCount() == 1 && result.is(BuiltInRegistries.ITEM.get(id(machine.startsWith("empty_") ? "netherite_blaze_burner" : machine))), "conversion output is one correct machine: " + machine);
            if (machine.contains("blaze_burner")) helper.assertTrue(result.get(DataComponents.CUSTOM_DATA).copyTag().getInt("BlazeCount") == (machine.startsWith("empty_") ? 0 : 1), "empty and full burner results differ");
            roundTrip(helper, conversion);
        }
        roundTrip(helper, helper.getLevel().getRecipeManager().byKey(id("mixing/nether_essence")).orElseThrow().value());
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 240)
    public static void actualStressCapacityBoundary(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        helper.runAfterDelay(5, () -> {
            supply(f, 1000); put(f, new ItemStack(AllBlocks.MECHANICAL_PRESS.get()));
            f.motor.getOrCreateNetwork().updateCapacityFor(f.motor, 31);
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(f.be.isOverStressed() && f.be.getSpeed() == 0 && f.be.getProgress() == 0 && f.be.getEssenceTank().getFluidAmount() == 1000,
                "1984SU actual generator capacity cannot start a 2048SU converter or consume resources");
            f.motor.getOrCreateNetwork().updateCapacityFor(f.motor, 32);
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 1
                && f.be.getOrCreateNetwork().getActualStressOf(f.be) == 2048 && f.be.getEssenceTank().getFluidAmount() == 750,
                "exactly 2048SU actual capacity runs one full conversion");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 280)
    public static void actualMovingBeltConvertsOneAtATime(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos controller = helper.absolutePos(new BlockPos(1, 250, 1));
        BlockPos machine = controller.south().above(2), beltDrive = controller.west(), machineDrive = machine.west();
        for (int i = 0; i < 3; i++) {
            var state = AllBlocks.BELT.getDefaultState()
                .setValue(com.simibubi.create.content.kinetics.belt.BeltBlock.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(com.simibubi.create.content.kinetics.belt.BeltBlock.SLOPE, com.simibubi.create.content.kinetics.belt.BeltSlope.HORIZONTAL)
                .setValue(com.simibubi.create.content.kinetics.belt.BeltBlock.PART, i == 0 ? com.simibubi.create.content.kinetics.belt.BeltPart.START
                    : i == 2 ? com.simibubi.create.content.kinetics.belt.BeltPart.END : com.simibubi.create.content.kinetics.belt.BeltPart.MIDDLE);
            level.setBlock(controller.south(i), state, 3);
            var segment = (com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller.south(i));
            segment.setController(controller); segment.beltLength = 3; segment.index = i;
        }
        level.setBlock(machine, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        for (BlockPos motor : List.of(beltDrive, machineDrive)) {
            level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
            ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        }
        var converter = (NetheriteConverterBlockEntity) level.getBlockEntity(machine);
        helper.runAfterDelay(5, () -> {
            converter.getPlateInventory().insertItem(0, new ItemStack(BoosterItems.NETHERITE_SHEET.get()), false);
            converter.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1000), FluidAction.EXECUTE);
            var carried = new TransportedItemStack(new ItemStack(AllBlocks.MECHANICAL_PRESS.get(), 2)); carried.beltPosition = carried.prevBeltPosition = .5F;
            ((com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller)).getInventory().addItem(carried);
        });
        helper.succeedWhen(() -> {
            var items = ((com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller)).getInventory().getTransportedItems();
            helper.assertTrue(items.stream().anyMatch(s -> s.stack.is(BoosterItems.NETHERITE_MECHANICAL_PRESS.get())), "real moving belt reaches the converter and receives an upgraded machine");
            helper.assertTrue(items.stream().filter(s -> s.stack.is(BoosterItems.NETHERITE_MECHANICAL_PRESS.get())).mapToInt(s -> s.stack.getCount()).sum() == 1
                && items.stream().filter(s -> s.stack.is(AllBlocks.MECHANICAL_PRESS.asItem())).mapToInt(s -> s.stack.getCount()).sum() == 1
                && converter.getEssenceTank().getFluidAmount() == 750 && ConversionPlate.remaining(converter.getPlateInventory().getStackInSlot(0)) == 15,
                "one five-second belt operation upgrades only one input and preserves the waiting remainder");
            for (BlockPos pos : List.of(machine, beltDrive, machineDrive, controller, controller.south(), controller.south(2))) level.removeBlock(pos, false);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void roundTrip(GameTestHelper helper, net.minecraft.world.item.crafting.Recipe<?> recipe) {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            net.minecraft.world.item.crafting.RecipeSerializer serializer = recipe.getSerializer();
            serializer.streamCodec().encode(buffer, recipe);
            var decoded = (net.minecraft.world.item.crafting.Recipe<?>) serializer.streamCodec().decode(buffer);
            helper.assertTrue(decoded.getType() == recipe.getType() && ItemStack.matches(decoded.getResultItem(helper.getLevel().registryAccess()), recipe.getResultItem(helper.getLevel().registryAccess())),
                "custom recipe network round-trip retains type and result components");
        } finally { buffer.release(); }
    }

    private record Fixture(GameTestHelper helper, BlockPos pos, BlockPos depot, CreativeMotorBlockEntity motor, NetheriteConverterBlockEntity be, int rpm) {
        IItemHandler inventory() { return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, depot, null); }
    }
    private static Fixture fixture(GameTestHelper helper, int height, int rpm) {
        var level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(2, height + 3, 1)), depot = pos.below(2);
        level.setBlock(depot, AllBlocks.DEPOT.getDefaultState(), 3);
        level.setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        level.setBlock(pos.west(), AllBlocks.SHAFT.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X), 3);
        level.setBlock(pos.west(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
        var motor = (CreativeMotorBlockEntity) level.getBlockEntity(pos.west(2)); motor.generatedSpeed.setValue(rpm);
        return new Fixture(helper, pos, depot, motor, (NetheriteConverterBlockEntity) level.getBlockEntity(pos), rpm);
    }
    private static void supply(Fixture f, int fluid) {
        f.be.getPlateInventory().insertItem(0, new ItemStack(BoosterItems.NETHERITE_SHEET.get()), false);
        f.be.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), fluid), FluidAction.EXECUTE);
    }
    private static void put(Fixture f, ItemStack stack) { ((SmartBlockEntity) f.helper.getLevel().getBlockEntity(f.depot)).getBehaviour(DepotBehaviour.TYPE).setCenteredHeldItem(new TransportedItemStack(stack)); }
    private static void cleanup(GameTestHelper helper, Fixture f) {
        for (BlockPos pos : List.of(f.pos, f.pos.west(), f.pos.west(2), f.depot)) helper.getLevel().removeBlock(pos, false);
    }
    private static int count(IItemHandler inventory, Item item) { int n = 0; for (int i = 0; i < inventory.getSlots(); i++) if (inventory.getStackInSlot(i).is(item)) n += inventory.getStackInSlot(i).getCount(); return n; }
    private static ItemStack find(IItemHandler inventory, Item item) { for (int i = 0; i < inventory.getSlots(); i++) if (inventory.getStackInSlot(i).is(item)) return inventory.getStackInSlot(i); return ItemStack.EMPTY; }
    private static int amount(IFluidHandler handler, net.minecraft.world.level.material.Fluid fluid) { int n = 0; for (int i = 0; i < handler.getTanks(); i++) if (handler.getFluidInTank(i).is(fluid)) n += handler.getFluidInTank(i).getAmount(); return n; }
    private static BlockPos mixer(GameTestHelper helper, int height, Block block) {
        var level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(1, height, 1));
        level.setBlock(pos.below(2), AllBlocks.BASIN.getDefaultState(), 3); level.setBlock(pos, block.defaultBlockState(), 3);
        level.setBlock(pos.west(), AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y), 3);
        level.setBlock(pos.west().above(), AllBlocks.SHAFT.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y), 3);
        level.setBlock(pos.west().above(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(pos.west().above(2))).generatedSpeed.setValue(64);
        return pos;
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, path); }
}
