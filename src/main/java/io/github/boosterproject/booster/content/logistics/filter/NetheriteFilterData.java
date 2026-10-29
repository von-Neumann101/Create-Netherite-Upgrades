package io.github.boosterproject.booster.content.logistics.filter;

import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class NetheriteFilterData {
    public static final int GROUP_SIZE = 9;
    public static final int GROUP_COUNT = 4;
    public static final int SLOT_COUNT = GROUP_SIZE * GROUP_COUNT;

    private NetheriteFilterData() {}

    public static ItemStackHandler inventory(ItemStack filter) {
        ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT);
        ListTag rules = rules(filter);
        for (int slot = 0; slot < Math.min(rules.size(), SLOT_COUNT); slot++) {
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
        boolean any = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack sample = inventory.getStackInSlot(slot);
            any |= !sample.isEmpty();
            rules.add(StringTag.valueOf(sample.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(sample.getItem()).toString()));
        }
        if (any) data.put("FanRules", rules);
        else data.remove("FanRules");
        if (data.isEmpty()) filter.remove(DataComponents.CUSTOM_DATA);
        else filter.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static FanProcessingMode findMode(ItemStack filter, ItemStack target) {
        if (filter.isEmpty() || target.isEmpty()) return null;
        String itemId = BuiltInRegistries.ITEM.getKey(target.getItem()).toString();
        ListTag rules = rules(filter);
        for (int slot = 0; slot < Math.min(rules.size(), SLOT_COUNT); slot++) {
            if (rules.getString(slot).equals(itemId)) return FanProcessingMode.byIndex(slot / GROUP_SIZE);
        }
        return null;
    }

    public static FanProcessingMode resolve(ItemStack filter, ItemStack target, FanProcessingMode fallback) {
        FanProcessingMode match = findMode(filter, target);
        return match == null ? fallback : match;
    }

    public static boolean assign(ItemStackHandler inventory, int slot, ItemStack sample) {
        if (slot < 0 || slot >= SLOT_COUNT || slot >= inventory.getSlots()) return false;
        ItemStack normalized = sample.isEmpty() ? ItemStack.EMPTY : new ItemStack(sample.getItem());
        if (!normalized.isEmpty()) {
            for (int other = 0; other < Math.min(SLOT_COUNT, inventory.getSlots()); other++) {
                if (other != slot && ItemStack.isSameItem(inventory.getStackInSlot(other), normalized)) {
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
            if (ItemStack.isSameItem(existing, sample)) return assign(inventory, slot, sample);
            if (empty < 0 && existing.isEmpty()) empty = slot;
        }
        return empty >= 0 && assign(inventory, empty, sample);
    }

    private static ListTag rules(ItemStack filter) {
        return filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getList("FanRules", Tag.TAG_STRING);
    }
}
