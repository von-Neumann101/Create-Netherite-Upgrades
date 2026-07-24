package io.github.boosterproject.booster.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BoosterConfigs {
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    private BoosterConfigs() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "createnetherite-server.toml");
    }

    public static final class Server {
        public final ModConfigSpec.DoubleValue powerfulPumpStressImpact;
        public final ModConfigSpec.DoubleValue powerfulPumpPressureMultiplier;
        public final ModConfigSpec.IntValue netheriteFluidTankCapacityMultiplier;

        private Server(ModConfigSpec.Builder builder) {
            powerfulPumpStressImpact = builder
                .comment(
                    "Create base stress impact for createnetherite:powerful_mechanical_pump at 1 RPM.",
                    "Create's kinetic network still scales the final stress cost with RPM; Booster does not multiply by speed here.",
                    "Must be greater than 0.",
                    "Restart the world or server after changing this value to guarantee existing kinetic networks are recalculated."
                )
                .defineInRange("powerfulPumpStressImpact", 16.0D, Double.MIN_VALUE, Double.MAX_VALUE);

            powerfulPumpPressureMultiplier = builder
                .comment(
                    "Pressure multiplier applied by createnetherite:powerful_mechanical_pump.",
                    "Effective pressure is abs(speed) multiplied by this value, then clamped by Booster's safety cap.",
                    "Create's FluidNetwork derives transfer speed from pressure, so higher values increase throughput.",
                    "Must be greater than 0.",
                    "Restart the world or server after changing this value to guarantee pipe networks are recalculated."
                )
                .defineInRange("powerfulPumpPressureMultiplier", 8.0D, Double.MIN_VALUE, Double.MAX_VALUE);

            netheriteFluidTankCapacityMultiplier = builder
                .comment(
                    "Capacity multiplier for createnetherite:netherite_fluid_tank compared to one regular Create Fluid Tank block.",
                    "Final capacity is Create's configured single-block Fluid Tank capacity multiplied by this value and by the multiblock block count.",
                    "Must be at least 1.",
                    "Restart the world or server after changing this value to guarantee existing tank capacities are recalculated."
                )
                .defineInRange("netheriteFluidTankCapacityMultiplier", 16, 1, 1024);
        }
    }
}