package io.github.boosterproject.booster.content.equipment;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteExtendoGripPonderGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 120)
    public static void tutorialTankAndHandPowerLayout(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var structure = new StructureTemplate();
        try (var stream = NetheriteExtendoGripPonderGameTest.class.getClassLoader()
                .getResourceAsStream("assets/createnetherite/ponder/extendo_grip/tutorial.nbt")) {
            helper.assertTrue(stream != null, "production grip Ponder structure is available");
            structure.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap()));
        }
        BlockPos origin = helper.absolutePos(new BlockPos(1, 300, 1));
        BlockPos inputPos = origin.offset(1, 2, 2), pumpPos = origin.offset(2, 2, 2), outputPos = origin.offset(3, 2, 2);
        helper.assertTrue(structure.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 3), "production tutorial structure places");
        helper.assertTrue(level.getBlockState(pumpPos).is(BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get())
            && level.getBlockState(pumpPos.below()).is(AllBlocks.ANDESITE_CASING.get()), "tutorial pump is registered and supported");
        var input = level.getCapability(Capabilities.FluidHandler.BLOCK, inputPos, null);
        var output = level.getCapability(Capabilities.FluidHandler.BLOCK, outputPos, null);
        var pump = (KineticBlockEntity) level.getBlockEntity(pumpPos);
        var cog = (KineticBlockEntity) level.getBlockEntity(pumpPos.north());
        helper.assertTrue(ICogWheel.isSmallCog(level.getBlockState(pumpPos))
            && ICogWheel.isSmallCog(level.getBlockState(pumpPos.north()))
            && ((PumpBlock) level.getBlockState(pumpPos).getBlock()).getRotationAxis(level.getBlockState(pumpPos))
                == level.getBlockState(pumpPos.north()).getValue(BlockStateProperties.AXIS),
            "tutorial cogwheel really meshes with the pump before testing stationary transmission");
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GripPonderQA"));
        var grip = new ItemStack(BoosterItems.NETHERITE_EXTENDO_GRIP.get());
        var driving = new boolean[] {false};
        long start = level.getGameTime();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(input.getFluidInTank(0).getAmount() == 8000 && output.getFluidInTank(0).isEmpty(), "tutorial tanks are separate and source contains water");
            player.setPos(inputPos.getCenter().add(0, 0, 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            player.setItemInHand(InteractionHand.OFF_HAND, grip);
            level.getBlockState(inputPos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit(inputPos));
            helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && input.getFluidInTank(0).getAmount() == 7000
                && grip.getDamageValue() == 1, "offhand assistance really fills one bucket from the tutorial tank and costs one action");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, grip);
            player.setPos(pumpPos.getCenter().add(0, 0, 2));
            Vec3 delta = pumpPos.getCenter().subtract(player.getEyePosition());
            player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); player.setYHeadRot(player.getYRot());
            player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
            var result = grip.getItem().onItemUseFirst(grip, new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pumpPos)));
            helper.assertTrue(result.consumesAction() && player.isUsingItem() && pump.getSpeed() == 16 && cog.getSpeed() == 0,
                "main-hand use powers only the tutorial pump, leaving the meshed cogwheel stationary");
            driving[0] = true;
        });
        helper.onEachTick(() -> {
            if (driving[0]) grip.getItem().onUseTick(level, player, grip, 72000 - (int) (level.getGameTime() - start));
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(output.getFluidInTank(0).getAmount() > 0 && input.getFluidInTank(0).getAmount() < 7000,
                "the actual tutorial layout transfers water while hand-powered");
            helper.assertTrue(grip.getDamageValue() >= 4 && pump.getSpeed() == 16 && cog.getSpeed() == 0,
                "holding use costs resources and never powers the meshed cogwheel");
            driving[0] = false; player.releaseUsingItem();
            helper.assertTrue(pump.getSpeed() == 0 && pump.getTheoreticalSpeed() == 0, "release stops temporary power immediately");
            for (int x = 0; x < structure.getSize().getX(); x++) for (int y = 0; y < structure.getSize().getY(); y++)
                for (int z = 0; z < structure.getSize().getZ(); z++) level.removeBlock(origin.offset(x, y, z), false);
            helper.succeed();
        });
    }

    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false); }
}
