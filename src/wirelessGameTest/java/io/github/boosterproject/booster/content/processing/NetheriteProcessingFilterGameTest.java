package io.github.boosterproject.booster.content.processing;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.content.kinetics.press.NetheriteMechanicalPressBlockEntity;
import io.github.boosterproject.booster.content.kinetics.mixer.NetheriteMechanicalMixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteProcessingFilterGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 260)
    public static void pressBlacklistKeepsLapisUntilFilterIsRemoved(GameTestHelper helper) {
        Fixture f = fixture(helper, false, 270);
        filter(helper, f).setFilter(list(true, Items.LAPIS_BLOCK));
        f.basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, 64));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(count(f.basin.getInputInventory(), Items.LAPIS_LAZULI) == 64
                && count(f.basin.getOutputInventory(), Items.LAPIS_BLOCK) == 0,
                "blacklisted compression must not consume any lapis");
            filter(helper, f).setFilter(ItemStack.EMPTY);
        });
        helper.runAfterDelay(220, () -> {
            helper.assertTrue(count(f.basin.getOutputInventory(), Items.LAPIS_BLOCK) == 7
                && count(f.basin.getInputInventory(), Items.LAPIS_LAZULI) == 1,
                "removing the machine filter wakes the press and bulk-compresses seven blocks");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 260)
    public static void mixerBlacklistAllowsAllSixtyFourTargetRecipes(GameTestHelper helper) {
        Fixture f = fixture(helper, true, 270);
        filter(helper, f).setFilter(list(true, Items.LAPIS_BLOCK));
        var input = f.basin.getInputInventory();
        input.setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, 64));
        for (int slot = 1; slot <= 4; slot++) input.setStackInSlot(slot, new ItemStack(Items.EGG, 16));
        input.setStackInSlot(5, new ItemStack(Items.SUGAR, 64));
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(count(f.basin.getOutputInventory(), Items.DIAMOND) == 64
                && count(f.basin.getOutputInventory(), Items.LAPIS_BLOCK) == 0,
                "all sixty-four sets must use the target recipe instead of competing lapis compression");
            helper.assertTrue(input.isEmpty(), "target recipes consume exactly all three input types");
            cleanup(helper, f); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 380)
    public static void machineWhitelistAndBasinFilterBothRestrictRecipes(GameTestHelper helper) {
        Fixture press = fixture(helper, false, 260), mixer = fixture(helper, true, 280);
        List<Fixture> fixtures = List.of(press, mixer);
        for (Fixture f : fixtures) filter(helper, f).setFilter(list(false, Items.DIRT));
        press.basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, 18));
        targetInputs(mixer, 8);
        helper.runAfterDelay(100, () -> {
            for (Fixture f : fixtures) {
                helper.assertTrue(f.basin.getOutputInventory().isEmpty(), "non-matching machine whitelist blocks recipes");
                Item result = f == press ? Items.LAPIS_BLOCK : Items.DIAMOND;
                f.basin.getFilter().setFilter(list(true, result));
                filter(helper, f).setFilter(list(false, result));
            }
        });
        helper.runAfterDelay(200, () -> {
            for (Fixture f : fixtures) {
                helper.assertTrue(f.basin.getOutputInventory().isEmpty(), "machine permission must not bypass the basin blacklist");
                f.basin.getFilter().setFilter(ItemStack.EMPTY);
            }
        });
        helper.runAfterDelay(350, () -> {
            helper.assertTrue(count(press.basin.getOutputInventory(), Items.LAPIS_BLOCK) == 2
                && count(mixer.basin.getOutputInventory(), Items.DIAMOND) == 8,
                "whitelisted outputs are processed in bulk once both filters permit them");
            for (Fixture f : fixtures) cleanup(helper, f);
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void directPressingFiltersProductsInsteadOfIngredients(GameTestHelper helper) {
        Fixture f = fixture(helper, false, 270);
        var press = (NetheriteMechanicalPressBlockEntity) helper.getLevel().getBlockEntity(f.machine);
        Item sheet = AllItems.IRON_SHEET.get();
        filter(helper, f).setFilter(list(true, sheet));
        var input = new TransportedItemStack(new ItemStack(Items.IRON_INGOT, 64));
        List<ItemStack> outputs = new ArrayList<>();
        var entity = new ItemEntity(helper.getLevel(), f.machine.getX(), f.machine.getY() - 2, f.machine.getZ(), new ItemStack(Items.IRON_INGOT, 64));
        helper.assertTrue(!press.tryProcessOnBelt(input, outputs, true) && !press.tryProcessOnBelt(input, outputs, false)
            && outputs.isEmpty() && input.stack.getCount() == 64 && !press.tryProcessInWorld(entity, false)
            && entity.getItem().is(Items.IRON_INGOT), "blacklisted sheets prevent belt, depot and world pressing without consuming iron");
        filter(helper, f).setFilter(list(false, sheet));
        helper.assertTrue(press.tryProcessOnBelt(input, outputs, false)
            && outputs.stream().filter(stack -> stack.is(sheet)).mapToInt(ItemStack::getCount).sum() == 64,
            "whitelisting the product permits a whole stack although iron itself is not listed");
        helper.assertTrue(press.tryProcessInWorld(entity, false) && entity.getItem().is(sheet) && entity.getItem().getCount() == 64,
            "world pressing uses the same permitted output and original bulk processing");
        cleanup(helper, f); helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 260)
    public static void changedFilterCancelsAlreadySelectedBasinRecipes(GameTestHelper helper) {
        Fixture press = fixture(helper, false, 260), mixer = fixture(helper, true, 280);
        filter(helper, press); filter(helper, mixer);
        press.basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, 18));
        targetInputs(mixer, 8);
        boolean[] changed = {false, false};
        helper.onEachTick(() -> {
            var p = (NetheriteMechanicalPressBlockEntity) helper.getLevel().getBlockEntity(press.machine);
            var m = (NetheriteMechanicalMixerBlockEntity) helper.getLevel().getBlockEntity(mixer.machine);
            if (!changed[0] && p.pressingBehaviour.running) {
                filter(helper, press).setFilter(list(true, Items.LAPIS_BLOCK)); changed[0] = true;
            }
            if (!changed[1] && m.running) {
                filter(helper, mixer).setFilter(list(true, Items.DIAMOND)); changed[1] = true;
            }
        });
        helper.runAfterDelay(220, () -> {
            helper.assertTrue(changed[0] && changed[1], "both machines selected a recipe before the filter change");
            helper.assertTrue(count(press.basin.getInputInventory(), Items.LAPIS_LAZULI) == 18
                && count(mixer.basin.getInputInventory(), Items.LAPIS_LAZULI) == 8
                && press.basin.getOutputInventory().isEmpty() && mixer.basin.getOutputInventory().isEmpty(),
                "new blacklist applies before the selected recipe consumes materials");
            cleanup(helper, press); cleanup(helper, mixer); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void nativeInteractionPersistsAndReturnsOneRealFilter(GameTestHelper helper) {
        for (boolean mixer : new boolean[] {false, true}) {
            Fixture f = fixture(helper, mixer, mixer ? 280 : 260);
            var behaviour = filter(helper, f);
            var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ProcessingFilterQA"));
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(f.machine.getCenter().add(0, 0, 2));
            ItemStack installed = list(true, Items.LAPIS_BLOCK);
            // Create strips the default enchantment and attribute components on installation.
            ItemStack canonical = FilterItemStack.of(installed.copy()).item();
            player.setItemInHand(InteractionHand.MAIN_HAND, installed.copy());
            BlockHitResult hit = new BlockHitResult(f.machine.getCenter().add(0, 0, .5), Direction.SOUTH, f.machine, false);
            behaviour.onShortInteract(player, InteractionHand.MAIN_HAND, Direction.SOUTH, hit);
            helper.assertTrue(player.getMainHandItem().isEmpty() && ItemStack.isSameItemSameComponents(behaviour.getFilter(), canonical),
                "native interaction installs and consumes exactly one configured filter");
            var be = (SmartBlockEntity) helper.getLevel().getBlockEntity(f.machine);
            var saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
            behaviour.setFilter(ItemStack.EMPTY); be.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(ItemStack.isSameItemSameComponents(behaviour.getFilter(), canonical), "filter and blacklist persist on machine reload");
            behaviour.onShortInteract(player, InteractionHand.MAIN_HAND, Direction.SOUTH, hit);
            helper.assertTrue(behaviour.getFilter().isEmpty() && count(new net.neoforged.neoforge.items.wrapper.InvWrapper(player.getInventory()), AllItems.FILTER.get()) == 1,
                "empty-hand interaction returns the real filter once");
            behaviour.setFilter(installed.copy());
            AABB area = new AABB(f.machine).inflate(1);
            helper.getLevel().removeBlock(f.machine, false);
            int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                .filter(entity -> ItemStack.isSameItemSameComponents(entity.getItem(), canonical)).mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(drops == 1, "breaking the machine returns exactly one installed filter with its settings");
            cleanup(helper, f);
        }
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 300)
    public static void fluidOnlyMixingUsesTheFluidResultFilter(GameTestHelper helper) {
        Fixture f = fixture(helper, true, 270);
        filter(helper, f).setFilter(list(true, BoosterFluids.ESSENCE_BUCKET.get()));
        f.basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.BLAZE_POWDER));
        f.basin.getInputInventory().setStackInSlot(1, new ItemStack(Items.NETHER_WART));
        IFluidHandler fluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, f.basin.getBlockPos(), null);
        fluids.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(fluidAmount(fluids, Fluids.LAVA) == 1000 && fluidAmount(fluids, BoosterFluids.NETHER_ESSENCE.get()) == 0,
                "blacklisting an essence bucket blocks fluid-only essence production");
            filter(helper, f).setFilter(list(false, BoosterFluids.ESSENCE_BUCKET.get()));
        });
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(fluidAmount(fluids, Fluids.LAVA) == 0 && fluidAmount(fluids, BoosterFluids.NETHER_ESSENCE.get()) == 1000,
                "whitelisting the fluid result wakes the mixer and permits the native essence recipe");
            cleanup(helper, f); helper.succeed();
        });
    }

    private static void targetInputs(Fixture f, int amount) {
        f.basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI, amount));
        f.basin.getInputInventory().setStackInSlot(1, new ItemStack(Items.EGG, amount));
        f.basin.getInputInventory().setStackInSlot(2, new ItemStack(Items.SUGAR, amount));
    }

    private static int fluidAmount(IFluidHandler handler, net.minecraft.world.level.material.Fluid fluid) {
        int result = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) if (handler.getFluidInTank(tank).is(fluid)) result += handler.getFluidInTank(tank).getAmount();
        return result;
    }

    private record Fixture(BlockPos machine, BasinBlockEntity basin, List<BlockPos> blocks) {}

    private static Fixture fixture(GameTestHelper helper, boolean mixer, int height) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, height, 1));
        level.setBlock(pos.below(2), AllBlocks.BASIN.getDefaultState(), 3);
        Block machine = mixer ? BoosterBlocks.NETHERITE_MECHANICAL_MIXER.get() : BoosterBlocks.NETHERITE_MECHANICAL_PRESS.get();
        var state = machine.defaultBlockState();
        if (!mixer) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        level.setBlock(pos, state, 3);
        List<BlockPos> blocks;
        if (mixer) {
            level.setBlock(pos.west(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 3);
            level.setBlock(pos.west().above(), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 3);
            motor(helper, pos.west().above(2), Direction.DOWN);
            blocks = List.of(pos, pos.below(2), pos.west(), pos.west().above(), pos.west().above(2));
        } else {
            motor(helper, pos.west(), Direction.EAST);
            blocks = List.of(pos, pos.below(2), pos.west());
        }
        helper.runAfterDelay(4, () -> helper.assertTrue(Math.abs(((KineticBlockEntity) level.getBlockEntity(pos)).getSpeed()) >= 64,
            "machine must receive real motor power"));
        return new Fixture(pos, (BasinBlockEntity) level.getBlockEntity(pos.below(2)), blocks);
    }

    private static void motor(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.getLevel().setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, facing), 3);
        ((CreativeMotorBlockEntity) helper.getLevel().getBlockEntity(pos)).generatedSpeed.setValue(64);
    }

    private static FilteringBehaviour filter(GameTestHelper helper, Fixture f) {
        FilteringBehaviour result = ((SmartBlockEntity) helper.getLevel().getBlockEntity(f.machine)).getBehaviour(FilteringBehaviour.TYPE);
        helper.assertTrue(result != null, "netherite processing machine must expose a native recipe filter slot");
        return result;
    }

    private static ItemStack list(boolean blacklist, Item... items) {
        ItemStack result = new ItemStack(AllItems.FILTER.get());
        result.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(java.util.Arrays.stream(items).map(ItemStack::new).toList()));
        result.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, blacklist);
        return result;
    }

    private static int count(IItemHandler inventory, Item item) {
        int result = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) if (inventory.getStackInSlot(slot).is(item)) result += inventory.getStackInSlot(slot).getCount();
        return result;
    }

    private static void cleanup(GameTestHelper helper, Fixture f) {
        for (BlockPos pos : f.blocks) helper.getLevel().removeBlock(pos, false);
    }
}
