package io.github.boosterproject.booster.content.conversion;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConverterInteractionGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 30)
    public static void containerClicksNeitherFillNorDrainOnAnyFace(GameTestHelper h) {
        var machine = machine(h);
        var player = new RecordingPlayer(h.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        for (Direction face : Direction.values()) {
            machine.getEssenceTank().setFluid(FluidStack.EMPTY);
            var filled = new ItemStack(BoosterFluids.ESSENCE_BUCKET.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, filled);
            var result = player.gameMode.useItemOn(player, h.getLevel(), filled, InteractionHand.MAIN_HAND,
                new BlockHitResult(machine.getBlockPos().getCenter(), face, machine.getBlockPos(), false));
            h.assertTrue(result.consumesAction() && machine.getEssenceTank().isEmpty() && player.getMainHandItem().is(BoosterFluids.ESSENCE_BUCKET.get()), "filled bucket is consumed as an interaction without moving fluid on " + face);
            machine.getEssenceTank().setFluid(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1000));
            var empty = new ItemStack(Items.BUCKET);
            player.setItemInHand(InteractionHand.MAIN_HAND, empty);
            player.gameMode.useItemOn(player, h.getLevel(), empty, InteractionHand.MAIN_HAND,
                new BlockHitResult(machine.getBlockPos().getCenter(), face, machine.getBlockPos(), false));
            h.assertTrue(machine.getEssenceTank().getFluidAmount() == 1000 && player.getMainHandItem().is(Items.BUCKET), "empty bucket cannot extract essence on " + face);
        }
        h.assertTrue(player.actionBarMessages == 0, "container clicks send no action-bar hints");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 30)
    public static void emptyHandSendsNoTextAboveHotbar(GameTestHelper h) {
        var machine = machine(h);
        var player = new RecordingPlayer(h.getLevel());
        player.actionBarMessages = 0;
        for (Direction face : Direction.values())
            machine.getBlockState().useItemOn(ItemStack.EMPTY, h.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(machine.getBlockPos().getCenter(), face, machine.getBlockPos(), false));
        h.assertTrue(player.actionBarMessages == 0, "empty hand sends no action-bar text");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 180)
    public static void poweredPumpAndPipeStillSupplyEssence(GameTestHelper h) {
        var machine = machine(h);
        BlockPos source = installSupply(h, machine.getBlockPos());
        h.runAfterDelay(4, () -> h.assertTrue(Math.abs(((KineticBlockEntity) h.getLevel().getBlockEntity(machine.getBlockPos().south(2))).getSpeed()) == 64,
            "ordinary pump receives real gear and motor power"));
        h.succeedWhen(() -> {
            var cap = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, source, null);
            h.assertTrue(machine.getEssenceTank().getFluidAmount() == 1000 && cap.getFluidInTank(0).isEmpty(), "the complete bucket-equivalent travels from tank through actual pump and pipe");
        });
    }

    static BlockPos installSupply(GameTestHelper h, BlockPos converter) { return installSupply(h, converter, 1000); }

    static BlockPos installSupply(GameTestHelper h, BlockPos converter, int amount) {
        var level = h.getLevel();
        BlockPos pipe = converter.south(), pump = converter.south(2), source = converter.south(3), cog = pump.east(), motor = cog.south();
        level.setBlockAndUpdate(pipe, AllBlocks.FLUID_PIPE.getDefaultState().setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true));
        level.setBlockAndUpdate(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        level.setBlockAndUpdate(source, AllBlocks.FLUID_TANK.getDefaultState());
        level.setBlockAndUpdate(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
        level.setBlockAndUpdate(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(-64);
        var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, source, null);
        h.assertTrue(cap != null && cap.fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), amount), FluidAction.EXECUTE) == amount, "supply tank receives test essence before pumping");
        return source;
    }

    private static NetheriteConverterBlockEntity machine(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 240, 1));
        h.getLevel().setBlockAndUpdate(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState());
        return (NetheriteConverterBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static class RecordingPlayer extends FakePlayer {
        int actionBarMessages;
        RecordingPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "ConverterHintQA")); }
        @Override public void displayClientMessage(Component message, boolean actionBar) {
            if (actionBar) actionBarMessages++;
        }
    }
}
