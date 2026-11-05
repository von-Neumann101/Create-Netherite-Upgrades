package io.github.boosterproject.booster.content.conversion;

import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class ConversionPlate {
    private static final String USES = "NetheriteConversionUses";
    public static boolean isCatalyst(ItemStack stack) {
        return stack.is(BoosterItems.NETHERITE_SHEET.get()) || stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    }
    public static int remaining(ItemStack stack) {
        if (stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) return 1;
        if (!stack.is(BoosterItems.NETHERITE_SHEET.get())) return 0;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(USES) ? Math.clamp(tag.getInt(USES), 0, 16) : 16;
    }
    public static void consume(ItemStack stack) {
        int remaining = remaining(stack);
        if (remaining == 0) return;
        if (remaining <= 1) stack.shrink(1);
        else CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(USES, remaining - 1));
    }
    private ConversionPlate() {}
}
