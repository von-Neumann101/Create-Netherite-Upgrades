package io.github.boosterproject.booster.content.logistics.filter;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import io.github.boosterproject.booster.content.kinetics.fan.FanProcessingMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NetheriteFilterItem extends FilterItem {
    public NetheriteFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public List<Component> makeSummary(ItemStack filter) {
        ItemStackHandler inventory = NetheriteFilterData.inventory(filter);
        List<Component> summary = new ArrayList<>();
        for (FanProcessingMode mode : FanProcessingMode.values()) {
            int count = 0;
            int firstSlot = mode.ordinal() * NetheriteFilterData.GROUP_SIZE;
            for (int slot = firstSlot; slot < firstSlot + NetheriteFilterData.GROUP_SIZE; slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) {
                    count++;
                }
            }
            if (count > 0) {
                summary.add(Component.translatable("tooltip.booster.netherite_filter.rules", mode.displayName(), count)
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        return summary;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NetheriteFilterMenu(id, inventory, player.getMainHandItem());
    }

    @Override
    public DataComponentType<?> getComponentType() {
        return DataComponents.CUSTOM_DATA;
    }

    @Override
    public FilterItemStack makeStackWrapper(ItemStack filter) {
        return new RuleFilterStack(filter);
    }

    @Override
    public ItemStack[] getFilterItems(ItemStack filter) {
        ItemStackHandler inventory = NetheriteFilterData.inventory(filter);
        List<ItemStack> samples = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack sample = inventory.getStackInSlot(slot);
            if (!sample.isEmpty()) {
                samples.add(sample.copy());
            }
        }
        return samples.toArray(ItemStack[]::new);
    }

    private static class RuleFilterStack extends FilterItemStack {
        private final ItemStack[] samples;

        RuleFilterStack(ItemStack filter) {
            super(filter);
            samples = ((NetheriteFilterItem) filter.getItem()).getFilterItems(filter);
        }

        @Override
        public boolean test(Level level, ItemStack stack, boolean matchNBT) {
            if (stack.isEmpty()) {
                return false;
            }
            for (ItemStack sample : samples) {
                if (stack.is(sample.getItem())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean test(Level level, FluidStack stack, boolean matchNBT) {
            for (ItemStack sample : samples) {
                if (FilterItemStack.of(sample).test(level, stack, false)) {
                    return true;
                }
            }
            return false;
        }
    }
}
