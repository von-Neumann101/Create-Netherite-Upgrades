package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler;
import io.github.boosterproject.booster.content.kinetics.mechanicalArm.NetheriteArmBehavior;
import io.github.boosterproject.booster.content.kinetics.mechanicalArm.NetheriteMechanicalArmBlock;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ArmInteractionPointHandler.class, remap = false)
public abstract class NetheriteArmSelectionMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
        target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean booster$recognizeArmItem(boolean original) {
        return original || Minecraft.getInstance().player.getMainHandItem()
            .is(BoosterItems.NETHERITE_MECHANICAL_ARM.get());
    }

    @ModifyExpressionValue(method = "flushSettings", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;getRange()I"))
    private static int booster$selectionRange(int original, BlockPos pos) {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockState(pos).getBlock() instanceof NetheriteMechanicalArmBlock
            ? NetheriteArmBehavior.workRange(original) : original;
    }
}
