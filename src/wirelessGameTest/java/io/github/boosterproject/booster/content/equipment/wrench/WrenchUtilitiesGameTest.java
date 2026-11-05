package io.github.boosterproject.booster.content.equipment.wrench;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.conversion.ConversionPlate;
import io.github.boosterproject.booster.content.conversion.NetheriteConverterBlockEntity;
import io.github.boosterproject.booster.content.equipment.HandheldKineticPower;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchUtilitiesGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void sharedReservoirFullCollectionAndExactPlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 120, 1));
        level.setBlock(source, Blocks.WATER.defaultBlockState(), 3);
        var player = player(helper, "WrenchTankQA", source);
        ItemStack wrench = wrench(WrenchModule.TANK);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        WrenchData.setFluids(wrench, List.of(new FluidStack(Fluids.WATER, 32000), new FluidStack(Fluids.LAVA, 32000)), level.registryAccess());
        helper.assertTrue(!WrenchTank.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && level.getFluidState(source).isSource() && WrenchData.totalFluid(wrench, level.registryAccess()) == 64000,
            "full shared capacity rejects collection without removing the real water source");
        WrenchData.setFluids(wrench, List.of(new FluidStack(Fluids.WATER, 32000), new FluidStack(Fluids.LAVA, 31000)), level.registryAccess());
        helper.assertTrue(WrenchTank.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && level.getFluidState(source).isEmpty() && WrenchData.totalFluid(wrench, level.registryAccess()) == 64000
            && WrenchData.fluids(wrench, level.registryAccess()).get(0).getAmount() == 33000,
            "source pickup fills the shared final bucket and merges its fluid type");

        level.setBlock(source, Blocks.STONE.defaultBlockState(), 3);
        WrenchData.selectFluid(wrench, 1); player.setShiftKeyDown(true); aim(player, source.getCenter());
        helper.assertTrue(WrenchTank.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && level.getFluidState(source.south()).is(Fluids.LAVA) && level.getFluidState(source.south()).isSource()
            && WrenchData.totalFluid(wrench, level.registryAccess()) == 63000
            && WrenchData.fluids(wrench, level.registryAccess()).get(1).getAmount() == 30000,
            "selected lava places an actual source and debits exactly one bucket, leaving water unchanged");
        WrenchData.setActiveSlot(wrench, -1);
        helper.assertTrue(!WrenchTank.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && WrenchData.totalFluid(wrench, level.registryAccess()) == 63000,
            "removing the tank ability disables use without deleting its contents");
        level.removeBlock(source, false); level.removeBlock(source.south(), false);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void waterloggedPickupAndBuildPermission(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(1, 150, 1));
        level.setBlock(pos, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 3);
        var player = player(helper, "WrenchWaterlogQA", pos);
        ItemStack wrench = wrench(WrenchModule.TANK); player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        player.getAbilities().mayBuild = false;
        helper.assertTrue(!WrenchTank.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED), "build-disabled player cannot remove water");
        player.getAbilities().mayBuild = true;
        helper.assertTrue(WrenchTank.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && level.getBlockState(pos).is(Blocks.OAK_SLAB) && !level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED)
            && WrenchData.totalFluid(wrench, level.registryAccess()) == 1000,
            "native pickup removes the water while retaining its slab");
        player.setShiftKeyDown(true); aim(player, pos.getCenter());
        helper.assertTrue(WrenchTank.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED)
            && WrenchData.totalFluid(wrench, level.registryAccess()) == 0, "native placement waterlogs the slab and drains one bucket");
        level.removeBlock(pos, false); helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void enginePowersOnlySelectedPumpWithoutResourceCost(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(1, 180, 1));
        level.setBlock(pos.west(), AllBlocks.FLUID_TANK.getDefaultState(), 3);
        level.setBlock(pos.east(), AllBlocks.FLUID_TANK.getDefaultState(), 3);
        level.setBlock(pos, BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST), 3);
        var input = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.west(), null);
        var output = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.east(), null);
        input.fill(new FluidStack(Fluids.WATER, 8000), FluidAction.EXECUTE);
        var player = player(helper, "WrenchPowerQA", pos); ItemStack wrench = wrench(WrenchModule.ENGINE);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var backtank = new ItemStack(AllItems.COPPER_BACKTANK.get()); backtank.set(AllDataComponents.BACKTANK_AIR, 100);
        player.setItemSlot(EquipmentSlot.CHEST, backtank);
        var pump = (KineticBlockEntity) level.getBlockEntity(pos); boolean[] driving = {false};
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(HandheldKineticPower.begin(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false))).consumesAction(), "engine begins real hand power");
            helper.assertTrue(pump.getSpeed() == 16 && !pump.hasNetwork() && pump.getTheoreticalSpeed() == 0
                && HandheldKineticPower.capacityPerRpm() == pump.calculateStressApplied(), "engine supplies exactly one pump without a torque network");
            driving[0] = true;
        });
        helper.onEachTick(() -> { if (driving[0]) HandheldKineticPower.continueUsing(player, wrench, (int) level.getGameTime()); });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(output.getFluidInTank(0).getAmount() > 0, "engine-powered pump transfers real water");
            helper.assertTrue(backtank.get(AllDataComponents.BACKTANK_AIR) == 100 && wrench.getDamageValue() == 0,
                "wrench engine consumes neither backtank air nor durability");
            driving[0] = false; WrenchData.setActiveSlot(wrench, -1);
            HandheldKineticPower.continueUsing(player, wrench, 61);
            helper.assertTrue(pump.getSpeed() == 0 && !player.isUsingItem(), "deactivating the engine releases its temporary power");
            for (var block : List.of(pos, pos.west(), pos.east())) level.removeBlock(block, false);
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 280)
    public static void upgradeTemplateConvertsExactlyOnceAtNormalFluidCost(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(2, 210, 1)), depot = pos.below(2);
        level.setBlock(depot, AllBlocks.DEPOT.getDefaultState(), 3);
        level.setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        level.setBlock(pos.west(), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
        level.setBlock(pos.west(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), 3);
        ((CreativeMotorBlockEntity) level.getBlockEntity(pos.west(2))).generatedSpeed.setValue(64);
        var machine = (NetheriteConverterBlockEntity) level.getBlockEntity(pos);
        var user = player(helper, "WrenchTemplateQA", pos);
        var template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        helper.assertTrue(ConversionPlate.remaining(template) == 1, "vanilla template has exactly one conversion");
        var sheet = new ItemStack(BoosterItems.NETHERITE_SHEET.get());
        for (int i = 0; i < 15; i++) ConversionPlate.consume(sheet);
        helper.assertTrue(sheet.getCount() == 1 && ConversionPlate.remaining(sheet) == 1, "sheet retains its original sixteen-use behavior");
        ConversionPlate.consume(sheet); helper.assertTrue(sheet.isEmpty(), "sixteenth sheet conversion consumes its plate");
        user.setItemInHand(InteractionHand.MAIN_HAND, template);
        level.getBlockState(pos).useItemOn(template, level, user, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false));
        helper.assertTrue(template.isEmpty() && machine.getPlateInventory().getStackInSlot(0).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            "the normal right-click insertion accepts the actual vanilla template");
        helper.runAfterDelay(5, () -> {
            machine.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1000), FluidAction.EXECUTE);
            ((SmartBlockEntity) level.getBlockEntity(depot)).getBehaviour(DepotBehaviour.TYPE)
                .setCenteredHeldItem(new TransportedItemStack(new ItemStack(AllBlocks.MECHANICAL_PRESS.get(), 2)));
        });
        helper.runAfterDelay(240, () -> {
            var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, depot, null);
            helper.assertTrue(count(inventory, BoosterItems.NETHERITE_MECHANICAL_PRESS.get()) == 1
                && count(inventory, AllBlocks.MECHANICAL_PRESS.asItem()) == 1 && machine.getPlateInventory().getStackInSlot(0).isEmpty()
                && machine.getEssenceTank().getFluidAmount() == 750,
                "one template produces exactly one upgraded machine and consumes only the normal 250mB essence");
            for (var block : List.of(pos, depot, pos.west(), pos.west(2))) level.removeBlock(block, false);
            helper.succeed();
        });
    }

    private static ItemStack wrench(WrenchModule module) {
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_netherite_wrench")));
        WrenchData.setModules(stack, List.of(new ItemStack(module.item()))); WrenchData.setActiveSlot(stack, 0); return stack;
    }
    private static Player player(GameTestHelper helper, String name, BlockPos target) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setPos(target.getCenter().add(0, 0, 2)); aim(player, target.getCenter()); return player;
    }
    private static void aim(Player player, Vec3 target) {
        Vec3 delta = target.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); player.setYHeadRot(player.getYRot());
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
    }
    private static int count(IItemHandler inventory, Item item) {
        int total = 0; for (int i = 0; i < inventory.getSlots(); i++) if (inventory.getStackInSlot(i).is(item)) total += inventory.getStackInSlot(i).getCount(); return total;
    }
}
