package io.github.boosterproject.booster.content.processing.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlock;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.List;

public class NetheriteBasinBlockEntity extends BasinBlockEntity {
    public NetheriteBasinBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_BASIN.get(), pos, state);
    }

    public IItemHandler getItemCapability() { return itemCapability; }
    public IFluidHandler getFluidCapability() { return fluidCapability; }

    @Override
    public boolean acceptOutputs(List<ItemStack> items, List<FluidStack> fluids, boolean simulate) {
        if (!items.isEmpty() && getBlockState().getValue(BasinBlock.FACING) == Direction.DOWN) {
            // Native simulation checks outputs separately. Reserve the whole list together so
            // repeated products cannot promise the same free slot and then lose items on execution.
            var preview = new ItemStackHandler(outputInventory.getSlots());
            for (int slot = 0; slot < outputInventory.getSlots(); slot++)
                preview.setStackInSlot(slot, outputInventory.getStackInSlot(slot).copy());
            for (ItemStack stack : items)
                if (!ItemHandlerHelper.insertItemStacked(preview, stack.copy(), false).isEmpty()) return false;
        }
        return super.acceptOutputs(items, fluids, simulate);
    }
}
