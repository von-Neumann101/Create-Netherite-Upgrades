package io.github.boosterproject.booster.content.processing.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinInventory;
import net.minecraft.world.item.ItemStack;

/** Uses native storage and extraction, allowing identical stacks in multiple cells. */
public class NetheriteBasinInventory extends BasinInventory {
    public NetheriteBasinInventory(int slots, BasinBlockEntity basin) { super(slots, basin); }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!insertionAllowed) return stack;
        return inv.insertItem(slot, stack, simulate);
    }
}
