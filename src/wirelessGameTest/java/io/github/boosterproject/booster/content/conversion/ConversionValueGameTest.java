package io.github.boosterproject.booster.content.conversion;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.registry.BoosterItems;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConversionValueGameTest {
    @GameTest(template = "wireless_empty")
    public static void noItemCapabilityOrMaterialDepositRemains(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        var machine = (NetheriteConverterBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null, "converter has no unsided material deposit inventory");
        for (Direction side : Direction.values()) helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) == null,
            "converter has no sided material deposit inventory: " + side);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "EssenceDepositQA"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(pos.getCenter().add(0, 0, 2));
        for (var material : new net.minecraft.world.item.Item[] {BoosterItems.NETHERITE_SHEET.get(), Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.DIAMOND}) {
            var input = new ItemStack(material, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, input);
            level.getBlockState(pos).useItemOn(input, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false));
            helper.assertTrue(input.getCount() == 2 && player.getMainHandItem() == input && machine.getEssenceTank().isEmpty(),
                "right-click cannot consume or redeem material: " + material);
        }
        var saved = machine.saveWithoutMetadata(level.registryAccess());
        helper.assertTrue(!saved.contains("NetheriteValue") && !saved.contains("Plate"), "material interactions never create legacy point or inventory storage");
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void legacyValueAndPlatesAreIgnoredWhileEssenceReloadsAndCapsAtFourBuckets(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        var machine = (NetheriteConverterBlockEntity) level.getBlockEntity(pos);
        machine.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 2500), FluidAction.EXECUTE);
        var plate = new ItemStack(BoosterItems.NETHERITE_SHEET.get());
        CustomData.update(DataComponents.CUSTOM_DATA, plate, tag -> tag.putInt("NetheriteConversionUses", 7));
        var old = new ItemStackHandler(1); old.setStackInSlot(0, plate);
        var tag = machine.saveWithoutMetadata(level.registryAccess());
        tag.putInt("NetheriteValue", Integer.MAX_VALUE); tag.put("Plate", old.serializeNBT(level.registryAccess()));
        machine.getEssenceTank().drain(4000, FluidAction.EXECUTE);
        machine.loadWithComponents(tag, level.registryAccess());
        helper.assertTrue(machine.getEssenceTank().getFluidAmount() == 2500 && level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null,
            "legacy point and partial-plate data do not create fluid, items or a deposit capability");
        var saved = machine.saveWithoutMetadata(level.registryAccess());
        helper.assertTrue(!saved.contains("NetheriteValue") && !saved.contains("Plate"), "new saves discard obsolete point and plate data");
        machine.loadWithComponents(saved, level.registryAccess());
        machine.loadWithComponents(machine.getUpdateTag(level.registryAccess()), level.registryAccess());
        helper.assertTrue(machine.getEssenceTank().getFluidAmount() == 2500, "save and update-tag reload retain only the actual essence");
        helper.assertTrue(machine.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 2000), FluidAction.SIMULATE) == 1500
            && machine.getEssenceTank().getFluidAmount() == 2500, "essence insertion simulation reports capacity without changing storage");
        helper.assertTrue(machine.getEssenceTank().fill(new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 2000), FluidAction.EXECUTE) == 1500
            && machine.getEssenceTank().getFluidAmount() == 4000, "the converter accepts only the remaining capacity up to four buckets");
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void nativeWrenchPickupAndRealPlacementDoNotCarryLegacyValue(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        var machine = (NetheriteConverterBlockEntity) level.getBlockEntity(pos);
        var legacy = machine.saveWithoutMetadata(level.registryAccess()); legacy.putInt("NetheriteValue", 32);
        machine.loadWithComponents(legacy, level.registryAccess());
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "EssencePickupQA"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(pos.getCenter().add(0, 0, 2)); player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.WRENCH.get()));
        AllItems.WRENCH.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(pos.getCenter(), Direction.UP, pos, false)));
        var item = player.getInventory().items.stream().filter(s -> s.is(BoosterItems.NETHERITE_CONVERTER.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(level.getBlockState(pos).isAir() && item.getCount() == 1
            && player.getInventory().items.stream().filter(s -> s.is(BoosterItems.NETHERITE_CONVERTER.get())).mapToInt(ItemStack::getCount).sum() == 1
            && !item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("NetheriteValue"),
            "native wrench pickup returns one converter without obsolete stored points");
        helper.assertTrue(player.getInventory().items.stream().noneMatch(s -> s.is(BoosterItems.NETHERITE_SHEET.get())), "pickup does not refund legacy sheets");
        // Move the actual returned stack into the hand, then use the normal BlockItem placement path.
        for (int i = 0; i < player.getInventory().items.size(); i++) if (player.getInventory().items.get(i) == item) player.getInventory().items.set(i, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, item); player.setShiftKeyDown(false);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var result = item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.below().getCenter().add(0, .5, 0), Direction.UP, pos.below(), false)));
        helper.assertTrue(result.consumesAction() && item.isEmpty() && level.getBlockEntity(pos) instanceof NetheriteConverterBlockEntity restored
            && restored.getEssenceTank().isEmpty() && level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null,
            "actual survival placement consumes the returned converter and creates a fluid-only machine");
        var saved = ((NetheriteConverterBlockEntity) level.getBlockEntity(pos)).saveWithoutMetadata(level.registryAccess());
        helper.assertTrue(!saved.contains("NetheriteValue") && !saved.contains("Plate"), "placement does not restore legacy point or inventory storage");
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.stream().filter(e -> e.getItem().is(BoosterItems.NETHERITE_CONVERTER.get())).mapToInt(e -> e.getItem().getCount()).sum() == 1
            && drops.stream().noneMatch(e -> e.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("NetheriteValue"))
            && drops.stream().noneMatch(e -> e.getItem().is(BoosterItems.NETHERITE_SHEET.get())), "ordinary breaking drops one converter with no legacy value or material refund");
        level.removeBlock(pos.below(), false);
        helper.succeed();
    }
}
