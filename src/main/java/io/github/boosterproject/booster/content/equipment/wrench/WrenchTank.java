package io.github.boosterproject.booster.content.equipment.wrench;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** A shared 64-bucket reservoir; world actions retain the native bucket fluid rules. */
public final class WrenchTank {
    public static final int CAPACITY = 64000;
    public static final int BUCKET = 1000;

    private WrenchTank() {}

    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack wrench = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || WrenchData.active(wrench) != WrenchModule.TANK
            || player.isSpectator() || !player.getAbilities().mayBuild) return InteractionResultHolder.pass(wrench);
        var eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
            ClipContext.Block.OUTLINE, player.isShiftKeyDown() ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(wrench);
        if (!canUse(level, player, wrench, hit.getBlockPos(), hit)) return InteractionResultHolder.fail(wrench);
        return player.isShiftKeyDown() ? place(level, player, hand, wrench, hit) : collect(level, player, wrench, hit);
    }

    private static boolean canUse(Level level, Player player, ItemStack wrench, BlockPos pos, BlockHitResult hit) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.mayInteract(player, pos)
            && player.mayUseItemAt(pos, hit.getDirection(), wrench);
    }

    private static InteractionResultHolder<ItemStack> collect(Level level, Player player, ItemStack wrench, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        if (!state.getFluidState().isSource() || !(state.getBlock() instanceof BucketPickup))
            return InteractionResultHolder.pass(wrench);
        if (WrenchData.totalFluid(wrench, level.registryAccess()) > CAPACITY - BUCKET)
            return InteractionResultHolder.fail(wrench);
        if (level.isClientSide) return InteractionResultHolder.success(wrench);

        // Capacity has already been reserved. The native bucket wrapper handles waterlogging,
        // source removal and custom BucketPickup blocks instead of deleting a block ourselves.
        var result = FluidUtil.tryPickUpFluid(new ItemStack(Items.BUCKET), player, level, pos, hit.getDirection());
        if (!result.isSuccess()) return InteractionResultHolder.fail(wrench);
        FluidStack picked = FluidUtil.getFluidContained(result.getResult()).orElse(FluidStack.EMPTY);
        if (picked.isEmpty()) return InteractionResultHolder.fail(wrench);
        var fluids = new ArrayList<>(WrenchData.fluids(wrench, level.registryAccess()));
        int existing = -1;
        for (int i = 0; i < fluids.size(); i++) {
            if (FluidStack.isSameFluidSameComponents(fluids.get(i), picked)) { existing = i; break; }
        }
        if (existing >= 0) fluids.get(existing).grow(picked.getAmount());
        else fluids.add(picked.copy());
        WrenchData.setFluids(wrench, fluids, level.registryAccess());
        if (fluids.size() == 1) WrenchData.selectFluid(wrench, 0);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        player.getInventory().setChanged();
        return InteractionResultHolder.success(wrench);
    }

    private static InteractionResultHolder<ItemStack> place(Level level, Player player, InteractionHand hand,
                                                            ItemStack wrench, BlockHitResult hit) {
        var fluids = new ArrayList<>(WrenchData.fluids(wrench, level.registryAccess()));
        int selected = WrenchData.selectedFluid(wrench);
        if (selected < 0 || selected >= fluids.size() || fluids.get(selected).getAmount() < BUCKET)
            return InteractionResultHolder.fail(wrench);
        FluidStack resource = fluids.get(selected).copyWithAmount(BUCKET);
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LiquidBlockContainer container
            && container.canPlaceLiquid(player, level, pos, state, resource.getFluid()))) pos = pos.relative(hit.getDirection());
        if (!canUse(level, player, wrench, pos, hit)) return InteractionResultHolder.fail(wrench);
        if (level.isClientSide) return InteractionResultHolder.success(wrench);

        // Use an isolated one-bucket source so failed placement never debits the saved reservoir.
        var source = new FluidTank(BUCKET);
        source.fill(resource, FluidAction.EXECUTE);
        if (!FluidUtil.tryPlaceFluid(player, level, hand, pos, source, resource)) return InteractionResultHolder.fail(wrench);
        fluids.get(selected).shrink(BUCKET - source.getFluidAmount());
        if (fluids.get(selected).isEmpty()) fluids.remove(selected);
        WrenchData.setFluids(wrench, fluids, level.registryAccess());
        WrenchData.selectFluid(wrench, fluids.isEmpty() ? -1 : Math.min(selected, fluids.size() - 1));
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        player.getInventory().setChanged();
        return InteractionResultHolder.success(wrench);
    }
}
