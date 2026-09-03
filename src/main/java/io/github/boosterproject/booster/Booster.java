package io.github.boosterproject.booster;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour;
import io.github.boosterproject.booster.client.BoosterClient;
import io.github.boosterproject.booster.config.BoosterConfigs;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerBlock;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterCreativeModeTabs;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Booster.MOD_ID)
public class Booster {
    public static final String MOD_ID = "createnetherite";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Booster(IEventBus modEventBus, ModContainer modContainer) {
        BoosterConfigs.register(modContainer);
        BoosterBlocks.register(modEventBus);
        BoosterItems.register(modEventBus);
        BoosterCreativeModeTabs.register(modEventBus);
        BoosterBlockEntityTypes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BoosterClient.register(modEventBus);
        }

        LOGGER.info("Create Netherite loaded");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(
                BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get(),
                () -> BoosterConfigs.SERVER.powerfulPumpStressImpact.get()
            );
            BlockStressValues.IMPACTS.register(
                BoosterBlocks.NETHERITE_ELEVATOR_PULLEY.get(),
                () -> BlockStressValues.getImpact(AllBlocks.ELEVATOR_PULLEY.get())
            );
            BlockStressValues.IMPACTS.register(
                BoosterBlocks.NETHERITE_MECHANICAL_PRESS.get(),
                () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get())
            );

            Block netheriteSteamEngine = BoosterBlocks.NETHERITE_STEAM_ENGINE.get();
            BlockStressValues.CAPACITIES.register(netheriteSteamEngine, () -> 2048.0D);
            BlockStressValues.setGeneratorSpeed(64, true).accept(netheriteSteamEngine);
            MountedItemStorageType.REGISTRY.register(
                BoosterBlocks.NETHERITE_ITEM_VAULT.get(),
                AllMountedStorageTypes.VAULT.get()
            );

            Block netheriteBlazeBurner = BoosterBlocks.NETHERITE_BLAZE_BURNER.get();
            BoilerHeater.REGISTRY.register(netheriteBlazeBurner,
                (level, pos, state) -> NetheriteBlazeBurnerBlock.HEAT_MULTIPLIER
                    * BoilerHeater.BLAZE_BURNER.getHeat(level, pos, state));
            MovementBehaviour.REGISTRY.register(netheriteBlazeBurner, new BlazeBurnerMovementBehaviour());
        });
    }
}
