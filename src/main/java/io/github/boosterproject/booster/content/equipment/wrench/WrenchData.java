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
        var result = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(SLOTS, modules.size()); i++) {
            var module = modules.get(i);
            if (WrenchModule.fromStack(module) != null) result.set(i, module.copyWithCount(1));
        }
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(result));
        int active = activeSlot(stack);
        if (active >= 0 && result.get(active).isEmpty()) setActiveSlot(stack, -1);
    }
    public static int activeSlot(ItemStack stack) {
        var tag = data(stack);
        int slot = tag.contains("Active", Tag.TAG_INT) ? tag.getInt("Active") : -1;
        return slot >= 0 && slot < SLOTS ? slot : -1;
    }
    public static void setActiveSlot(ItemStack stack, int slot) {
        int valid = slot >= 0 && slot < SLOTS && !modules(stack).get(slot).isEmpty() ? slot : -1;
        update(stack, tag -> tag.putInt("Active", valid));
    }
    public static WrenchModule active(ItemStack stack) {
        int slot = activeSlot(stack);
        return !isWrench(stack) || slot < 0 ? null : WrenchModule.fromStack(modules(stack).get(slot));
    }
    public static WrenchModule active(Player player) { return active(player.getMainHandItem()); }
    public static boolean has(ItemStack stack, WrenchModule module) { return active(stack) == module; }
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
