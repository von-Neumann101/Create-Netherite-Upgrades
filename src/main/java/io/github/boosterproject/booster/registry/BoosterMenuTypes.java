package io.github.boosterproject.booster.registry;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.kinetics.fan.NetheriteFanMenu;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteFilterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BoosterMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Booster.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<NetheriteFilterMenu>> NETHERITE_FILTER =
        MENU_TYPES.register("netherite_filter", () -> IMenuTypeExtension.create(NetheriteFilterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NetheriteFanMenu>> NETHERITE_FAN =
        MENU_TYPES.register("netherite_fan", () -> IMenuTypeExtension.create(NetheriteFanMenu::new));

    private BoosterMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
