package io.github.boosterproject.booster.content.conversion;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerBlockEntity;
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
    @GameTest(template = "wireless_empty", timeoutTicks = 250)
    public static void advancedWrenchNeedsFourBucketsAndKeepsResultAttributes(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        var input = new ItemStack(BoosterItems.NETHERITE_WRENCH.get());
        input.set(DataComponents.CUSTOM_NAME, Component.literal("Upgraded wrench"));
        CustomData.update(DataComponents.CUSTOM_DATA, input, tag -> tag.putString("OtherMod", "keep"));
        long[] start = {-1};
        helper.runAfterDelay(5, () -> {
            supply(f, 3999);
            put(f, input);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(f.be.getProgress() == 0 && f.be.getEssenceTank().getFluidAmount() == 3999
                && count(f.inventory(), BoosterItems.NETHERITE_WRENCH.get()) == 1, "3999mB cannot start, consume fluid or replace the basic wrench");
            f.be.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1), FluidAction.EXECUTE);
        });
        helper.onEachTick(() -> {
            if (f.be.getProgress() == 1 && start[0] < 0) start[0] = helper.getLevel().getGameTime();
            var output = find(f.inventory(), BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
            if (output.isEmpty()) return;
            helper.assertTrue(start[0] >= 0 && helper.getLevel().getGameTime() - start[0] >= 99
                && f.be.getEssenceTank().isEmpty() && output.getCount() == 1,
                "advanced upgrade requires the full five seconds and consumes exactly 4000mB");
            helper.assertTrue(output.getHoverName().getString().equals("Upgraded wrench")
                && output.get(DataComponents.CUSTOM_DATA).copyTag().getString("OtherMod").equals("keep")
                && output.get(DataComponents.UNBREAKABLE) != null, "result preserves source custom components and gets the advanced unbreakable default");
            double[] attributes = {0, 0};
            output.getAttributeModifiers().forEach(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) attributes[0] += modifier.amount();
                if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE)) attributes[1] += modifier.amount();
            });
            helper.assertTrue(attributes[0] == 9 && attributes[1] == 2, "advanced sword retains its ten total base damage and the basic wrench's two-block reach bonus");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty")
    public static void conversionFluidJsonAndNetworkRetainPositiveExplicitAndDefaultAmounts(GameTestHelper helper) {
        for (String path : new String[] {"conversion/netherite_wrench", "conversion/netherite_netherite_wrench", "conversion/netherite_mechanical_press"}) {
            var recipe = (NetheriteConversionRecipe) helper.getLevel().getRecipeManager().byKey(id(path)).orElseThrow().value();
            int expected = path.endsWith("/netherite_netherite_wrench") ? 4000 : 1000;
            helper.assertTrue(recipe.getEssenceAmount() == expected, "JSON loads the correct fluid amount for " + path);
            var serializer = (NetheriteConversionRecipe.Serializer) recipe.getSerializer();
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                serializer.streamCodec().encode(buffer, recipe);
                var decoded = serializer.streamCodec().decode(buffer);
                helper.assertTrue(decoded.getEssenceAmount() == expected && ItemStack.matches(decoded.getResultItem(helper.getLevel().registryAccess()), recipe.getResultItem(helper.getLevel().registryAccess())), "network round trip keeps recipe fluid amount and result");
            } finally { buffer.release(); }
            var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, helper.getLevel().registryAccess());
            var json = serializer.codec().codec().encodeStart(ops, recipe).getOrThrow().getAsJsonObject();
            if (expected == 4000) helper.assertTrue(json.get("fluid_amount").getAsInt() == 4000, "advanced recipe serializes its explicit four-bucket amount");
            json.remove("fluid_amount");
            helper.assertTrue(serializer.codec().codec().parse(ops, json).getOrThrow().getEssenceAmount() == 1000,
                "omitting fluid_amount retains the ordinary 1000mB default");
            for (int bad : new int[] {0, -1}) {
                json.addProperty("fluid_amount", bad);
                helper.assertTrue(serializer.codec().codec().parse(ops, json).error().isPresent(), "invalid nonpositive JSON amounts cannot make free conversions");
                var invalid = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
                try {
                    var base = new com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Serializer<NetheriteConversionRecipe>(NetheriteConversionRecipe::new);
                    base.streamCodec().encode(invalid, recipe);
                    invalid.writeVarInt(bad);
                    boolean rejected = false;
                    try { serializer.streamCodec().decode(invalid); }
                    catch (IllegalArgumentException expectedError) { rejected = true; }
                    helper.assertTrue(rejected, "invalid nonpositive network amounts cannot make free conversions");
                } finally { invalid.release(); }
            }
        }
        helper.succeed();
    }

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
                    && f.be.getEssenceTank().getFluidAmount() == 4000,
                    "nothing is consumed or converted before 100 processing ticks");
                if (count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 1) {
                    helper.assertTrue(elapsed >= 99 && elapsed <= 100, "conversion takes 100 ticks at " + f.rpm + " RPM; elapsed=" + elapsed);
                    helper.assertTrue(f.be.getEssenceTank().getFluidAmount() == 3000,
                        "one output costs exactly 1000mB essence");
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
            helper.assertTrue(f.be.getProgress() == 0, "missing essence does not start progress");
            supply(f, 999);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(f.be.getProgress() == 0 && f.be.getEssenceTank().getFluidAmount() == 999, "999mB cannot start or be partially consumed");
            f.be.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1), FluidAction.EXECUTE);
        });
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (stage[0] == 0 && f.be.getProgress() >= 30) { f.motor.generatedSpeed.setValue(0); stopped[0] = now; stage[0] = 1; }
            if (stage[0] == 1 && now - stopped[0] >= 10) {
                helper.assertTrue(f.be.getProgress() == 0 && f.be.getEssenceTank().getFluidAmount() == 1000,
                    "power loss clears elapsed work without consuming resources");
                f.motor.generatedSpeed.setValue(256); stage[0] = 2;
            }
            if (stage[0] == 2 && f.be.getProgress() == 1) { restarted[0] = now; stage[0] = 3; }
            if (stage[0] == 3 && count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 1) {
                helper.assertTrue(now - restarted[0] >= 99 && f.be.getEssenceTank().isEmpty(), "restored faster power still needs a fresh full 100 ticks");
                cleanup(helper, f); helper.succeed();
            }
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 650)
    public static void fourBucketsConvertFourItemsAndFifthWaits(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        helper.runAfterDelay(5, () -> { supply(f, 4000); put(f, new ItemStack(AllBlocks.MECHANICAL_PRESS.get(), 5)); });
        helper.runAfterDelay(600, () -> {
            helper.assertTrue(count(f.inventory(), BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 4
                && count(f.inventory(), AllBlocks.MECHANICAL_PRESS.asItem()) == 1, "4000mB upgrades exactly four actual depot items");
            helper.assertTrue(f.be.getEssenceTank().isEmpty() && f.be.getProgress() == 0,
                "all 4000mB are exhausted; the fifth item waits");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 240)
    public static void componentsFluidReloadAndDrops(GameTestHelper helper) {
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
            f.be.getEssenceTank().drain(4000, FluidAction.EXECUTE);
            f.be.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(f.be.getEssenceTank().getFluidAmount() == 3000 && !saved.contains("NetheriteValue") && !saved.contains("Plate"),
                "reload preserves essence without retaining obsolete point or plate data");
            helper.getLevel().destroyBlock(f.pos, true);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(f.pos).inflate(2));
            helper.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(BoosterItems.NETHERITE_SHEET.get()))
                && drops.stream().filter(e -> e.getItem().is(BoosterItems.NETHERITE_CONVERTER.get())).count() == 1
                && drops.stream().anyMatch(e -> e.getItem().is(BoosterItems.NETHERITE_CONVERTER.get())
                    && !e.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("NetheriteValue")),
                "break drops one converter without obsolete points or a plate refund");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 70)
    public static void fluidOnlyPortsRejectMaterialDepositsAndBuckets(GameTestHelper helper) {
        Fixture f = fixture(helper, 230, 64);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ConverterPortQA"));
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, f.pos, null) == null,
            "converter exposes no item deposit capability");
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, f.pos, side) == null,
                "converter has no item deposit capability on " + side);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, f.pos, side) != null,
                "converter exposes a pipe fluid port on " + side);
        }
        helper.assertTrue(f.be.getEssenceTank().fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE) == 0, "fluid port rejects lava");
        player.setPos(f.pos.getCenter().add(0, 0, 2));
        for (Item item : List.of(BoosterItems.NETHERITE_SHEET.get(), Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, BoosterFluids.ESSENCE_BUCKET.get())) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            helper.getLevel().getBlockState(f.pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(f.pos.getCenter(), Direction.SOUTH, f.pos, false));
            helper.assertTrue(player.getMainHandItem().is(item) && player.getMainHandItem().getCount() == 1 && f.be.getEssenceTank().isEmpty(),
                "right-click does not deposit material or transfer a bucket: " + item);
        }
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, f.pos, Direction.SOUTH)
                .fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1000), FluidAction.EXECUTE) == 1000,
            "the pipe capability accepts essence without any material deposit");
        var saved = f.be.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(!saved.contains("NetheriteValue") && !saved.contains("Plate"), "fluid-only storage contains no material points or deposit inventory");
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
                ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.ANCIENT_DEBRIS), false);
                ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.BLAZE_POWDER), false);
                ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.NETHER_WART), false);
            }
        });
        helper.runAfterDelay(350, () -> {
            var nether = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, upgraded.below(2), null);
            var original = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, regular.below(2), null);
            helper.assertTrue(amount(nether, BoosterFluids.NETHER_ESSENCE.get()) == 1000 && amount(nether, Fluids.LAVA) == 0,
                "actual netherite mixer produces exactly 1000mB essence from debris, wart and blaze powder without lava");
            var upgradedItems = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, upgraded.below(2), null);
            var regularItems = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, regular.below(2), null);
            helper.assertTrue(count(upgradedItems, Items.ANCIENT_DEBRIS) == 0 && count(upgradedItems, Items.NETHER_WART) == 0
                && count(upgradedItems, Items.BLAZE_POWDER) == 0, "essence production consumes exactly its three ingredients");
            helper.assertTrue(amount(original, BoosterFluids.NETHER_ESSENCE.get()) == 0 && count(regularItems, Items.ANCIENT_DEBRIS) == 1
                && count(regularItems, Items.NETHER_WART) == 1 && count(regularItems, Items.BLAZE_POWDER) == 1,
                "normal mixer cannot consume or produce the exclusive essence recipe");
            for (BlockPos pos : List.of(upgraded, regular)) {
                for (int dy = -3; dy <= 0; dy++) helper.getLevel().removeBlock(pos.above(dy), false);
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
                && f.be.getOrCreateNetwork().getActualStressOf(f.be) == 2048 && f.be.getEssenceTank().isEmpty(),
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
            converter.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1000), FluidAction.EXECUTE);
            var carried = new TransportedItemStack(new ItemStack(AllBlocks.MECHANICAL_PRESS.get(), 2)); carried.beltPosition = carried.prevBeltPosition = .5F;
            ((com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller)).getInventory().addItem(carried);
        });
        helper.succeedWhen(() -> {
            var items = ((com.simibubi.create.content.kinetics.belt.BeltBlockEntity) level.getBlockEntity(controller)).getInventory().getTransportedItems();
            helper.assertTrue(items.stream().anyMatch(s -> s.stack.is(BoosterItems.NETHERITE_MECHANICAL_PRESS.get())), "real moving belt reaches the converter and receives an upgraded machine");
            helper.assertTrue(items.stream().filter(s -> s.stack.is(BoosterItems.NETHERITE_MECHANICAL_PRESS.get())).mapToInt(s -> s.stack.getCount()).sum() == 1
                && items.stream().filter(s -> s.stack.is(AllBlocks.MECHANICAL_PRESS.asItem())).mapToInt(s -> s.stack.getCount()).sum() == 1
                && converter.getEssenceTank().isEmpty(),
                "one five-second belt operation upgrades only one input and preserves the waiting remainder");
            for (BlockPos pos : List.of(machine, beltDrive, machineDrive, controller, controller.south(), controller.south(2))) level.removeBlock(pos, false);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 400)
    public static void secondActivationSlotNeedsFourBucketsAndPreservesWrenchState(GameTestHelper helper) {
        var f = fixture(helper, 180, 64);
        var wrench = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        var modules = List.of(new ItemStack(BoosterItems.NETHERITE_ENCASED_FAN.get()),
            new ItemStack(BoosterItems.POWERFUL_MECHANICAL_PUMP.get()), new ItemStack(BoosterItems.NETHERITE_FLUID_TANK.get()));
        WrenchData.setModules(wrench, modules);
        WrenchData.toggleSlot(wrench, 0);
        WrenchData.setFanPull(wrench, true);
        WrenchData.setFluids(wrench, List.of(new FluidStack(Fluids.WATER, 2300)), helper.getLevel().registryAccess());
        wrench.set(DataComponents.CUSTOM_NAME, Component.literal("Keep my configured wrench"));
        var root = wrench.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); root.putString("OtherMod", "keep");
        wrench.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        var expected = wrench.copy(); WrenchData.unlockSecondSlot(expected);
        helper.runAfterDelay(5, () -> { supply(f, 3999); put(f, wrench); });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(ItemStack.matches(find(f.inventory(), wrench.getItem()), wrench) && f.be.getProgress() == 0
                && f.be.getEssenceTank().getFluidAmount() == 3999, "3999mB cannot start or alter the configured wrench");
            supply(f, 1);
        });
        helper.runAfterDelay(230, () -> {
            helper.assertTrue(ItemStack.matches(find(f.inventory(), wrench.getItem()), expected) && f.be.getEssenceTank().isEmpty(), "4000mB unlocks second activation and preserves all existing components");
            supply(f, 4000);
        });
        helper.runAfterDelay(345, () -> {
            var output = find(f.inventory(), wrench.getItem());
            helper.assertTrue(ItemStack.matches(output, expected) && f.be.getEssenceTank().getFluidAmount() == 4000
                && f.be.getProgress() == 0, "already upgraded wrench never repeats or spends more essence");
            WrenchData.toggleSlot(output, 1);
            helper.assertTrue(WrenchData.activeSlots(output).equals(List.of(0, 1)), "upgraded wrench can activate both compatible modules");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void activationUpgradeRecipeComponentsAndNetworkRoundTrip(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(id("conversion/netherite_wrench_activation"));
        helper.assertTrue(holder.isPresent(), "second activation has a real conversion recipe");
        var recipe = (NetheriteConversionRecipe) holder.orElseThrow().value();
        var input = new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get());
        helper.assertTrue(recipe.getEssenceAmount() == 4000 && recipe.matches(new net.minecraft.world.item.crafting.SingleRecipeInput(input), helper.getLevel()), "fresh advanced wrench matches the four-bucket recipe");
        WrenchData.unlockSecondSlot(input);
        helper.assertTrue(!recipe.matches(new net.minecraft.world.item.crafting.SingleRecipeInput(input), helper.getLevel()), "two-slot wrench is excluded");
        helper.assertTrue(WrenchData.activationLimit(recipe.getResultItem(helper.getLevel().registryAccess())) == 2, "JSON result exposes the unlocked state");
        roundTrip(helper, recipe);
        helper.succeed();
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
        level.setBlock(pos.below(2), BoosterBlocks.NETHERITE_BASIN.get().defaultBlockState(), 3); level.setBlock(pos, block.defaultBlockState(), 3);
        BlockPos burnerPos = pos.below(3);
        level.setBlock(burnerPos, BoosterBlocks.NETHERITE_BLAZE_BURNER.get().defaultBlockState()
            .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING), 3);
        var burner = (NetheriteBlazeBurnerBlockEntity) level.getBlockEntity(burnerPos);
        burner.setBlazeCount(4);
        var coal = new ItemStack(Items.COAL);
        helper.assertTrue(BlazeBurnerBlock.tryInsert(level.getBlockState(burnerPos), level, burnerPos, coal, false, false, false).getResult().consumesAction()
            && coal.isEmpty() && burner.getRemainingBurnTime() > 0 && burner.getHeatLevelFromBlock() == BlazeBurnerBlock.HeatLevel.KINDLED,
            "native fuel insertion activates the four-blaze netherite burner");
        level.setBlock(pos.west(), AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y), 3);
        level.setBlock(pos.west().above(), AllBlocks.SHAFT.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y), 3);
        level.setBlock(pos.west().above(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(pos.west().above(2))).generatedSpeed.setValue(64);
        return pos;
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, path); }
}
