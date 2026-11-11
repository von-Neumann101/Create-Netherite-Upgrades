package io.github.boosterproject.booster;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.impl.unpacking.BasinUnpackingHandler;
import com.simibubi.create.content.processing.basin.BasinMovementBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerMovingInteraction;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import io.github.boosterproject.booster.client.BoosterClient;
import io.github.boosterproject.booster.config.BoosterConfigs;
import io.github.boosterproject.booster.content.BoosterExplosionProtection;
import io.github.boosterproject.booster.content.logistics.filter.NetheriteNbtFilterNetworking;
import io.github.boosterproject.booster.content.equipment.NetheriteNetheriteItem;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchCombat;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchLaunch;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchNetworking;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchWheelNetworking;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchSaw;
import io.github.boosterproject.booster.content.kinetics.saw.NetheriteSawMovementBehaviour;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerBlock;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerMovementBehaviour;
import io.github.boosterproject.booster.content.kinetics.mechanicalArm.NetheriteArmBehavior;
import io.github.boosterproject.booster.content.redstone.wireless.WirelessNetworking;
import io.github.boosterproject.booster.content.redstone.wireless.WirelessTerminalService;
import io.github.boosterproject.booster.registry.BoosterBlockEntityTypes;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterCreativeModeTabs;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import io.github.boosterproject.booster.registry.BoosterArmInteractionPointTypes;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import org.slf4j.Logger;

@Mod(Booster.MOD_ID)
public class Booster {
    public static final String MOD_ID = "createnetherite";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Booster(IEventBus modEventBus, ModContainer modContainer) {
        BoosterConfigs.register(modContainer);
        BoosterFluids.register(modEventBus);
        BoosterRecipeTypes.register(modEventBus);
        BoosterBlocks.register(modEventBus);
        BoosterItems.register(modEventBus);
        BoosterCreativeModeTabs.register(modEventBus);
        BoosterBlockEntityTypes.register(modEventBus);
        BoosterMenuTypes.register(modEventBus);
        BoosterArmInteractionPointTypes.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(BoosterExplosionProtection::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(BoosterExplosionProtection::onEntityInvulnerabilityCheck);
        NeoForge.EVENT_BUS.addListener(NetheriteNetheriteItem::keepChewing);
        NeoForge.EVENT_BUS.addListener(io.github.boosterproject.booster.content.kinetics.portable.PortableStressService::tick);
        NeoForge.EVENT_BUS.addListener(WirelessTerminalService::tick);
        modEventBus.addListener(WirelessNetworking::register);
        modEventBus.addListener(WrenchNetworking::register);
        modEventBus.addListener(WrenchWheelNetworking::register);
        modEventBus.addListener(NetheriteNbtFilterNetworking::register);
        NeoForge.EVENT_BUS.addListener(WrenchWheelNetworking::logout);
        NeoForge.EVENT_BUS.addListener(WrenchWheelNetworking::clone);
        WrenchCombat.register(NeoForge.EVENT_BUS);
        WrenchSaw.register(NeoForge.EVENT_BUS);
        WrenchLaunch.register(NeoForge.EVENT_BUS);
        modEventBus.addListener((RegisterTicketControllersEvent event) -> event.register(WirelessTerminalService.TICKETS));

        modEventBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BoosterClient.register(modEventBus);
        }

        LOGGER.info("Create Netherite loaded");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) -> state.is(BoosterBlocks.NETHERITE_PORTABLE_STRESS_DEVICE.get())
                ? BlockMovementChecks.CheckResult.FAIL : BlockMovementChecks.CheckResult.PASS);
            BlockMovementChecks.registerNotSupportiveCheck((state, direction) -> state.is(BoosterBlocks.NETHERITE_MECHANICAL_SAW.get())
                ? BlockMovementChecks.CheckResult.of(direction == state.getValue(SawBlock.FACING)) : BlockMovementChecks.CheckResult.PASS);
            BlockStressValues.IMPACTS.register(BoosterBlocks.NETHERITE_MECHANICAL_SAW.get(), () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_SAW.get()));
            MovementBehaviour.REGISTRY.register(BoosterBlocks.NETHERITE_BASIN.get(), new BasinMovementBehaviour());
            UnpackingHandler.REGISTRY.register(BoosterBlocks.NETHERITE_BASIN.get(), BasinUnpackingHandler.INSTANCE);
            BlockStressValues.IMPACTS.register(BoosterBlocks.NETHERITE_MECHANICAL_CRAFTER.get(), () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_CRAFTER.get()));
            MovementBehaviour.REGISTRY.register(BoosterBlocks.NETHERITE_MECHANICAL_SAW.get(), new NetheriteSawMovementBehaviour());
            BlockStressValues.CAPACITIES.register(BoosterBlocks.NETHERITE_PORTABLE_STRESS_DEVICE.get(), () -> 32D);
            BlockStressValues.setGeneratorSpeed(64, true).accept(BoosterBlocks.NETHERITE_PORTABLE_STRESS_DEVICE.get());
            BlockStressValues.IMPACTS.register(BoosterBlocks.NETHERITE_CONVERTER.get(), () -> 2048D);
            BlockStressValues.IMPACTS.register(BoosterBlocks.NETHERITE_DEPLOYER.get(), () -> BlockStressValues.getImpact(AllBlocks.DEPLOYER.get()));
            MovementBehaviour.REGISTRY.register(BoosterBlocks.NETHERITE_DEPLOYER.get(), new DeployerMovementBehaviour());
            MovingInteractionBehaviour.REGISTRY.register(BoosterBlocks.NETHERITE_DEPLOYER.get(), new DeployerMovingInteraction());
            BlockStressValues.IMPACTS.register(
                BoosterBlocks.NETHERITE_WEIGHTED_EJECTOR.get(),
                () -> BlockStressValues.getImpact(AllBlocks.WEIGHTED_EJECTOR.get())
            );
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
            BlockStressValues.IMPACTS.register(
                BoosterBlocks.NETHERITE_MECHANICAL_MIXER.get(),
                () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_MIXER.get())
            );

            BlockStressValues.IMPACTS.register(BoosterBlocks.NETHERITE_MECHANICAL_ARM.get(),
                () -> NetheriteArmBehavior.stressImpact(BlockStressValues.getImpact(AllBlocks.MECHANICAL_ARM.get())));

            Block netheriteSteamEngine = BoosterBlocks.NETHERITE_STEAM_ENGINE.get();
            BlockStressValues.IMPACTS.register(
                BoosterBlocks.NETHERITE_ENCASED_FAN.get(),
                () -> BlockStressValues.getImpact(AllBlocks.ENCASED_FAN.get())
            );
            BlockStressValues.CAPACITIES.register(netheriteSteamEngine,
                () -> BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get()) * 256.0D);
            BlockStressValues.setGeneratorSpeed(64, true).accept(netheriteSteamEngine);
            MountedItemStorageType.REGISTRY.register(
                BoosterBlocks.NETHERITE_ITEM_VAULT.get(),
                AllMountedStorageTypes.VAULT.get()
            );

            Block netheriteBlazeBurner = BoosterBlocks.NETHERITE_BLAZE_BURNER.get();
            BoilerHeater.REGISTRY.register(netheriteBlazeBurner, NetheriteBlazeBurnerBlock::getBoilerHeat);
            MovementBehaviour.REGISTRY.register(netheriteBlazeBurner, new NetheriteBlazeBurnerMovementBehaviour());
        });
    }
}
