package io.github.boosterproject.booster.content.equipment.assembly;

import io.github.boosterproject.booster.Booster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblyTableRegistrationGameTest {
    @GameTest(template = "wireless_empty")
    public static void assemblyTableIsRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "assembly_table")) != Blocks.AIR,
            "assembly table is registered");
        helper.succeed();
    }
}
