package io.github.boosterproject.booster.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import io.github.boosterproject.booster.content.kinetics.fan.NetheriteEncasedFanBlockEntity;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteFilterData;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class NetheriteFanScenes {
    private static final BlockPos FAN = new BlockPos(1, 2, 3);
    private static final BlockPos DEPOT = new BlockPos(4, 1, 3);

    private NetheriteFanScenes() {}

    public static void modes(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("netherite_fan_modes", "Built-in processing modes");
        setup(scene, util, false);
        text(scene, "fan_power", FAN, 80);
        scene.effects().rotationDirectionIndicator(FAN.west());
        scene.idle(90);
        scene.overlay().showControls(util.vector().topOf(FAN), Pointing.DOWN, 70).rightClick();
        text(scene, "fan_no_media", FAN.east(), 100);
        scene.overlay().showOutline(PonderPalette.GREEN, "empty_intake", util.select().position(FAN.east()), 100);
        scene.idle(110);

        for (FanProcessingMode mode : FanProcessingMode.values()) {
            mode(scene, mode);
            item(scene, DEPOT, input(mode));
            scene.overlay().showControls(util.vector().topOf(FAN), Pointing.DOWN, 55).withItem(mode.icon());
            text(scene, "fan_" + mode.serializedName(), DEPOT, 85);
            particles(scene, DEPOT, mode, 35);
            scene.idle(35);
            item(scene, DEPOT, output(mode));
            scene.effects().indicateSuccess(DEPOT);
            scene.idle(60);
            clear(scene, DEPOT);
            scene.idle(10);
        }
        scene.markAsFinished();
    }

    public static void filter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("netherite_fan_filter", "Filter rules and the selected mode");
        setup(scene, util, true);
        ItemStack filter = new ItemStack(BoosterItems.NETHERITE_FILTER.get());
        var rules = NetheriteFilterData.inventory(filter);
        for (FanProcessingMode mode : new FanProcessingMode[] {
            FanProcessingMode.WASHING, FanProcessingMode.BLASTING, FanProcessingMode.HAUNTING
        }) {
            NetheriteFilterData.assignToGroup(rules, mode, new ItemStack(input(mode)));
        }
        NetheriteFilterData.save(filter, rules);

        scene.overlay().showControls(util.vector().topOf(FAN), Pointing.DOWN, 80)
            .rightClick().withItem(new ItemStack(BoosterItems.NETHERITE_FILTER.get()));
        text(scene, "fan_filter_open", FAN, 85);
        scene.idle(95);
        text(scene, "fan_filter_keys", DEPOT, 120);
        scene.idle(130);
        text(scene, "fan_filter_samples", DEPOT, 85);
        scene.idle(95);
        scene.overlay().showControls(util.vector().topOf(FAN), Pointing.DOWN, 80).rightClick().withItem(filter);
        text(scene, "fan_filter_install", FAN, 90);
        scene.world().modifyBlockEntity(FAN, NetheriteEncasedFanBlockEntity.class,
            fan -> fan.getFilterInventory().setStackInSlot(0, filter.copy()));
        mode(scene, FanProcessingMode.SMOKING);
        scene.idle(100);

        FanProcessingMode[] modes = FanProcessingMode.values();
        for (int i = 0; i < modes.length; i++) item(scene, FAN.east(2 + i).below(), input(modes[i]));
        text(scene, "fan_filter_override", DEPOT, 100);
        for (int i = 0; i < 3; i++) particles(scene, FAN.east(2 + i).below(), modes[i], 40);
        scene.idle(40);
        for (int i = 0; i < 3; i++) item(scene, FAN.east(2 + i).below(), output(modes[i]));
        scene.idle(70);

        BlockPos fallback = FAN.east(5).below();
        text(scene, "fan_filter_fallback", fallback, 100);
        particles(scene, fallback, FanProcessingMode.SMOKING, 35);
        scene.idle(35);
        item(scene, fallback, Items.COOKED_BEEF);
        scene.effects().indicateSuccess(fallback);
        scene.idle(75);

        for (int i = 0; i < 4; i++) clear(scene, FAN.east(2 + i).below());
        mode(scene, FanProcessingMode.WASHING);
        scene.overlay().showControls(util.vector().topOf(FAN), Pointing.DOWN, 80)
            .rightClick().withItem(FanProcessingMode.WASHING.icon());
        text(scene, "fan_filter_switch", FAN, 110);
        item(scene, DEPOT, Items.RAW_IRON);
        item(scene, fallback, Items.BLUE_WOOL);
        scene.idle(120);
        particles(scene, DEPOT, FanProcessingMode.BLASTING, 35);
        particles(scene, fallback, FanProcessingMode.WASHING, 35);
        scene.idle(35);
        item(scene, DEPOT, Items.IRON_INGOT);
        item(scene, fallback, Items.WHITE_WOOL);
        text(scene, "fan_filter_result", fallback, 110);
        scene.idle(120);
        scene.markAsFinished();
    }

    public static void speed(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("netherite_fan_speed", "Faster processing and stronger airflow");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(.85f);
        scene.world().setBlocks(util.select().fromTo(0, 1, 0, 6, 2, 6), Blocks.AIR.defaultBlockState(), false);
        BlockPos fast = new BlockPos(1, 2, 1);
        BlockPos regular = new BlockPos(1, 2, 5);
        BlockPos fastDepot = fast.east(3).below();
        BlockPos regularDepot = regular.east(3).below();
        scene.world().setBlock(fast, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState()
            .setValue(EncasedFanBlock.FACING, Direction.EAST), false);
        scene.world().setBlock(regular, AllBlocks.ENCASED_FAN.getDefaultState()
            .setValue(EncasedFanBlock.FACING, Direction.EAST), false);
        for (BlockPos fan : new BlockPos[] {fast, regular}) {
            scene.world().setBlock(fan.west(), AllBlocks.SHAFT.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
            scene.world().setBlock(fan.east(3).below(), AllBlocks.DEPOT.getDefaultState(), false);
        }
        scene.world().setBlock(regular.east(), Blocks.WATER.defaultBlockState(), false);
        scene.world().setKineticSpeed(util.select().everywhere(), 64);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 2, 6), Direction.DOWN);
        scene.idle(20);
        text(scene, "fan_speed_compare", fast, 120);
        scene.effects().rotationSpeedIndicator(fast.west());
        scene.effects().rotationSpeedIndicator(regular.west());
        scene.idle(130);

        // Ponder illustrates the default 4:1 progress ratio, rather than timing a live recipe.
        item(scene, fastDepot, Items.RED_WOOL);
        item(scene, regularDepot, Items.RED_WOOL);
        particles(scene, fastDepot, FanProcessingMode.WASHING, 40);
        particles(scene, regularDepot, FanProcessingMode.WASHING, 160);
        scene.idle(40);
        item(scene, fastDepot, Items.WHITE_WOOL);
        scene.effects().indicateSuccess(fastDepot);
        scene.idle(120);
        item(scene, regularDepot, Items.WHITE_WOOL);
        scene.effects().indicateSuccess(regularDepot);
        scene.idle(20);
        text(scene, "fan_speed_wind", fast, 110);
        scene.idle(120);
        text(scene, "fan_speed_range", fast.east(2), 110);
        scene.idle(120);
        text(scene, "fan_transport", fastDepot, 100);
        scene.idle(110);
        scene.markAsFinished();
    }

    private static void setup(CreateSceneBuilder scene, SceneBuildingUtil util, boolean allDepots) {
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(.85f);
        scene.showBasePlate();
        scene.world().setKineticSpeed(util.select().everywhere(), 64);
        scene.world().showSection(util.select().fromTo(FAN.west(), FAN), Direction.DOWN);
        scene.world().showSection(allDepots ? util.select().fromTo(FAN.east(2).below(), FAN.east(5).below())
            : util.select().position(DEPOT), Direction.UP);
        scene.idle(20);
    }

    private static void text(CreateSceneBuilder scene, String key, BlockPos target, int duration) {
        scene.overlay().showText(duration).sharedText(key).pointAt(Vec3.atCenterOf(target).add(0, .5, 0))
            .placeNearTarget().attachKeyFrame();
    }

    private static void mode(CreateSceneBuilder scene, FanProcessingMode mode) {
        scene.world().modifyBlockEntity(FAN, NetheriteEncasedFanBlockEntity.class, fan -> fan.setProcessingMode(mode));
    }

    private static void item(CreateSceneBuilder scene, BlockPos depot, Item item) {
        scene.world().modifyBlockEntity(depot, DepotBlockEntity.class, be -> be.setHeldItem(new ItemStack(item)));
    }

    private static void clear(CreateSceneBuilder scene, BlockPos depot) {
        scene.world().modifyBlockEntity(depot, DepotBlockEntity.class, be -> be.setHeldItem(ItemStack.EMPTY));
    }

    private static void particles(CreateSceneBuilder scene, BlockPos depot, FanProcessingMode mode, int duration) {
        SimpleParticleType particle = switch (mode) {
            case WASHING -> ParticleTypes.SPLASH;
            case BLASTING -> ParticleTypes.FLAME;
            case HAUNTING -> ParticleTypes.SOUL_FIRE_FLAME;
            case SMOKING -> ParticleTypes.SMOKE;
        };
        scene.effects().emitParticles(Vec3.atCenterOf(depot).add(0, .7, 0),
            scene.effects().simpleParticleEmitter(particle, new Vec3(0, .03, 0)), .4f, duration);
    }

    private static Item input(FanProcessingMode mode) {
        return switch (mode) {
            case WASHING -> Items.RED_WOOL;
            case BLASTING -> Items.RAW_IRON;
            case HAUNTING -> Items.SAND;
            case SMOKING -> Items.BEEF;
        };
    }

    private static Item output(FanProcessingMode mode) {
        return switch (mode) {
            case WASHING -> Items.WHITE_WOOL;
            case BLASTING -> Items.IRON_INGOT;
            case HAUNTING -> Items.SOUL_SAND;
            case SMOKING -> Items.COOKED_BEEF;
        };
    }
}
