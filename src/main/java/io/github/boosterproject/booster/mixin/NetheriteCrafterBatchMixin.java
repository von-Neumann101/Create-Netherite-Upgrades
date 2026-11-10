package io.github.boosterproject.booster.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import io.github.boosterproject.booster.content.kinetics.crafter.NetheriteMechanicalCrafterBlock;
import io.github.boosterproject.booster.content.kinetics.crafter.NetheriteMechanicalCrafterBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class NetheriteCrafterBatchMixin {
    @WrapOperation(method = "eject", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean booster$ejectFromFront(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return original.call(entry, state) || state.getBlock() instanceof NetheriteMechanicalCrafterBlock;
    }

    @WrapOperation(method = "checkCompletedRecipe", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private void booster$prepareBatch(List<MechanicalCrafterBlockEntity> chain, Consumer<MechanicalCrafterBlockEntity> begin, Operation<Void> original) {
        if (NetheriteMechanicalCrafterBlockEntity.prepareBatch(chain)) original.call(chain, begin);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack booster$assembleBatch(Level level, GroupedItems items, Operation<ItemStack> original) {
        return (Object) this instanceof NetheriteMechanicalCrafterBlockEntity n ? n.assembleBatch(items) : original.call(level, items);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(Lnet/minecraft/world/item/ItemStack;)Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;"))
    private GroupedItems booster$useBatchProducts(GroupedItems original) {
        return (Object) this instanceof NetheriteMechanicalCrafterBlockEntity n ? n.takeCompletedBatch(original) : original;
    }
}
