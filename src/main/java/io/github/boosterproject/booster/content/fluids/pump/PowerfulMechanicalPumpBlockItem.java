package io.github.boosterproject.booster.content.fluids.pump;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class PowerfulMechanicalPumpBlockItem extends BlockItem {
    public PowerfulMechanicalPumpBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.createnetherite.powerful_mechanical_pump"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}