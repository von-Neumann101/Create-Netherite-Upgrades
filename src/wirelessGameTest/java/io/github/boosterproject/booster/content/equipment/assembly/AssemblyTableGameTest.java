package io.github.boosterproject.booster.content.equipment.assembly;

import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.boosterproject.booster.Booster;
import io.github.boosterproject.booster.content.equipment.wrench.AssemblyTableBlockEntity;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchMenu;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblyTableGameTest {
    @GameTest(template = "wireless_empty")
    public static void invalidSlotRequestsAreIgnoredAndNativeDragStillWorks(GameTestHelper helper) {
        var table = place(helper);
        table.getInventory().setStackInSlot(0, wrench());
        var player = viewer(helper, table);
        var menu = new WrenchMenu(26, player.getInventory(), table);
        for (int slot : new int[] {-1, -999, -2, 46, Integer.MAX_VALUE})
            menu.clicked(slot, 0, ClickType.SWAP, player);
        menu.setCarried(new ItemStack(BoosterItems.POWERFUL_MECHANICAL_PUMP.get(), 4));
        menu.clicked(-999, 0, ClickType.QUICK_CRAFT, player);
        menu.clicked(-1, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, 2, ClickType.QUICK_CRAFT, player);
        helper.assertTrue(menu.getCarried().getCount() == 4 && menu.loadedCount() == 0, "invalid swap and drag slot requests do not mutate inventory");
        menu.clicked(-999, 0, ClickType.QUICK_CRAFT, player);
        menu.clicked(2, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(3, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, 2, ClickType.QUICK_CRAFT, player);
        helper.assertTrue(menu.loadedCount() == 2 && menu.getCarried().getCount() == 2, "native drag start/end outside slots still installs one real block per slot");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void rightClickOpensPhysicalAssemblySlots(GameTestHelper helper) {
        var table = place(helper);
        var player = viewer(helper, table);
        var hit = new BlockHitResult(table.getBlockPos().getCenter(), Direction.UP, table.getBlockPos(), false);
        table.getBlockState().useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(player.containerMenu instanceof WrenchMenu, "right clicking the unpowered table opens its assembly menu");
        var menu = (WrenchMenu) player.containerMenu;
        helper.assertTrue(menu.slots.size() == 46, "two physical inputs, eight module slots and 36 player slots");
        menu.setCarried(new ItemStack(BoosterItems.POWERFUL_MECHANICAL_PUMP.get(), 16));
        menu.clicked(2, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.loadedCount() == 0 && menu.getCarried().getCount() == 16, "empty table rejects modules without consuming them");
        menu.setCarried(wrench());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(table.getInventory().getStackInSlot(0).getCount() == 1 && menu.getCarried().isEmpty(), "center accepts one real wrench");
        menu.setCarried(new ItemStack(Items.DIRT));
        menu.clicked(2, 0, ClickType.PICKUP, player);
        helper.assertTrue(!menu.getSlot(2).hasItem() && menu.getCarried().is(Items.DIRT), "module slots reject unlisted blocks");
        menu.setCarried(new ItemStack(BoosterItems.POWERFUL_MECHANICAL_PUMP.get(), 16));
        for (int slot = 2; slot < 10; slot++) menu.clicked(slot, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.loadedCount() == 8 && menu.getCarried().getCount() == 8, "installing eight modules consumes exactly eight real blocks");
        menu.clicked(2, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getSlot(2).getItem().getCount() == 1 && menu.getCarried().getCount() == 8, "each module slot is capped at one");
        helper.assertTrue(WrenchData.activeSlots(table.getInventory().getStackInSlot(0)).isEmpty(), "assembly never activates an installed module");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(WrenchData.modules(menu.getCarried()).stream().filter(s -> !s.isEmpty()).count() == 8
            && menu.loadedCount() == 0, "taking the wrench returns its eight blocks and clears the shared mirror");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void shiftTransferAndRawSlotChangesPersistToWrench(GameTestHelper helper) {
        var table = place(helper);
        var player = viewer(helper, table);
        var menu = new WrenchMenu(21, player.getInventory(), table);
        player.getInventory().setItem(9, wrench());
        menu.quickMoveStack(player, 10);
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && menu.getSlot(0).hasItem(), "shift transfers a wrench into its physical input");
        player.getInventory().setItem(9, new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get(), 10));
        menu.quickMoveStack(player, 10);
        helper.assertTrue(menu.loadedCount() == 8 && player.getInventory().getItem(9).getCount() == 2, "shift installs eight actual one-item modules");
        menu.quickMoveStack(player, 2);
        helper.assertTrue(menu.loadedCount() == 7 && WrenchData.modules(menu.getSlot(0).getItem()).getFirst().isEmpty(), "shift extracting a module updates the wrench component");
        menu.getSlot(3).getItem().set(DataComponents.CUSTOM_NAME, Component.literal("Updated in place"));
        menu.getSlot(3).setChanged();
        helper.assertTrue(WrenchData.modules(menu.getSlot(0).getItem()).get(1).getHoverName().getString().equals("Updated in place"),
            "native slot mutations are committed through setChanged");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(menu.loadedCount() == 0 && table.getInventory().getStackInSlot(0).isEmpty(), "shift extracting the wrench clears modules instead of dropping or copying them");
        int wrenches = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (WrenchData.isWrench(stack)) {
                wrenches += stack.getCount();
                helper.assertTrue(WrenchData.modules(stack).stream().filter(s -> !s.isEmpty()).count() == 7, "transferred wrench retains the seven remaining modules");
            }
        }
        helper.assertTrue(wrenches == 1, "shift transfer returns exactly one wrench");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void templateUnlockConsumesOnceAndSurvivesReload(GameTestHelper helper) {
        var table = place(helper);
        var player = viewer(helper, table);
        var menu = new WrenchMenu(22, player.getInventory(), table);
        table.getInventory().setStackInSlot(1, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 3));
        helper.assertTrue(!menu.clickMenuButton(player, 0) && table.getInventory().getStackInSlot(1).getCount() == 3, "template alone cannot buy an upgrade");
        table.getInventory().setStackInSlot(0, wrench());
        helper.assertTrue(menu.clickMenuButton(player, 0) && WrenchData.activationLimit(menu.getSlot(0).getItem()) == 2
            && table.getInventory().getStackInSlot(1).getCount() == 2, "upgrade consumes exactly one vanilla template");
        helper.assertTrue(!menu.clickMenuButton(player, 0) && !menu.clickMenuButton(player, 1)
            && table.getInventory().getStackInSlot(1).getCount() == 2, "repeated upgrade and obsolete activation buttons consume nothing");
        menu.removed(player);
        var restored = new AssemblyTableBlockEntity(table.getBlockPos(), table.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(table.saveWithoutMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(WrenchData.activationLimit(restored.getInventory().getStackInSlot(0)) == 2
            && restored.getInventory().getStackInSlot(1).getCount() == 2, "closing keeps the actual inputs and permanent upgrade across reload");
        helper.assertTrue(!restored.upgrade() && restored.getInventory().getStackInSlot(1).getCount() == 2, "reloaded permanent upgrade cannot consume a second template");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void sharedViewersCannotResurrectRemovedModules(GameTestHelper helper) {
        var table = place(helper);
        table.getInventory().setStackInSlot(0, wrench());
        var first = viewer(helper, table);
        var second = viewer(helper, table);
        var firstMenu = new WrenchMenu(23, first.getInventory(), table);
        var secondMenu = new WrenchMenu(24, second.getInventory(), table);
        firstMenu.setCarried(new ItemStack(BoosterItems.POWERFUL_MECHANICAL_PUMP.get()));
        firstMenu.clicked(2, 0, ClickType.PICKUP, first);
        helper.assertTrue(secondMenu.getSlot(2).hasItem(), "second viewer sees the same shared module handler immediately");
        secondMenu.clicked(2, 0, ClickType.PICKUP, second);
        helper.assertTrue(firstMenu.loadedCount() == 0 && secondMenu.getCarried().getCount() == 1
            && WrenchData.modules(table.getInventory().getStackInSlot(0)).getFirst().isEmpty(), "taking a module through the other viewer does not leave a stale copy");
        firstMenu.clicked(0, 0, ClickType.PICKUP, first);
        secondMenu.setCarried(new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get()));
        secondMenu.clicked(3, 0, ClickType.PICKUP, second);
        helper.assertTrue(secondMenu.getCarried().getCount() == 1 && firstMenu.loadedCount() == 0, "removing the wrench closes module insertion for all viewers");
        secondMenu.removed(second);
        helper.assertTrue(WrenchData.modules(firstMenu.getCarried()).stream().allMatch(ItemStack::isEmpty), "closing another viewer cannot write cached modules back into a removed wrench");
        var replacement = wrench();
        WrenchData.setModules(replacement, List.of(new ItemStack(BoosterItems.NETHERITE_FLUID_TANK.get())));
        table.getInventory().setStackInSlot(0, replacement);
        helper.assertTrue(firstMenu.getSlot(2).getItem().is(BoosterItems.NETHERITE_FLUID_TANK.get())
            && secondMenu.getSlot(2).getItem().is(BoosterItems.NETHERITE_FLUID_TANK.get()), "inserting a different wrench refreshes the mirror for every viewer");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void removalDropsOnlyPhysicalInputsWithAllComponents(GameTestHelper helper) {
        var table = place(helper);
        var stack = wrench();
        var module = new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get());
        module.set(DataComponents.CUSTOM_NAME, Component.literal("Owned module"));
        WrenchData.setModules(stack, List.of(module));
        WrenchData.setFluids(stack, List.of(new FluidStack(Fluids.WATER, 1200)), helper.getLevel().registryAccess());
        WrenchData.setActiveSlot(stack, 0);
        table.getInventory().setStackInSlot(0, stack);
        table.getInventory().setStackInSlot(1, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
        var saved = table.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new AssemblyTableBlockEntity(table.getBlockPos(), table.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.matches(restored.getInventory().getStackInSlot(0), stack)
            && ItemStack.matches(restored.getModules().getStackInSlot(0), module), "reload reconstructs the editable mirror from the preserved wrench");
        var pos = table.getBlockPos();
        helper.getLevel().removeBlock(pos, false);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(.8));
        helper.assertTrue(drops.stream().filter(e -> WrenchData.isWrench(e.getItem())).mapToInt(e -> e.getItem().getCount()).sum() == 1
            && drops.stream().filter(e -> e.getItem().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)).mapToInt(e -> e.getItem().getCount()).sum() == 2,
            "breaking returns the one stored wrench and both real templates exactly once");
        helper.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(BoosterItems.NETHERITE_BLAZE_BURNER.get())), "module mirror is never dropped separately");
        var dropped = drops.stream().map(ItemEntity::getItem).filter(WrenchData::isWrench).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(ItemStack.matches(dropped, stack) && WrenchData.totalFluid(dropped, helper.getLevel().registryAccess()) == 1200,
            "dropped wrench preserves its real module components, active state and fluids");
        helper.succeed();
    }

    @GameTest(template = "wireless_empty")
    public static void oldMenusRejectDistanceDimensionAndReplacement(GameTestHelper helper) {
        var table = place(helper);
        table.getInventory().setStackInSlot(0, wrench());
        table.getInventory().setStackInSlot(1, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
        var player = viewer(helper, table);
        var menu = new WrenchMenu(25, player.getInventory(), table);
        player.setPos(table.getBlockPos().getCenter().add(8, 0, 0));
        helper.assertTrue(menu.stillValid(player), "exactly eight blocks remains valid");
        player.setPos(table.getBlockPos().getCenter().add(8.01, 0, 0));
        rejectedMutation(helper, menu, player);
        player.setPos(table.getBlockPos().getCenter());
        var other = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(other != null, "test server has a second dimension");
        player.setServerLevel(other);
        rejectedMutation(helper, menu, player);
        player.setServerLevel(helper.getLevel());
        var replacement = new AssemblyTableBlockEntity(table.getBlockPos(), table.getBlockState());
        helper.getLevel().setBlockEntity(replacement);
        rejectedMutation(helper, menu, player);
        helper.assertTrue(table.getInventory().getStackInSlot(1).getCount() == 2 && WrenchData.activationLimit(table.getInventory().getStackInSlot(0)) == 1,
            "invalid menu paths never consume templates or upgrade the detached original wrench");
        helper.succeed();
    }

    private static void rejectedMutation(GameTestHelper helper, WrenchMenu menu, ServerPlayer player) {
        helper.assertTrue(!menu.stillValid(player) && !menu.clickMenuButton(player, 0), "invalid table binding rejects upgrade");
        menu.setCarried(new ItemStack(BoosterItems.POWERFUL_MECHANICAL_PUMP.get()));
        menu.clicked(2, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 1 && menu.loadedCount() == 0, "invalid table binding rejects click mutations");
        helper.assertTrue(menu.quickMoveStack(player, 0).isEmpty() && menu.getSlot(0).hasItem(), "invalid table binding rejects shift extraction");
    }

    private static AssemblyTableBlockEntity place(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, BoosterBlocks.ASSEMBLY_TABLE.get().defaultBlockState(), 3);
        return (AssemblyTableBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static ItemStack wrench() { return new ItemStack(BoosterItems.NETHERITE_NETHERITE_WRENCH.get()); }

    private static ServerPlayer viewer(GameTestHelper helper, AssemblyTableBlockEntity table) {
        var profile = new GameProfile(UUID.randomUUID(), "AssemblyQA");
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setPos(table.getBlockPos().getCenter());
        return player;
    }
}
