package io.github.boosterproject.booster.content.equipment;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.Booster;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteExtendoGripGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_extendo_grip");

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void reachAndNativeResourceCosts(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GripReachQA"));
        double base = player.blockInteractionRange();
        var grip = new ItemStack(BuiltInRegistries.ITEM.get(ID));
        player.setItemInHand(InteractionHand.MAIN_HAND, grip);
        ExtendoGripItem.holdingExtendoGripIncreasesRange(new EntityTickEvent.Pre(player));
        helper.assertTrue(player.blockInteractionRange() == base + 6 && ExtendoGripItem.isHoldingExtendoGrip(player), "one upgraded grip adds six blocks and native recognition");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AllItems.EXTENDO_GRIP.get()));
        ExtendoGripItem.holdingExtendoGripIncreasesRange(new EntityTickEvent.Pre(player));
        helper.assertTrue(player.blockInteractionRange() == base + 8, "mixed grips add eight blocks");
        player.setItemInHand(InteractionHand.OFF_HAND, grip.copy());
        ExtendoGripItem.holdingExtendoGripIncreasesRange(new EntityTickEvent.Pre(player));
        helper.assertTrue(player.blockInteractionRange() == base + 11, "two upgraded grips add eleven blocks");
        ExtendoGripItem.consumeDurabilityOnBlockBreak(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(helper.getLevel(),
            helper.absolutePos(BlockPos.ZERO), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), player));
        helper.assertTrue(grip.getDamageValue() == 1, "native block-break action really damages the new grip");
        var backtank = new ItemStack(AllItems.COPPER_BACKTANK.get());
        backtank.set(AllDataComponents.BACKTANK_AIR, 100);
        player.setItemSlot(EquipmentSlot.CHEST, backtank);
        ExtendoGripItem.consumeDurabilityOnBlockBreak(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(helper.getLevel(),
            helper.absolutePos(BlockPos.ZERO), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), player));
        helper.assertTrue(backtank.get(AllDataComponents.BACKTANK_AIR) < 100 && grip.getDamageValue() == 1, "native backtank air is consumed before durability");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        ExtendoGripItem.holdingExtendoGripIncreasesRange(new EntityTickEvent.Pre(player));
        helper.assertTrue(player.blockInteractionRange() == base, "removing both grips restores original reach");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.EXTENDO_GRIP.get()));
        ExtendoGripItem.holdingExtendoGripIncreasesRange(new EntityTickEvent.Pre(player));
        helper.assertTrue(player.blockInteractionRange() == base + 3, "original grip keeps original reach");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void handPowerReallyPumpsAndStops(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 180, 1));
        level.setBlock(pos.west(), AllBlocks.FLUID_TANK.get().defaultBlockState(), 3);
        level.setBlock(pos.east(), AllBlocks.FLUID_TANK.get().defaultBlockState(), 3);
        level.setBlock(pos, BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST), 3);
        var input = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.west(), null);
        var output = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.east(), null);
        input.fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GripPowerQA"));
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ID));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(pos.getCenter().add(0, 0, 2)); aim(player, pos.getCenter());
        var pump = (KineticBlockEntity) level.getBlockEntity(pos);
        long start = level.getGameTime();
        var driving = new boolean[] {false};
        helper.runAfterDelay(3, () -> {
            var result = stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false)));
            helper.assertTrue(result.consumesAction() && player.isUsingItem(), "main-hand use begins continuous hand power");
            helper.assertTrue(pump.getSpeed() == 16 && pump.getTheoreticalSpeed() == 0 && !pump.hasNetwork(), "hand power drives only the selected machine without a persistent network");
            helper.assertTrue(HandheldKineticPower.capacityPerRpm() == pump.calculateStressApplied(), "capacity exactly matches one upgraded pump");
            var eye = player.getEyePosition();
            var ray = level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            helper.assertTrue(HandheldKineticPower.isStillDriving(player, pump), "initial driver validation: hit=" + ray.getBlockPos() + ", target=" + pos
                + ", eye=" + eye + ", view=" + player.getViewVector(1) + ", range=" + player.blockInteractionRange()
                + ", data=" + player.getPersistentData() + ", alive=" + player.isAlive() + ", mayBuild=" + player.getAbilities().mayBuild);
            driving[0] = true;
        });
        helper.onEachTick(() -> {
            if (driving[0]) stack.getItem().onUseTick(level, player, stack, 72000 - (int) (level.getGameTime() - start));
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(output.getFluidInTank(0).getAmount() > 0, "actual hand-powered pump transfers real water; source=" + input.getFluidInTank(0).getAmount()
                + ", speed=" + pump.getSpeed() + ", using=" + player.isUsingItem() + ", stillDriving=" + HandheldKineticPower.isStillDriving(player, pump)
                + ", damage=" + stack.getDamageValue());
            helper.assertTrue(stack.getDamageValue() >= 3, "sustained hand power consumes repeated durability");
            var saved = pump.saveWithoutMetadata(level.registryAccess());
            helper.assertTrue(saved.getFloat("Speed") == 0 && !saved.contains("NetheriteGripSpeed"), "temporary hand power is absent from persistent speed and saved state");
            driving[0] = false;
            player.releaseUsingItem();
            helper.assertTrue(pump.getSpeed() == 0, "releasing stops the pump immediately");
            pump.loadWithComponents(saved, level.registryAccess());
            helper.assertTrue(pump.getSpeed() == 0, "save reload cannot restore free hand power");
            level.removeBlock(pos, false); level.removeBlock(pos.west(), false); level.removeBlock(pos.east(), false);
            helper.succeed();
        });
    }

    private static void aim(net.minecraft.world.entity.player.Player player, Vec3 target) {
        Vec3 delta = target.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setYHeadRot(player.getYRot());
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void handPowerOwnershipLimitsAndLoss(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 220, 1));
        BlockPos powered = pos.east(6), gear = powered.north(), motor = gear.west(), drill = pos.above(3);
        level.setBlock(pos, BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST), 3);
        level.setBlock(powered, BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST), 3);
        level.setBlock(gear, AllBlocks.COGWHEEL.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X), 3);
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
            .setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING, Direction.EAST), 3);
        ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        level.setBlock(drill, AllBlocks.MECHANICAL_DRILL.get().defaultBlockState(), 3);
        var owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GripOwnerQA"));
        var other = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GripOtherQA"));
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ID));
        var otherStack = stack.copy();
        var air = new ItemStack(AllItems.COPPER_BACKTANK.get()); air.set(AllDataComponents.BACKTANK_AIR, 100);
        owner.setItemSlot(EquipmentSlot.CHEST, air);
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack); other.setItemInHand(InteractionHand.MAIN_HAND, otherStack);
        var pump = (KineticBlockEntity) level.getBlockEntity(pos);
        helper.runAfterDelay(3, () -> {
            owner.setPos(pos.getCenter().add(0, 0, 2)); aim(owner, pos.getCenter());
            other.setPos(pos.getCenter().add(0, 0, 2)); aim(other, pos.getCenter());
            helper.assertTrue(use(owner, pos).consumesAction() && pump.getSpeed() == 16, "owner starts one hand-powered block");
            helper.assertTrue(stack.getDamageValue() == 0 && air.get(AllDataComponents.BACKTANK_AIR) < 100, "power activation consumes air before durability");
            helper.assertTrue(use(other, pos) == net.minecraft.world.InteractionResult.FAIL && otherStack.getDamageValue() == 0,
                "second player cannot steal active hand power or pay for a rejected claim");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(pump.getSpeed() == 0, "switching away from the tool stops hand power");
            owner.stopUsingItem(); owner.setItemInHand(InteractionHand.MAIN_HAND, stack); aim(owner, pos.getCenter());
            owner.setShiftKeyDown(true); helper.assertTrue(use(owner, pos).consumesAction() && pump.getSpeed() == -16, "sneaking starts reverse rotation");
            owner.setShiftKeyDown(false); owner.setYHeadRot(owner.getYHeadRot() + 180);
        });
        helper.runAfterDelay(9, () -> {
            helper.assertTrue(pump.getSpeed() == 0, "looking away stops hand power"); owner.stopUsingItem();
            owner.setPos(powered.getCenter().add(0, 0, 2)); aim(owner, powered.getCenter());
            var external = (KineticBlockEntity) level.getBlockEntity(powered);
            helper.assertTrue(Math.abs(external.getSpeed()) == 64, "real external motor fixture provides 64 RPM; actual=" + external.getSpeed());
            helper.assertTrue(use(owner, powered) == net.minecraft.world.InteractionResult.FAIL && Math.abs(external.getSpeed()) == 64,
                "hand power does not override a real external motor");
            helper.assertTrue(use(owner, motor) == net.minecraft.world.InteractionResult.FAIL, "generators cannot become hand-powered consumers");
            double previous = io.github.boosterproject.booster.config.BoosterConfigs.SERVER.powerfulPumpStressImpact.get();
            try {
                io.github.boosterproject.booster.config.BoosterConfigs.SERVER.powerfulPumpStressImpact.set(1D);
                owner.setPos(drill.getCenter().add(0, 0, 2)); aim(owner, drill.getCenter());
                helper.assertTrue(use(owner, drill) == net.minecraft.world.InteractionResult.FAIL
                    && ((KineticBlockEntity) level.getBlockEntity(drill)).getSpeed() == 0, "a consumer exceeding configured hand capacity cannot start");
            } finally { io.github.boosterproject.booster.config.BoosterConfigs.SERVER.powerfulPumpStressImpact.set(previous); }
            level.removeBlock(pos, false); level.removeBlock(powered, false); level.removeBlock(motor, false);
            level.removeBlock(gear, false); level.removeBlock(drill, false);
            helper.succeed();
        });
    }

    private static net.minecraft.world.InteractionResult use(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        var stack = player.getMainHandItem();
        return stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false)));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void survivalBucketFromTank(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ID), "netherite extendo grip must be registered");
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 160, 1));
        level.setBlock(pos, AllBlocks.FLUID_TANK.get().defaultBlockState(), 3);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GripBucketQA"));
        player.setPos(pos.getCenter().add(0, 0, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        ItemStack grip = new ItemStack(BuiltInRegistries.ITEM.get(ID));
        player.setItemSlot(EquipmentSlot.OFFHAND, grip);
        var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        tank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(2, () -> {
            var hit = new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false);
            level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "survival player really fills the main-hand bucket");
            helper.assertTrue(tank.getFluidInTank(0).getAmount() == 1000, "tank loses exactly one bucket");
            helper.assertTrue(grip.getDamageValue() == 1, "one successful bucket costs one durability without air");
            level.removeBlock(pos, false);
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void upgradedTankStackedBucketsAndAir(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 200, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_FLUID_TANK.get().defaultBlockState(), 3);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GripLavaQA"));
        player.setPos(pos.getCenter().add(0, 0, 2));
        var grip = new ItemStack(BuiltInRegistries.ITEM.get(ID));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 2));
        var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        tank.fill(new FluidStack(Fluids.LAVA, 1999), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(2, () -> {
            var hit = new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false);
            level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(tank.getFluidInTank(0).getAmount() == 1999 && player.getMainHandItem().getCount() == 2,
                "without the upgraded offhand grip survival tank interaction stays unchanged");
            player.setItemInHand(InteractionHand.OFF_HAND, grip);
            level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && player.getMainHandItem().getCount() == 1
                && tank.getFluidInTank(0).getAmount() == 999 && grip.getDamageValue() == 1, "stacked buckets consume exactly one container and one resource action");
            int filled = player.getInventory().items.stream().filter(s -> s.is(Items.LAVA_BUCKET)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(filled == 1, "filled lava bucket is really placed in inventory");
            level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(tank.getFluidInTank(0).getAmount() == 999 && grip.getDamageValue() == 1,
                "insufficient fluid does not consume air, durability or container");
            tank.fill(new FluidStack(Fluids.LAVA, 1), IFluidHandler.FluidAction.EXECUTE);
            var air = new ItemStack(AllItems.COPPER_BACKTANK.get()); air.set(AllDataComponents.BACKTANK_AIR, 100);
            player.setItemSlot(EquipmentSlot.CHEST, air);
            player.getAbilities().mayBuild = false;
            level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(tank.getFluidInTank(0).getAmount() == 1000 && air.get(AllDataComponents.BACKTANK_AIR) == 100,
                "restricted players cannot drain tanks");
            player.getAbilities().mayBuild = true;
            level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET) && tank.getFluidInTank(0).isEmpty()
                && air.get(AllDataComponents.BACKTANK_AIR) < 100 && grip.getDamageValue() == 1, "last bucket uses backtank air before tool durability");
            level.removeBlock(pos, false); helper.succeed();
        });
    }
}
