package io.github.boosterproject.booster.content.conversion;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class NetheriteConverterBlockEntity extends KineticBlockEntity {
    public static final int REQUIRED_STRESS = 2048, PROCESSING_TICKS = 100;
    private final FluidTank essence = new FluidTank(4000, stack -> stack.getFluid().isSame(BoosterFluids.NETHER_ESSENCE.get())) {
        @Override protected void onContentsChanged() { changedInventory(); }
    };
    private TransportedItemStack tracked;
    private ItemStack inputSnapshot = ItemStack.EMPTY;
    private int progress;

    public NetheriteConverterBlockEntity(BlockPos pos, BlockState state) { super(BoosterBlockEntityTypes.NETHERITE_CONVERTER.get(), pos, state); }
    public FluidTank getEssenceTank() { return essence; }
    public int getProgress() { return progress; }
    private void changedInventory() { setChanged(); if (level != null) sendData(); }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new BeltProcessingBehaviour(this).whenItemEnters((stack, handler) -> hold(stack)).whileItemHeld((stack, handler) -> hold(stack)));
    }

    private ProcessingResult hold(TransportedItemStack stack) { return findRecipe(stack.stack) == null ? ProcessingResult.PASS : ProcessingResult.HOLD; }
    private RecipeHolder<NetheriteConversionRecipe> findRecipe(ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        RecipeType<NetheriteConversionRecipe> type = BoosterRecipeTypes.CONVERSION.getType();
        return level.getRecipeManager().getRecipeFor(type, new SingleRecipeInput(stack), level).orElse(null);
    }

    @Override public float calculateStressApplied() {
        float rpm = Math.abs(getTheoreticalSpeed());
        // At rest this also prevents a small handheld power source from claiming the machine.
        return lastStressApplied = rpm == 0 ? REQUIRED_STRESS : REQUIRED_STRESS / rpm;
    }

    @Override public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide) return;
        if (getSpeed() == 0) resetProgress();
        if (hasNetwork()) {
            var network = getOrCreateNetwork();
            if (network.members.containsKey(this)) {
                float impact = calculateStressApplied();
                if (Float.compare(network.members.get(this), impact) != 0) network.updateStressFor(this, impact);
            }
        }
    }

    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (getSpeed() == 0 || !hasNetwork() || BeltProcessingBehaviour.isBlocked(level, worldPosition.below(2))) { resetProgress(); return; }
        var handler = BlockEntityBehaviour.get(level, worldPosition.below(2), TransportedItemStackHandlerBehaviour.TYPE);
        if (handler == null) { resetProgress(); return; }
        boolean[] found = {false};
        // Scan the actual current transport inventory, even while its belt is stopped.
        // This prevents stale item references and makes duration independent of belt RPM.
        handler.handleCenteredProcessingOnAllItems(.05F, item -> {
            if (found[0]) return TransportedResult.doNothing();
            var recipe = findRecipe(item.stack);
            if (recipe == null) return TransportedResult.doNothing();
            found[0] = true;
            return process(item, recipe.value());
        });
        if (!found[0]) resetProgress();
    }

    private TransportedResult process(TransportedItemStack item, NetheriteConversionRecipe recipe) {
        if (essence.getFluidAmount() < recipe.getEssenceAmount()) {
            resetProgress(); return TransportedResult.doNothing();
        }
        if (tracked != item || !ItemStack.isSameItemSameComponents(inputSnapshot, item.stack)) {
            resetProgress(); tracked = item; inputSnapshot = item.stack.copyWithCount(1);
        }
        if (++progress < PROCESSING_TICKS) {
            if (progress % 20 == 0) sendData();
            return TransportedResult.doNothing();
        }
        ItemStack output = upgrade(item.stack, recipe.getResultItem(level.registryAccess()));
        if (output.isEmpty()) { resetProgress(); return TransportedResult.doNothing(); }
        essence.drain(recipe.getEssenceAmount(), FluidAction.EXECUTE);
        changedInventory();
        var result = item.copy(); result.stack = output; result.locked = false; result.clearFanProcessingData();
        var remainder = item.copy(); remainder.stack.shrink(1);
        resetProgress();
        return remainder.stack.isEmpty() ? TransportedResult.convertTo(result) : TransportedResult.convertToAndLeaveHeld(List.of(result), remainder);
    }

    private ItemStack upgrade(ItemStack input, ItemStack result) {
        if (result.isEmpty()) return ItemStack.EMPTY;
        if (WrenchData.isWrench(input) && WrenchData.isWrench(result) && WrenchData.activationLimit(result) == 2) {
            // Merge this state change into the actual wrench. Replacing CUSTOM_DATA
            // with the recipe's minimal result would erase fluids and activation order.
            ItemStack output = input.copyWithCount(1);
            WrenchData.unlockSecondSlot(output);
            return output;
        }
        ItemStack output = input.transmuteCopy(result.getItem(), 1);
        output.applyComponents(result.getComponentsPatch());
        CustomData stored = output.get(DataComponents.BLOCK_ENTITY_DATA);
        if (stored != null && output.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock block) {
            var target = block.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
            if (target != null) {
                var tag = stored.copyTag(); tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(target.getType()).toString());
                output.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
            }
        }
        return output;
    }

    private void resetProgress() {
        boolean changed = progress != 0;
        progress = 0; tracked = null; inputSnapshot = ItemStack.EMPTY;
        if (changed && level != null) sendData();
    }

    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Essence", essence.writeToNBT(registries, new CompoundTag()));
        if (clientPacket) tag.putInt("Progress", progress);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        essence.readFromNBT(registries, tag.getCompound("Essence"));
        progress = clientPacket ? tag.getInt("Progress") : 0;
        if (!clientPacket) { tracked = null; inputSnapshot = ItemStack.EMPTY; }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.translatable("converter.createnetherite.status", essence.getFluidAmount(), progress, REQUIRED_STRESS));
        return true;
    }
}
