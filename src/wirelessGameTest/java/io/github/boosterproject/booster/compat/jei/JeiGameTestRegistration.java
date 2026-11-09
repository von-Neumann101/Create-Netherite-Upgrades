package io.github.boosterproject.booster.compat.jei;

import io.github.boosterproject.booster.Booster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = Booster.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class JeiGameTestRegistration {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("jei")) event.register(JeiRecipeGameTest.class);
    }
}
