package io.github.boosterproject.booster.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import io.github.boosterproject.booster.content.conversion.NetheriteConverterBlockEntity;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public final class NetheriteConverterScenes {
    private static final BlockPos CONVERTER = new BlockPos(3, 3, 3);
    private static final BlockPos DEPOT = CONVERTER.below(2);
    private static final BlockPos SHAFT = CONVERTER.west();
    private static final BlockPos TABLE = new BlockPos(5, 1, 2);

    private NetheriteConverterScenes() {}

    public static void value(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_converter_value", "Getting and checking Netherite Value");
        setup(scene, util);
        machine(scene, util);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(CONVERTER), Pointing.DOWN, 75)
            .rightClick().withItem(new ItemStack(BoosterItems.NETHERITE_SHEET.get()));
        inventory(scene, 16, 0);
        text(scene, "converter_value_sheet", CONVERTER, 95);
        scene.idle(105);
        scene.overlay().showControls(util.vector().blockSurface(CONVERTER, Direction.SOUTH), Pointing.RIGHT, 80).rightClick();
        text(scene, "converter_value_read", CONVERTER, 95);
        scene.idle(105);
        scene.overlay().showControls(util.vector().topOf(CONVERTER), Pointing.DOWN, 75)
            .rightClick().withItem(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        inventory(scene, 17, 0);
        text(scene, "converter_value_template", CONVERTER, 90);
        scene.idle(100);
        text(scene, "converter_value_stored", CONVERTER, 95);
        scene.idle(105);
        scene.markAsFinished();
    }

    public static void convert(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_converter_convert", "Converting a machine");
        setup(scene, util);
        machine(scene, util);
        scene.world().showSection(util.select().position(DEPOT), Direction.UP);
        scene.overlay().showOutline(PonderPalette.INPUT, "converter_gap", util.select().fromTo(DEPOT, CONVERTER), 100);
        text(scene, "converter_layout", DEPOT, 100);
        scene.idle(110);
        power(scene, util);
        scene.overlay().showControls(util.vector().topOf(CONVERTER), Pointing.DOWN, 75)
            .rightClick().withItem(new ItemStack(BoosterItems.NETHERITE_SHEET.get()));
        inventory(scene, 16, 0);
        text(scene, "converter_power", SHAFT, 100);
        scene.idle(110);
        scene.overlay().showControls(util.vector().blockSurface(CONVERTER, Direction.SOUTH), Pointing.RIGHT, 80)
            .rightClick().withItem(new ItemStack(BoosterFluids.ESSENCE_BUCKET.get()));
        inventory(scene, 16, 1000);
        text(scene, "converter_fluid", CONVERTER, 95);
        scene.idle(105);
        process(scene, util, AllBlocks.MECHANICAL_PRESS.asStack(), new ItemStack(BoosterItems.NETHERITE_MECHANICAL_PRESS.get()),
            "converter_process", 15, 750);
        collect(scene, util);
        scene.markAsFinished();
    }

    public static void wrenches(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_converter_wrenches", "Making both wrench tiers");
        setup(scene, util);
        scene.world().showSection(util.select().position(TABLE), Direction.UP);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(TABLE).add(-.6, .2, 0), Pointing.RIGHT, 80).withItem(AllItems.WRENCH.asStack());
        scene.overlay().showControls(util.vector().topOf(TABLE).add(.6, .2, 0), Pointing.LEFT, 80)
            .withItem(new ItemStack(BoosterItems.NETHERITE_SHEET.get()));
        text(scene, "converter_wrench_craft", TABLE, 105);
        scene.idle(40);
        var crafted = scene.world().createItemEntity(util.vector().topOf(TABLE).add(0, .3, 0), Vec3.ZERO,
            new ItemStack(BoosterItems.NETHERITE_WRENCH.get()));
        scene.world().modifyEntity(crafted, entity -> entity.setNoGravity(true));
        scene.effects().indicateSuccess(TABLE);
        scene.idle(75);
        scene.world().modifyEntity(crafted, Entity::discard);
        scene.world().hideSection(util.select().position(TABLE), Direction.DOWN);
        machine(scene, util);
        scene.world().showSection(util.select().position(DEPOT), Direction.UP);
        power(scene, util);
        text(scene, "converter_wrench_supply", CONVERTER, 165);
        scene.overlay().showControls(util.vector().topOf(CONVERTER), Pointing.DOWN, 50)
            .rightClick().withItem(new ItemStack(BoosterItems.NETHERITE_SHEET.get()));
        inventory(scene, 16, 0);
        scene.idle(55);
        scene.overlay().showControls(util.vector().topOf(CONVERTER), Pointing.DOWN, 50)
            .rightClick().withItem(new ItemStack(BoosterItems.NETHERITE_SHEET.get()));
        inventory(scene, 32, 0);
        scene.idle(55);
        scene.overlay().showControls(util.vector().blockSurface(CONVERTER, Direction.SOUTH), Pointing.RIGHT, 55)
            .rightClick().withItem(new ItemStack(BoosterFluids.ESSENCE_BUCKET.get()));
        inventory(scene, 32, 1000);
        scene.idle(65);
        process(scene, util, AllItems.WRENCH.asStack(), new ItemStack(BoosterItems.NETHERITE_WRENCH.get()),
            "converter_wrench_basic", 31, 750);
        scene.overlay().showControls(util.vector().topOf(DEPOT), Pointing.DOWN, 45).rightClick();
        item(scene, ItemStack.EMPTY);
        scene.idle(55);
        process(scene, util, new ItemStack(BoosterItems.NETHERITE_WRENCH.get()), new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get()),
            "converter_wrench_advanced", 15, 500);
        collect(scene, util);
        scene.markAsFinished();
    }

    private static void setup(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(.85F);
        scene.showBasePlate();
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        inventory(scene, 0, 0);
    }

    private static void machine(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().showSection(util.select().fromTo(1, 1, 3, 1, 2, 3), Direction.UP);
        scene.world().showSection(util.select().fromTo(SHAFT.west(), CONVERTER), Direction.DOWN);
    }

    private static void power(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().setKineticSpeed(util.select().fromTo(SHAFT.west(), CONVERTER), 64);
        scene.effects().rotationDirectionIndicator(SHAFT);
    }

    private static void process(CreateSceneBuilder scene, SceneBuildingUtil util, ItemStack input, ItemStack output,
                                String key, int remainingValue, int remainingFluid) {
        scene.overlay().showControls(util.vector().topOf(DEPOT), Pointing.DOWN, 70).withItem(input);
        item(scene, input);
        text(scene, key, DEPOT, 110);
        scene.idle(NetheriteConverterBlockEntity.PROCESSING_TICKS);
        item(scene, output);
        inventory(scene, remainingValue, remainingFluid);
        scene.effects().indicateSuccess(DEPOT);
        scene.idle(20);
    }

    private static void collect(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.overlay().showControls(util.vector().topOf(DEPOT), Pointing.DOWN, 75).rightClick();
        text(scene, "converter_collect", DEPOT, 90);
        scene.idle(100);
    }

    private static void item(CreateSceneBuilder scene, ItemStack stack) {
        scene.world().modifyBlockEntity(DEPOT, DepotBlockEntity.class, depot -> depot.setHeldItem(stack.copy()));
    }

    private static void inventory(CreateSceneBuilder scene, int value, int fluid) {
        // Storyboards stage client-side state; the real server processing chain is tested separately.
        scene.world().modifyBlockEntity(CONVERTER, NetheriteConverterBlockEntity.class, machine -> {
            machine.restoreNetheriteValue(value);
            machine.getEssenceTank().setFluid(fluid == 0 ? FluidStack.EMPTY : new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), fluid));
        });
    }

    private static void text(CreateSceneBuilder scene, String key, BlockPos pos, int duration) {
        scene.overlay().showText(duration).sharedText(key).pointAt(Vec3.atCenterOf(pos))
            .placeNearTarget().attachKeyFrame();
    }
}
