package io.github.boosterproject.booster.content.processing.burner;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class NetheriteBlazeBurnerData {
    public static final String BLAZE_COUNT = "BlazeCount";
    public static final int MAX_BLAZES = 4;

    private NetheriteBlazeBurnerData() {}

    public static int clampCount(int count) {
        return Mth.clamp(count, 1, MAX_BLAZES);
    }

    public static int getCount(ItemStack stack) {
        return readCount(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    public static int readCount(CompoundTag tag) {
        return tag.contains(BLAZE_COUNT) ? clampCount(tag.getInt(BLAZE_COUNT)) : 1;
    }

    public static void setCount(ItemStack stack, int count) {
        if (!stack.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(BLAZE_COUNT, clampCount(count)));
        }
    }

    public static ItemStack captureOne(ItemStack stack) {
        int count = getCount(stack);
        if (stack.isEmpty() || count >= MAX_BLAZES) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copyWithCount(1);
        setCount(result, count + 1);
        return result;
    }

    public static float scaleHeat(float heat, int count) {
        return heat > 0 ? heat * clampCount(count) : heat;
    }

    static ItemStack finishCapture(ItemStack held, ItemStack actualHand, ItemStack filled, boolean creative) {
        if (creative) {
            // Creative Player.interactOn passes a copy, not the real hand stack.
            held = actualHand;
        }
        if (held.getCount() == 1) {
            // Player.interactOn clears the hand if its original stack becomes empty.
            setCount(held, getCount(filled));
            return ItemStack.EMPTY;
        }
        if (!creative) {
            held.shrink(1);
        }
        return filled;
    }
}
