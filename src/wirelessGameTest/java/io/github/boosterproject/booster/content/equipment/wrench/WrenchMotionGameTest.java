package io.github.boosterproject.booster.content.equipment.wrench;

import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.Booster;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchMotionGameTest {
    private WrenchMotionGameTest() {}

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void airflowPushPullAndWall(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 100, 1));
        var player = player(level, origin.getBottomCenter());
        ItemStack wrench = equip(player, WrenchModule.FAN);
        var pig = EntityType.PIG.create(level);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setPos(origin.getX() + .5, origin.getY() + 1.2, origin.getZ() + 4.5);
        level.addFreshEntity(pig);
        WrenchAirflow.tick(player, wrench);
        helper.assertTrue(pig.getDeltaMovement().z > 0, "fan really pushes a living entity forward");
        pig.setDeltaMovement(Vec3.ZERO);
        WrenchData.setFanPull(wrench, true);
        WrenchAirflow.tick(player, wrench);
        helper.assertTrue(pig.getDeltaMovement().z < 0, "pull mode reverses the actual entity force");
        BlockPos wall = origin.offset(0, 1, 2);
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        pig.setDeltaMovement(Vec3.ZERO);
        WrenchAirflow.tick(player, wrench);
        helper.assertTrue(pig.getDeltaMovement().lengthSqr() == 0, "solid wall blocks the pull current");
        WrenchData.setFanPull(wrench, false);
        WrenchAirflow.tick(player, wrench);
        helper.assertTrue(pig.getDeltaMovement().lengthSqr() == 0, "solid wall blocks the push current");
        level.removeBlock(wall, false);
        WrenchData.setActiveSlot(wrench, -1);
        WrenchAirflow.tick(player, wrench);
        helper.assertTrue(pig.getDeltaMovement().lengthSqr() == 0, "stored but inactive fan grants no force");
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void previewUsesAllDirectionsAndSixtyFourBlockLimit(GameTestHelper helper) {
        var player = player(helper.getLevel(), helper.absoluteVec(new Vec3(1.5, 130, 1.5)));
        equip(player, WrenchModule.EJECTOR);
        for (float[] view : new float[][] {{0, 0}, {45, -30}, {135, 30}, {-90, -80}}) {
            player.setYRot(view[0]);
            player.setXRot(view[1]);
            var trajectory = WrenchLaunch.trajectory(player);
            var points = WrenchLaunch.preview(player);
            helper.assertTrue(trajectory.target().distanceTo(player.position()) <= 64.00001,
                "arbitrary diagonal and vertical target stays inside 64 blocks");
            helper.assertTrue(points.get(0).distanceTo(player.position()) < .00001
                && points.get(points.size() - 1).distanceTo(trajectory.target()) < .00001,
                "displayed arc starts at player and ends at selected target");
            helper.assertTrue(points.stream().allMatch(point -> Double.isFinite(point.x + point.y + point.z)),
                "all trajectory samples remain finite");
        }
        WrenchData.setActiveSlot(player.getMainHandItem(), -1);
        helper.assertTrue(!WrenchLaunch.start(player) && !WrenchLaunch.isProtected(player),
            "inactive ejector cannot launch or grant fall protection");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 120)
    public static void realUnblockedFlightAndSingleLandingProtection(GameTestHelper helper) {
        flight(helper, false);
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 120)
    public static void realWallCollisionRestoresFallDamage(GameTestHelper helper) {
        flight(helper, true);
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void respawnedPlayerDoesNotInheritPreviousFlight(GameTestHelper helper) {
        var level = helper.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "WrenchRespawnQA");
        var original = player(level, helper.absoluteVec(new Vec3(1.5, 285, 1.5)), profile);
        equip(original, WrenchModule.EJECTOR);
        Vec3 oldTarget = WrenchLaunch.trajectory(original).target();
        long sameTick = level.getGameTime();
        helper.assertTrue(WrenchLaunch.start(original) && WrenchLaunch.isProtected(original),
            "original living player owns a protected launch");
        original.setHealth(0);
        var respawned = player(level, oldTarget, profile);
        helper.assertTrue(respawned != original && respawned.getUUID().equals(original.getUUID())
            && respawned.level() == original.level() && level.getGameTime() == sameTick,
            "replacement player has the same profile, dimension and game tick");
        helper.assertTrue(!WrenchLaunch.isProtected(respawned), "replacement player cannot inherit old entity flight protection");
        float before = respawned.getHealth();
        respawned.causeFallDamage(8, 1, respawned.damageSources().fall());
        helper.assertTrue(respawned.getHealth() < before,
            "new native player takes ordinary fall damage even at the previous launch destination");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void ceilingCollisionSurvivesSecondMoveInSameTick(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 265, 1));
        List<BlockPos> placed = new ArrayList<>();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            for (int y : new int[] {-16, 4}) {
                BlockPos pos = origin.offset(x, y, z);
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                placed.add(pos);
            }
        }
        var player = player(level, origin.getBottomCenter());
        equip(player, WrenchModule.EJECTOR);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        player.setHealth(100);
        player.causeFallDamage(8, 1, player.damageSources().fall());
        helper.assertTrue(player.getHealth() < 100, "ceiling fixture permits ordinary fall damage before launch");
        player.setHealth(100);
        player.invulnerableTime = 0;
        player.setXRot(-90);
        helper.assertTrue(WrenchLaunch.start(player), "vertical launch starts below real ceiling");
        boolean[] hitCeiling = {false};
        int[] ticks = {0};
        helper.onEachTick(() -> {
            ticks[0]++;
            Vec3 before = player.position();
            player.travel(Vec3.ZERO);
            Vec3 moved = player.position().subtract(before);
            if (!hitCeiling[0] && player.verticalCollision && !player.onGround()) {
                hitCeiling[0] = true;
                helper.assertTrue(!WrenchLaunch.isProtected(player), "native move records ceiling collision before player tick event");
                player.move(MoverType.SELF, Vec3.ZERO);
                helper.assertTrue(!player.horizontalCollision && !player.verticalCollision,
                    "second native move in the same tick really clears transient collision flags");
                helper.assertTrue(!WrenchLaunch.isProtected(player), "recorded collision survives cleared native flags");
            }
            player.doCheckFallDamage(moved.x, moved.y, moved.z, player.onGround());
            WrenchLaunch.tick(new PlayerTickEvent.Post(player));
            if (ticks[0] < 3 || !player.onGround()) return;
            helper.assertTrue(hitCeiling[0] && player.getY() < origin.getY(), "ceiling blocks the actual upward flight before descending to floor");
            helper.assertTrue(player.getHealth() < 100 && !WrenchLaunch.isProtected(player),
                "real fall after head collision remains harmful and clears the flight");
            placed.forEach(pos -> level.removeBlock(pos, false));
            helper.succeed();
        });
    }

    private static void flight(GameTestHelper helper, boolean blocked) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, blocked ? 210 : 160, 1));
        List<BlockPos> placed = new ArrayList<>();
        int floorOffset = blocked ? -16 : -1;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 76; z++) {
            BlockPos pos = origin.offset(x, floorOffset, z);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            placed.add(pos);
        }
        if (blocked) for (int x = -2; x <= 2; x++) for (int y = 4; y <= 40; y++) {
            BlockPos pos = origin.offset(x, y, 8);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            placed.add(pos);
        }
        var player = player(level, origin.getBottomCenter());
        equip(player, WrenchModule.EJECTOR);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        player.setHealth(100);
        player.causeFallDamage(8, 1, player.damageSources().fall());
        helper.assertTrue(player.getHealth() < 100, "ordinary fall damages the native test player before any launch protection");
        player.setHealth(100);
        player.invulnerableTime = 0;
        player.resetFallDistance();
        Vec3 initial = player.position();
        var trajectory = WrenchLaunch.trajectory(player);
        helper.assertTrue(WrenchLaunch.start(player) && WrenchLaunch.isProtected(player), "active ejector launches native player physics");
        helper.assertTrue(player.getDeltaMovement().distanceTo(trajectory.velocity()) < 1e-8
            && player.getDeltaMovement().z > 3.9, "full range keeps real initial velocity above vanilla packet clamp");
        // Flight protection belongs to this flight, so unequipping cannot create a persistent ability.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        int[] ticks = {0};
        boolean[] collided = {false};
        helper.onEachTick(() -> {
            ticks[0]++;
            Vec3 before = player.position();
            player.travel(Vec3.ZERO);
            Vec3 moved = player.position().subtract(before);
            collided[0] |= player.horizontalCollision;
            player.doCheckFallDamage(moved.x, moved.y, moved.z, player.onGround());
            WrenchLaunch.tick(new PlayerTickEvent.Post(player));
            if (ticks[0] < 3 || !player.onGround()) return;
            if (blocked) {
                helper.assertTrue(collided[0] && player.getZ() < origin.getZ() + 8,
                    "native movement hits elevated wall without passing through it");
                helper.assertTrue(player.getHealth() < 100, "collision restores actual landing damage");
            } else {
                helper.assertTrue(!collided[0] && player.position().subtract(initial).horizontalDistance() > 60,
                    "unblocked player really travels approximately the selected 64 blocks");
                helper.assertTrue(player.getHealth() == 100, "first unobstructed landing cancels actual fall damage");
            }
            helper.assertTrue(!WrenchLaunch.isProtected(player), "landing consumes flight protection even after unequipping");
            player.invulnerableTime = 0;
            float beforeSecondFall = player.getHealth();
            player.causeFallDamage(8, 1, player.damageSources().fall());
            helper.assertTrue(player.getHealth() < beforeSecondFall, "later ordinary falls remain harmful");
            placed.forEach(pos -> level.removeBlock(pos, false));
            helper.succeed();
        });
    }

    private static ServerPlayer player(ServerLevel level, Vec3 pos) {
        return player(level, pos, new GameProfile(UUID.randomUUID(), "WrenchMotionQA"));
    }

    private static ServerPlayer player(ServerLevel level, Vec3 pos, GameProfile profile) {
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault()) {
            @Override public boolean isControlledByLocalInstance() { return true; }
        };
        // Use the real native player damage/physics paths. FakePlayer.tick() is empty and
        // leaves ServerPlayer's 60-tick spawn invulnerability permanently active here.
        player.connection = FakePlayerFactory.get(level, profile).connection;
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.getAbilities().flying = false;
        player.setPos(pos);
        player.setYRot(0);
        player.setXRot(0);
        player.setOnGround(false);
        for (int tick = 0; tick < 60; tick++) player.tick();
        return player;
    }

    private static ItemStack equip(ServerPlayer player, WrenchModule module) {
        var id = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_netherite_wrench");
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        List<ItemStack> modules = new ArrayList<>();
        for (int i = 0; i < 8; i++) modules.add(i == 0 ? new ItemStack(module.item()) : ItemStack.EMPTY);
        WrenchData.setModules(stack, modules);
        WrenchData.setActiveSlot(stack, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }
}
