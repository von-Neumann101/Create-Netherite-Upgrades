package io.github.boosterproject.booster.content.logistics.filter;

import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute.ItemAttributeEntry;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.AddedByAttribute;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import io.github.boosterproject.booster.content.kinetics.fan.NetheriteEncasedFanBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteNestedFilterGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void nestedRulesPersistAndProcess(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack nativeFilter = list(new ItemStack(Items.RAW_IRON), new ItemStack(Items.RAW_GOLD));
        ItemStack original = nativeFilter.copy();
        ItemStack outer = new ItemStack(BoosterItems.NETHERITE_FILTER.get());
        var rules = NetheriteFilterData.inventory(outer);
        NetheriteFilterData.assignToGroup(rules, FanProcessingMode.BLASTING, nativeFilter);
        helper.assertTrue(ItemStack.isSameItemSameComponents(rules.getStackInSlot(9), original), "assigning an original filter preserves its configured item list");
        helper.assertTrue(ItemStack.matches(nativeFilter, original), "ghost assignment preserves the source filter");
        NetheriteFilterData.save(outer, rules);
        outer = ItemStack.parse(level.registryAccess(), outer.save(level.registryAccess())).orElseThrow();
        helper.assertTrue(ItemStack.isSameItemSameComponents(NetheriteFilterData.inventory(outer).getStackInSlot(9), original), "nested filter survives item save and reload");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState(), 3);
        var fan = (NetheriteEncasedFanBlockEntity) level.getBlockEntity(pos);
        fan.getFilterInventory().setStackInSlot(FanProcessingMode.BLASTING.ordinal(), nativeFilter.copy());
        fan.setProcessingMode(FanProcessingMode.WASHING);
        helper.assertTrue(fan.resolveProcessingMode(new ItemStack(Items.RAW_IRON)) == FanProcessingMode.BLASTING, "first contained item selects blasting");
        helper.assertTrue(fan.resolveProcessingMode(new ItemStack(Items.RAW_GOLD)) == FanProcessingMode.BLASTING, "second contained item selects blasting");
        helper.assertTrue(fan.resolveProcessingMode(new ItemStack(Items.SAND)) == FanProcessingMode.WASHING, "unmatched item retains fan fallback");
        helper.assertTrue(FilterItemStack.of(outer.copy()).test(level, new ItemStack(Items.RAW_GOLD)), "logistics wrapper also expands nested filter rules");
        helper.assertTrue(!FilterItemStack.of(outer.copy()).test(level, new ItemStack(Items.SAND)), "logistics wrapper rejects unmatched items");
        CompoundTag savedFan = fan.saveWithoutMetadata(level.registryAccess());
        var restoredFan = new NetheriteEncasedFanBlockEntity(pos, fan.getBlockState());
        restoredFan.setLevel(level);
        restoredFan.loadWithComponents(savedFan, level.registryAccess());
        helper.assertTrue(restoredFan.resolveProcessingMode(new ItemStack(Items.RAW_GOLD)) == FanProcessingMode.BLASTING, "installed fan rules survive BE serialization");
        for (var pair : List.of(List.of(Items.RAW_IRON, Items.IRON_INGOT), List.of(Items.RAW_GOLD, Items.GOLD_INGOT))) {
            ItemEntity input = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(pair.get(0)));
            var type = fan.resolveProcessingMode(input.getItem()).processingType();
            boolean processed = false;
            for (int step = 0; step < 2000 && !processed; step++) processed = FanProcessing.applyProcessing(input, type);
            helper.assertTrue(processed && input.getItem().is(pair.get(1)), "contained raw material is processed into its real recipe result");
        }
        ItemStack copied = ((NetheriteFilterItem) outer.getItem()).createCopy(outer, 2);
        helper.assertTrue(((NetheriteFilterItem) copied.getItem()).canCopyFromItem(copied)
            && ItemStack.isSameItemSameComponents(NetheriteFilterData.inventory(copied).getStackInSlot(9), original), "Create filter copying retains nested rules");
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void nativeOptionsAndGhostMenu(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "FilterQA"));
        ItemStack outer = new ItemStack(BoosterItems.NETHERITE_FILTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, outer);
        var menu = new NetheriteFilterMenu(1, player.getInventory(), outer);
        ItemStack iron = list(new ItemStack(Items.RAW_IRON));
        ItemStack gold = list(new ItemStack(Items.RAW_GOLD));
        player.getInventory().setItem(9, gold);
        menu.setCarried(iron);
        menu.clicked(36 + 9, 0, ClickType.PICKUP, player);
        menu.clicked(0, FanProcessingMode.HAUNTING.ordinal(), ClickType.SWAP, player);
        var rules = NetheriteFilterData.inventory(outer);
        helper.assertTrue(ItemStack.isSameItemSameComponents(rules.getStackInSlot(9), iron)
            && ItemStack.isSameItemSameComponents(rules.getStackInSlot(18), gold), "mouse and number keys keep differently configured original filters in separate groups");
        helper.assertTrue(ItemStack.matches(menu.getCarried(), iron) && ItemStack.matches(player.getInventory().getItem(9), gold), "editing ghost rules consumes no source filters");
        ItemStack copper = list(new ItemStack(Items.RAW_COPPER));
        player.getInventory().setItem(10, copper);
        menu.quickMoveStack(player, 1);
        helper.assertTrue(ItemStack.isSameItemSameComponents(NetheriteFilterData.inventory(outer).getStackInSlot(0), copper), "shift-click accepts another original filter with different contents");
        rules = NetheriteFilterData.inventory(outer);
        NetheriteFilterData.assignToGroup(rules, FanProcessingMode.SMOKING, gold);
        helper.assertTrue(rules.getStackInSlot(18).isEmpty() && ItemStack.isSameItemSameComponents(rules.getStackInSlot(27), gold), "reassigning an identical configured filter moves only that rule");
        helper.assertTrue(ItemStack.isSameItemSameComponents(rules.getStackInSlot(9), iron), "moving one original filter does not remove a differently configured filter");

        ItemStack deny = iron.copy();
        deny.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, true);
        ItemStack strict = new ItemStack(Items.IRON_INGOT);
        strict.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("named"));
        ItemStack strictFilter = list(strict);
        strictFilter.set(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, true);
        ItemStack nestedNative = list(iron, gold);
        ItemStack attribute = new ItemStack(AllItems.ATTRIBUTE_FILTER.get());
        attribute.set(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES, List.of(new ItemAttributeEntry(new AddedByAttribute("minecraft"), false)));
        for (ItemStack sample : List.of(deny, strictFilter, nestedNative, attribute)) {
            ItemStack configured = new ItemStack(BoosterItems.NETHERITE_FILTER.get());
            var oneRule = NetheriteFilterData.inventory(configured);
            NetheriteFilterData.assignToGroup(oneRule, FanProcessingMode.BLASTING, sample);
            NetheriteFilterData.save(configured, oneRule);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState(), 3);
            var fan = (NetheriteEncasedFanBlockEntity) level.getBlockEntity(pos);
            fan.getFilterInventory().setStackInSlot(FanProcessingMode.BLASTING.ordinal(), sample.copy());
            fan.setProcessingMode(FanProcessingMode.WASHING);
            for (ItemStack target : List.of(new ItemStack(Items.RAW_IRON), new ItemStack(Items.RAW_GOLD), strict, new ItemStack(Items.IRON_INGOT), new ItemStack(AllItems.FILTER.get()))) {
                boolean expected = FilterItemStack.of(sample.copy()).test(level, target);
                helper.assertTrue((fan.resolveProcessingMode(target) == FanProcessingMode.BLASTING) == expected, "fan follows native blacklist, component, recursive-list and attribute semantics");
            }
            level.removeBlock(pos, false);
        }
        CompoundTag metadata = new CompoundTag(); metadata.putString("Unrelated", "keep");
        outer.set(DataComponents.CUSTOM_DATA, CustomData.of(metadata));
        NetheriteFilterData.save(outer, rules);
        NetheriteFilterData.save(outer, new net.neoforged.neoforge.items.ItemStackHandler(36));
        helper.assertTrue(!outer.has(DataComponents.CONTAINER) && outer.get(DataComponents.CUSTOM_DATA).copyTag().getString("Unrelated").equals("keep"), "clear removes nested rules and preserves unrelated data");
        helper.succeed();
    }

    private static ItemStack list(ItemStack... samples) {
        ItemStack filter = new ItemStack(AllItems.FILTER.get());
        filter.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(List.of(samples)));
        return filter;
    }
}
