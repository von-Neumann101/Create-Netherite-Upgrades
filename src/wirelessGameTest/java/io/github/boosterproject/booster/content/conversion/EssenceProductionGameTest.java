package io.github.boosterproject.booster.content.conversion;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public class EssenceProductionGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 30)
    public static void threeIngredientsRequireUpgradedBasinAndFourFueledBlazes(GameTestHelper h) {
        var recipe = recipe(h);
        h.assertTrue(recipe.getIngredients().size() == 3 && recipe.getFluidIngredients().isEmpty()
            && recipe.getFluidResults().size() == 1 && recipe.getFluidResults().getFirst().getAmount() == 1000,
            "three dry ingredients produce1000mB, with no lava ingredient");
        var invalid = new BasinBlockEntity[] {
            fixture(h, 140, false, true, 4, true), fixture(h, 160, true, false, 1, true),
            fixture(h, 180, true, true, 3, true), fixture(h, 200, true, true, 4, false)
        };
        for (var basin : invalid) {
            h.assertTrue(!BasinRecipe.match(basin, recipe) && !BasinRecipe.apply(basin, recipe), "wrong basin, ordinary burner, three blazes or no fuel cannot process essence");
            h.assertTrue(totalInput(basin) == 3 && essence(h, basin) == 0, "rejected conditions consume neither ingredients nor fluid");
        }
        var valid = fixture(h, 220, true, true, 4, true);
        h.assertTrue(BasinRecipe.match(valid, recipe) && totalInput(valid) == 3, "valid matching is simulated without consuming inputs");
        h.assertTrue(BasinRecipe.apply(valid, recipe) && valid.inputInventory.isEmpty() && essence(h, valid) == 1000,
            "one debris, wart and powder are consumed exactly once for1000mB essence");
        h.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 600)
    public static void realMixerRechecksFourBlazesBeforeApplying(GameTestHelper h) {
        var basin = fixture(h, 210, true, true, 4, true);
        var level = h.getLevel();
        BlockPos pos = basin.getBlockPos().above(2), cog = pos.west(), motor = cog.above(2);
        level.setBlockAndUpdate(pos, BoosterBlocks.NETHERITE_MECHANICAL_MIXER.get().defaultBlockState());
        level.setBlockAndUpdate(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        level.setBlockAndUpdate(cog.above(), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        level.setBlockAndUpdate(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        ((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(64);
        var mixer = (MechanicalMixerBlockEntity) level.getBlockEntity(pos);
        var burner = (NetheriteBlazeBurnerBlockEntity) level.getBlockEntity(basin.getBlockPos().below());
        boolean[] changed = {false};
        h.onEachTick(() -> {
            if (!changed[0] && mixer.running) { burner.setBlazeCount(3); changed[0] = true; }
        });
        h.runAfterDelay(250, () -> {
            h.assertTrue(changed[0] && totalInput(basin) == 3 && essence(h, basin) == 0, "losing the fourth blaze during the cycle cannot consume the recipe");
            burner.setBlazeCount(4); basin.notifyChangeOfContents();
        });
        h.succeedWhen(() -> h.assertTrue(changed[0] && basin.inputInventory.isEmpty() && essence(h, basin) == 1000, "restored four-blaze heating completes the actual mixer recipe"));
    }

    private static BasinRecipe recipe(GameTestHelper h) {
        return (BasinRecipe) h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "mixing/nether_essence")).orElseThrow().value();
    }
    private static BasinBlockEntity fixture(GameTestHelper h, int height, boolean upgradedBasin, boolean upgradedBurner, int blazes, boolean fuel) {
        var level = h.getLevel(); BlockPos pos = h.absolutePos(new BlockPos(1, height, 1));
        level.setBlockAndUpdate(pos, (upgradedBasin ? BoosterBlocks.NETHERITE_BASIN.get() : AllBlocks.BASIN.get()).defaultBlockState());
        level.setBlockAndUpdate(pos.below(), (upgradedBurner ? BoosterBlocks.NETHERITE_BLAZE_BURNER.get() : AllBlocks.BLAZE_BURNER.get()).defaultBlockState()
            .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
        if (upgradedBurner) ((NetheriteBlazeBurnerBlockEntity) level.getBlockEntity(pos.below())).setBlazeCount(blazes);
        if (fuel) h.assertTrue(BlazeBurnerBlock.tryInsert(level.getBlockState(pos.below()), level, pos.below(), new ItemStack(Items.COAL), false, false, false).getResult().consumesAction(), "real coal fuels the burner");
        var basin = (BasinBlockEntity) level.getBlockEntity(pos);
        int start = basin.inputInventory.getSlots() - 3;
        basin.inputInventory.setStackInSlot(start, new ItemStack(Items.ANCIENT_DEBRIS));
        basin.inputInventory.setStackInSlot(start + 1, new ItemStack(Items.NETHER_WART));
        basin.inputInventory.setStackInSlot(start + 2, new ItemStack(Items.BLAZE_POWDER));
        return basin;
    }
    private static int totalInput(BasinBlockEntity basin) {
        int result = 0;
        for (int slot = 0; slot < basin.inputInventory.getSlots(); slot++) result += basin.inputInventory.getItem(slot).getCount();
        return result;
    }
    private static int essence(GameTestHelper h, BasinBlockEntity basin) {
        var cap = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, basin.getBlockPos(), null); int amount = 0;
        for (int tank = 0; tank < cap.getTanks(); tank++) if (cap.getFluidInTank(tank).is(BoosterFluids.NETHER_ESSENCE.get())) amount += cap.getFluidInTank(tank).getAmount();
        return amount;
    }
}
