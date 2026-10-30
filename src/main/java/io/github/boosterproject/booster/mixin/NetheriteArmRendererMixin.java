package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.NetheriteArmModels;
import io.github.boosterproject.booster.content.kinetics.mechanicalArm.NetheriteMechanicalArmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ArmRenderer.class, remap = false)
public abstract class NetheriteArmRendererMixin {
    @ModifyArg(method = {"renderArm", "getRotatedModel"}, at = @At(value = "INVOKE",
        target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), index = 0)
    private static PartialModel booster$skin(PartialModel original, @Local(argsOnly = true) BlockState state) {
        return state.getBlock() instanceof NetheriteMechanicalArmBlock ? NetheriteArmModels.skin(original) : original;
    }
}
