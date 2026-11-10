package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.boosterproject.booster.content.kinetics.crafter.NetheriteMechanicalCrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ConnectedInputHandler.class, remap = false)
public abstract class NetheriteCrafterConnectionMixin {
    @WrapOperation(method = "shouldConnect", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean booster$recognizeInput(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return original.call(entry, state) || state.getBlock() instanceof NetheriteMechanicalCrafterBlock;
    }
}
