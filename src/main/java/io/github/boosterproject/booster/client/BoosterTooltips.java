package io.github.boosterproject.booster.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlockEntity;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerData;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class BoosterTooltips {
    private BoosterTooltips() {
    }

    public static void register() {
        // The fan and filter already have native Ponder entries.
        for (Item item : List.of(
            BoosterItems.POWERFUL_MECHANICAL_PUMP.get(),
            BoosterItems.NETHERITE_SHEET.get(),
            BoosterItems.NETHERITE_STEAM_ENGINE.get(),
            BoosterItems.NETHERITE_ITEM_VAULT.get(),
            BoosterItems.NETHERITE_ELEVATOR_PULLEY.get(),
            BoosterItems.NETHERITE_MECHANICAL_PRESS.get(),
            BoosterItems.NETHERITE_MECHANICAL_ARM.get(),
            BoosterItems.NETHERITE_MECHANICAL_MIXER.get()
        )) {
            TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, Palette.STANDARD_CREATE));
        }

        registerWithDetails(BoosterItems.NETHERITE_FLUID_TANK.get(), stack -> List.of(Component.translatable(
            "tooltip.createnetherite.netherite_fluid_tank",
            NetheriteFluidTankBlockEntity.getConfiguredCapacityMultiplier()
        )));
        registerWithDetails(BoosterItems.NETHERITE_BLAZE_BURNER.get(), stack -> {
            int count = NetheriteBlazeBurnerData.getCount(stack);
            return List.of(
                Component.translatable("tooltip.createnetherite.netherite_blaze_burner.count", count),
                Component.translatable("tooltip.createnetherite.netherite_blaze_burner.rates", count, count / 2F)
            );
        });
    }

    private static void registerWithDetails(Item item, Function<ItemStack, List<Component>> details) {
        TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, Palette.STANDARD_CREATE) {
            @Override
            public void modify(ItemTooltipEvent context) {
                super.modify(context);
                if (description == null || !Screen.hasShiftDown()) {
                    return;
                }
                List<Component> lines = new ArrayList<>();
                for (Component detail : details.apply(context.getItemStack())) {
                    lines.addAll(TooltipHelper.cutTextComponent(detail, Palette.STANDARD_CREATE));
                }
                context.getToolTip().addAll(1 + description.getCurrentLines().size(), lines);
            }
        });
    }
}
