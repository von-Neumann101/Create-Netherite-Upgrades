package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItemRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.client.BoosterPartialModels;
import io.github.boosterproject.booster.content.equipment.NetheriteExtendoGripItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ExtendoGripItemRenderer.class, remap = false)
public abstract class NetheriteGripItemRendererMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;THIN_SHORT:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private static PartialModel booster$thinShort(PartialModel original, @Local(argsOnly = true) ItemStack stack) {
        return NetheriteExtendoGripItem.isGrip(stack) ? BoosterPartialModels.NETHERITE_GRIP_THIN_SHORT : original;
    }
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;WIDE_SHORT:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private static PartialModel booster$wideShort(PartialModel original, @Local(argsOnly = true) ItemStack stack) {
        return NetheriteExtendoGripItem.isGrip(stack) ? BoosterPartialModels.NETHERITE_GRIP_WIDE_SHORT : original;
    }
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;THIN_LONG:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private static PartialModel booster$thinLong(PartialModel original, @Local(argsOnly = true) ItemStack stack) {
        return NetheriteExtendoGripItem.isGrip(stack) ? BoosterPartialModels.NETHERITE_GRIP_THIN_LONG : original;
    }
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;WIDE_LONG:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private static PartialModel booster$wideLong(PartialModel original, @Local(argsOnly = true) ItemStack stack) {
        return NetheriteExtendoGripItem.isGrip(stack) ? BoosterPartialModels.NETHERITE_GRIP_WIDE_LONG : original;
    }
}
