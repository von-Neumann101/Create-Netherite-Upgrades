package io.github.boosterproject.booster.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyRenderer;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyVisual;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineRenderer;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineVisual;
import com.simibubi.create.content.kinetics.press.MechanicalPressRenderer;
import com.simibubi.create.content.kinetics.press.PressVisual;
import com.simibubi.create.content.kinetics.fan.EncasedFanRenderer;
import com.simibubi.create.content.kinetics.fan.FanVisual;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer;
import com.simibubi.create.content.kinetics.mixer.MixerVisual;
import com.simibubi.create.foundation.block.connected.CTModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerData;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class BoosterClient {
    private BoosterClient() {
    }

    public static void register(IEventBus modEventBus) {
        BoosterPartialModels.init();
        NetheriteArmModels.init();
        modEventBus.addListener(BoosterClient::registerRenderers);
        modEventBus.addListener(BoosterClient::clientSetup);
        modEventBus.addListener(BoosterClient::registerScreens);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
            .register(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "powerful_mechanical_pump"), PipeAttachmentModel::withAO);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
            .register(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_fluid_tank"), NetheriteFluidTankModel::standard);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
            .register(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_item_vault"),
                model -> new CTModel(model, new NetheriteItemVaultCTBehaviour()));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BoosterBlockEntityTypes.NETHERITE_ENCASED_FAN.get(), EncasedFanRenderer::new);
        event.registerBlockEntityRenderer(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_ARM.get(), ArmRenderer::new);
        event.registerBlockEntityRenderer(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_MIXER.get(), MechanicalMixerRenderer::new);
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
            BoosterBlockEntityTypes.NETHERITE_MECHANICAL_PRESS.get(),
            MechanicalPressRenderer::new
        );
        event.registerBlockEntityRenderer(
            BoosterBlockEntityTypes.NETHERITE_BLAZE_BURNER.get(),
            NetheriteBlazeBurnerRenderer::new
        );
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BoosterMenuTypes.NETHERITE_FILTER.get(), NetheriteFilterScreen::new);
        event.register(BoosterMenuTypes.NETHERITE_FAN.get(), NetheriteFanScreen::new);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SimpleBlockEntityVisualizer.builder(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_MIXER.get())
                .factory(MixerVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_ARM.get())
                .factory(ArmVisual::new).apply();
            ItemProperties.register(BoosterItems.NETHERITE_BLAZE_BURNER.get(),
                ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "empty"),
                (stack, level, entity, seed) -> NetheriteBlazeBurnerData.getCount(stack) == 0 ? 1 : 0);
            SimpleBlockEntityVisualizer.builder(BoosterBlockEntityTypes.NETHERITE_ENCASED_FAN.get())
                .factory(FanVisual::new).apply();
            ItemBlockRenderTypes.setRenderLayer(BoosterBlocks.NETHERITE_FLUID_TANK.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(BoosterBlocks.NETHERITE_MECHANICAL_MIXER.get(), RenderType.cutoutMipped());
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
                .builder(BoosterBlockEntityTypes.NETHERITE_MECHANICAL_PRESS.get())
                .factory(PressVisual::new)
                .apply();
        });
    }
}
