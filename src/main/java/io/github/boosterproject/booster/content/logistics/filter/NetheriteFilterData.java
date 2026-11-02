package io.github.boosterproject.booster.content.logistics.filter;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class NetheriteFilterData {
    public static final int GROUP_SIZE = 9;
    public static final int GROUP_COUNT = 4;
    public static final int SLOT_COUNT = GROUP_SIZE * GROUP_COUNT;

    private NetheriteFilterData() {}

    public static ItemStackHandler inventory(ItemStack filter) {
        ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT);
        ListTag rules = rules(filter);
        ItemContainerContents nested = filter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (int slot = 0; slot < Math.min(rules.size(), SLOT_COUNT); slot++) {
            ItemStack configured = nestedSample(nested, slot, rules.getString(slot));
            if (!configured.isEmpty()) {
                inventory.setStackInSlot(slot, configured);
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(rules.getString(slot));
            if (id != null) {
                final int index = slot;
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> inventory.setStackInSlot(index, new ItemStack(item)));
            }
        }
        return inventory;
    }

    public static void save(ItemStack filter, ItemStackHandler inventory) {
        if (filter.isEmpty()) return;
        CompoundTag data = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag rules = new ListTag();
        NonNullList<ItemStack> nested = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        boolean any = false;
        boolean anyNested = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack sample = inventory.getStackInSlot(slot);
            any |= !sample.isEmpty();
            if (isNestedFilter(sample)) {
                nested.set(slot, sample.copyWithCount(1));
                anyNested = true;
            }
            rules.add(StringTag.valueOf(sample.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(sample.getItem()).toString()));
        }
        if (any) data.put("FanRules", rules);
        else data.remove("FanRules");
        if (anyNested) filter.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(nested));
        else filter.remove(DataComponents.CONTAINER);
        if (data.isEmpty()) filter.remove(DataComponents.CUSTOM_DATA);
        else filter.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static FanProcessingMode findMode(ItemStack filter, ItemStack target) {
        return findMode(null, filter, target);
    }

    public static FanProcessingMode findMode(Level level, ItemStack filter, ItemStack target) {
        if (filter.isEmpty() || target.isEmpty()) return null;
        String itemId = BuiltInRegistries.ITEM.getKey(target.getItem()).toString();
        ListTag rules = rules(filter);
        ItemContainerContents nested = filter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (int slot = 0; slot < Math.min(rules.size(), SLOT_COUNT); slot++) {
            String rule = rules.getString(slot);
            ItemStack sample = nestedSample(nested, slot, rule);
            if (sample.isEmpty() ? rule.equals(itemId) : matches(level, sample, target)) {
                return FanProcessingMode.byIndex(slot / GROUP_SIZE);
            }
        }
        return null;
    }

    public static FanProcessingMode resolve(ItemStack filter, ItemStack target, FanProcessingMode fallback) {
        return resolve(null, filter, target, fallback);
    }

    public static FanProcessingMode resolve(Level level, ItemStack filter, ItemStack target, FanProcessingMode fallback) {
        FanProcessingMode match = findMode(level, filter, target);
        return match == null ? fallback : match;
    }

    public static boolean assign(ItemStackHandler inventory, int slot, ItemStack sample) {
        if (slot < 0 || slot >= SLOT_COUNT || slot >= inventory.getSlots()) return false;
        ItemStack normalized = sample.isEmpty() ? ItemStack.EMPTY
            : isNestedFilter(sample) ? sample.copyWithCount(1) : new ItemStack(sample.getItem());
        if (!normalized.isEmpty()) {
            for (int other = 0; other < Math.min(SLOT_COUNT, inventory.getSlots()); other++) {
                if (other != slot && sameSample(inventory.getStackInSlot(other), normalized)) {
                    inventory.setStackInSlot(other, ItemStack.EMPTY);
                }
            }
        }
        inventory.setStackInSlot(slot, normalized);
        return true;
    }

    public static boolean assignToGroup(ItemStackHandler inventory, FanProcessingMode mode, ItemStack sample) {
        if (sample.isEmpty()) return false;
        int start = mode.ordinal() * GROUP_SIZE;
        int end = Math.min(start + GROUP_SIZE, inventory.getSlots());
        int empty = -1;
        for (int slot = start; slot < end; slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (sameSample(existing, sample)) return assign(inventory, slot, sample);
            if (empty < 0 && existing.isEmpty()) empty = slot;
        }
        return empty >= 0 && assign(inventory, empty, sample);
    }

    private static ListTag rules(ItemStack filter) {
        return filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getList("FanRules", Tag.TAG_STRING);
    }

    public static boolean sameSample(ItemStack first, ItemStack second) {
        return ItemStack.isSameItem(first, second)
            && (!isNestedFilter(first) || ItemStack.isSameItemSameComponents(first, second));
    }

    public static boolean matches(Level level, ItemStack sample, ItemStack target) {
        if (sample.isEmpty() || target.isEmpty()) return false;
        return isNestedFilter(sample) ? FilterItemStack.of(sample.copy()).test(level, target) : target.is(sample.getItem());
    }

    private static boolean isNestedFilter(ItemStack sample) {
        return sample.getItem() instanceof FilterItem && !(sample.getItem() instanceof NetheriteFilterItem);
    }

    private static ItemStack nestedSample(ItemContainerContents contents, int slot, String id) {
        if (slot >= contents.getSlots()) return ItemStack.EMPTY;
        ItemStack sample = contents.getStackInSlot(slot);
        return isNestedFilter(sample) && BuiltInRegistries.ITEM.getKey(sample.getItem()).toString().equals(id)
            ? sample : ItemStack.EMPTY;
    }
}
