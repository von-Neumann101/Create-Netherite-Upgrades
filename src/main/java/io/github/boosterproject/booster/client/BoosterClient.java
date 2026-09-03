package io.github.boosterproject.booster.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyRenderer;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyVisual;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineRenderer;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineVisual;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.content.processing.burner.BlazeBurnerVisual;
import com.simibubi.create.foundation.block.connected.CTModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class BoosterClient {
    private BoosterClient() {
    }

    public static void register(IEventBus modEventBus) {
        BoosterPartialModels.init();
        modEventBus.addListener(BoosterClient::registerRenderers);
        modEventBus.addListener(BoosterClient::clientSetup);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
            .register(new ResourceLocation(Booster.MOD_ID, "powerful_mechanical_pump"), PipeAttachmentModel::withAO);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
            .register(new ResourceLocation(Booster.MOD_ID, "netherite_fluid_tank"), NetheriteFluidTankModel::standard);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
            .register(new ResourceLocation(Booster.MOD_ID, "netherite_item_vault"),
                model -> new CTModel(model, new NetheriteItemVaultCTBehaviour()));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            BoosterBlockEntityTypes.POWERFUL_MECHANICAL_PUMP.get(),
            PowerfulMechanicalPumpRenderer::new
        );
        event.registerBlockEntityRenderer(
            BoosterBlockEntityTypes.NETHERITE_FLUID_TANK.get(),
            NetheriteFluidTankRenderer::new
        );
        event.registerBlockEntityRenderer(
            BoosterBlockEntityTypes.NETHERITE_STEAM_ENGINE.get(),
            SteamEngineRenderer::new
        );
        event.registerBlockEntityRenderer(
            BoosterBlockEntityTypes.NETHERITE_ELEVATOR_PULLEY.get(),
            ElevatorPulleyRenderer::new
        );
        event.registerBlockEntityRenderer(
            BoosterBlockEntityTypes.NETHERITE_BLAZE_BURNER.get(),
            BlazeBurnerRenderer::new
        );
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(BoosterBlocks.NETHERITE_FLUID_TANK.get(), RenderType.cutoutMipped());
            SimpleBlockEntityVisualizer
                .builder(BoosterBlockEntityTypes.POWERFUL_MECHANICAL_PUMP.get())
                .factory(PowerfulMechanicalPumpVisual::new)
                .apply();
            SimpleBlockEntityVisualizer
                .builder(BoosterBlockEntityTypes.NETHERITE_STEAM_ENGINE.get())
                .factory(SteamEngineVisual::new)
                .apply();
            SimpleBlockEntityVisualizer
                .builder(BoosterBlockEntityTypes.NETHERITE_ELEVATOR_PULLEY.get())
                .factory(ElevatorPulleyVisual::new)
                .apply();
            SimpleBlockEntityVisualizer
                .builder(BoosterBlockEntityTypes.NETHERITE_BLAZE_BURNER.get())
                .factory(BlazeBurnerVisual::new)
                .apply();
        });
    }
}
