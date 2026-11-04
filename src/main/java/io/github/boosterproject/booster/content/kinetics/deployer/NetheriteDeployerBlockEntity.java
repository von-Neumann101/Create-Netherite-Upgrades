package io.github.boosterproject.booster.content.kinetics.deployer;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NetheriteDeployerBlockEntity extends DeployerBlockEntity {
    private boolean applyingBatch;

    public NetheriteDeployerBlockEntity(BlockPos pos, BlockState state) {
        super(BoosterBlockEntityTypes.NETHERITE_DEPLOYER.get(), pos, state);
    }

    @Override protected Vec3 getMovementVector() { return Vec3.atLowerCornerOf(getBlockState().getValue(NetheriteDeployerBlock.FACING).getNormal()); }
    public boolean isApplyingBatch() { return applyingBatch; }

    public void applyBatch(TransportedItemStack input, TransportedItemStackHandlerBehaviour handler) {
        if (level == null || level.isClientSide || player == null) return;
        var initial = getRecipe(input.stack);
        if (initial == null) return;
        var remaining = new TransportedItemStack[] {input.copy()};
        List<ItemStack> results = new ArrayList<>();
        var capture = new TransportedItemStackHandlerBehaviour(this, (distance, process) -> {
            var result = process.apply(remaining[0]);
            if (result == null || result.doesNothing()) return;
            if (result.hasHeldOutput()) {
                for (var output : result.getOutputs()) ItemHelper.addToList(output.stack.copy(), results);
                remaining[0] = result.getHeldOutput();
            } else {
                // Native zero-output rolls return only the unprocessed input remainder.
                remaining[0] = result.getOutputs().getFirst();
            }
        });
        applyingBatch = true;
        int processed = 0;
        try {
            int limit = Math.min(input.stack.getCount(), input.stack.getMaxStackSize());
            for (; processed < limit && !remaining[0].stack.isEmpty() && !player.getMainHandItem().isEmpty(); processed++) {
                var recipe = getRecipe(remaining[0].stack);
                if (recipe == null || !recipe.id().equals(initial.id())) break;
                BeltDeployerCallbacks.activate(remaining[0], capture, this, recipe.value());
            }
        } finally { applyingBatch = false; }
        if (processed == 0) return;
        input.clearFanProcessingData();
        var outputs = results.stream().map(stack -> {
            var output = input.copy(); output.stack = stack; output.locked = false; return output;
        }).toList();
        handler.handleProcessingOnItem(input, TransportedResult.convertToAndLeaveHeld(outputs, remaining[0]));
        notifyUpdate();
    }
}
