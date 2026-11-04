package io.github.boosterproject.booster.content.logistics.depot;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteEjectorGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_weighted_ejector");

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void diagonalFlightAndInflightReload(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ID), "netherite ejector must be registered");
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
        BlockPos target = pos.offset(6, 2, 4);
        level.setBlock(target, AllBlocks.DEPOT.get().defaultBlockState(), 3);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState(), 3);
        var ejector = (EjectorBlockEntity) level.getBlockEntity(pos);
        var saved = ejector.saveWithoutMetadata(level.registryAccess());
        saved.putIntArray("TargetOffset", new int[] {6, 2, 4});
        saved.putString("State", "CHARGED");
        saved.putFloat("ForceAngle", 0);
        ejector.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(ejector.getTargetPosition().equals(target), "full diagonal target must be retained");
        var carried = new TransportedItemStack(new ItemStack(Items.IRON_INGOT, 8));
        carried.beltPosition = carried.prevBeltPosition = .5F;
        ejector.getBehaviour(DepotBehaviour.TYPE).setHeldItem(carried);
        helper.runAfterDelay(5, () -> {
            var flying = ejector.saveWithoutMetadata(level.registryAccess());
            helper.assertTrue(!flying.getList("LaunchedItems", 10).isEmpty(), "items are really in flight before reload");
            helper.assertTrue(!((NetheriteEjectorBlockEntity) ejector).setTargetOffset(new BlockPos(-6, 2, 4))
                && ejector.getTargetPosition().equals(target), "in-flight retarget is rejected without changing the trajectory");
            ejector.loadWithComponents(flying, level.registryAccess());
            helper.assertTrue(ejector.getTargetPosition().equals(target), "reload preserves full diagonal target");
        });
        helper.succeedWhen(() -> {
            var depot = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(level, target, DepotBehaviour.TYPE);
            helper.assertTrue(depot.getHeldItemStack().is(Items.IRON_INGOT) && depot.getHeldItemStack().getCount() == 8, "eight real items reach diagonal elevated depot");
            level.removeBlock(pos, false);
            level.removeBlock(target, false);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void targetSelectionPlacementAndRange(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 100, 1));
        BlockPos target = source.offset(40, 5, 30);
        level.setBlock(source.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "EjectorTargetQA"));
        ItemStack stack = new ItemStack(BoosterItems.NETHERITE_WEIGHTED_EJECTOR.get());
        CompoundTag unrelated = new CompoundTag(); unrelated.putString("Unrelated", "keep");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(unrelated));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(target.getCenter());
        player.setShiftKeyDown(true);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(target)));
        helper.assertTrue(target.equals(NetheriteEjectorItem.selectedTarget(stack, level))
            && stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("Unrelated").equals("keep"), "selecting arbitrary target preserves unrelated item data");
        player.setShiftKeyDown(false);
        player.setPos(source.getCenter());
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(source.below())));
        var ejector = (NetheriteEjectorBlockEntity) level.getBlockEntity(source);
        helper.assertTrue(ejector != null && ejector.getTargetPosition().equals(target) && player.getMainHandItem().isEmpty(), "last item really places with a diagonal target beyond vanilla range");
        ejector.loadWithComponents(ejector.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
        helper.assertTrue(ejector.getTargetPosition().equals(target), "reload does not truncate expanded range to original limit");
        int max = NetheriteEjectorBlockEntity.getMaximumRange();
        helper.assertTrue(max == com.simibubi.create.infrastructure.config.AllConfigs.server().kinetics.maxEjectorDistance.get() * 2,
            "range follows twice the original config");
        helper.assertTrue(NetheriteEjectorBlockEntity.isValidOffset(new BlockPos(max, max, 0))
            && !NetheriteEjectorBlockEntity.isValidOffset(new BlockPos(max, 0, 1))
            && !NetheriteEjectorBlockEntity.isValidOffset(new BlockPos(0, max + 1, 0)), "range boundaries use horizontal radius and vertical height");
        for (BlockPos offset : new BlockPos[] {new BlockPos(3, 2, 4), new BlockPos(-4, -3, 2), new BlockPos(-3, 5, -4), new BlockPos(4, 0, -3), new BlockPos(0, 8, 0), new BlockPos(0, -8, 0)}) {
            var trajectory = new NetheriteEntityLauncher(offset);
            Vec3 end = trajectory.getGlobalPos(trajectory.getTotalFlyingTicks(), Direction.NORTH, source);
            helper.assertTrue(end.distanceTo(Vec3.atCenterOf(source.offset(offset))) < .001, "all quadrants and vertical targets retain exact trajectory endpoint");
            Vec3 velocity = trajectory.getGlobalVelocity(3, Direction.NORTH, source);
            helper.assertTrue(Double.isFinite(velocity.x + velocity.y + velocity.z), "arbitrary-direction velocity stays finite");
        }
        ItemStack tool = new ItemStack(BoosterItems.NETHERITE_WEIGHTED_EJECTOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos second = source.offset(-4, 2, 3);
        level.setBlock(second, Blocks.STONE.defaultBlockState(), 3);
        player.setPos(second.getCenter()); player.setShiftKeyDown(true);
        tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(second)));
        player.setPos(source.getCenter()); player.setShiftKeyDown(false);
        level.getBlockState(source).useItemOn(tool, level, player, InteractionHand.MAIN_HAND, hit(source));
        helper.assertTrue(ejector.getTargetPosition().equals(second) && tool.getCount() == 1, "normal block interaction retargets existing platform without consuming held platform");
        BlockPos invalid = source.east(max + 1);
        level.setBlock(invalid, Blocks.STONE.defaultBlockState(), 3);
        player.setPos(invalid.getCenter()); player.setShiftKeyDown(true);
        tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(invalid)));
        player.setPos(source.getCenter()); player.setShiftKeyDown(false);
        level.getBlockState(source).useItemOn(tool, level, player, InteractionHand.MAIN_HAND, hit(source));
        helper.assertTrue(ejector.getTargetPosition().equals(second) && tool.getCount() == 1, "out of range edits leave existing target and item unchanged");
        level.removeBlock(source, false); level.removeBlock(source.below(), false);
        level.removeBlock(target, false); level.removeBlock(second, false); level.removeBlock(invalid, false);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void chargeRedstoneAndEntityLaunch(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 40, 1));
        BlockPos nativePos = pos.east(2);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState(), 3);
        level.setBlock(nativePos, AllBlocks.WEIGHTED_EJECTOR.get().defaultBlockState(), 3);
        var ejector = (NetheriteEjectorBlockEntity) level.getBlockEntity(pos);
        var original = (EjectorBlockEntity) level.getBlockEntity(nativePos);
        ejector.setTargetOffset(new BlockPos(16, 0, 0));
        original.setTarget(16, 0);
        helper.assertTrue(ejector.getWindUpSpeed() == 0, "unpowered platform cannot charge");
        for (BlockPos motor : new BlockPos[] {pos.west(), nativePos.east()}) {
            level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(CreativeMotorBlock.FACING, motor.equals(pos.west()) ? Direction.EAST : Direction.WEST), 3);
            ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        }
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(Math.abs(ejector.getSpeed()) == 64 && Math.abs(original.getSpeed()) == 64, "both real kinetic networks supply 64 RPM");
            helper.assertTrue(Math.abs(ejector.getWindUpSpeed() - original.getWindUpSpeed() * 8) < 1e-6, "charge progress is exactly eight times native at matching speed and distance");
            helper.assertTrue(ejector.getState() == EjectorBlockEntity.State.CHARGED && original.getState() == EjectorBlockEntity.State.RETRACTING,
                "actual native tick charges upgrade much sooner: speeds=" + ejector.getSpeed() + "/" + original.getSpeed()
                    + ", states=" + ejector.getState() + "/" + original.getState());
            ejector.setTargetOffset(new BlockPos(12, 3, -9));
            var pig = EntityType.PIG.create(level);
            pig.setNoAi(true); pig.setPos(pos.getX() + .5, pos.getY() + .82, pos.getZ() + .5); pig.setOnGround(true);
            level.addFreshEntity(pig);
            level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            level.getBlockState(pos).getBlock().updateEntityAfterFallOn(level, pig);
            helper.assertTrue(ejector.isPowered() && ejector.getState() == EjectorBlockEntity.State.CHARGED, "real redstone prevents landing trigger");
            level.removeBlock(pos.north(), false);
            level.getBlockState(pos).getBlock().updateEntityAfterFallOn(level, pig);
            ejector.tick();
            Vec3 motion = pig.getDeltaMovement();
            helper.assertTrue(ejector.getState() == EjectorBlockEntity.State.LAUNCHING && motion.x > 0 && motion.z < 0 && motion.y > 0
                && Math.abs(motion.x / motion.z + 4 / 3D) < 1e-5, "landing really launches a living entity along diagonal target");
            var up = ejector.saveWithoutMetadata(level.registryAccess());
            up.putString("State", "CHARGED"); up.putFloat("ForceAngle", 0); up.putIntArray("TargetOffset", new int[] {0, 8, 0});
            ejector.loadWithComponents(up, level.registryAccess());
            pig.setPos(pos.getX() + .5, pos.getY() + .82, pos.getZ() + .5); pig.setOnGround(true);
            level.getBlockState(pos).getBlock().updateEntityAfterFallOn(level, pig); ejector.tick();
            helper.assertTrue(Math.abs(pig.getDeltaMovement().x) < 1e-8 && Math.abs(pig.getDeltaMovement().z) < 1e-8 && pig.getDeltaMovement().y > 0,
                "pure upward landing launch is not rejected as zero horizontal distance");
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "EjectorElytraQA"));
            ejector.setTargetOffset(new BlockPos(12, 3, -9));
            player.setYRot(0); ejector.deployElytra(player);
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot() - (float) Math.toDegrees(Math.atan2(-12, -9)))) < .001,
                "elytra deployment follows the diagonal target heading");
            pig.discard(); level.removeBlock(pos, false); level.removeBlock(nativePos, false);
            level.removeBlock(pos.west(), false); level.removeBlock(nativePos.east(), false); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 160)
    public static void retargetClearsOldCollisionInventory(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 140, 1));
        BlockPos oldTarget = source.offset(6, 2, 4), target = source.offset(-7, -2, 3);
        level.setBlock(source, BuiltInRegistries.BLOCK.get(ID).defaultBlockState(), 3);
        level.setBlock(oldTarget, AllBlocks.DEPOT.get().defaultBlockState(), 3);
        level.setBlock(target, AllBlocks.DEPOT.get().defaultBlockState(), 3);
        var ejector = (NetheriteEjectorBlockEntity) level.getBlockEntity(source);
        var oldDepot = (com.simibubi.create.content.logistics.depot.DepotBlockEntity) level.getBlockEntity(oldTarget);
        oldDepot.getBehaviour(DepotBehaviour.TYPE).setHeldItem(new TransportedItemStack(new ItemStack(Items.IRON_INGOT, 64)));
        ready(ejector, oldTarget.subtract(source), new ItemStack(Items.IRON_INGOT, 3));
        var saved = ejector.saveWithoutMetadata(level.registryAccess());
        saved.put("EarlyTarget", net.createmod.catnip.math.VecHelper.writeNBT(oldTarget.getCenter()));
        saved.put("EarlyTargetPos", net.minecraft.nbt.NbtUtils.writeBlockPos(oldTarget));
        saved.putFloat("EarlyTargetTime", 10);
        ejector.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(ejector.setTargetOffset(target.subtract(source)), "idle retarget succeeds");
        helper.assertTrue(ejector.getTargetOpenInv() == com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(level, target,
            com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour.TYPE), "old full collision inventory is invalidated immediately");
        helper.succeedWhen(() -> {
            var depot = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(level, target, DepotBehaviour.TYPE);
            helper.assertTrue(depot.getHeldItemStack().is(Items.IRON_INGOT) && depot.getHeldItemStack().getCount() == 3,
                "new target receives all items even when old collision target is full");
            level.removeBlock(source, false); level.removeBlock(oldTarget, false); level.removeBlock(target, false);
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void obstaclesAndBreakingKeepRealItems(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 60, 1));
        BlockPos target = pos.offset(10, 0, 7);
        BlockPos broken = pos.above(20);
        BlockPos farTarget = broken.offset(40, 4, 30);
        level.setBlock(target, AllBlocks.DEPOT.get().defaultBlockState(), 3);
        level.setBlock(farTarget, AllBlocks.DEPOT.get().defaultBlockState(), 3);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState(), 3);
        level.setBlock(broken, BuiltInRegistries.BLOCK.get(ID).defaultBlockState(), 3);
        for (int y = 0; y <= 16; y++) for (int z = 0; z <= 10; z++) level.setBlock(pos.offset(4, y, z), Blocks.STONE.defaultBlockState(), 3);
        ItemStack emeralds = new ItemStack(Items.EMERALD, 5); emeralds.set(DataComponents.CUSTOM_NAME, Component.literal("Ejector obstacle QA"));
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 7); diamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Ejector break QA"));
        ready((EjectorBlockEntity) level.getBlockEntity(pos), new BlockPos(10, 0, 7), emeralds);
        ready((EjectorBlockEntity) level.getBlockEntity(broken), new BlockPos(40, 4, 30), diamonds);
        helper.runAfterDelay(5, () -> {
            var ejector = (EjectorBlockEntity) level.getBlockEntity(broken);
            helper.assertTrue(!ejector.saveWithoutMetadata(level.registryAccess()).getList("LaunchedItems", 10).isEmpty(), "break fixture has real items still in flight");
            level.removeBlock(broken, false);
            int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(broken).inflate(60, 30, 60)).stream()
                .filter(e -> ItemStack.isSameItemSameComponents(e.getItem(), diamonds)).mapToInt(e -> e.getItem().getCount()).sum();
            StringBuilder diagnostic = new StringBuilder();
            for (var entity : level.getAllEntities()) if (entity instanceof ItemEntity item && ItemStack.isSameItemSameComponents(item.getItem(), diamonds))
                diagnostic.append(" count=").append(item.getItem().getCount()).append(" pos=").append(item.position()).append(" box=").append(item.getBoundingBox());
            helper.assertTrue(dropped == 7, "breaking source drops all seven flying items once with components; found=" + dropped + diagnostic);
        });
        helper.runAfterDelay(25, () -> {
            var depot = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(level, target, DepotBehaviour.TYPE);
            helper.assertTrue(depot.getHeldItemStack().isEmpty(), "solid wall prevents reaching target");
            int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(20, 60, 20)).stream()
                .filter(e -> ItemStack.isSameItemSameComponents(e.getItem(), emeralds)).mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(dropped == 5, "obstacle collision preserves five items and their components");
            level.removeBlock(pos, false); level.removeBlock(target, false); level.removeBlock(farTarget, false);
            for (int y = 0; y <= 16; y++) for (int z = 0; z <= 10; z++) level.removeBlock(pos.offset(4, y, z), false);
            helper.succeed();
        });
    }

    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false); }
    private static void ready(EjectorBlockEntity ejector, BlockPos offset, ItemStack stack) {
        var tag = ejector.saveWithoutMetadata(ejector.getLevel().registryAccess());
        tag.putString("State", "CHARGED"); tag.putFloat("ForceAngle", 0);
        tag.putIntArray("TargetOffset", new int[] {offset.getX(), offset.getY(), offset.getZ()});
        ejector.loadWithComponents(tag, ejector.getLevel().registryAccess());
        var carried = new TransportedItemStack(stack.copy()); carried.beltPosition = carried.prevBeltPosition = .5F;
        ejector.getBehaviour(DepotBehaviour.TYPE).setHeldItem(carried);
    }
}
