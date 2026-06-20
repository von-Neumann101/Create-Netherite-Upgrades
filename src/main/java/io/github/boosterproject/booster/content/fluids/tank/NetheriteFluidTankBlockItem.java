package io.github.boosterproject.booster.content.fluids.tank;

import java.util.List;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

public class NetheriteFluidTankBlockItem extends BlockItem {
    public NetheriteFluidTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
            "tooltip.createnetherite.netherite_fluid_tank",
            NetheriteFluidTankBlockEntity.getConfiguredCapacityMultiplier()
        ));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult initialResult = super.place(context);
        if (initialResult.consumesAction()) {
            tryMultiPlace(context);
        }
        return initialResult;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack,
                                                BlockState state) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }

        CompoundTag nbt = stack.getTagElement("BlockEntityTag");
        if (nbt != null) {
            nbt.remove("Luminosity");
            nbt.remove("Size");
            nbt.remove("Height");
            nbt.remove("Controller");
            nbt.remove("LastKnownPos");
            if (nbt.contains("TankContent")) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt.getCompound("TankContent"));
                if (!fluid.isEmpty()) {
                    fluid.setAmount(Math.min(
                        NetheriteFluidTankBlockEntity.getNetheriteCapacityMultiplierValue(),
                        fluid.getAmount()
                    ));
                    nbt.put("TankContent", fluid.writeToNBT(new CompoundTag()));
                }
            }
        }

        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    private void tryMultiPlace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown() || SymmetryWandItem.presentInHotbar(player)) {
            return;
        }

        Direction face = context.getClickedFace();
        if (!face.getAxis().isVertical()) {
            return;
        }

        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = level.getBlockState(placedOnPos);
        if (placedOnState.getBlock() != BoosterBlocks.NETHERITE_FLUID_TANK.get()) {
            return;
        }

        NetheriteFluidTankBlockEntity tankAt = ConnectivityHandler.partAt(
            BoosterBlockEntityTypes.NETHERITE_FLUID_TANK.get(),
            level,
            placedOnPos
        );
        if (tankAt == null) {
            return;
        }

        NetheriteFluidTankBlockEntity controller = (NetheriteFluidTankBlockEntity) tankAt.getControllerBE();
        if (controller == null) {
            return;
        }

        int width = controller.getWidth();
        if (width == 1) {
            return;
        }

        int height = controller.getHeight();
        BlockPos startPos = face == Direction.DOWN
            ? controller.getBlockPos().below()
            : controller.getBlockPos().above(height);
        if (startPos.getY() != pos.getY()) {
            return;
        }

        int tanksToPlace = 0;
        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (blockState.getBlock() == BoosterBlocks.NETHERITE_FLUID_TANK.get()) {
                    continue;
                }
                if (!blockState.canBeReplaced()) {
                    return;
                }
                tanksToPlace++;
            }
        }

        if (!player.isCreative() && stack.getCount() < tanksToPlace) {
            return;
        }

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (blockState.getBlock() == BoosterBlocks.NETHERITE_FLUID_TANK.get()) {
                    continue;
                }

                BlockPlaceContext layerContext = BlockPlaceContext.at(context, offsetPos, face);
                player.getPersistentData().putBoolean("SilenceTankSound", true);
                super.place(layerContext);
                player.getPersistentData().remove("SilenceTankSound");
            }
        }
    }
}
