package io.github.boosterproject.booster.content.kinetics.fan;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NetheriteEncasedFanBlock extends EncasedFanBlock {
    public NetheriteEncasedFanBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends EncasedFanBlockEntity> getBlockEntityType() {
        return BoosterBlockEntityTypes.NETHERITE_ENCASED_FAN.get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (AllItems.WRENCH.isIn(player.getMainHandItem())) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof NetheriteEncasedFanBlockEntity fan)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(fan, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
