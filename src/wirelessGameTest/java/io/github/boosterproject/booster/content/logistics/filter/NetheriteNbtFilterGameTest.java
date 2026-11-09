package io.github.boosterproject.booster.content.logistics.filter;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.content.kinetics.fan.NetheriteEncasedFanBlockEntity;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import io.netty.buffer.Unpooled;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteNbtFilterGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void selectedNumericNbtFieldIsFiltered(GameTestHelper helper) {
        ItemStack reference = sample(100), filter = new ItemStack(BoosterItems.NETHERITE_FILTER.get());
        CompoundTag config = new CompoundTag();
        config.put("Reference", reference.save(helper.getLevel().registryAccess())); config.putBoolean("SameItem", true);
        CompoundTag condition = new CompoundTag(); ListTag path = new ListTag();
        path.add(StringTag.valueOf("minecraft:custom_data")); path.add(StringTag.valueOf("Energy"));
        condition.put("Path", path); condition.putInt("Op", 1); condition.putString("First", "50"); condition.putString("Second", "");
        ListTag conditions = new ListTag(); conditions.add(condition); config.put("Conditions", conditions);
        CompoundTag data = new CompoundTag(); data.put("NbtFilter", config); filter.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        var wrapper = FilterItemStack.of(filter);
        helper.assertTrue(wrapper.test(helper.getLevel(), sample(100)), "general Create filter wrapper must match an allowed selected NBT field");
        helper.assertTrue(!wrapper.test(helper.getLevel(), sample(50)) && !wrapper.test(helper.getLevel(), sample(10)), "greater-than condition excludes its boundary and lower values");
        helper.succeed();
    }

    private static ItemStack sample(int energy) {
        ItemStack result = new ItemStack(Items.IRON_INGOT); CompoundTag data = new CompoundTag(); data.putInt("Energy", energy);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return result;
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void editableConditionsHandleBoundariesPrecisionAndModes(GameTestHelper helper) {
        ItemStack filter = configured(sample(100)); var field = field(helper, filter, "minecraft:custom_data", "Energy");
        helper.assertTrue(NetheriteNbtFilterData.setCondition(filter, field, 3, "50", "100"), "editable inclusive range accepted");
        for (int value : new int[] {49, 50, 100, 101}) helper.assertTrue(test(helper, filter, sample(value)) == (value == 50 || value == 100), "range endpoints are inclusive");
        helper.assertTrue(!NetheriteNbtFilterData.setCondition(filter, field, 3, "100", "50")
            && !NetheriteNbtFilterData.setCondition(filter, field, 0, "oops", "") && test(helper, filter, sample(50)), "invalid values never replace a working rule");
        NetheriteNbtFilterData.setCondition(filter, field, 2, "50", "");
        helper.assertTrue(test(helper, filter, sample(49)) && !test(helper, filter, sample(50)), "less-than is strict");
        ItemStack huge = sample(0); CompoundTag values = new CompoundTag(); values.putLong("Energy", 9007199254740993L); huge.set(DataComponents.CUSTOM_DATA, CustomData.of(values));
        NetheriteNbtFilterData.setCondition(filter, field, 1, "9007199254740992", "");
        helper.assertTrue(test(helper, filter, huge), "long comparisons preserve precision beyond double's exact integer range");
        values.putLong("Energy", 9007199254740992L); huge.set(DataComponents.CUSTOM_DATA, CustomData.of(values));
        helper.assertTrue(!test(helper, filter, huge), "large integer boundary is not rounded up");
        NetheriteNbtFilterData.setCondition(filter, field, 0, "100", "");
        ItemStack unrelated = sample(100); values = unrelated.get(DataComponents.CUSTOM_DATA).copyTag(); values.putString("Other", "ignored"); unrelated.set(DataComponents.CUSTOM_DATA, CustomData.of(values));
        helper.assertTrue(test(helper, filter, unrelated) && !test(helper, filter, new ItemStack(Items.IRON_INGOT)), "only selected fields must match, missing fields fail");
        values = new CompoundTag(); values.putString("Energy", "100"); huge.set(DataComponents.CUSTOM_DATA, CustomData.of(values));
        helper.assertTrue(!test(helper, filter, huge), "a numeric-looking string is not a numeric NBT value");
        ItemStack otherKind = new ItemStack(Items.GOLD_INGOT); otherKind.set(DataComponents.CUSTOM_DATA, sample(100).get(DataComponents.CUSTOM_DATA));
        helper.assertTrue(!test(helper, filter, otherKind), "same-item matching is enabled initially");
        NetheriteNbtFilterData.toggleSameItem(filter);
        helper.assertTrue(test(helper, filter, otherKind), "item ID can be ignored while keeping selected fields");
        NetheriteNbtFilterData.toggleBlacklist(filter);
        helper.assertTrue(!test(helper, filter, otherKind) && test(helper, filter, sample(99)) && !test(helper, filter, ItemStack.EMPTY), "blacklist inverts the whole rule without matching empty stacks");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void referenceDefaultsListsAndCopyingPersist(GameTestHelper helper) {
        ItemStack reference = new ItemStack(Items.DIAMOND_PICKAXE); CompoundTag data = new CompoundTag(), child = new CompoundTag();
        child.putString("text", "old"); data.put("key.with/slashes[0]", child); data.put("numbers", new LongArrayTag(new long[] {3, 7}));
        reference.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        ItemStack filter = configured(reference);
        var damage = field(helper, filter, "minecraft:damage");
        helper.assertTrue(damage.numeric() && damage.text().equals("0"), "effective components include undamaged default zero");
        NetheriteNbtFilterData.setCondition(filter, damage, 0, "0", "");
        helper.assertTrue(test(helper, filter, reference), "default component equality matches");
        ItemStack damaged = reference.copy(); damaged.setDamageValue(1);
        helper.assertTrue(!test(helper, filter, damaged), "modified default component does not match");
        NetheriteNbtFilterData.clearConditions(filter);
        var text = field(helper, filter, "minecraft:custom_data", "key.with/slashes[0]", "text");
        NetheriteNbtFilterData.setCondition(filter, text, 0, "edited", "");
        ItemStack edited = reference.copy(); data = edited.get(DataComponents.CUSTOM_DATA).copyTag(); data.getCompound("key.with/slashes[0]").putString("text", "edited");
        edited.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        helper.assertTrue(!test(helper, filter, reference) && test(helper, filter, edited), "arbitrary compound keys are path segments, not dotted-path syntax");
        var number = field(helper, filter, "minecraft:custom_data", "numbers", "1");
        NetheriteNbtFilterData.setCondition(filter, number, 0, "7", "");
        ItemStack restored = ItemStack.parse(helper.getLevel().registryAccess(), filter.save(helper.getLevel().registryAccess())).orElseThrow();
        ItemStack copy = ((NetheriteFilterItem) filter.getItem()).createCopy(restored, 2);
        helper.assertTrue(test(helper, restored, edited) && test(helper, copy, edited)
            && ItemStack.isSameItemSameComponents(NetheriteNbtFilterData.reference(copy, helper.getLevel().registryAccess()), reference), "reference data and field rules survive item persistence and native filter copying");
        helper.assertTrue(((NetheriteFilterItem) copy.getItem()).getFilterItems(copy)[0].has(DataComponents.CUSTOM_DATA), "native filter previews retain the reference's full components");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void originalCreateSlotsInstallAndApplyNbtFilter(GameTestHelper helper) {
        var level = helper.getLevel(); ItemStack filter = configured(sample(100));
        NetheriteNbtFilterData.setCondition(filter, field(helper, filter, "minecraft:custom_data", "Energy"), 0, "100", "");
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "NativeNbtFilterQA")); player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 270, 1));
        for (var block : List.of(AllBlocks.BRASS_FUNNEL.get(), AllBlocks.SMART_CHUTE.get(), AllBlocks.BASIN.get(), AllBlocks.MECHANICAL_SAW.get(), AllBlocks.DEPLOYER.get())) {
            level.setBlock(pos, block.defaultBlockState(), 3);
            var behaviour = ((SmartBlockEntity) level.getBlockEntity(pos)).<FilteringBehaviour>getBehaviour(FilteringBehaviour.TYPE);
            helper.assertTrue(behaviour != null, "original machine has its real filtering behaviour");
            player.setItemInHand(InteractionHand.MAIN_HAND, filter.copy());
            behaviour.onShortInteract(player, InteractionHand.MAIN_HAND, Direction.SOUTH, new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false));
            helper.assertTrue(player.getMainHandItem().isEmpty() && behaviour.test(sample(100)) && !behaviour.test(sample(99)), "original slot installs a real Netherite Filter and applies its NBT rule");
            behaviour.onShortInteract(player, InteractionHand.MAIN_HAND, Direction.SOUTH, new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false));
            helper.assertTrue(behaviour.getFilter().isEmpty(), "native empty-hand removal clears the real filter slot");
            level.removeBlock(pos, false);
        }
        ItemStack outer = new ItemStack(AllItems.FILTER.get()); outer.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(List.of(filter)));
        helper.assertTrue(test(helper, outer, sample(100)) && !test(helper, outer, sample(99)), "original list filters can nest the advanced NBT filter");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void menuUsesServerReferenceAndRejectsStaleEdits(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "NbtEditorQA"));
        ItemStack filter = configured(sample(100)); player.setItemInHand(InteractionHand.MAIN_HAND, filter);
        var menu = new NetheriteNbtFilterMenu(3, player.getInventory(), filter); player.containerMenu = menu;
        ItemStack source = sample(350); source.setCount(64); player.getInventory().setItem(9, source);
        menu.clicked(0, 0, ClickType.SWAP, player);
        helper.assertTrue(source.getCount() == 64 && menu.ghostInventory.getStackInSlot(0).getCount() == 1
            && menu.ghostInventory.getStackInSlot(0).get(DataComponents.CUSTOM_DATA).copyTag().getInt("Energy") == 350, "reference is copied from the real server inventory without consuming or stripping NBT");
        int index = menu.fields().indexOf(field(helper, filter, "minecraft:custom_data", "Energy"));
        var action = new NetheriteNbtFilterNetworking.Action(3, menu.referenceHash(), 1, index, 3, "300", "400");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try { NetheriteNbtFilterNetworking.Action.CODEC.encode(buffer, action); helper.assertTrue(action.equals(NetheriteNbtFilterNetworking.Action.CODEC.decode(buffer)), "field action round-trips through real payload codec"); }
        finally { buffer.release(); }
        NetheriteNbtFilterNetworking.handle(player, action);
        helper.assertTrue(test(helper, filter, sample(350)) && !test(helper, filter, sample(299)), "validated server action edits the actual filter");
        ItemStack saved = filter.copy();
        for (var invalid : List.of(new NetheriteNbtFilterNetworking.Action(4, menu.referenceHash(), 4, -1, 0, "", ""),
            new NetheriteNbtFilterNetworking.Action(3, menu.referenceHash() + 1, 4, -1, 0, "", ""),
            new NetheriteNbtFilterNetworking.Action(3, menu.referenceHash(), 1, -1, 0, "1", ""),
            new NetheriteNbtFilterNetworking.Action(3, menu.referenceHash(), 1, index, 3, "bad", "400"))) NetheriteNbtFilterNetworking.handle(player, invalid);
        helper.assertTrue(ItemStack.matches(saved, filter), "stale menu/reference, invalid field and invalid numeric edits cannot change the filter");
        int oldHash = menu.referenceHash(); ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        gold.set(DataComponents.CUSTOM_DATA, source.get(DataComponents.CUSTOM_DATA)); player.getInventory().setItem(10, gold);
        menu.clicked(1, 0, ClickType.SWAP, player);
        NetheriteNbtFilterNetworking.handle(player, action);
        helper.assertTrue(menu.referenceHash() != oldHash && NetheriteNbtFilterData.conditions(filter).isEmpty(),
            "replacing the reference with another item having identical components rejects the old edit");
        menu.clicked(27 + player.getInventory().selected, 0, ClickType.THROW, player);
        helper.assertTrue(player.getMainHandItem() == filter, "held filter cannot be thrown out of its own editor");
        ItemStack replacement = filter.copy(); saved = replacement.copy(); player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
        NetheriteNbtFilterNetworking.handle(player, new NetheriteNbtFilterNetworking.Action(3, menu.referenceHash(), 4, -1, 0, "", ""));
        helper.assertTrue(!menu.stillValid(player) && ItemStack.matches(saved, replacement), "replacing the held source revokes the old editor session");
        player.containerMenu = player.inventoryMenu; helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void fanAndBucketFiltersUseGeneralNbtRules(GameTestHelper helper) {
        var level = helper.getLevel(); ItemStack reference = new ItemStack(Items.RAW_IRON);
        reference.set(DataComponents.CUSTOM_DATA, sample(100).get(DataComponents.CUSTOM_DATA)); ItemStack filter = configured(reference);
        NetheriteNbtFilterData.setCondition(filter, field(helper, filter, "minecraft:custom_data", "Energy"), 0, "100", "");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 270, 1)); level.setBlock(pos, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState(), 3);
        var fan = (NetheriteEncasedFanBlockEntity) level.getBlockEntity(pos); fan.setProcessingMode(FanProcessingMode.WASHING);
        fan.getFilterInventory().setStackInSlot(FanProcessingMode.BLASTING.ordinal(), filter);
        ItemStack different = reference.copy(); different.set(DataComponents.CUSTOM_DATA, sample(99).get(DataComponents.CUSTOM_DATA));
        helper.assertTrue(fan.resolveProcessingMode(reference) == FanProcessingMode.BLASTING && fan.resolveProcessingMode(different) == FanProcessingMode.WASHING, "fan slots accept the general filter and keep their own fixed processing modes");
        level.removeBlock(pos, false);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET); bucket.set(DataComponents.CUSTOM_DATA, sample(100).get(DataComponents.CUSTOM_DATA)); filter = configured(bucket);
        NetheriteNbtFilterData.setCondition(filter, field(helper, filter, "minecraft:custom_data", "Energy"), 0, "100", "");
        FluidStack water = new FluidStack(Fluids.WATER, 1000); water.set(DataComponents.CUSTOM_DATA, sample(100).get(DataComponents.CUSTOM_DATA));
        helper.assertTrue(FilterItemStack.of(filter).test(level, water) && !FilterItemStack.of(filter).test(level, new FluidStack(Fluids.WATER, 1000)), "bucket-backed fluid filtering checks the fluid's components rather than inventing reference NBT");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void restoredCraftingProducesUnconfiguredSafeFilter(GameTestHelper helper) {
        var input = CraftingInput.of(2, 1, List.of(new ItemStack(AllItems.FILTER.get()), new ItemStack(BoosterItems.NETHERITE_SHEET.get())));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        ItemStack output = recipe.value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(output.is(BoosterItems.NETHERITE_FILTER.get()) && NetheriteNbtFilterData.configured(output)
            && !test(helper, output, sample(100)) && !test(helper, output, output), "filter plus sheet crafts a general filter which rejects all items until configured");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void nativeCopyRecipeAcceptsFreshBlankTarget(GameTestHelper helper) {
        ItemStack source = configured(sample(100));
        NetheriteNbtFilterData.setCondition(source, field(helper, source, "minecraft:custom_data", "Energy"), 1, "50", "");
        var blankInput = CraftingInput.of(2, 1, List.of(new ItemStack(AllItems.FILTER.get()), new ItemStack(BoosterItems.NETHERITE_SHEET.get())));
        var recipes = helper.getLevel().getRecipeManager();
        ItemStack blank = recipes.getRecipeFor(RecipeType.CRAFTING, blankInput, helper.getLevel()).orElseThrow().value().assemble(blankInput, helper.getLevel().registryAccess());
        var input = CraftingInput.of(2, 1, List.of(source, blank));
        var copying = recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(copying.isPresent(), "native filter-copying recipe must accept a freshly crafted blank Netherite Filter");
        ItemStack result = copying.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.getCount() == 2 && test(helper, result, sample(100)) && !test(helper, result, sample(50)),
            "real native copying produces two filters with the full reference and conditions");
        var item = (NetheriteFilterItem) blank.getItem();
        helper.assertTrue(item.canCopyToItem(configured(ItemStack.EMPTY)) && !item.canCopyFromItem(configured(ItemStack.EMPTY)),
            "creative blank filters are copy targets rather than empty sources");
        helper.succeed();
    }

    private static ItemStack configured(ItemStack reference) {
        ItemStack filter = new ItemStack(BoosterItems.NETHERITE_FILTER.get()); NetheriteNbtFilterData.setReference(filter, reference); return filter;
    }
    private static NetheriteNbtFilterData.Field field(GameTestHelper helper, ItemStack filter, String... path) {
        return NetheriteNbtFilterData.fields(NetheriteNbtFilterData.reference(filter, helper.getLevel().registryAccess()), helper.getLevel().registryAccess())
            .stream().filter(field -> field.path().equals(List.of(path))).findFirst().orElseThrow();
    }
    private static boolean test(GameTestHelper helper, ItemStack filter, ItemStack target) { return FilterItemStack.of(filter.copy()).test(helper.getLevel(), target); }
}
