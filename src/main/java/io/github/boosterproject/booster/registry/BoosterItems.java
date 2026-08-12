package io.github.boosterproject.booster.registry;

import com.simibubi.create.content.logistics.vault.ItemVaultItem;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.fluids.pump.PowerfulMechanicalPumpBlockItem;
import io.github.boosterproject.booster.content.fluids.tank.NetheriteFluidTankBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BoosterItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, Booster.MOD_ID);

    public static final RegistryObject<Item> POWERFUL_MECHANICAL_PUMP =
        ITEMS.register("powerful_mechanical_pump", () -> new PowerfulMechanicalPumpBlockItem(
            BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get(),
            new Item.Properties()
        ));

    public static final RegistryObject<Item> NETHERITE_SHEET =
        ITEMS.register("netherite_sheet", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NETHERITE_FLUID_TANK =
        ITEMS.register("netherite_fluid_tank", () -> new NetheriteFluidTankBlockItem(
            BoosterBlocks.NETHERITE_FLUID_TANK.get(),
            new Item.Properties()
        ));

    public static final RegistryObject<Item> NETHERITE_STEAM_ENGINE =
        ITEMS.register("netherite_steam_engine", () -> new BlockItem(
            BoosterBlocks.NETHERITE_STEAM_ENGINE.get(),
            new Item.Properties()
        ));

    public static final RegistryObject<Item> NETHERITE_ITEM_VAULT =
        ITEMS.register("netherite_item_vault", () -> new ItemVaultItem(
            BoosterBlocks.NETHERITE_ITEM_VAULT.get(),
            new Item.Properties()
        ));

    private BoosterItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
