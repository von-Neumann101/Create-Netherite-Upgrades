package io.github.boosterproject.booster;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.stress.BlockStressValues;
import io.github.boosterproject.booster.client.BoosterClient;
import io.github.boosterproject.booster.config.BoosterConfigs;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterCreativeModeTabs;
import io.github.boosterproject.booster.registry.BoosterItems;
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
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(
            BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get(),
            () -> BoosterConfigs.SERVER.powerfulPumpStressImpact.get()
        ));
    }
}