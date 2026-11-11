package io.github.boosterproject.booster.content.kinetics.portable;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.UUID;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortableStressLifecycleGameTest {
    private static ItemStack device() { return new ItemStack(BoosterItems.NETHERITE_PORTABLE_STRESS_DEVICE.get()); }
    private static Player player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortableQA"));
    }
    private static void shaft(GameTestHelper helper, BlockPos pos, Direction.Axis axis) {
        helper.getLevel().setBlock(pos, AllBlocks.SHAFT.get().defaultBlockState().setValue(BlockStateProperties.AXIS, axis), 3);
    }
    private static InteractionResult deploy(Player player, ItemStack stack, BlockPos pos, Direction side) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(pos.getCenter().add(2, 0, 2));
        var hit = new BlockHitResult(pos.getCenter().add(side.getStepX() * .5, side.getStepY() * .5, side.getStepZ() * .5), side, pos, false);
        return stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void placementCancellationLeavesNoBlockOrCooldown(GameTestHelper helper) {
        var level = helper.getLevel(); var player = player(helper); var stack = device();
        var pos = helper.absolutePos(new BlockPos(2, 180, 2)); shaft(helper, pos, Direction.Axis.X);
        boolean[] eventSeen = {false};
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> deny = event -> {
            if (event.getPos().equals(pos.east())) { eventSeen[0] = true; event.setCanceled(true); }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
        try {
            helper.assertTrue(!deploy(player, stack, pos, Direction.EAST).consumesAction(), "placement event can reject device");
            helper.assertTrue(eventSeen[0] && level.isEmptyBlock(pos.east()), "native event runs and rolls the block back");
            helper.assertTrue(stack.getCount() == 1 && PortableStressDeviceItem.id(stack) == null, "rejected deployment keeps item ready without creating an identity");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny); }
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 60)
    public static void unloadedSourceDetachesAndValidReloadResumes(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 180, 2));
        var stack = device(); shaft(helper, pos, Direction.Axis.X); deploy(player(helper), stack, pos, Direction.EAST);
        helper.runAfterDelay(8, () -> {
            var device = (PortableStressDeviceBlockEntity) level.getBlockEntity(pos.east());
            device.onChunkUnloaded();
        });
        helper.runAfterDelay(12, () -> {
            var device = (PortableStressDeviceBlockEntity) level.getBlockEntity(pos.east());
            helper.assertTrue(((KineticBlockEntity) level.getBlockEntity(pos)).getSpeed() == 0, "source unload detaches loaded downstream machines immediately");
            var saved = device.saveWithFullMetadata(level.registryAccess()); var state = device.getBlockState();
            level.removeBlockEntity(pos.east());
            var restored = (PortableStressDeviceBlockEntity) BlockEntity.loadStatic(pos.east(), state, saved, level.registryAccess());
            level.setBlockEntity(restored); restored.initialize();
        });
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(Math.abs(((KineticBlockEntity) level.getBlockEntity(pos)).getSpeed()) == 64, "unexpired reload reconnects the source");
            helper.assertTrue(PortableStressData.get(level.getServer()).entry(PortableStressDeviceItem.id(stack)).deployment() != null, "unload does not retire the item or reset its timer");
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 60)
    public static void staleSourceReconcilesSavedCapacityAfterFullNetworkReload(GameTestHelper helper) {
        restoreSharedNetwork(helper, false);
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 60)
    public static void staleSourceRemovalBeforeFirstTickReconcilesSavedCapacity(GameTestHelper helper) {
        restoreSharedNetwork(helper, true);
    }

    @SuppressWarnings("unchecked")
    private static void restoreSharedNetwork(GameTestHelper helper, boolean removeBeforeFirstTick) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(3, 180, 3));
        var stack = device(); shaft(helper, pos, Direction.Axis.X); deploy(player(helper), stack, pos, Direction.EAST);
        level.setBlock(pos.west(), AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST), 3);
        ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity) level.getBlockEntity(pos.west())).generatedSpeed.setValue(-128);
        helper.runAfterDelay(10, () -> {
            var positions = new BlockPos[] {pos.west(), pos, pos.east()};
            CompoundTag[] tags = new CompoundTag[3];
            var states = new net.minecraft.world.level.block.state.BlockState[3];
            var motor = (KineticBlockEntity) level.getBlockEntity(pos.west());
            float expectedCapacity = motor.calculateAddedStressCapacity() * 128;
            helper.assertTrue(motor.getOrCreateNetwork().calculateCapacity() == expectedCapacity + 2048, "portable contributes 2048SU beside stronger source");
            Long networkId = motor.network;
            for (int i = 0; i < 3; i++) {
                var be = (KineticBlockEntity) level.getBlockEntity(positions[i]);
                tags[i] = be.saveWithFullMetadata(level.registryAccess()); states[i] = be.getBlockState();
            }
            for (BlockPos position : positions) {
                ((KineticBlockEntity) level.getBlockEntity(position)).onChunkUnloaded(); level.removeBlockEntity(position);
            }
            // Recreate just this test's native network as it would be recreated after server restart.
            try {
                var field = com.simibubi.create.content.kinetics.TorquePropagator.class.getDeclaredField("networks"); field.setAccessible(true);
                var networks = (java.util.Map<Object, java.util.Map<Long, com.simibubi.create.content.kinetics.KineticNetwork>>) field.get(null);
                networks.get(level).remove(networkId);
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
            var data = PortableStressData.get(level.getServer()); var id = PortableStressDeviceItem.id(stack);
            data.finish(id, data.entry(id).deployment().token(), PortableStressData.now(level.getServer()));
            for (int i = 0; i < 3; i++) level.setBlockEntity(BlockEntity.loadStatic(positions[i], states[i], tags[i], level.registryAccess()));
            for (int i = 0; i < 2; i++) ((KineticBlockEntity) level.getBlockEntity(positions[i])).initialize();
            // Exercise the normal first-tick path, rather than calling the stale device's
            // initialize manually: its tick must reconcile saved capacity before removal.
            if (removeBeforeFirstTick) level.removeBlock(pos.east(), false);
            else ((KineticBlockEntity) level.getBlockEntity(pos.east())).tick();
            helper.assertTrue(level.isEmptyBlock(pos.east()), "stale source is removed during restored initialization");
            var restoredMotor = (KineticBlockEntity) level.getBlockEntity(pos.west());
            helper.assertTrue(restoredMotor.getOrCreateNetwork().calculateCapacity() == expectedCapacity, "saved portable capacity is fully withdrawn from shared network");
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void wrenchRemovalRespectsNativeBreakEvent(GameTestHelper helper) {
        var level = helper.getLevel(); var player = player(helper); var stack = device();
        var pos = helper.absolutePos(new BlockPos(2, 180, 2)); shaft(helper, pos, Direction.Axis.X); deploy(player, stack, pos, Direction.EAST);
        var state = level.getBlockState(pos.east()); boolean[] eventSeen = {false};
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> deny = event -> {
            if (event.getPos().equals(pos.east())) { eventSeen[0] = true; event.setCanceled(true); }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
        try {
            var ctx = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(pos.east().getCenter(), Direction.EAST, pos.east(), false));
            ((PortableStressDeviceBlock) state.getBlock()).onSneakWrenched(state, ctx);
            helper.assertTrue(eventSeen[0] && level.getBlockState(pos.east()).is(state.getBlock()), "native break event protects wrench removal");
            helper.assertTrue(PortableStressData.get(level.getServer()).entry(PortableStressDeviceItem.id(stack)).deployment() != null, "denied removal keeps active device");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny); }
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 60)
    public static void separateItemsRecallAndCoolDownIndependently(GameTestHelper helper) {
        var level = helper.getLevel(); var player = player(helper); var a = device(); var b = device();
        var pa = helper.absolutePos(new BlockPos(2, 180, 2)); var pb = pa.south(4);
        shaft(helper, pa, Direction.Axis.X); shaft(helper, pb, Direction.Axis.X);
        helper.assertTrue(deploy(player, a, pa, Direction.EAST).consumesAction()
            && deploy(player, b, pb, Direction.EAST).consumesAction(), "two items can deploy for the same player");
        var data = PortableStressData.get(level.getServer());
        var aId = PortableStressDeviceItem.id(a); var bId = PortableStressDeviceItem.id(b);
        helper.assertTrue(!aId.equals(bId), "items get independent identities");
        player.setXRot(-90);
        helper.assertTrue(b.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "air right click recalls held device");
        helper.assertTrue(level.isEmptyBlock(pb.east()) && !level.isEmptyBlock(pa.east()), "recalling b leaves a deployed");
        long now = PortableStressData.now(level.getServer());
        helper.assertTrue(data.entry(bId).readyAt() == now + 2400 && data.entry(aId).deployment() != null, "only b starts a 120 second cooldown");
        helper.assertTrue(!deploy(player, b, pb, Direction.EAST).consumesAction() && b.getCount() == 1, "cooling item cannot deploy and is retained");
        var receiver = player(helper); receiver.setItemInHand(InteractionHand.MAIN_HAND, b);
        b.getItem().inventoryTick(b, level, receiver, 0, true);
        helper.assertTrue(bId.equals(PortableStressDeviceItem.id(receiver.getMainHandItem())) && !data.available(bId, now), "transfer preserves the item's cooldown");
        helper.runAfterDelay(3, () -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, a); player.setXRot(-90);
            a.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(level.isEmptyBlock(pa.east()) && data.entry(aId).readyAt() > data.entry(bId).readyAt(), "each item has its own recall and cooldown deadline");
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void placementPreviewAndPlacementAgreeOnAllSixPorts(GameTestHelper helper) {
        var player = player(helper);
        for (Direction side : Direction.values()) {
            var pos = helper.absolutePos(new BlockPos(2 + side.ordinal() * 3, 180, 2));
            var stack = device(); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            shaft(helper, pos, side.getAxis());
            var hit = new BlockHitResult(pos.getCenter().add(side.getStepX() * .5, side.getStepY() * .5, side.getStepZ() * .5), side, pos, false);
            var placementHelper = PlacementHelpers.getHelpersView().stream().filter(h -> h.matchesItem(stack)).findFirst().orElseThrow();
            var offset = placementHelper.getOffset(player, helper.getLevel(), helper.getLevel().getBlockState(pos), pos, hit, stack);
            helper.assertTrue(offset.isSuccessful() && offset.getBlockPos().equals(pos.relative(side)), "native helper points to shaft port: " + side);
            helper.assertTrue(deploy(player, stack, pos, side).consumesAction(), "placement succeeds: " + side);
            var placed = helper.getLevel().getBlockState(pos.relative(side));
            helper.assertTrue(placed.getValue(PortableStressDeviceBlock.FACING) == side.getOpposite(), "device faces its target: " + side);
            helper.assertTrue(placed.equals(offset.getGhostState()), "ghost and real block state match");
            PortableStressService.recall(helper.getLevel().getServer(), PortableStressDeviceItem.id(stack));
        }
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void invalidPlacementDoesNotAllocateOrConsumeItem(GameTestHelper helper) {
        var player = player(helper); var stack = device(); var pos = helper.absolutePos(new BlockPos(2, 180, 2));
        helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(!deploy(player, stack, pos, Direction.EAST).consumesAction(), "stone is not a kinetic port");
        shaft(helper, pos, Direction.Axis.X);
        helper.getLevel().setBlock(pos.east(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos.west(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(!deploy(player, stack, pos, Direction.EAST).consumesAction(), "occupied ports cannot place");
        helper.getLevel().removeBlock(pos.east(), false); player.getAbilities().mayBuild = false;
        helper.assertTrue(!deploy(player, stack, pos, Direction.EAST).consumesAction(), "adventure placement is rejected");
        player.getAbilities().mayBuild = true;
        helper.assertTrue(stack.getCount() == 1 && PortableStressDeviceItem.id(stack) == null && helper.getLevel().isEmptyBlock(pos.east()), "invalid placement creates no identity, block or item loss");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void normalNetworkUsesExactly2048SUAndOverloads(GameTestHelper helper) {
        var level = helper.getLevel(); var player = player(helper); var stack = device(); var pos = helper.absolutePos(new BlockPos(3, 180, 3));
        var state = BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        level.setBlock(pos, state, 3);
        helper.assertTrue(deploy(player, stack, pos, Direction.EAST).consumesAction(), "connects to converter shaft");
        helper.runAfterDelay(8, () -> {
            var converter = (KineticBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(Math.abs(converter.getSpeed()) == 64 && !converter.isOverStressed(), "one 2048SU converter runs at 64RPM");
            level.setBlock(pos.west(), state, 3);
        });
        helper.runAfterDelay(18, () -> {
            var converter = (KineticBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(converter.isOverStressed() && converter.getSpeed() == 0, "two converters exceed shared 2048SU budget");
            level.removeBlock(pos.west(), false);
        });
        helper.runAfterDelay(28, () -> {
            var converter = (KineticBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(!converter.isOverStressed() && Math.abs(converter.getSpeed()) == 64, "network resumes after excess load removed");
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void deadlinesAndPersistentDataRoundTrip(GameTestHelper helper) {
        var data = new PortableStressData(); var id = UUID.randomUUID(); var second = UUID.randomUUID();
        var first = data.deploy(id, Level.NETHER, new BlockPos(5, 80, -2), 100);
        helper.assertTrue(first.expiresAt() == 2500 && !data.available(id, 2499), "runs for 2400 ticks");
        var activeCopy = PortableStressData.load(data.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(activeCopy.active(id, first.token(), first.dimension(), first.pos(), 2499)
            && !activeCopy.active(id, first.token(), first.dimension(), first.pos(), 2500), "saved active lease retains dimension, position and precise expiry");
        helper.assertTrue(data.finish(id, first.token(), 2600) && data.entry(id).readyAt() == 4900, "unloaded expiry starts cooldown at deadline");
        helper.assertTrue(!data.finish(id, first.token(), 3000) && data.entry(id).readyAt() == 4900, "duplicate removal never extends cooldown");
        helper.assertTrue(!data.available(id, 4899) && data.available(id, 4900), "cooldown boundary is exactly 120 seconds");
        data.deploy(second, Level.OVERWORLD, BlockPos.ZERO, 4900);
        var copy = PortableStressData.load(data.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(copy.entry(id).readyAt() == 4900 && copy.entry(second).deployment() != null, "cooldown and active entries both survive save/load");
        var next = data.deploy(id, first.dimension(), first.pos(), 4900);
        helper.assertTrue(next != null && !next.token().equals(first.token()) && !data.finish(id, first.token(), 4901), "old activation cannot retire reused item");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void expiresAndStartsCooldownWithoutHoldingItem(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 180, 2)); shaft(helper, pos, Direction.Axis.X);
        var id = UUID.randomUUID(); var data = PortableStressData.get(level.getServer()); long now = PortableStressData.now(level.getServer());
        var deployment = data.deploy(id, level.dimension(), pos.east(), now - 2398);
        level.setBlock(pos.east(), BoosterBlocks.NETHERITE_PORTABLE_STRESS_DEVICE.get().defaultBlockState().setValue(PortableStressDeviceBlock.FACING, Direction.WEST), 3);
        ((PortableStressDeviceBlockEntity) level.getBlockEntity(pos.east())).bind(id, deployment);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(level.isEmptyBlock(pos.east()) && data.entry(id).deployment() == null, "device expires without a held item");
            helper.assertTrue(data.entry(id).readyAt() == deployment.expiresAt() + 2400, "automatic expiry starts own cooldown");
            helper.assertTrue(((KineticBlockEntity) level.getBlockEntity(pos)).getSpeed() == 0, "power leaves real network");
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void savedBlockResumesAndStaleBlockCannotGenerate(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 180, 2)); shaft(helper, pos, Direction.Axis.X);
        var stack = device(); deploy(player(helper), stack, pos, Direction.EAST);
        var device = (PortableStressDeviceBlockEntity) level.getBlockEntity(pos.east());
        var saved = device.saveWithFullMetadata(level.registryAccess()); var state = level.getBlockState(pos.east());
        device.onChunkUnloaded(); level.removeBlockEntity(pos.east());
        var restored = (PortableStressDeviceBlockEntity) BlockEntity.loadStatic(pos.east(), state, saved, level.registryAccess());
        level.setBlockEntity(restored); restored.initialize();
        helper.assertTrue(Math.abs(restored.getGeneratedSpeed()) == 64, "valid saved deployment resumes generation");
        var data = PortableStressData.get(level.getServer()); var id = PortableStressDeviceItem.id(stack); var token = data.entry(id).deployment().token();
        restored.onChunkUnloaded(); level.removeBlockEntity(pos.east()); data.finish(id, token, PortableStressData.now(level.getServer()));
        var stale = (PortableStressDeviceBlockEntity) BlockEntity.loadStatic(pos.east(), state, saved, level.registryAccess());
        level.setBlockEntity(stale);
        helper.assertTrue(stale.getGeneratedSpeed() == 0, "recalled block cannot generate even before initialize");
        stale.initialize();
        helper.assertTrue(level.isEmptyBlock(pos.east()), "unloaded stale block removes itself before restoring network");
        var restoredStack = ItemStack.parse(level.registryAccess(), stack.save(level.registryAccess())).orElseThrow();
        PortableStressDeviceItem.sync(restoredStack, level.getServer());
        helper.assertTrue(id.equals(PortableStressDeviceItem.id(restoredStack)) && !data.available(id, PortableStressData.now(level.getServer())), "serialized item keeps cooldown identity");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void wrenchRemovalDoesNotDuplicateDevice(GameTestHelper helper) {
        var level = helper.getLevel(); var player = player(helper); var stack = device(); var pos = helper.absolutePos(new BlockPos(2, 180, 2));
        shaft(helper, pos, Direction.Axis.X); deploy(player, stack, pos, Direction.EAST);
        var state = level.getBlockState(pos.east());
        helper.assertTrue(Block.getDrops(state, level, pos.east(), level.getBlockEntity(pos.east())).isEmpty(), "temporary block has no loot");
        helper.assertTrue(state.getBlock().getCloneItemStack(level, pos.east(), state).isEmpty(), "pick block cannot copy reusable item");
        var ctx = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(pos.east().getCenter(), Direction.EAST, pos.east(), false));
        ((PortableStressDeviceBlock) state.getBlock()).onSneakWrenched(state, ctx);
        helper.assertTrue(level.isEmptyBlock(pos.east()) && stack.getCount() == 1, "wrench removal retains exactly held item");
        var data = PortableStressData.get(level.getServer()); var id = PortableStressDeviceItem.id(stack);
        helper.assertTrue(data.entry(id).deployment() == null && data.entry(id).readyAt() == PortableStressData.now(level.getServer()) + 2400, "wrench removal starts cooldown");
        helper.assertTrue(stack.getMaxStackSize() == 1 && stack.has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT) && state.getBlock().getExplosionResistance() >= 1200, "netherite protection and unstackable rules apply");
        helper.succeed();
    }
}
