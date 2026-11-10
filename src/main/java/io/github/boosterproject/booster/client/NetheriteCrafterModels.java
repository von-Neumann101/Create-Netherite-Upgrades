package io.github.boosterproject.booster.client;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.boosterproject.booster.Booster;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.minecraft.resources.ResourceLocation;

public final class NetheriteCrafterModels {
    private static PartialModel model(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_mechanical_crafter/" + name));
    }
    private static final PartialModel ARROW = model("arrow"), LID = model("lid"), BELT = model("belt_animated"), FRAME = model("belt");
    private static final ResourceLocation THINGIES = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "block/netherite_crafter_thingies");
    public static final SpriteShiftEntry BELT_SHIFT = SpriteShifter.get(THINGIES, THINGIES);

    public static PartialModel skin(PartialModel original) {
        if (original == AllPartialModels.MECHANICAL_CRAFTER_ARROW) return ARROW;
        if (original == AllPartialModels.MECHANICAL_CRAFTER_LID) return LID;
        if (original == AllPartialModels.MECHANICAL_CRAFTER_BELT) return BELT;
        if (original == AllPartialModels.MECHANICAL_CRAFTER_BELT_FRAME) return FRAME;
        return original;
    }
    public static void init() {}
    private NetheriteCrafterModels() {}
}
