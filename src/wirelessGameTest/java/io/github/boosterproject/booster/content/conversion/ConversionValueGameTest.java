package io.github.boosterproject.booster.content.conversion;

import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.registry.BoosterBlocks;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConversionValueGameTest {
    @GameTest(template = "wireless_empty")
    public static void platesBecomeStoredValueImmediately(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        var machine = (NetheriteConverterBlockEntity) helper.getLevel().getBlockEntity(pos);
        var input = new ItemStack(BoosterItems.NETHERITE_SHEET.get(), 2);
        helper.assertTrue(machine.getPlateInventory().insertItem(0, input, false).isEmpty(), "deposit accepts both sheets immediately");
        helper.assertTrue(machine.getNetheriteValue() == 32 && input.getCount() == 2 && machine.getPlateInventory().getStackInSlot(0).isEmpty(), "each deposited sheet is worth sixteen; source argument is not mutated");
        helper.assertTrue(machine.getPlateInventory().insertItem(0, input, true).isEmpty() && machine.getNetheriteValue() == 32, "insertion simulation accepts without redeeming value");
        helper.assertTrue(machine.getPlateInventory().extractItem(0, 64, false).isEmpty(), "redeemed value cannot be extracted as a fresh plate");
        var wrong = new ItemStack(Items.DIAMOND);
        helper.assertTrue(machine.getPlateInventory().insertItem(0, wrong, false) == wrong && machine.getNetheriteValue() == 32, "non-catalysts rejected without side effects");
        machine.getPlateInventory().insertItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), false);
        helper.assertTrue(machine.getNetheriteValue() == 33, "existing upgrade templates remain worth one ordinary conversion");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void legacyPlateMigrationAndCapacityDoNotCreateValue(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        var machine = (NetheriteConverterBlockEntity) helper.getLevel().getBlockEntity(pos);
        var plate = new ItemStack(BoosterItems.NETHERITE_SHEET.get());
        CustomData.update(DataComponents.CUSTOM_DATA, plate, tag -> tag.putInt("NetheriteConversionUses", 7));
        var old = new ItemStackHandler(1); old.setStackInSlot(0, plate);
        var tag = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.remove(NetheriteConverterBlockEntity.VALUE_KEY); tag.put("Plate", old.serializeNBT(helper.getLevel().registryAccess()));
        machine.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(machine.getNetheriteValue() == 7 && machine.getPlateInventory().getStackInSlot(0).isEmpty(), "legacy partially used sheet becomes only its seven remaining points");
        var saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getInt(NetheriteConverterBlockEntity.VALUE_KEY) == 7 && !saved.contains("Plate"), "new saves no longer contain a redeemable duplicate legacy inventory");
        machine.loadWithComponents(saved, helper.getLevel().registryAccess());
        machine.loadWithComponents(machine.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(machine.getNetheriteValue() == 7, "save/update-tag reload never redeems the same plate twice");
        machine.getPlateInventory().insertItem(0, plate, false);
        helper.assertTrue(machine.getNetheriteValue() == 14, "legacy partial plates still in player inventory deposit only their remaining value");
        machine.restoreNetheriteValue(Integer.MAX_VALUE - 8);
        var fresh = new ItemStack(BoosterItems.NETHERITE_SHEET.get());
        helper.assertTrue(machine.getPlateInventory().insertItem(0, fresh, false).getCount() == 1 && machine.getNetheriteValue() == Integer.MAX_VALUE - 8, "capacity cannot partially redeem or overflow a sheet");
        var templates = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 10);
        helper.assertTrue(machine.getPlateInventory().insertItem(0, templates, true).getCount() == 2 && machine.getNetheriteValue() == Integer.MAX_VALUE - 8, "partial acceptance simulation does not credit value");
        helper.assertTrue(machine.getPlateInventory().insertItem(0, templates, false).getCount() == 2 && machine.getNetheriteValue() == Integer.MAX_VALUE, "only eight fitting templates are redeemed, with correct remainder");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void wrenchPickupAndRealPlacementRetainStoredValue(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, BoosterBlocks.NETHERITE_CONVERTER.get().defaultBlockState(), 3);
        var machine = (NetheriteConverterBlockEntity) level.getBlockEntity(pos);
        machine.getPlateInventory().insertItem(0, new ItemStack(BoosterItems.NETHERITE_SHEET.get(), 2), false);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ValuePickupQA"));
        player.setPos(pos.getCenter().add(0, 0, 2)); player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.WRENCH.get()));
        AllItems.WRENCH.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(pos.getCenter(), Direction.UP, pos, false)));
        var item = player.getInventory().items.stream().filter(s -> s.is(BoosterItems.NETHERITE_CONVERTER.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(level.getBlockState(pos).isAir() && item.getCount() == 1
            && item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(NetheriteConverterBlockEntity.VALUE_KEY) == 32,
            "native wrench pickup retains thirty-two points on exactly one converter item");
        helper.assertTrue(player.getInventory().items.stream().noneMatch(s -> s.is(BoosterItems.NETHERITE_SHEET.get())), "pickup does not also refund redeemed sheets");
        // Move the actual returned stack into the hand, then use the normal BlockItem placement path.
        for (int i = 0; i < player.getInventory().items.size(); i++) if (player.getInventory().items.get(i) == item) player.getInventory().items.set(i, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, item); player.setShiftKeyDown(false);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var result = item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.below().getCenter().add(0, .5, 0), Direction.UP, pos.below(), false)));
        helper.assertTrue(result.consumesAction() && item.isEmpty() && level.getBlockEntity(pos) instanceof NetheriteConverterBlockEntity restored
            && restored.getNetheriteValue() == 32, "actual survival placement consumes the returned item and restores all stored value");
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.stream().filter(e -> e.getItem().is(BoosterItems.NETHERITE_CONVERTER.get())).count() == 1
            && drops.stream().anyMatch(e -> e.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(NetheriteConverterBlockEntity.VALUE_KEY) == 32)
            && drops.stream().noneMatch(e -> e.getItem().is(BoosterItems.NETHERITE_SHEET.get())), "ordinary breaking preserves value once, without an additional plate refund");
        helper.succeed();
    }
}
