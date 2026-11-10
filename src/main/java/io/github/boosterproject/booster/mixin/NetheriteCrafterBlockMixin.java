package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.boosterproject.booster.content.kinetics.crafter.NetheriteMechanicalCrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MechanicalCrafterBlock.class, remap = false)
public abstract class NetheriteCrafterBlockMixin {
    @WrapOperation(method = {"getTargetDirection", "isValidTarget"}, at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean booster$recognizeCrafter(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return original.call(entry, state) || state.getBlock() instanceof NetheriteMechanicalCrafterBlock;
    }
}
