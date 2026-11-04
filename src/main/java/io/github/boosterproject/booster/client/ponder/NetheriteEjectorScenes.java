package io.github.boosterproject.booster.client.ponder;

import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEjectorBlockEntity;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEntityLauncher;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class NetheriteEjectorScenes {
    private static final BlockPos SOURCE = new BlockPos(1, 2, 5);
    private static final BlockPos FIRST = new BlockPos(4, 3, 2);
    private static final BlockPos SECOND = new BlockPos(5, 1, 4);
    private static final BlockPos LEVER = SOURCE.south();

    private NetheriteEjectorScenes() {}

    public static void launch(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_ejector_launch", "Selecting a target and launching items");
        setup(scene, util, false);
        scene.overlay().showControls(util.vector().topOf(FIRST), Pointing.DOWN, 80)
            .rightClick().whileSneaking().withItem(ejector());
        scene.overlay().showOutline(PonderPalette.OUTPUT, "ejector_target", util.select().position(FIRST), 180);
        text(scene, "ejector_select", FIRST, 90);
        scene.idle(100);
        preview(scene, FIRST);
        text(scene, "ejector_diagonal", FIRST, 90);
        scene.idle(100);

        scene.overlay().showControls(util.vector().topOf(SOURCE.below()), Pointing.DOWN, 75).rightClick().withItem(ejector());
        target(scene, FIRST);
        scene.world().showSection(util.select().position(SOURCE), Direction.DOWN);
        text(scene, "ejector_place", SOURCE, 85);
        scene.idle(95);

        scene.world().showSection(util.select().position(SOURCE.west()), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(SOURCE.west(), SOURCE), 64);
        scene.effects().rotationDirectionIndicator(SOURCE.west());
        text(scene, "ejector_power", SOURCE.west(), 85);
        scene.idle(95);
        scene.overlay().showControls(util.vector().topOf(SOURCE), Pointing.DOWN, 65).withItem(new ItemStack(Items.COPPER_INGOT));
        scene.world().createItemOnBeltLike(SOURCE, Direction.UP, new ItemStack(Items.COPPER_INGOT));
        text(scene, "ejector_insert", SOURCE, 85);
        scene.idle(95);
        scene.effects().indicateSuccess(FIRST);
        text(scene, "ejector_arrive", FIRST, 85);
        scene.idle(95);
        scene.markAsFinished();
    }

    public static void controls(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("netherite_ejector_controls", "Retargeting, stack size and redstone");
        setup(scene, util, true);
        target(scene, FIRST);
        scene.world().showSection(util.select().fromTo(SOURCE.west(), SOURCE), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(SOURCE.west(), SOURCE), 64);
        scene.idle(30);
        scene.overlay().showControls(util.vector().topOf(SECOND), Pointing.DOWN, 75)
            .rightClick().whileSneaking().withItem(ejector());
        scene.overlay().showOutline(PonderPalette.OUTPUT, "ejector_new_target", util.select().position(SECOND), 180);
        text(scene, "ejector_reselect", SECOND, 85);
        scene.idle(95);
        scene.overlay().showControls(util.vector().topOf(SOURCE), Pointing.DOWN, 75).rightClick().withItem(ejector());
        target(scene, SECOND);
        preview(scene, SECOND);
        text(scene, "ejector_apply", SOURCE, 95);
        scene.idle(105);

        Vec3 panel = util.vector().blockSurface(SOURCE, Direction.NORTH).add(0, -2 / 16F, 0);
        scene.overlay().showFilterSlotInput(panel, Direction.NORTH, 90);
        scene.overlay().showControls(panel.add(0, .2, 0), Pointing.DOWN, 75).rightClick();
        scene.world().modifyBlockEntity(SOURCE, EjectorBlockEntity.class, be -> be.getBehaviour(ScrollValueBehaviour.TYPE).setValue(4));
        text(scene, "ejector_quantity", SOURCE, 100);
        scene.idle(110);
        insertIron(scene, 2);
        text(scene, "ejector_wait", SOURCE, 85);
        scene.idle(95);
        insertIron(scene, 2);
        text(scene, "ejector_full", SOURCE, 85);
        scene.idle(95);
        scene.effects().indicateSuccess(SECOND);
        scene.overlay().showControls(util.vector().topOf(SECOND), Pointing.DOWN, 60).rightClick();
        scene.idle(20);
        scene.world().removeItemsFromBelt(SECOND);
        scene.idle(50);

        scene.world().showSection(util.select().position(LEVER.below()), Direction.UP);
        scene.world().showSection(util.select().position(LEVER), Direction.NORTH);
        scene.overlay().showControls(util.vector().topOf(LEVER), Pointing.DOWN, 65).rightClick();
        togglePower(scene, util);
        insertIron(scene, 4);
        text(scene, "ejector_pause", LEVER, 100);
        scene.idle(110);
        scene.overlay().showControls(util.vector().topOf(LEVER), Pointing.DOWN, 65).rightClick();
        togglePower(scene, util);
        text(scene, "ejector_resume", LEVER, 85);
        scene.idle(95);
        scene.effects().indicateSuccess(SECOND);
        scene.markAsFinished();
    }

    private static ItemStack ejector() { return new ItemStack(BoosterItems.NETHERITE_WEIGHTED_EJECTOR.get()); }
    private static void insertIron(CreateSceneBuilder scene, int count) {
        // Create's merging depot initially accepts only one item while its held stack is empty.
        scene.world().createItemOnBeltLike(SOURCE, Direction.UP, new ItemStack(Items.IRON_INGOT));
        if (count > 1)
            scene.world().createItemOnBeltLike(SOURCE, Direction.UP, new ItemStack(Items.IRON_INGOT, count - 1));
    }
    private static void setup(CreateSceneBuilder scene, SceneBuildingUtil util, boolean secondTarget) {
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(.8F);
        scene.showBasePlate();
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(util.select().position(SOURCE.below()), Direction.UP);
        scene.world().showSection(util.select().fromTo(FIRST.atY(1), FIRST), Direction.DOWN);
        if (secondTarget) scene.world().showSection(util.select().position(SECOND), Direction.UP);
        scene.idle(20);
    }
    private static void target(CreateSceneBuilder scene, BlockPos target) {
        scene.world().modifyBlockEntity(SOURCE, NetheriteEjectorBlockEntity.class, be -> be.setTargetOffset(target.subtract(SOURCE)));
    }
    private static void togglePower(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().toggleRedstonePower(util.select().position(LEVER));
        scene.world().modifyBlockEntity(SOURCE, EjectorBlockEntity.class, EjectorBlockEntity::updateSignal);
        scene.effects().indicateRedstone(LEVER);
    }
    private static void preview(CreateSceneBuilder scene, BlockPos target) {
        var trajectory = new NetheriteEntityLauncher(target.subtract(SOURCE));
        for (int i = 0; i <= 12; i++) {
            var point = trajectory.getGlobalPos(trajectory.getTotalFlyingTicks() * i / 12D, Direction.NORTH, SOURCE);
            scene.effects().emitParticles(point, scene.effects().simpleParticleEmitter(ParticleTypes.END_ROD, Vec3.ZERO), .1F, 60);
        }
    }
    private static void text(CreateSceneBuilder scene, String key, BlockPos target, int duration) {
        scene.overlay().showText(duration).sharedText(key).pointAt(Vec3.atCenterOf(target).add(0, .5, 0))
            .placeNearTarget().attachKeyFrame();
    }
}
