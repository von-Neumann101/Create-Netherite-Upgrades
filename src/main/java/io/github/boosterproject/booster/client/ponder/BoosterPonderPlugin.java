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
        helper.forComponents(BoosterItems.NETHERITE_CONVERTER.getId())
            .addStoryBoard("converter/tutorial", NetheriteConverterScenes::value)
            .addStoryBoard("converter/tutorial", NetheriteConverterScenes::convert)
            .addStoryBoard("converter/tutorial", NetheriteConverterScenes::wrenches);
        helper.forComponents(BoosterItems.NETHERITE_ENCASED_FAN.getId())
            .addStoryBoard("fan/tutorial", NetheriteFanScenes::modes)
            .addStoryBoard("fan/tutorial", NetheriteFanScenes::filter);
        helper.forComponents(BoosterItems.NETHERITE_WEIGHTED_EJECTOR.getId())
            .addStoryBoard("ejector/tutorial", NetheriteEjectorScenes::launch)
            .addStoryBoard("ejector/tutorial", NetheriteEjectorScenes::controls);
        helper.forComponents(BoosterItems.NETHERITE_EXTENDO_GRIP.getId())
            .addStoryBoard("extendo_grip/tutorial", NetheriteExtendoGripScenes::bucket)
            .addStoryBoard("extendo_grip/tutorial", NetheriteExtendoGripScenes::power);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("converter_value_sheet", "Right-click the converter with a Netherite Sheet. The sheet immediately becomes 16 Netherite Value.");
        helper.registerSharedText("converter_value_read", "Right-click with an empty hand or wear Engineer's Goggles to check the stored value.");
        helper.registerSharedText("converter_value_template", "A vanilla Netherite Upgrade Smithing Template can also be deposited. Each one adds 1 value.");
        helper.registerSharedText("converter_value_stored", "Deposit more sheets to accumulate value. Redeemed sheets cannot be taken back out.");
        helper.registerSharedText("converter_layout", "Place the converter two blocks above a depot or belt. Put the item to upgrade directly below it.");
        helper.registerSharedText("converter_power", "Connect at least 2048 SU through the side shaft, then deposit a sheet for Netherite Value.");
        helper.registerSharedText("converter_fluid", "Add Nether Essence with a bucket or a pipe. This bucket adds 1000 mB.");
        helper.registerSharedText("converter_process", "Put a regular machine below the converter and keep power connected for 5 seconds. Each upgrade costs 1 value and 250 mB essence.");
        helper.registerSharedText("converter_collect", "Right-click empty-handed to collect the result. Put in another item to continue converting.");
        helper.registerSharedText("converter_wrench_craft", "Craft a Create Wrench with one Netherite Sheet in any arrangement to make a Netherite Wrench.");
        helper.registerSharedText("converter_wrench_supply", "Connect at least 2048 SU, deposit two sheets one at a time, then add Nether Essence.");
        helper.registerSharedText("converter_wrench_basic", "Convert a Create Wrench for 5 seconds to make a Netherite Wrench. This uses 1 value and 250 mB essence.");
        helper.registerSharedText("converter_wrench_advanced", "Put a Netherite Wrench below the converter for another 5 seconds. It becomes a Netherite Netherite Wrench, using 16 value and 250 mB essence.");
        helper.registerSharedText("grip_offhand", "In Survival, put the Netherite Extendo Grip in your offhand.");
        helper.registerSharedText("grip_bucket", "Hold an empty bucket in your main hand and right-click a fluid tank.");
        helper.registerSharedText("grip_filled", "One bucket of fluid leaves the tank and fills your bucket.");
        helper.registerSharedText("grip_repeat", "The tank needs at least one bucket of the fluid. Use another empty bucket to collect more.");
        helper.registerSharedText("grip_resources", "Successful bucket fills and sustained hand power use resources. Equipped backtank air is used first, then grip durability when no air is available.");
        helper.registerSharedText("grip_mainhand", "To power a working block, move the Netherite Extendo Grip to your main hand.");
        helper.registerSharedText("grip_limits", "Choose an idle working block without external power and within the grip's capacity. One Powerful Mechanical Pump can run this way.");
        helper.registerSharedText("grip_hold", "Keep aiming at the working block and hold right-click. This pump transfers water between the tanks.");
        helper.registerSharedText("grip_single", "Hand power applies only to the clicked block. Connected shafts and gears do not power other machines.");
        helper.registerSharedText("grip_stop", "Release right-click, look away or switch items to stop supplying power.");
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
