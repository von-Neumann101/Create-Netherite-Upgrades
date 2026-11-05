package io.github.boosterproject.booster.client.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import io.github.boosterproject.booster.content.equipment.HandheldKineticPower;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public final class NetheriteExtendoGripScenes {
    private static final BlockPos INPUT = new BlockPos(1, 2, 2);
    private static final BlockPos PUMP = new BlockPos(2, 2, 2);
    private static final BlockPos OUTPUT = new BlockPos(3, 2, 2);
    private static final BlockPos COG = PUMP.north();

    private NetheriteExtendoGripScenes() {}

    public static void bucket(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_grip_bucket", "Filling buckets with an offhand grip");
        setup(scene, util);
        scene.world().showSection(util.select().fromTo(INPUT.below(), INPUT), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(INPUT).add(-.6, .5, 0), Pointing.DOWN, 80).withItem(grip());
        text(scene, "grip_offhand", INPUT, 90);
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(INPUT, Direction.SOUTH), Pointing.RIGHT, 80)
            .rightClick().withItem(new ItemStack(Items.BUCKET));
        text(scene, "grip_bucket", INPUT, 90);
        scene.idle(35);
        scene.world().modifyBlockEntity(INPUT, FluidTankBlockEntity.class, be -> be.getTankInventory().drain(1000, FluidAction.EXECUTE));
        scene.idle(65);
        scene.overlay().showControls(util.vector().topOf(INPUT), Pointing.DOWN, 70).withItem(new ItemStack(Items.WATER_BUCKET));
        scene.effects().indicateSuccess(INPUT);
        text(scene, "grip_filled", INPUT, 80);
        scene.idle(90);
        text(scene, "grip_repeat", INPUT, 95);
        scene.idle(105);
        resources(scene, util, INPUT);
        scene.markAsFinished();
    }

    public static void power(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_grip_power", "Powering a working block by hand");
        setup(scene, util);
        scene.world().showSection(util.select().fromTo(INPUT.below(), OUTPUT), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(COG.below(), COG), Direction.DOWN);
        scene.idle(25);
        scene.overlay().showControls(util.vector().topOf(PUMP), Pointing.DOWN, 80).withItem(grip());
        text(scene, "grip_mainhand", PUMP, 90);
        scene.idle(100);
        text(scene, "grip_limits", PUMP, 100);
        scene.idle(110);
        scene.overlay().showOutline(PonderPalette.RED, "grip_stationary_cog", util.select().position(COG), 100);
        text(scene, "grip_single", COG, 100);
        scene.idle(110);

        scene.overlay().showControls(util.vector().blockSurface(PUMP, Direction.SOUTH), Pointing.RIGHT, 100)
            .rightClick().withItem(grip());
        scene.world().setKineticSpeed(util.select().position(PUMP), HandheldKineticPower.RPM);
        scene.world().propagatePipeChange(PUMP);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "grip_water_output", util.select().position(OUTPUT), 110);
        text(scene, "grip_hold", PUMP, 95);
        scene.idle(100);

        scene.world().setKineticSpeed(util.select().position(PUMP), 0);
        scene.world().propagatePipeChange(PUMP);
        text(scene, "grip_stop", PUMP, 95);
        scene.idle(105);
        resources(scene, util, PUMP);
        scene.markAsFinished();
    }

    private static void setup(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(.9F);
        scene.showBasePlate();
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
    }

    private static ItemStack grip() { return new ItemStack(BoosterItems.NETHERITE_EXTENDO_GRIP.get()); }

    private static void resources(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos target) {
        scene.overlay().showControls(util.vector().topOf(target).add(0, .4, 0), Pointing.DOWN, 100).withItem(AllItems.COPPER_BACKTANK.asStack());
        text(scene, "grip_resources", target, 110);
        scene.idle(120);
    }

    private static void text(CreateSceneBuilder scene, String key, BlockPos target, int duration) {
        scene.overlay().showText(duration).sharedText(key).pointAt(Vec3.atCenterOf(target))
            .placeNearTarget().attachKeyFrame();
    }
}
