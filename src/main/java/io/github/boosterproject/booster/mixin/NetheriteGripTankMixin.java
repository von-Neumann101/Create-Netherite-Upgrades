package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import io.github.boosterproject.booster.content.equipment.NetheriteExtendoGripItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidTankBlock.class, remap = false)
public abstract class NetheriteGripTankMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void booster$survivalBucket(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (player.isCreative() || hand != InteractionHand.MAIN_HAND || !stack.is(Items.BUCKET)
            || !NetheriteExtendoGripItem.isGrip(player.getOffhandItem())) return;
        if (player.isSpectator() || !player.getAbilities().mayBuild || !player.canInteractWithBlock(pos, 0)
            || !level.mayInteract(player, pos) || !(level.getBlockEntity(pos) instanceof FluidTankBlockEntity)) {
            cir.setReturnValue(ItemInteractionResult.FAIL); return;
        }
        var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (tank == null) { cir.setReturnValue(ItemInteractionResult.FAIL); return; }
        var result = FluidUtil.tryFillContainerAndStow(stack, tank, new InvWrapper(player.getInventory()), FluidType.BUCKET_VOLUME, player, !level.isClientSide);
        if (result.isSuccess() && !level.isClientSide) {
            player.setItemInHand(hand, result.getResult());
            NetheriteExtendoGripItem.consumeAction(player, player.getOffhandItem(), EquipmentSlot.OFFHAND);
        }
        cir.setReturnValue(result.isSuccess() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL);
    }
}
