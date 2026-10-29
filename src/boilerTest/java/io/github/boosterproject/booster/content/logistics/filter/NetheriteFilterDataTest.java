package io.github.boosterproject.booster.content.logistics.filter;

import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.fml.loading.LoadingModList;

public final class NetheriteFilterDataTest {
    private static int checks;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
        ItemStack filter = new ItemStack(Items.PAPER);
        ItemStackHandler samples = NetheriteFilterData.inventory(filter);
        ItemStack source = new ItemStack(Items.IRON_INGOT, 64);
        source.set(DataComponents.CUSTOM_NAME, Component.literal("keep original"));
        check(NetheriteFilterData.assign(samples, 0, source), "mouse assigns a washing sample");
        check(samples.getStackInSlot(0).getCount() == 1, "sample count is one");
        check(!samples.getStackInSlot(0).has(DataComponents.CUSTOM_NAME), "rules store item kinds, not sample data");
        check(source.getCount() == 64 && source.has(DataComponents.CUSTOM_NAME), "sample assignment preserves source inventory");
        NetheriteFilterData.save(filter, samples);
        check(NetheriteFilterData.resolve(filter, source, FanProcessingMode.SMOKING) == FanProcessingMode.WASHING,
            "matching item overrides selected fan mode");
        check(NetheriteFilterData.resolve(filter, new ItemStack(Items.BOOK), FanProcessingMode.HAUNTING) == FanProcessingMode.HAUNTING,
            "unmatched item uses selected fan mode even with a filter");
        check(NetheriteFilterData.assignToGroup(samples, FanProcessingMode.BLASTING, source), "number key assigns another group");
        check(samples.getStackInSlot(0).isEmpty() && samples.getStackInSlot(9).is(Items.IRON_INGOT), "cross-group assignment moves rule");
        Item[] fillers = {Items.STONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.COBBLESTONE,
            Items.CLAY_BALL, Items.GLASS, Items.BRICK, Items.COAL};
        for (int i = 0; i < fillers.length; i++) NetheriteFilterData.assign(samples, i, new ItemStack(fillers[i]));
        check(!NetheriteFilterData.assignToGroup(samples, FanProcessingMode.WASHING, source), "full group rejects assignment");
        check(samples.getStackInSlot(9).is(Items.IRON_INGOT), "full group retains previous rule");
        check(!NetheriteFilterData.assign(samples, -1, source) && !NetheriteFilterData.assign(samples, 36, source), "invalid slot is rejected");
        CompoundTag metadata = new CompoundTag();
        metadata.putString("Unrelated", "preserve");
        filter.set(DataComponents.CUSTOM_DATA, CustomData.of(metadata));
        NetheriteFilterData.save(filter, samples);
        ItemStackHandler restored = NetheriteFilterData.inventory(filter);
        check(restored.getStackInSlot(9).is(Items.IRON_INGOT) && restored.getStackInSlot(8).is(Items.COAL), "rules survive data round-trip");
        check(filter.get(DataComponents.CUSTOM_DATA).copyTag().getString("Unrelated").equals("preserve"), "save preserves unrelated data");
        NetheriteFilterData.save(filter, new ItemStackHandler(36));
        CompoundTag cleared = filter.get(DataComponents.CUSTOM_DATA).copyTag();
        check(!cleared.contains("FanRules") && cleared.getString("Unrelated").equals("preserve"), "clear removes only owned data");
        check(NetheriteFilterData.resolve(ItemStack.EMPTY, source, FanProcessingMode.BLASTING) == FanProcessingMode.BLASTING,
            "no filter uses selected fan mode");
        System.out.println("Netherite filter regression checks passed (" + checks + " cases).");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }
}
