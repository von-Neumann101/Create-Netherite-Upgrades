package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import io.github.boosterproject.booster.content.kinetics.crafter.NetheriteMechanicalCrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecipeGridHandler.class, remap = false)
public abstract class NetheriteCrafterRecipeGridMixin {
    @Inject(method = "isCrafter", at = @At("HEAD"), cancellable = true)
    private static void booster$recognizeGrid(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof NetheriteMechanicalCrafterBlock) cir.setReturnValue(true);
    }
}
