package io.github.boosterproject.booster.content.kinetics.portable;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.github.boosterproject.booster.Booster;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortableStressDeviceGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_portable_stress_device");

    @GameTest(template = "wireless_empty", timeoutTicks = 60)
    public static void placesOnShaftAndPowersNormalNetwork(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ID), "portable stress device is registered");
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 180, 2));
        level.setBlock(pos, AllBlocks.SHAFT.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PortableStressQA"));
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ID));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(pos.getCenter().add(2, 0, 2));
        var hit = new BlockHitResult(pos.getCenter().add(.5, 0, 0), Direction.EAST, pos, false);
        var result = stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(result.consumesAction() && stack.getCount() == 1, "deployment keeps the reusable item");
        helper.assertTrue(level.getBlockState(pos.east()).is(BuiltInRegistries.BLOCK.get(ID)), "device occupies the indicated adjacent shaft port");
        helper.runAfterDelay(10, () -> {
            var device = (KineticBlockEntity) level.getBlockEntity(pos.east());
            var shaft = (KineticBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(Math.abs(device.getSpeed()) == 64 && Math.abs(shaft.getSpeed()) == 64 && shaft.hasNetwork(), "64RPM reaches the normal kinetic network");
            helper.assertTrue(device.calculateAddedStressCapacity() * Math.abs(device.getGeneratedSpeed()) == 2048, "total generated capacity is exactly 2048SU");
            helper.succeed();
        });
    }
}
