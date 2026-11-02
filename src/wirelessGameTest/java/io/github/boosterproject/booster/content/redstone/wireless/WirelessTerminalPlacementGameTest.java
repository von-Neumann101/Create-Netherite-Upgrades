package io.github.boosterproject.booster.content.redstone.wireless;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WirelessTerminalPlacementGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void clickedFacesAndPlayerRotation(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "MountingQA"));
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        // Start with a wall: a horizontal model sitting on the floor must not pass as wall placement.
        BlockState wall = place(helper, player, pos, Direction.NORTH, 0);
        helper.assertTrue(wall.getShape(level, pos).bounds().getZsize() < 0.5, "wall terminal must lie against its clicked support face");
        level.removeBlock(pos, false);

        Set<Direction> floorDirections = new HashSet<>();
        for (Direction face : Direction.values()) {
            int rotations = face.getAxis().isVertical() ? 4 : 1;
            for (int i = 0; i < rotations; i++) {
                float yaw = i * 90;
                BlockState state = place(helper, player, pos, face, yaw);
                AttachFace attachment = face == Direction.UP ? AttachFace.FLOOR : face == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL;
                Direction facing = face.getAxis().isHorizontal() ? face : Direction.fromYRot(yaw).getOpposite();
                helper.assertTrue(state.getValue(BlockStateProperties.ATTACH_FACE) == attachment, "clicked face chooses floor, wall, or ceiling");
                helper.assertTrue(state.getValue(WirelessTerminalBlock.FACING) == facing, "horizontal direction follows player yaw or clicked wall");
                if (face == Direction.UP) floorDirections.add(facing);
                BlockState powered = state.setValue(WirelessTerminalBlock.POWERED, true);
                for (Direction side : Direction.values()) {
                    helper.assertTrue(powered.getDirectSignal(level, pos, side) == (side == face ? 15 : 0), "strong signal enters the actual support, not always the floor");
                }
                var terminal = (WirelessTerminalBlockEntity) level.getBlockEntity(pos);
                UUID identity = terminal.terminalId();
                terminal.updatePower(true);
                helper.assertTrue(level.getBlockState(pos) == powered, "power-on preserves mounting and rotation");
                terminal.updatePower(false);
                helper.assertTrue(level.getBlockState(pos) == state && identity.equals(terminal.terminalId()), "power-off preserves rotation and identity");
                var tag = NbtUtils.writeBlockState(state);
                helper.assertTrue(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag) == state, "mounting state survives save and load");
                if (face == Direction.UP) {
                    tag.getCompound("Properties").remove("face");
                    helper.assertTrue(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag) == state, "old saves without mounting property retain floor orientation");
                }
                level.removeBlock(pos, false);
                level.removeBlock(pos.relative(face.getOpposite()), false);
            }
        }
        helper.assertTrue(floorDirections.size() == 4, "floor placement has four distinct player-relative directions");

        BlockPos support = pos.south();
        BlockPos lamp = support.below();
        level.setBlock(lamp, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        BlockState state = place(helper, player, pos, Direction.NORTH, 180);
        var terminal = (WirelessTerminalBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(WirelessTerminalService.activate(level.getServer(), player.getUUID(), terminal.terminalId()), "wall-mounted terminal activates normally");
        helper.assertTrue(level.getBlockState(pos) == state.setValue(WirelessTerminalBlock.POWERED, true), "activation keeps wall mounting");
        helper.assertTrue(level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "wall support transmits signal to a real lamp");
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(level.getBlockState(pos) == state && !level.hasNeighborSignal(lamp), "pulse ends and notifies the wall support circuit");
            level.removeBlock(pos, false);
            level.removeBlock(support, false);
            level.removeBlock(lamp, false);
            helper.succeed();
        });
    }

    private static BlockState place(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction face, float yaw) {
        var level = helper.getLevel();
        level.removeBlock(pos, false);
        BlockPos support = pos.relative(face.getOpposite());
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        player.setYRot(yaw);
        ItemStack item = new ItemStack(BoosterItems.NETHERITE_WIRELESS_TERMINAL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        var result = item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, support, false)));
        helper.assertTrue(result.consumesAction() && level.getBlockState(pos).is(BoosterBlocks.NETHERITE_WIRELESS_TERMINAL.get()), "real item places a terminal on the selected face");
        return level.getBlockState(pos);
    }
}
