package io.github.boosterproject.booster.registry;

import com.simibubi.create.content.logistics.vault.ItemVaultItem;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerItem;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmItem;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlockItem;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlockItem;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteFilterItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BoosterItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Booster.MOD_ID);

    public static final DeferredItem<Item> POWERFUL_MECHANICAL_PUMP =
        ITEMS.register("powerful_mechanical_pump", () -> new PowerfulMechanicalPumpBlockItem(
            BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get(),
            new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_SHEET =
        ITEMS.register("netherite_sheet", () -> new Item(new Item.Properties().fireResistant()));

    public static final DeferredItem<Item> NETHERITE_FLUID_TANK =
        ITEMS.register("netherite_fluid_tank", () -> new NetheriteFluidTankBlockItem(
            BoosterBlocks.NETHERITE_FLUID_TANK.get(),
            new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_STEAM_ENGINE =
        ITEMS.register("netherite_steam_engine", () -> new BlockItem(
            BoosterBlocks.NETHERITE_STEAM_ENGINE.get(),
            new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_ITEM_VAULT =
        ITEMS.register("netherite_item_vault", () -> new ItemVaultItem(
            BoosterBlocks.NETHERITE_ITEM_VAULT.get(),
            new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_ELEVATOR_PULLEY =
        ITEMS.register("netherite_elevator_pulley", () -> new BlockItem(
            BoosterBlocks.NETHERITE_ELEVATOR_PULLEY.get(),
            new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_MECHANICAL_PRESS =
        ITEMS.register("netherite_mechanical_press", () -> new BlockItem(
            BoosterBlocks.NETHERITE_MECHANICAL_PRESS.get(),
            new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_BLAZE_BURNER =
        ITEMS.register("netherite_blaze_burner", () -> new NetheriteBlazeBurnerItem(
            BoosterBlocks.NETHERITE_BLAZE_BURNER.get(),
            new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_ENCASED_FAN =
        ITEMS.register("netherite_encased_fan", () -> new BlockItem(
            BoosterBlocks.NETHERITE_ENCASED_FAN.get(), new Item.Properties().fireResistant()
        ));

    public static final DeferredItem<Item> NETHERITE_FILTER =
        ITEMS.register("netherite_filter", () -> new NetheriteFilterItem(new Item.Properties().fireResistant()));

    public static final DeferredItem<Item> NETHERITE_MECHANICAL_ARM =
        ITEMS.register("netherite_mechanical_arm", () -> new ArmItem(
            BoosterBlocks.NETHERITE_MECHANICAL_ARM.get(), new Item.Properties().fireResistant()
        ));

    private BoosterItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
