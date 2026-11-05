package io.github.boosterproject.booster.content.redstone.wireless;

import io.github.boosterproject.booster.Booster;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Booster.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WirelessManagerBlockGameTest {
    @GameTest(template = "wireless_empty", timeoutTicks = 40)
    public static void fixedManagerIsRegistered(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_wireless_manager");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.ITEM.containsKey(id), "the fixed wireless manager must be a registered block and item");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 190, 1));
        helper.getLevel().setBlock(pos, BoosterBlocks.NETHERITE_WIRELESS_MANAGER.get().defaultBlockState(), 3);
        helper.assertTrue(!((net.minecraft.world.level.block.Block) BoosterBlocks.NETHERITE_WIRELESS_MANAGER.get() instanceof KineticBlock)
            && helper.getLevel().getBlockEntity(pos) == null && BlockStressValues.getImpact(BoosterBlocks.NETHERITE_WIRELESS_MANAGER.get()) == 0,
            "fixed manager is an ordinary block with no kinetic BE or stress requirement");
        var input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.ENDER_EYE), new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "transmitter"))), new ItemStack(Items.NETHER_WART),
            AllBlocks.BRASS_CASING.asStack(), new ItemStack(Items.NETHERITE_INGOT), AllBlocks.BRASS_CASING.asStack(),
            new ItemStack(Blocks.OBSIDIAN), new ItemStack(Blocks.CRYING_OBSIDIAN), new ItemStack(Blocks.OBSIDIAN)));
        var result = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(BoosterItems.NETHERITE_WIRELESS_MANAGER.get()) && result.getCount() == 1 && result.has(DataComponents.FIRE_RESISTANT),
            "the specified 3x3 recipe makes one fire-resistant manager block");
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void sharedBlockKeepsViewerPermissionsAndBatchActivation(GameTestHelper helper) {
        BlockPos pos = placeManager(helper);
        Viewer alice = viewer(helper, pos, "0_ManagerA"), bob = viewer(helper, pos, "0_ManagerB");
        UUID own = terminal(helper, pos.east(2), alice.player, "A", false);
        UUID shared = terminal(helper, pos.east(3), bob.player, "B", true);
        UUID hidden = terminal(helper, pos.east(4), bob.player, "C", false);
        var data = WirelessTerminalData.get(helper.getLevel().getServer());
        var menu = open(helper, pos, alice);
        menu.broadcastChanges();
        helper.assertTrue(menu.stillValid(alice.player) && !alice.pages.getLast().terminalOnly()
            && has(alice, own) && has(alice, shared) && !has(alice, hidden), "fixed manager lists the viewer's terminals and public terminals, hiding other private entries");
        helper.assertTrue(titleKey(alice).equals("block.createnetherite.netherite_wireless_manager"), "block menu uses its block title");
        action(menu, WirelessActionPayload.SELECT, own, "", false);
        action(menu, WirelessActionPayload.SELECT, shared, "", false);
        action(menu, WirelessActionPayload.SELECT, hidden, "", false);
        helper.assertTrue(alice.pages.getLast().selectedCount() == 2, "forged private target cannot be selected");
        action(menu, WirelessActionPayload.SAVE, shared, "forged", false);
        helper.assertTrue(data.get(shared).name().equals("B") && data.get(shared).shared(), "shared visibility does not grant edit ownership");
        action(menu, WirelessActionPayload.ACTIVATE, WirelessActionPayload.NO_TARGET, "", false);
        helper.assertTrue(data.activation(own) != null && data.activation(own).equals(data.activation(shared)) && data.activation(hidden) == null,
            "multi-select activation sends one synchronized pulse only to allowed targets");
        var peer = open(helper, pos, bob); peer.broadcastChanges();
        helper.assertTrue(has(bob, shared) && has(bob, hidden) && !has(bob, own), "another player using the same block receives their own independent list");
        alice.player.closeContainer(); bob.player.closeContainer();
        remove(helper, pos, pos.east(2), pos.east(3), pos.east(4));
        helper.succeed();
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void blockMenuRejectsDistantRemovedAndOtherDimensionSources(GameTestHelper helper) {
        BlockPos pos = placeManager(helper);
        Viewer viewer = viewer(helper, pos, "0_ManagerSource");
        UUID target = terminal(helper, pos.east(2), viewer.player, "Source", false);
        var data = WirelessTerminalData.get(helper.getLevel().getServer());
        var menu = open(helper, pos, viewer);
        action(menu, WirelessActionPayload.SELECT, target, "", false);
        action(menu, WirelessActionPayload.ACTIVATE, WirelessActionPayload.NO_TARGET, "", false);
        var original = data.activation(target);
        helper.runAfterDelay(2, () -> {
            viewer.player.setPos(pos.getCenter().add(8, 0, 0));
            helper.assertTrue(menu.stillValid(viewer.player), "exactly eight blocks remains within the fixed menu source range");
            viewer.player.setPos(pos.getCenter().add(8.6, 0, 0));
            helper.assertTrue(!menu.stillValid(viewer.player), "8.6 blocks must invalidate the source even when native extended interaction range would allow it");
            action(menu, WirelessActionPayload.SAVE, target, "edge edit", false);
            action(menu, WirelessActionPayload.ACTIVATE, WirelessActionPayload.NO_TARGET, "", false);
            helper.assertTrue(data.get(target).name().equals("Source") && data.activation(target).equals(original), "out-of-range edge actions cannot edit or renew activation");
            viewer.player.setPos(pos.getCenter().add(12, 0, 0));
            helper.assertTrue(!menu.stillValid(viewer.player), "leaving eight blocks invalidates the fixed source");
            action(menu, WirelessActionPayload.SAVE, target, "remote edit", false);
            action(menu, WirelessActionPayload.ACTIVATE, WirelessActionPayload.NO_TARGET, "", false);
            helper.assertTrue(data.get(target).name().equals("Source") && data.activation(target).equals(original), "a stale distant menu cannot edit or renew activation");
            var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            nether.setBlock(pos, BoosterBlocks.NETHERITE_WIRELESS_MANAGER.get().defaultBlockState(), 3);
            viewer.player.setServerLevel(nether); viewer.player.setPos(pos.getCenter().add(0, 0, 2));
            helper.assertTrue(!menu.stillValid(viewer.player), "same coordinates and another manager in another dimension cannot validate the old menu");
            action(menu, WirelessActionPayload.SAVE, target, "dimension edit", false);
            action(menu, WirelessActionPayload.ACTIVATE, WirelessActionPayload.NO_TARGET, "", false);
            helper.assertTrue(data.get(target).name().equals("Source") && data.activation(target).equals(original), "cross-dimension stale menu actions are rejected server-side");
            viewer.player.setServerLevel(helper.getLevel()); nether.removeBlock(pos, false);
            helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(!menu.stillValid(viewer.player), "replacing the manager invalidates its menu");
            action(menu, WirelessActionPayload.SAVE, target, "removed edit", false);
            helper.assertTrue(data.get(target).name().equals("Source"), "removed source cannot edit terminals");
            viewer.player.closeContainer(); remove(helper, pos, pos.east(2)); helper.succeed();
        });
    }

    @GameTest(template = "wireless_empty", timeoutTicks = 80)
    public static void portableAndTerminalMenusKeepTheirSources(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 190, 1));
        Viewer viewer = viewer(helper, pos, "0_Portable");
        ItemStack portable = new ItemStack(BoosterItems.NETHERITE_INFINITE_MANAGER.get());
        viewer.player.setItemInHand(InteractionHand.OFF_HAND, portable);
        portable.getItem().use(helper.getLevel(), viewer.player, InteractionHand.OFF_HAND);
        helper.assertTrue(viewer.player.containerMenu instanceof WirelessManagerMenu && titleKey(viewer).equals("item.createnetherite.netherite_infinite_manager"),
            "legacy item ID still opens the portable menu with its renamed item title");
        var menu = (WirelessManagerMenu) viewer.player.containerMenu;
        helper.assertTrue(menu.stillValid(viewer.player), "offhand portable remains valid while held");
        viewer.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
        helper.assertTrue(!menu.stillValid(viewer.player), "changing the portable's bound hand invalidates its menu");
        viewer.player.closeContainer();
        UUID own = terminal(helper, pos, viewer.player, "Terminal", false);
        var terminal = (WirelessTerminalBlockEntity) helper.getLevel().getBlockEntity(pos);
        WirelessManagerMenu.openTerminal(viewer.player, terminal);
        var terminalMenu = (WirelessManagerMenu) viewer.player.containerMenu; terminalMenu.broadcastChanges();
        helper.assertTrue(terminalMenu.stillValid(viewer.player) && viewer.pages.getLast().terminalOnly()
            && viewer.pages.getLast().entries().size() == 1 && viewer.pages.getLast().entries().getFirst().id().equals(own), "terminal configuration still uses its original owner-only single-terminal mode");
        viewer.player.closeContainer(); remove(helper, pos); helper.succeed();
    }

    private record Viewer(ServerPlayer player, List<WirelessPagePayload> pages, List<Component> titles) {}
    private static Viewer viewer(GameTestHelper helper, BlockPos pos, String name) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        List<WirelessPagePayload> pages = new ArrayList<>(); List<Component> titles = new ArrayList<>();
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof WirelessPagePayload page) pages.add(page);
                if (packet instanceof ClientboundOpenScreenPacket screen) titles.add(screen.getTitle());
            }
        };
        player.setPos(pos.getCenter().add(0, 0, 2)); player.setNoGravity(true);
        return new Viewer(player, pages, titles);
    }
    private static BlockPos placeManager(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 190, 1));
        helper.getLevel().setBlock(pos, BoosterBlocks.NETHERITE_WIRELESS_MANAGER.get().defaultBlockState(), 3); return pos;
    }
    private static WirelessManagerMenu open(GameTestHelper helper, BlockPos pos, Viewer viewer) {
        helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), viewer.player, new BlockHitResult(pos.getCenter(), Direction.SOUTH, pos, false));
        helper.assertTrue(viewer.player.containerMenu instanceof WirelessManagerMenu, "right-clicking the unpowered block opens the existing management menu");
        return (WirelessManagerMenu) viewer.player.containerMenu;
    }
    private static UUID terminal(GameTestHelper helper, BlockPos pos, ServerPlayer owner, String name, boolean shared) {
        helper.getLevel().setBlock(pos, BoosterBlocks.NETHERITE_WIRELESS_TERMINAL.get().defaultBlockState(), 3);
        var terminal = (WirelessTerminalBlockEntity) helper.getLevel().getBlockEntity(pos); terminal.placedBy(owner);
        WirelessTerminalData.get(helper.getLevel().getServer()).configure(owner.getUUID(), terminal.terminalId(), name, shared); return terminal.terminalId();
    }
    private static boolean has(Viewer viewer, UUID id) { return viewer.pages.getLast().entries().stream().anyMatch(e -> e.id().equals(id)); }
    private static String titleKey(Viewer viewer) { return ((TranslatableContents) viewer.titles.getLast().getContents()).getKey(); }
    private static void action(WirelessManagerMenu menu, int action, UUID target, String name, boolean shared) { menu.handle(new WirelessActionPayload(menu.containerId, action, target, name, shared, 0)); }
    private static void remove(GameTestHelper helper, BlockPos... positions) { for (BlockPos pos : positions) helper.getLevel().removeBlock(pos, false); }
}
