package io.github.boosterproject.booster.content.kinetics.crafter;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import io.github.boosterproject.booster.mixin.NetheriteCrafterGridAccess;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetheriteMechanicalCrafterBlockEntity extends MechanicalCrafterBlockEntity {
    private int plannedBatch = 1;
    private boolean batchActive;
    private GroupedItems completedBatch;
    boolean removing;

    public NetheriteMechanicalCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_CRAFTER.get(), pos, state);
        // Check next tick, after a connected input has finished distributing its insertion.
        inventory.withMaxStackSize(64).whenContentsChanged(slot -> {});
    }

    public IItemHandler getItemCapability() { return getInvCapability(); }

    @Override
    public void initialize() {
        // SmartBlockEntity.initialize calls lazyTick before behaviours tick. A saved INSERTING phase
        // must discover its outlet first, or native tryInsert treats the missing cache as no inventory.
        getBehaviour(InvManipulationBehaviour.TYPE).findNewCapability();
        super.initialize();
    }

    public static boolean prepareBatch(List<MechanicalCrafterBlockEntity> chain) {
        if (chain.stream().anyMatch(be -> be instanceof NetheriteMechanicalCrafterBlockEntity n && n.batchActive)) return false;
        int amount = chain.stream().allMatch(NetheriteMechanicalCrafterBlockEntity.class::isInstance)
            ? chain.stream().map(be -> be.getInventory().getItem(0)).filter(s -> !s.isEmpty()).mapToInt(ItemStack::getCount).min().orElse(1)
            : 1;
        for (var be : chain) if (be instanceof NetheriteMechanicalCrafterBlockEntity n) n.plannedBatch = amount;
        return true;
    }

    @Override
    public void checkCompletedRecipe(boolean poweredStart) {
        if (!batchActive) super.checkCompletedRecipe(poweredStart);
    }

    @Override
    protected void begin() {
        ItemStack available = inventory.getItem(0).copy();
        ItemStack reserved = available.split(Math.min(available.getCount(), plannedBatch));
        batchActive = true;
        super.begin();
        groupedItems = new GroupedItems(reserved);
        inventory.setStackInSlot(0, available);
        plannedBatch = 1;
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (!batchActive && !removing) checkCompletedRecipe(false);
    }

    @Override
    public void eject() {
        // Native completion ejects every inventory. Keep unused ingredients unless this cell is removed.
        ItemStack excess = removing ? ItemStack.EMPTY : inventory.getItem(0).copy();
        if (!removing) inventory.setStackInSlot(0, ItemStack.EMPTY);
        super.eject();
        inventory.setStackInSlot(0, excess);
        batchActive = false;
        completedBatch = null;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        String phaseName = tag.getString("Phase");
        batchActive = !phaseName.isEmpty() && !phaseName.equals("IDLE");
    }

    public ItemStack assembleBatch(GroupedItems inputs) {
        Map<Pair<Integer, Integer>, ItemStack> grid = grid(inputs);
        int amount = grid.values().stream().filter(s -> !s.isEmpty()).mapToInt(ItemStack::getCount).min().orElse(0);
        if (amount == 0) return null;
        GroupedItems single = new GroupedItems();
        grid.forEach((pos, stack) -> grid(single).put(pos, stack.copyWithCount(stack.isEmpty() ? 0 : 1)));
        List<ItemStack> products = new ArrayList<>();
        for (int copy = 0; copy < amount; copy++) {
            ItemStack result = RecipeGridHandler.tryToApplyRecipe(level, single);
            if (result == null || result.isEmpty()) return null;
            addProduct(products, result);
            for (ItemStack stack : grid.values()) if (!stack.isEmpty() && stack.hasCraftingRemainingItem())
                addProduct(products, stack.copyWithCount(1).getCraftingRemainingItem());
        }
        completedBatch = new GroupedItems();
        for (int i = 0; i < products.size(); i++) grid(completedBatch).put(Pair.of(i, 0), products.get(i));
        // The native tick next gathers containers; they are already included once per recipe copy above.
        grid.clear();
        return products.getFirst();
    }

    public GroupedItems takeCompletedBatch(GroupedItems fallback) {
        if (completedBatch == null) return fallback;
        GroupedItems result = completedBatch;
        completedBatch = null;
        return result;
    }

    private static Map<Pair<Integer, Integer>, ItemStack> grid(GroupedItems items) {
        return ((NetheriteCrafterGridAccess) items).booster$getGrid();
    }

    private static void addProduct(List<ItemStack> products, ItemStack input) {
        ItemStack remaining = input.copy();
        for (ItemStack existing : products) {
            if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;
            int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) return;
        }
        while (!remaining.isEmpty()) products.add(remaining.split(remaining.getMaxStackSize()));
    }
}
