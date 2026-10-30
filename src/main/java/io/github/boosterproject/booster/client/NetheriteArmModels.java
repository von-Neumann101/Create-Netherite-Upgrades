package io.github.boosterproject.booster.client;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.Booster;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class NetheriteArmModels {
    private static final Map<PartialModel, PartialModel> MODELS = Map.of(
        AllPartialModels.ARM_COG, model("cog"),
        AllPartialModels.ARM_BASE, model("base"),
        AllPartialModels.ARM_LOWER_BODY, model("lower_body"),
        AllPartialModels.ARM_UPPER_BODY, model("upper_body"),
        AllPartialModels.ARM_CLAW_BASE, model("claw_base"),
        AllPartialModels.ARM_CLAW_BASE_GOGGLES, model("claw_base_goggles"),
        AllPartialModels.ARM_CLAW_GRIP_UPPER, model("upper_claw_grip"),
        AllPartialModels.ARM_CLAW_GRIP_LOWER, model("lower_claw_grip")
    );

    private NetheriteArmModels() {}

    private static PartialModel model(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID,
            "block/netherite_mechanical_arm/" + name));
    }

    public static PartialModel skin(PartialModel original) {
        return MODELS.getOrDefault(original, original);
    }

    public static void init() {
        // Register the partials before model baking.
    }
}
