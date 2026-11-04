package io.github.boosterproject.booster.content.logistics.chute;

import java.util.List;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.chute.SmartChuteBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import io.github.boosterproject.booster.Booster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteSmartChuteGameTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_smart_chute");

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void opposingStreamsWithoutFans(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ID), "netherite smart chute must be registered");
        var level = helper.getLevel();
        BlockPos bottom = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos top = bottom.above(3);
        for (int y = 0; y <= 3; y++) level.setBlock(bottom.above(y), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(bottom, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(top, Blocks.CHEST.defaultBlockState(), 3);
        for (int y = 1; y <= 2; y++) {
            BlockPos pos = bottom.above(y);
            level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState().setValue(SmartChuteBlock.POWERED, false), 3);
            var chute = (SmartChuteBlockEntity) level.getBlockEntity(pos);
            configure(chute, list(new ItemStack(Items.IRON_INGOT)), new ItemStack(Items.GOLD_INGOT));
            chute.getBehaviour(FilteringBehaviour.TYPE).setFilter(list(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT)));
        }
        IItemHandler lower = level.getCapability(Capabilities.ItemHandler.BLOCK, bottom, Direction.UP);
        IItemHandler upper = level.getCapability(Capabilities.ItemHandler.BLOCK, top, Direction.DOWN);
        lower.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false);
        upper.insertItem(0, new ItemStack(Items.GOLD_INGOT, 5), false);
        upper.insertItem(1, new ItemStack(Items.STONE, 7), false);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(count(upper, Items.IRON_INGOT) == 8 && count(lower, Items.IRON_INGOT) == 0, "all iron travels up without any fan");
            helper.assertTrue(count(lower, Items.GOLD_INGOT) == 5 && count(upper, Items.GOLD_INGOT) == 0, "all gold travels down across the opposing stream");
            helper.assertTrue(count(upper, Items.STONE) == 7 && count(lower, Items.STONE) == 0, "native admission filter rejects unmatched stone");
            for (int y = 0; y <= 3; y++) level.removeBlock(bottom.above(y), false);
            helper.succeed();
        });
    }

    private static void configure(SmartChuteBlockEntity chute, ItemStack up, ItemStack down) {
        var rules = new ItemStackHandler(2);
        rules.setStackInSlot(0, up.copyWithCount(1));
        rules.setStackInSlot(1, down.copyWithCount(1));
        var tag = chute.saveWithoutMetadata(chute.getLevel().registryAccess());
        tag.put("DirectionFilters", rules.serializeNBT(chute.getLevel().registryAccess()));
        tag.putString("DefaultDirection", "down");
        chute.loadWithComponents(tag, chute.getLevel().registryAccess());
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void redstonePersistenceAndRuleMenu(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState().setValue(SmartChuteBlock.POWERED, false), 3);
        var chute = (NetheriteSmartChuteBlockEntity) level.getBlockEntity(pos);
        configure(chute, list(new ItemStack(Items.IRON_INGOT)), new ItemStack(Items.GOLD_INGOT));
        var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.NORTH);
        helper.assertTrue(cap.insertItem(0, new ItemStack(Items.IRON_INGOT, 6), true).isEmpty() && cap.getStackInSlot(0).isEmpty(), "simulation leaves channel empty");
        cap.insertItem(0, new ItemStack(Items.IRON_INGOT, 6), false);
        cap.insertItem(1, new ItemStack(Items.GOLD_INGOT, 5), false);
        level.setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(SmartChuteBlock.POWERED), "real adjacent redstone pauses chute");
            level.setBlock(pos.above(), Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(pos.below(), Blocks.CHEST.defaultBlockState(), 3);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(cap.getStackInSlot(0).getCount() == 6 && cap.getStackInSlot(1).getCount() == 5, "powered chute retains both channels even with free outputs");
            var saved = chute.saveWithoutMetadata(level.registryAccess());
            var restored = new NetheriteSmartChuteBlockEntity(pos, chute.getBlockState());
            restored.setLevel(level);
            restored.loadWithComponents(saved, level.registryAccess());
            helper.assertTrue(restored.getTransportInventory().getStackInSlot(0).getCount() == 6
                && restored.getTransportInventory().getStackInSlot(1).getCount() == 5
                && restored.route(new ItemStack(Items.IRON_INGOT)) == Direction.UP, "save and reload preserve both real buffers and native filter rules");
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ChuteQA"));
            player.setPos(pos.getCenter());
            var menu = new NetheriteSmartChuteMenu(17, player.getInventory(), chute);
            ItemStack source = list(new ItemStack(Items.COPPER_INGOT));
            player.getInventory().setItem(9, source);
            menu.clicked(0, 0, ClickType.SWAP, player);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(9), source)
                && ItemStack.isSameItemSameComponents(chute.getDirectionFilters().getStackInSlot(0), source), "number key copies original filter components without consumption");
            menu.setCarried(new ItemStack(Items.GOLD_INGOT, 3));
            menu.clicked(37, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().getCount() == 3 && chute.getDirectionFilters().getStackInSlot(1).getCount() == 1, "mouse copies one ghost sample without consuming carried items");
            menu.clicked(37, 1, ClickType.PICKUP, player);
            helper.assertTrue(chute.getDirectionFilters().getStackInSlot(1).isEmpty(), "right click clears only the ghost sample");
            helper.assertTrue(menu.clickMenuButton(player, 0) && chute.route(new ItemStack(Items.DIAMOND)) == Direction.UP, "unmatched direction is selectable");
            player.setPos(pos.getCenter().add(20, 0, 0));
            helper.assertTrue(!menu.clickMenuButton(player, 1), "remote menu edits are rejected");
            menu.clicked(36, 1, ClickType.PICKUP, player);
            helper.assertTrue(!chute.getDirectionFilters().getStackInSlot(0).isEmpty(), "out-of-range sample changes are rejected");
            menu.setCarried(ItemStack.EMPTY);
            // Restore routes before resuming; in-flight stacks keep their original channels.
            configure(chute, list(new ItemStack(Items.IRON_INGOT)), new ItemStack(Items.GOLD_INGOT));
            level.removeBlock(pos.east(), false);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(count(level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN), Items.IRON_INGOT) == 6
                && count(level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP), Items.GOLD_INGOT) == 5, "removing redstone resumes both original buffered directions");
            level.removeBlock(pos, false);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(.8)).stream()
                .noneMatch(e -> e.getItem().is(AllItems.FILTER.get())), "direction-filter samples never drop as real filters");
            level.removeBlock(pos.above(), false);
            level.removeBlock(pos.below(), false);
            helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void nativeConnectionPartialOutputAndBreak(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos.below(), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState().setValue(SmartChuteBlock.POWERED, false), 3);
        level.setBlock(pos.above(), AllBlocks.SMART_CHUTE.get().defaultBlockState().setValue(SmartChuteBlock.POWERED, false), 3);
        var chute = (NetheriteSmartChuteBlockEntity) level.getBlockEntity(pos);
        configure(chute, list(new ItemStack(Items.IRON_INGOT)), new ItemStack(Items.GOLD_INGOT));
        var nativeChute = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.UP);
        nativeChute.insertItem(0, new ItemStack(Items.GOLD_INGOT, 8), false);
        var output = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(count(output, Items.GOLD_INGOT) == 8, "native smart chute still ticks and feeds netherite chute");
            for (int slot = 0; slot < output.getSlots(); slot++) {
                output.extractItem(slot, 64, false);
                output.insertItem(slot, slot == 0 ? new ItemStack(Items.GOLD_INGOT, 61) : new ItemStack(Items.STONE, 64), false);
            }
            chute.getTransportInventory().insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false);
            chute.getTransportInventory().insertItem(1, new ItemStack(Items.GOLD_INGOT, 5), false);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(count(output, Items.GOLD_INGOT) == 64 && chute.getTransportInventory().getStackInSlot(1).getCount() == 2,
                "partial output retains exactly the uninserted remainder");
            helper.assertTrue(chute.getTransportInventory().getStackInSlot(0).getCount() == 2, "upward channel waits at a downward native chute");
            level.removeBlock(pos, false);
            var dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(.9));
            helper.assertTrue(dropped.stream().filter(e -> e.getItem().is(Items.IRON_INGOT)).mapToInt(e -> e.getItem().getCount()).sum() == 2
                && dropped.stream().filter(e -> e.getItem().is(Items.GOLD_INGOT)).mapToInt(e -> e.getItem().getCount()).sum() == 2,
                "breaking drops both real remainders once");
            helper.assertTrue(dropped.stream().noneMatch(e -> e.getItem().is(AllItems.FILTER.get())), "breaking never materializes ghost filters");
            level.removeBlock(pos.above(), false);
            level.removeBlock(pos.below(), false);
            helper.succeed();
        });
    }

    private static ItemStack list(ItemStack... samples) {
        ItemStack filter = new ItemStack(AllItems.FILTER.get());
        filter.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(List.of(samples)));
        return filter;
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void nativeBlacklistAndExtractionCount(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos.above(), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(pos.below(), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState().setValue(SmartChuteBlock.POWERED, false), 3);
        var chute = (NetheriteSmartChuteBlockEntity) level.getBlockEntity(pos);
        ItemStack denyIron = list(new ItemStack(Items.IRON_INGOT));
        denyIron.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, true);
        var filter = chute.getBehaviour(FilteringBehaviour.TYPE);
        filter.setFilter(denyIron);
        filter.count = 5;
        filter.upTo = false;
        var input = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
        var output = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        input.insertItem(0, new ItemStack(Items.IRON_INGOT, 5), false);
        input.insertItem(1, new ItemStack(Items.GOLD_INGOT, 11), false);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(count(input, Items.IRON_INGOT) == 5, "native blacklist leaves excluded iron untouched");
            helper.assertTrue(count(input, Items.GOLD_INGOT) == 1 && count(output, Items.GOLD_INGOT) == 10, "exact extraction moves complete groups of five and leaves short remainder");
            var restored = new NetheriteSmartChuteBlockEntity(pos, chute.getBlockState());
            restored.setLevel(level);
            restored.loadWithComponents(chute.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
            var restoredFilter = restored.getBehaviour(FilteringBehaviour.TYPE);
            helper.assertTrue(restoredFilter.count == 5 && !restoredFilter.upTo && !restoredFilter.test(new ItemStack(Items.IRON_INGOT)), "native count mode and blacklist survive serialization");
            filter.upTo = true;
            input.insertItem(1, new ItemStack(Items.GOLD_INGOT, 2), false);
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(count(input, Items.GOLD_INGOT) == 0 && count(output, Items.GOLD_INGOT) == 13, "up-to mode also moves a partial group");
            level.removeBlock(pos, false);
            level.removeBlock(pos.above(), false);
            level.removeBlock(pos.below(), false);
            helper.succeed();
        });
    }

    private static int count(IItemHandler inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void nativeSourcesKeepTheirDirectionAndPause(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos.below(), AllBlocks.SMART_CHUTE.get().defaultBlockState().setValue(SmartChuteBlock.POWERED, false), 3);
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ID).defaultBlockState().setValue(SmartChuteBlock.POWERED, false), 3);
        level.setBlock(pos.above(), Blocks.CHEST.defaultBlockState(), 3);
        var chute = (NetheriteSmartChuteBlockEntity) level.getBlockEntity(pos);
        chute.setDefaultDirection(Direction.UP);
        var nativeCap = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        nativeCap.insertItem(0, new ItemStack(Items.IRON_INGOT, 4), false);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(count(nativeCap, Items.IRON_INGOT) == 4 && chute.getItem().isEmpty(), "upward netherite chute must not steal downward native traffic");
            level.setBlock(pos.below().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(level.getBlockState(pos.below()).getValue(SmartChuteBlock.POWERED), "native source receives real redstone pause");
            helper.assertTrue(count(nativeCap, Items.IRON_INGOT) == 4 && chute.getItem().isEmpty(), "netherite chute must not extract paused native buffers");
            level.removeBlock(pos, false);
            level.removeBlock(pos.above(), false);
            level.removeBlock(pos.below(), false);
            level.removeBlock(pos.below(2), false);
            level.removeBlock(pos.below().east(), false);
            helper.succeed();
        });
    }
}
