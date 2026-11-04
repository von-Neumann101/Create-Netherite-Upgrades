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
        helper.forComponents(BoosterItems.NETHERITE_WEIGHTED_EJECTOR.getId())
            .addStoryBoard("ejector/tutorial", NetheriteEjectorScenes::launch)
            .addStoryBoard("ejector/tutorial", NetheriteEjectorScenes::controls);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("ejector_select", "Hold a Netherite Weighted Ejector, then sneak and right-click the target block.");
        helper.registerSharedText("ejector_diagonal", "The target may be diagonal, above or below the ejector. Check the previewed path.");
        helper.registerSharedText("ejector_place", "Place the ejector normally after selecting a target. The platform stays flat.");
        helper.registerSharedText("ejector_power", "Connect rotational power to the shaft so the ejector can charge.");
        helper.registerSharedText("ejector_insert", "Put items on the charged platform to launch them automatically.");
        helper.registerSharedText("ejector_arrive", "A targeted depot receives the items. Keep the flight path clear of solid blocks.");
        helper.registerSharedText("ejector_reselect", "To change the route, select a new target with sneak-right-click while holding an ejector.");
        helper.registerSharedText("ejector_apply", "Wait for flying items to arrive, then right-click the existing ejector with the selected target. The held ejector is not consumed.");
        helper.registerSharedText("ejector_quantity", "Use the side value panel to set the required stack size. Here it is set to four.");
        helper.registerSharedText("ejector_wait", "Two items are not enough, so the platform waits.");
        helper.registerSharedText("ejector_full", "Adding two more reaches four and triggers one launch.");
        helper.registerSharedText("ejector_pause", "A redstone signal pauses launching, even when the required number of items is present.");
        helper.registerSharedText("ejector_resume", "Turn the redstone signal off to resume launching.");
        helper.registerSharedText("fan_power", "Connect rotational power to the shaft at the back of the fan.");
        helper.registerSharedText("fan_no_media", "Right-click the fan to choose a processing mode. No water, lava or fire block is needed in front of it.");
        helper.registerSharedText("fan_washing", "Washing: coloured wool becomes white wool.");
        helper.registerSharedText("fan_blasting", "Blasting: raw iron becomes an iron ingot.");
        helper.registerSharedText("fan_haunting", "Haunting: sand becomes soul sand.");
        helper.registerSharedText("fan_smoking", "Smoking: raw beef becomes steak.");
        helper.registerSharedText("fan_filter_open", "Configure original Create filters by holding them and right-clicking.");
        helper.registerSharedText("fan_filter_keys", "The fan has one filter slot each for Washing, Blasting, Haunting and Smoking.");
        helper.registerSharedText("fan_filter_samples", "Each slot holds one real original filter. Remove it to edit its list.");
        helper.registerSharedText("fan_filter_install", "Right-click the fan and install each configured filter below its matching mode.");
        helper.registerSharedText("fan_filter_override", "Listed items follow their filter rules, even in the same air current.");
        helper.registerSharedText("fan_filter_fallback", "Unlisted items use the fan's selected mode. This beef is smoked.");
        helper.registerSharedText("fan_filter_switch", "The mode buttons change unmatched-item processing; installed filters keep their assigned modes.");
        helper.registerSharedText("fan_filter_result", "Unlisted blue wool now uses Washing; the listed raw iron is still blasted.");
        helper.registerSharedText("fan_transport", "Place items on a depot or belt in the air current, or drop them into the airflow.");
    }
}
