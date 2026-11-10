package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.NetheriteCrafterModels;
import io.github.boosterproject.booster.content.kinetics.crafter.NetheriteMechanicalCrafterBlock;
import io.github.boosterproject.booster.content.kinetics.crafter.NetheriteMechanicalCrafterBlockEntity;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = MechanicalCrafterRenderer.class, remap = false)
public abstract class NetheriteCrafterRendererMixin {
    @ModifyArg(method = "renderAndTransform", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), index = 0)
    private PartialModel booster$skinParts(PartialModel original, @Local(argsOnly = true) BlockState state) {
        return state.getBlock() instanceof NetheriteMechanicalCrafterBlock ? NetheriteCrafterModels.skin(original) : original;
    }
    @ModifyExpressionValue(method = "renderFast", at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllSpriteShifts;CRAFTER_THINGIES:Lnet/createmod/catnip/render/SpriteShiftEntry;"))
    private SpriteShiftEntry booster$skinBelt(SpriteShiftEntry original, @Local(argsOnly = true) MechanicalCrafterBlockEntity be) {
        return be instanceof NetheriteMechanicalCrafterBlockEntity ? NetheriteCrafterModels.BELT_SHIFT : original;
    }
}
