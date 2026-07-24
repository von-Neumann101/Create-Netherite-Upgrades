package io.github.boosterproject.booster.registry;

import io.github.boosterproject.booster.Booster;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BoosterCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Booster.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_NETHERITE =
        CREATIVE_MODE_TABS.register("createnetherite", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createnetherite.createnetherite"))
            .icon(() -> new ItemStack(BoosterItems.POWERFUL_MECHANICAL_PUMP.get()))
            .displayItems((parameters, output) -> {
                output.accept(BoosterItems.POWERFUL_MECHANICAL_PUMP.get());
                output.accept(BoosterItems.NETHERITE_FLUID_TANK.get());
                output.accept(BoosterItems.NETHERITE_SHEET.get());
            })
            .build());

    private BoosterCreativeModeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}