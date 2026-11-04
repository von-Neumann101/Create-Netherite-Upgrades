package io.github.boosterproject.booster.content.kinetics.fan;

import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteFilterData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetheriteFanSlotsGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void fourNativeSlotsAndSelectedFallback(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState(), 3);
        var fan = (NetheriteEncasedFanBlockEntity) level.getBlockEntity(pos);
        var filters = fan.getFilterInventory();
        helper.assertTrue(filters.getSlots() == 4, "fan must expose four original-filter slots");
        var targets = List.of(Items.RED_WOOL, Items.RAW_IRON, Items.SAND, Items.BEEF);
        for (var mode : FanProcessingMode.values()) {
            int slot = mode.ordinal();
            helper.assertTrue(filters.isItemValid(slot, list(new ItemStack(targets.get(slot)))), "slot accepts original list filter");
            helper.assertTrue(filters.isItemValid(slot, new ItemStack(AllItems.ATTRIBUTE_FILTER.get())), "slot accepts original attribute filter");
            helper.assertTrue(!filters.isItemValid(slot, new ItemStack(Items.RAW_IRON))
                && !filters.isItemValid(slot, new ItemStack(BoosterItems.NETHERITE_FILTER.get())), "slot rejects ordinary items and dedicated netherite filter");
            filters.setStackInSlot(slot, list(new ItemStack(targets.get(slot))));
        }
        fan.setProcessingMode(FanProcessingMode.HAUNTING);
        for (int slot = 0; slot < 4; slot++) helper.assertTrue(fan.resolveProcessingMode(new ItemStack(targets.get(slot))) == FanProcessingMode.byIndex(slot), "each slot selects its fixed processing mode");
        helper.assertTrue(fan.resolveProcessingMode(new ItemStack(Items.DIRT)) == FanProcessingMode.HAUNTING, "unmatched item uses selected mode with all four filters installed");
        filters.setStackInSlot(0, list(new ItemStack(Items.RAW_IRON)));
        helper.assertTrue(fan.resolveProcessingMode(new ItemStack(Items.RAW_IRON)) == FanProcessingMode.WASHING, "first matching mode retains the established priority");
        var restored = new NetheriteEncasedFanBlockEntity(pos, fan.getBlockState());
        restored.setLevel(level);
        restored.loadWithComponents(fan.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
        for (int slot = 0; slot < 4; slot++) helper.assertTrue(ItemStack.matches(filters.getStackInSlot(slot), restored.getFilterInventory().getStackInSlot(slot)), "all original filter components survive fan serialization");
        helper.assertTrue(restored.getProcessingMode() == FanProcessingMode.HAUNTING, "selected mode survives serialization");
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void physicalFilterSlotsAndRemoval(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState(), 3);
        var fan = (NetheriteEncasedFanBlockEntity) level.getBlockEntity(pos);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "FanSlotsQA"));
        player.setPos(pos.getCenter());
        var menu = new NetheriteFanMenu(23, player.getInventory(), fan);
        helper.assertTrue(menu.slots.size() == 40, "menu contains four real filters plus player inventory");
        ItemStack source = list(new ItemStack(Items.RAW_IRON));
        menu.setCarried(source.copyWithCount(2));
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(fan.getFilterInventory().getStackInSlot(1).getCount() == 1 && menu.getCarried().getCount() == 1,
            "installing consumes one real filter and leaves the other on the cursor");
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(fan.getFilterInventory().getStackInSlot(1).getCount() == 1 && menu.getCarried().getCount() == 1,
            "filter slot never grows beyond one item");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(9, list(new ItemStack(Items.RED_WOOL)));
        menu.quickMoveStack(player, 4);
        helper.assertTrue(fan.getFilterInventory().getStackInSlot(0).getCount() == 1 && player.getInventory().getItem(9).isEmpty(), "shift installs a real filter into first empty processing slot");
        fan.getFilterInventory().setStackInSlot(2, list(new ItemStack(Items.SAND)));
        fan.getFilterInventory().setStackInSlot(3, list(new ItemStack(Items.BEEF)));
        player.getInventory().setItem(9, source.copy());
        menu.quickMoveStack(player, 4);
        helper.assertTrue(ItemStack.matches(player.getInventory().getItem(9), source), "full fan leaves shift-clicked source unchanged");
        helper.assertTrue(menu.clickMenuButton(player, FanProcessingMode.SMOKING.ordinal()) && fan.getProcessingMode() == FanProcessingMode.SMOKING,
            "selected fallback remains editable with four filters");
        player.setPos(pos.getCenter().add(20, 0, 0));
        helper.assertTrue(!menu.clickMenuButton(player, 0), "out of range mode request rejected");
        level.removeBlock(pos, false);
        int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(.8)).stream()
            .filter(e -> e.getItem().is(AllItems.FILTER.get())).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(count == 4, "breaking returns exactly the four physically installed filters");
        helper.succeed();
    }

    private static ItemStack list(ItemStack... samples) {
        ItemStack filter = new ItemStack(AllItems.FILTER.get());
        filter.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(List.of(samples)));
        return filter;
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 100)
    public static void legacyInstalledFilterIsReturnedOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack legacy = new ItemStack(BoosterItems.NETHERITE_FILTER.get());
        var samples = NetheriteFilterData.inventory(legacy);
        NetheriteFilterData.assignToGroup(samples, FanProcessingMode.BLASTING, list(new ItemStack(Items.RAW_IRON)));
        NetheriteFilterData.save(legacy, samples);
        var oldInventory = new ItemStackHandler(1);
        oldInventory.setStackInSlot(0, legacy.copy());
        CompoundTag oldSave = new CompoundTag();
        oldSave.putString("Mode", "smoking");
        oldSave.put("Filter", oldInventory.serializeNBT(level.registryAccess()));
        level.setBlock(pos, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState(), 3);
        var fan = (NetheriteEncasedFanBlockEntity) level.getBlockEntity(pos);
        fan.loadWithComponents(oldSave, level.registryAccess());
        var migrated = fan.saveWithoutMetadata(level.registryAccess());
        helper.assertTrue(!migrated.contains("Filter") && migrated.contains("Filters")
            && ItemStack.matches(ItemStack.parseOptional(level.registryAccess(), migrated.getCompound("LegacyFilter")), legacy),
            "old installed item and complete configuration are preserved outside new native slots");
        fan.loadWithComponents(migrated, level.registryAccess());
        helper.assertTrue(fan.getFilterInventory().getSlots() == 4 && fan.getProcessingMode() == FanProcessingMode.SMOKING, "old fan migrates to four slots without changing fallback");
        for (int slot = 0; slot < 4; slot++) helper.assertTrue(fan.getFilterInventory().getStackInSlot(slot).isEmpty(), "migration never creates new original filters");
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "FanLegacyQA"));
        player.setPos(pos.getCenter());
        new NetheriteFanMenu(24, player.getInventory(), fan);
        new NetheriteFanMenu(25, player.getInventory(), fan);
        int returned = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(BoosterItems.NETHERITE_FILTER.get())) {
                returned += stack.getCount();
                helper.assertTrue(ItemStack.isSameItemSameComponents(stack, legacy), "returned legacy item keeps its full rule data");
            }
        }
        helper.assertTrue(returned == 1 && !fan.saveWithoutMetadata(level.registryAccess()).contains("LegacyFilter"), "opening twice returns the original item exactly once");
        level.removeBlock(pos, false);
        BlockPos unopened = pos.east();
        level.setBlock(unopened, BoosterBlocks.NETHERITE_ENCASED_FAN.get().defaultBlockState(), 3);
        ((NetheriteEncasedFanBlockEntity) level.getBlockEntity(unopened)).loadWithComponents(oldSave, level.registryAccess());
        level.removeBlock(unopened, false);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(unopened).inflate(.6));
        helper.assertTrue(drops.stream().filter(e -> ItemStack.isSameItemSameComponents(e.getItem(), legacy)).mapToInt(e -> e.getItem().getCount()).sum() == 1,
            "breaking an unopened old fan returns its one legacy item");
        helper.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(AllItems.FILTER.get())), "migration and breaking never duplicate native filters");
        helper.succeed();
    }
}
