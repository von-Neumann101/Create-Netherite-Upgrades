package io.github.boosterproject.booster.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;

public final class NetheriteConverterScenes {
    private static final BlockPos CONVERTER = new BlockPos(3, 3, 3);
    private static final BlockPos DEPOT = CONVERTER.below(2);
    private static final BlockPos SHAFT = CONVERTER.west();
    private static final BlockPos TABLE = new BlockPos(5, 1, 2);

    private NetheriteConverterScenes() {}

    public static void supply(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_converter_supply", "Supplying Nether Essence");
        setup(scene, util);
        machine(scene, util);
        text(scene, "converter_supply_only", CONVERTER, 105);
        scene.idle(115);
        fluidSupply(scene, util, 1000);
        scene.overlay().showControls(util.vector().topOf(CONVERTER), Pointing.DOWN, 70).withItem(AllItems.GOGGLES.asStack());
        text(scene, "converter_inspect", CONVERTER, 90);
        scene.idle(100);
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
        text(scene, "converter_power", SHAFT, 100);
        scene.idle(110);
        fluidSupply(scene, util, 1000);
        process(scene, util, AllBlocks.MECHANICAL_PRESS.asStack(), new ItemStack(BoosterItems.NETHERITE_MECHANICAL_PRESS.get()),
            "converter_process", 0);
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
        text(scene, "converter_wrench_supply", CONVERTER, 100);
        scene.idle(110);
        fluidSupply(scene, util, 1000);
        process(scene, util, AllItems.WRENCH.asStack(), new ItemStack(BoosterItems.NETHERITE_WRENCH.get()),
            "converter_wrench_basic", 0);
        fluidSupply(scene, util, 4000);
        process(scene, util, new ItemStack(BoosterItems.NETHERITE_WRENCH.get()),
            new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get()), "converter_wrench_advanced", 0);
        collect(scene, util);
        scene.markAsFinished();
    }

    private static void setup(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(.85F);
        scene.showBasePlate();
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        inventory(scene, 0);
    }

    private static void machine(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().showSection(util.select().fromTo(1, 1, 3, 1, 2, 3), Direction.UP);
        scene.world().showSection(util.select().fromTo(SHAFT.west(), CONVERTER), Direction.DOWN);
    }

    private static void power(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().setKineticSpeed(util.select().fromTo(SHAFT.west(), CONVERTER), 64);
        scene.effects().rotationDirectionIndicator(SHAFT);
    }

    private static void fluidSupply(CreateSceneBuilder scene, SceneBuildingUtil util, int amount) {
        BlockPos pipe = CONVERTER.south(), pump = CONVERTER.south(2), source = CONVERTER.south(3), cog = pump.east(), motor = cog.south();
        scene.world().setBlock(pipe, AllBlocks.FLUID_PIPE.getDefaultState().setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true), false);
        scene.world().setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH), false);
        scene.world().setBlock(source, AllBlocks.FLUID_TANK.getDefaultState(), false);
        scene.world().setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH), false);
        scene.world().modifyBlockEntity(source, FluidTankBlockEntity.class,
            tank -> tank.getTankInventory().setFluid(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), amount)));
        scene.world().showSection(util.select().fromTo(pipe, source), Direction.UP);
        scene.world().showSection(util.select().fromTo(cog, motor), Direction.UP);
        scene.world().setKineticSpeed(util.select().position(pump), 64);
        scene.world().setKineticSpeed(util.select().fromTo(cog, motor), -64);
        scene.world().propagatePipeChange(pump);
        text(scene, "converter_fluid", pipe, 100);
        scene.idle(35);
        scene.world().modifyBlockEntity(source, FluidTankBlockEntity.class, tank -> tank.getTankInventory().setFluid(FluidStack.EMPTY));
        inventory(scene, amount);
        scene.idle(75);
    }

    private static void process(CreateSceneBuilder scene, SceneBuildingUtil util, ItemStack input, ItemStack output,
                                String key, int remainingFluid) {
        scene.overlay().showControls(util.vector().topOf(DEPOT), Pointing.DOWN, 70).withItem(input);
        item(scene, input);
        text(scene, key, DEPOT, 110);
        scene.idle(NetheriteConverterBlockEntity.PROCESSING_TICKS);
        item(scene, output);
        inventory(scene, remainingFluid);
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

    private static void inventory(CreateSceneBuilder scene, int fluid) {
        // Storyboards stage client-side state; the real server processing chain is tested separately.
        scene.world().modifyBlockEntity(CONVERTER, NetheriteConverterBlockEntity.class, machine -> {
            machine.getEssenceTank().setFluid(fluid == 0 ? FluidStack.EMPTY : new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), fluid));
        });
    }

    private static void text(CreateSceneBuilder scene, String key, BlockPos pos, int duration) {
        scene.overlay().showText(duration).sharedText(key).pointAt(Vec3.atCenterOf(pos))
            .placeNearTarget().attachKeyFrame();
    }
}
