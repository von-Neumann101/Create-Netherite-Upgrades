package io.github.boosterproject.booster.content.equipment.wrench;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.fluids.FluidStack;

/** State belongs to the wrench stack, including the real installed block items. */
public final class WrenchData {
    public static final int SLOTS = 8, CAPACITY = 64000;
    private static final String KEY = "NetheriteWrench";
    private WrenchData() {}
    public static boolean isWrench(ItemStack stack) { return stack.getItem() instanceof NetheriteNetheriteWrenchItem; }
    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
    }
    private static void update(ItemStack stack, Consumer<CompoundTag> change) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var tag = root.getCompound(KEY); change.accept(tag); root.put(KEY, tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    public static List<ItemStack> modules(ItemStack stack) {
        var result = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(result);
        return result;
    }
    public static void setModules(ItemStack stack, List<ItemStack> modules) {
        var previous = modules(stack);
        var active = new ArrayList<>(activeSlots(stack));
        var result = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(SLOTS, modules.size()); i++) {
            var module = modules.get(i);
            if (WrenchModule.fromStack(module) != null) result.set(i, module.copyWithCount(1));
        }
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(result));
        active.removeIf(slot -> result.get(slot).isEmpty() || !ItemStack.isSameItemSameComponents(previous.get(slot), result.get(slot)));
        writeActive(stack, active);
    }
    public static int activationLimit(ItemStack stack) { return data(stack).getInt("ActivationLimit") >= 2 ? 2 : 1; }
    public static boolean unlockSecondSlot(ItemStack stack) {
        if (!isWrench(stack) || activationLimit(stack) == 2) return false;
        update(stack, tag -> tag.putInt("ActivationLimit", 2));
        return true;
    }
    public static List<Integer> activeSlots(ItemStack stack) {
        if (!isWrench(stack)) return List.of();
        var tag = data(stack);
        int[] stored = tag.contains("ActiveSlots", Tag.TAG_INT_ARRAY) ? tag.getIntArray("ActiveSlots")
            : new int[] { tag.contains("Active", Tag.TAG_INT) ? tag.getInt("Active") : -1 };
        var modules = modules(stack);
        List<Integer> result = new ArrayList<>();
        for (int slot : stored) {
            if (slot < 0 || slot >= SLOTS || WrenchModule.fromStack(modules.get(slot)) == null || result.contains(slot)) continue;
            if (result.size() == activationLimit(stack)) result.removeFirst();
            result.add(slot);
        }
        return List.copyOf(result);
    }
    private static void writeActive(ItemStack stack, List<Integer> slots) {
        update(stack, tag -> {
            tag.putIntArray("ActiveSlots", slots.stream().mapToInt(Integer::intValue).toArray());
            tag.putInt("Active", slots.isEmpty() ? -1 : slots.getLast());
        });
    }
    public static boolean toggleSlot(ItemStack stack, int slot) {
        if (!isWrench(stack) || slot < 0 || slot >= SLOTS || WrenchModule.fromStack(modules(stack).get(slot)) == null) return false;
        var active = new ArrayList<>(activeSlots(stack));
        if (!active.remove(Integer.valueOf(slot))) {
            if (active.size() == activationLimit(stack)) active.removeFirst();
            active.add(slot);
        }
        writeActive(stack, active);
        return true;
    }
    public static int activeSlot(ItemStack stack) {
        var active = activeSlots(stack);
        return active.isEmpty() ? -1 : active.getLast();
    }
    public static void setActiveSlot(ItemStack stack, int slot) {
        int valid = slot >= 0 && slot < SLOTS && !modules(stack).get(slot).isEmpty() ? slot : -1;
        writeActive(stack, valid < 0 ? List.of() : List.of(valid));
    }
    public static int rightClickSlot(ItemStack stack) {
        var active = activeSlots(stack);
        var modules = modules(stack);
        for (int i = active.size() - 1; i >= 0; i--) {
            int slot = active.get(i);
            if (WrenchModule.fromStack(modules.get(slot)).usesRightClick()) return slot;
        }
        return -1;
    }
    public static WrenchModule rightClickModule(ItemStack stack) {
        int slot = rightClickSlot(stack);
        return slot < 0 ? null : WrenchModule.fromStack(modules(stack).get(slot));
    }
    public static WrenchModule active(ItemStack stack) {
        var rightClick = rightClickModule(stack);
        if (rightClick != null) return rightClick;
        int slot = activeSlot(stack);
        return !isWrench(stack) || slot < 0 ? null : WrenchModule.fromStack(modules(stack).get(slot));
    }
    public static WrenchModule active(Player player) { return active(player.getMainHandItem()); }
    public static boolean has(ItemStack stack, WrenchModule module) {
        var modules = modules(stack);
        return module != null && activeSlots(stack).stream().anyMatch(slot -> WrenchModule.fromStack(modules.get(slot)) == module);
    }
    public static boolean has(Player player, WrenchModule module) { return has(player.getMainHandItem(), module); }
    public static boolean fanPull(ItemStack stack) { return data(stack).getBoolean("Pull"); }
    public static void setFanPull(ItemStack stack, boolean pull) { update(stack, tag -> tag.putBoolean("Pull", pull)); }
    public static List<FluidStack> fluids(ItemStack stack, HolderLookup.Provider lookup) {
        var list = data(stack).getList("Fluids", Tag.TAG_COMPOUND);
        List<FluidStack> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            var fluid = FluidStack.parseOptional(lookup, list.getCompound(i));
            if (!fluid.isEmpty()) result.add(fluid);
        }
        return result;
    }
    public static void setFluids(ItemStack stack, List<FluidStack> fluids, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (var fluid : fluids) if (!fluid.isEmpty()) list.add(fluid.save(lookup));
        update(stack, tag -> tag.put("Fluids", list));
        if (selectedFluid(stack) >= list.size()) selectFluid(stack, 0);
    }
    public static int selectedFluid(ItemStack stack) { return Math.max(0, data(stack).getInt("SelectedFluid")); }
    public static void selectFluid(ItemStack stack, int index) { update(stack, tag -> tag.putInt("SelectedFluid", Math.max(0, index))); }
    public static int totalFluid(ItemStack stack, HolderLookup.Provider lookup) {
        return fluids(stack, lookup).stream().mapToInt(FluidStack::getAmount).sum();
    }
}
