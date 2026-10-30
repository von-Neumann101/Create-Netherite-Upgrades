package io.github.boosterproject.booster.content.processing.burner;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.fml.loading.LoadingModList;

public final class NetheriteBlazeBurnerTest {
    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ItemStack stack = new ItemStack(Items.PAPER, 64);
        CompoundTag metadata = new CompoundTag();
        metadata.putString("unrelated", "keep");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(metadata));
        expect("legacy items start with one blaze", 1, NetheriteBlazeBurnerData.getCount(stack));

        ItemStack captured = NetheriteBlazeBurnerData.captureOne(stack);
        expect("capture creates exactly one filled item", 1, captured.getCount());
        expect("first capture adds the second blaze", 2, NetheriteBlazeBurnerData.getCount(captured));
        expect("capturing does not alter the source stack size", 64, stack.getCount());
        expect("capturing does not fill the rest of the stack", 1, NetheriteBlazeBurnerData.getCount(stack));
        check(captured.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
            .getString("unrelated").equals("keep"), "capture preserves unrelated item metadata");
        captured = NetheriteBlazeBurnerData.captureOne(captured);
        expect("second capture adds the third blaze", 3, NetheriteBlazeBurnerData.getCount(captured));
        captured = NetheriteBlazeBurnerData.captureOne(captured);
        expect("third capture fills four blazes", 4, NetheriteBlazeBurnerData.getCount(captured));
        check(NetheriteBlazeBurnerData.captureOne(captured).isEmpty(), "a full burner rejects another blaze");
        expect("rejected capture retains four blazes", 4, NetheriteBlazeBurnerData.getCount(captured));
        NetheriteBlazeBurnerData.setCount(stack, -1);
        expect("count cannot fall below zero", 0, NetheriteBlazeBurnerData.getCount(stack));
        NetheriteBlazeBurnerData.setCount(stack, 100);
        expect("count cannot exceed four", 4, NetheriteBlazeBurnerData.getCount(stack));

        ItemStack single = new ItemStack(Items.PAPER);
        ItemStack inventoryResult = NetheriteBlazeBurnerData.finishCapture(single, single,
            NetheriteBlazeBurnerData.captureOne(single), false);
        check(!single.isEmpty(), "single capture retains the original hand stack for Player.interactOn");
        expect("single capture updates the original hand stack", 2, NetheriteBlazeBurnerData.getCount(single));
        check(inventoryResult.isEmpty(), "single capture needs no replacement hand stack");
        ItemStack multiple = new ItemStack(Items.PAPER, 64);
        inventoryResult = NetheriteBlazeBurnerData.finishCapture(multiple, multiple,
            NetheriteBlazeBurnerData.captureOne(multiple), false);
        expect("stacked capture consumes one original item", 63, multiple.getCount());
        expect("stacked capture gives one filled item", 1, inventoryResult.getCount());
        expect("stacked capture fills only the delivered item", 2, NetheriteBlazeBurnerData.getCount(inventoryResult));
        ItemStack creativeHand = new ItemStack(Items.PAPER);
        ItemStack playerApiCopy = creativeHand.copy();
        inventoryResult = NetheriteBlazeBurnerData.finishCapture(playerApiCopy, creativeHand,
            NetheriteBlazeBurnerData.captureOne(playerApiCopy), true);
        expect("creative capture updates the real hand instead of Player.interactOn's copy", 2,
            NetheriteBlazeBurnerData.getCount(creativeHand));
        expect("creative Player API copy remains independent", 1, NetheriteBlazeBurnerData.getCount(playerApiCopy));
        check(inventoryResult.isEmpty(), "creative single capture stays in the real hand");

        ItemStack empty = new ItemStack(Items.PAPER);
        NetheriteBlazeBurnerData.setCount(empty, 0);
        expect("an explicitly empty burner retains zero blazes", 0, NetheriteBlazeBurnerData.getCount(empty));
        ItemStack filling = empty.copy();
        for (int expected = 1; expected <= 4; expected++) {
            ItemStack next = NetheriteBlazeBurnerData.captureOne(filling);
            check(!next.isEmpty(), "capture from empty succeeds for blaze " + expected);
            if (!next.isEmpty()) {
                inventoryResult = NetheriteBlazeBurnerData.finishCapture(filling, filling, next, false);
                check(inventoryResult.isEmpty(), "a single burner stays in hand after capture " + expected);
            }
            expect("empty burner can fill continuously to blaze " + expected,
                expected, NetheriteBlazeBurnerData.getCount(filling));
        }
        check(NetheriteBlazeBurnerData.captureOne(filling).isEmpty(), "a burner filled from empty rejects the fifth blaze");
        expect("the original empty sample remains empty", 0, NetheriteBlazeBurnerData.getCount(empty));
        expect("empty burners provide no active heat", BoilerHeater.NO_HEAT,
            NetheriteBlazeBurnerData.scaleHeat(1, 0));
        expect("empty burners provide no superheated heat", BoilerHeater.NO_HEAT,
            NetheriteBlazeBurnerData.scaleHeat(2, 0));
        expect("empty burners provide no passive heat", BoilerHeater.NO_HEAT,
            NetheriteBlazeBurnerData.scaleHeat(BoilerHeater.PASSIVE_HEAT, 0));
        TestBurner emptyState = new TestBurner();
        emptyState.setBlazeCount(0);
        CompoundTag emptySaved = new CompoundTag();
        emptyState.write(emptySaved, registries, false);
        emptyState.restore(emptySaved, registries);
        expect("zero survives BE data roundtrip", 0, emptyState.getBlazeCount());
        expect("zero blazes advance no fuel ticks", 0, emptyState.consumeFuelTicks());

        var emptyRequirement = new NetheriteBlazeBurnerBlock.BlazeCountRequirement(empty);
        check(emptyRequirement.matches(empty.copy()), "schematic accepts an empty burner for an empty target");
        check(!emptyRequirement.matches(new ItemStack(Items.PAPER)), "schematic distinguishes empty from legacy one-blaze burners");
        ItemStack emptyStack = new ItemStack(Items.PAPER, 64);
        NetheriteBlazeBurnerData.setCount(emptyStack, 0);
        inventoryResult = NetheriteBlazeBurnerData.finishCapture(emptyStack, emptyStack,
            NetheriteBlazeBurnerData.captureOne(emptyStack), false);
        expect("first capture splits one cage from an empty stack", 63, emptyStack.getCount());
        expect("remaining stacked cages stay empty", 0, NetheriteBlazeBurnerData.getCount(emptyStack));
        expect("the separated cage contains exactly one blaze", 1, NetheriteBlazeBurnerData.getCount(inventoryResult));
        ItemStack emptyCreativeHand = empty.copy();
        ItemStack emptyCreativeCopy = emptyCreativeHand.copy();
        NetheriteBlazeBurnerData.finishCapture(emptyCreativeCopy, emptyCreativeHand,
            NetheriteBlazeBurnerData.captureOne(emptyCreativeCopy), true);
        expect("creative first capture updates the real empty cage", 1,
            NetheriteBlazeBurnerData.getCount(emptyCreativeHand));
        expect("creative first capture leaves the API copy empty", 0,
            NetheriteBlazeBurnerData.getCount(emptyCreativeCopy));

        for (String recipeName : List.of("empty_netherite_blaze_burner", "empty_netherite_blaze_burner_bulk")) {
            try (var input = NetheriteBlazeBurnerTest.class.getResourceAsStream(
                "/data/createnetherite/recipe/" + recipeName + ".json")) {
                if (input == null) throw new AssertionError("Missing recipe: " + recipeName);
                var result = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("result");
                check(result.get("id").getAsString().equals("createnetherite:netherite_blaze_burner"),
                    recipeName + " produces the netherite burner");
                // The standalone registry lacks addon items; decode the actual components using a vanilla item.
                result.addProperty("id", "minecraft:paper");
                ItemStack decoded = ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), result)
                    .getOrThrow();
                expect(recipeName + " decodes an empty result", 0, NetheriteBlazeBurnerData.getCount(decoded));
                expect(recipeName + " retains its output count", recipeName.endsWith("_bulk") ? 3 : 1,
                    decoded.getCount());
            }
        }

        var requirement = new NetheriteBlazeBurnerBlock.BlazeCountRequirement(captured);
        check(requirement.matches(captured), "schematic matches the requested blaze count");
        check(!requirement.matches(new ItemStack(Items.PAPER)), "schematic cannot consume a different blaze count");
        ItemStack differentItem = new ItemStack(Items.STICK);
        NetheriteBlazeBurnerData.setCount(differentItem, 4);
        check(!requirement.matches(differentItem), "schematic still requires the same item kind");

        BlockState heater = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
            .add(BlazeBurnerBlock.HEAT_LEVEL).create(Block::defaultBlockState, BlockState::new).any();
        expect("empty block without a BE drops and copies as empty", 0,
            NetheriteBlazeBurnerBlock.getBlazeCount(heater.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.NONE), null));
        expect("filled block without stored count retains the legacy default", 1,
            NetheriteBlazeBurnerBlock.getBlazeCount(heater.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING), null));
        for (int count = 1; count <= 4; count++) {
            TestBurner burner = new TestBurner();
            burner.setBlazeCount(count);
            expect("BE stores blaze count " + count, count, burner.getBlazeCount());
            int spent = 0;
            for (int tick = 0; tick < 20; tick++) {
                spent += burner.consumeFuelTicks();
            }
            expect("20 ticks consume the agreed fuel amount for " + count + " blazes", count * 10, spent);
            for (HeatLevel heat : HeatLevel.values()) {
                float vanilla = BoilerHeater.BLAZE_BURNER.getHeat(null, BlockPos.ZERO,
                    heater.setValue(BlazeBurnerBlock.HEAT_LEVEL, heat));
                expect("native " + heat + " heat for " + count + " blazes",
                    vanilla > 0 ? vanilla * count : vanilla, NetheriteBlazeBurnerData.scaleHeat(vanilla, count));
            }
        }

        TestBurner burner = new TestBurner();
        expect("one blaze skips the first half fuel tick", 0, burner.consumeFuelTicks());
        CompoundTag saved = new CompoundTag();
        burner.write(saved, registries, false);
        TestBurner restored = new TestBurner();
        restored.restore(saved, registries);
        expect("half tick progress survives saving", 1, restored.consumeFuelTicks());
        burner.setBlazeCount(3);
        burner.write(saved, registries, false);
        restored.restore(saved, registries);
        expect("BE count survives saving", 3, restored.getBlazeCount());
        CompoundTag safe = new CompoundTag();
        burner.writeSafe(safe, registries);
        expect("schematics retain the captured blaze count", 3, NetheriteBlazeBurnerData.readCount(safe));
        check(!safe.contains("burnTimeRemaining") && !safe.contains("FuelHalfTick"),
            "schematics do not duplicate fuel or fractional fuel progress");
        ItemStack drop = new ItemStack(Items.PAPER);
        NetheriteBlazeBurnerData.setCount(drop, restored.getBlazeCount());
        TestBurner placed = new TestBurner();
        placed.setBlazeCount(NetheriteBlazeBurnerData.getCount(drop));
        expect("quantity survives the drop and placement data path", 3, placed.getBlazeCount());
        restored.restore(new CompoundTag(), registries);
        expect("legacy BE saves start with one blaze", 1, restored.getBlazeCount());

        if (!FAILURES.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), FAILURES));
        }
        System.out.println("Netherite blaze burner regression checks passed (" + checks + " cases).");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) FAILURES.add(message);
    }

    private static void expect(String message, float expected, float actual) {
        check(Float.compare(expected, actual) == 0, message + ": expected " + expected + ", got " + actual);
    }

    private static final class TestBurner extends NetheriteBlazeBurnerBlockEntity {
        TestBurner() {
            super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        void restore(CompoundTag tag, HolderLookup.Provider registries) {
            read(tag, registries, false);
        }
    }
}
