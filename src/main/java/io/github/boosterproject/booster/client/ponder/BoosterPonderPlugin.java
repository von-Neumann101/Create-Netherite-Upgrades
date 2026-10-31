package io.github.boosterproject.booster.client.ponder;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class BoosterPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Booster.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(BoosterItems.NETHERITE_ENCASED_FAN.getId())
            .addStoryBoard("fan/tutorial", NetheriteFanScenes::modes)
            .addStoryBoard("fan/tutorial", NetheriteFanScenes::filter);
        helper.forComponents(BoosterItems.NETHERITE_FILTER.getId())
            .addStoryBoard("fan/tutorial", NetheriteFanScenes::filter);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("fan_power", "Connect rotational power to the shaft at the back of the fan.");
        helper.registerSharedText("fan_no_media", "Right-click the fan to choose a processing mode. No water, lava or fire block is needed in front of it.");
        helper.registerSharedText("fan_washing", "Washing: coloured wool becomes white wool.");
        helper.registerSharedText("fan_blasting", "Blasting: raw iron becomes an iron ingot.");
        helper.registerSharedText("fan_haunting", "Haunting: sand becomes soul sand.");
        helper.registerSharedText("fan_smoking", "Smoking: raw beef becomes steak.");
        helper.registerSharedText("fan_filter_open", "Hold a Netherite Filter and right-click to edit its four processing groups.");
        helper.registerSharedText("fan_filter_keys", "Drag in samples, or hover an inventory item and press 1–4: Washing, Blasting, Haunting, Smoking.");
        helper.registerSharedText("fan_filter_samples", "Each group holds nine item samples. Samples do not consume real items.");
        helper.registerSharedText("fan_filter_install", "Right-click the fan, then place the configured filter into its filter slot.");
        helper.registerSharedText("fan_filter_override", "Listed items follow their filter rules, even in the same air current.");
        helper.registerSharedText("fan_filter_fallback", "Unlisted items use the fan's selected mode. This beef is smoked.");
        helper.registerSharedText("fan_filter_switch", "You can still change the selected mode with a filter installed. Listed items keep their rules.");
        helper.registerSharedText("fan_filter_result", "Unlisted blue wool now uses Washing; the listed raw iron is still blasted.");
        helper.registerSharedText("fan_transport", "Place items on a depot or belt in the air current, or drop them into the airflow.");
    }
}
