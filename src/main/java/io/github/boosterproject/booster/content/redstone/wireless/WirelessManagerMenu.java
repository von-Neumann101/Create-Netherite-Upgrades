package io.github.boosterproject.booster.content.redstone.wireless;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import io.github.boosterproject.booster.registry.BoosterMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class WirelessManagerMenu extends AbstractContainerMenu {
    private final Player viewer;
    private final InteractionHand hand;
    private final UUID terminalId;
    private final BlockPos terminalPos;
    private final BlockPos managerPos;
    private final ResourceKey<Level> dimension;
    private final Set<UUID> selected = new HashSet<>();
    private int page;
    private int ticks;
    private long lastRevision = -1;
    private WirelessPagePayload snapshot;

    public WirelessManagerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, null, null, null, null);
    }

    private WirelessManagerMenu(int id, Inventory inventory, InteractionHand hand, UUID terminalId, BlockPos terminalPos, BlockPos managerPos) {
        super(BoosterMenuTypes.NETHERITE_WIRELESS_MANAGER.get(), id);
        this.viewer = inventory.player;
        this.hand = hand;
        this.terminalId = terminalId;
        this.terminalPos = terminalPos;
        this.managerPos = managerPos;
        dimension = viewer.level().dimension();
        snapshot = new WirelessPagePayload(id, 0, 1, 0, terminalId != null, List.of());
    }

    public static void openManager(ServerPlayer player, InteractionHand hand) {
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new WirelessManagerMenu(id, inventory, hand, null, null, null),
            Component.translatable("item.createnetherite.netherite_infinite_manager")));
    }

    public static void openBlockManager(ServerPlayer player, BlockPos pos) {
        if (!player.level().getBlockState(pos).is(BoosterBlocks.NETHERITE_WIRELESS_MANAGER.get())
            || player.distanceToSqr(pos.getCenter()) > 64) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new WirelessManagerMenu(id, inventory, null, null, null, pos.immutable()),
            Component.translatable("block.createnetherite.netherite_wireless_manager")));
    }

    public static void openTerminal(ServerPlayer player, WirelessTerminalBlockEntity blockEntity) {
        var terminal = WirelessTerminalData.get(player.server).get(blockEntity.terminalId());
        if (terminal == null || !terminal.owner().equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("wireless.createnetherite.owner_only"), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new WirelessManagerMenu(id, inventory, null, terminal.id(), terminal.pos(), null),
            Component.translatable("block.createnetherite.netherite_wireless_terminal")));
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        if (hand != null) return player.getItemInHand(hand).is(BoosterItems.NETHERITE_INFINITE_MANAGER.get());
        if (managerPos != null) return dimension.equals(player.level().dimension())
            && player.distanceToSqr(managerPos.getCenter()) <= 64
            && AbstractContainerMenu.stillValid(ContainerLevelAccess.create(player.level(), managerPos), player, BoosterBlocks.NETHERITE_WIRELESS_MANAGER.get());
        if (terminalPos == null || !dimension.equals(player.level().dimension()) || player.distanceToSqr(terminalPos.getCenter()) > 64) return false;
        var terminal = WirelessTerminalData.get(serverPlayer.server).get(terminalId);
        return terminal != null && terminal.owner().equals(player.getUUID())
            && player.level().getBlockEntity(terminalPos) instanceof WirelessTerminalBlockEntity blockEntity
            && terminalId.equals(blockEntity.terminalId());
    }

    private List<WirelessTerminalData.Terminal> visible(WirelessTerminalData data) {
        if (terminalId == null) return data.visibleTo(viewer.getUUID());
        var terminal = data.get(terminalId);
        return terminal != null && terminal.owner().equals(viewer.getUUID()) ? List.of(terminal) : List.of();
    }

    private List<WirelessTerminalData.Terminal> currentPage(WirelessTerminalData data) {
        var all = visible(data);
        page = Math.max(0, Math.min(page, Math.max(0, (all.size() - 1) / WirelessPagePayload.PAGE_SIZE)));
        int from = page * WirelessPagePayload.PAGE_SIZE;
        return all.subList(from, Math.min(from + WirelessPagePayload.PAGE_SIZE, all.size()));
    }

    public void handle(WirelessActionPayload request) {
        if (!(viewer instanceof ServerPlayer player) || !stillValid(player)) return;
        WirelessTerminalData data = WirelessTerminalData.get(player.server);
        var target = data.get(request.target());
        boolean allowedTarget = target != null && target.visibleTo(player.getUUID())
            && (terminalId == null || terminalId.equals(target.id()));
        switch (request.action()) {
            case WirelessActionPayload.PAGE -> page = Math.max(0, request.page());
            case WirelessActionPayload.SELECT -> {
                if (allowedTarget && !selected.remove(target.id())) selected.add(target.id());
            }
            case WirelessActionPayload.SELECT_PAGE -> currentPage(data).forEach(t -> selected.add(t.id()));
            case WirelessActionPayload.CLEAR -> selected.clear();
            case WirelessActionPayload.SAVE -> {
                if (allowedTarget) data.configure(player.getUUID(), target.id(), request.name(), request.shared());
            }
            case WirelessActionPayload.ACTIVATE -> {
                int count = 0, total = selected.size();
                for (UUID id : Set.copyOf(selected)) {
                    if (WirelessTerminalService.activate(player.server, player.getUUID(), id)) count++;
                }
                player.displayClientMessage(Component.translatable("wireless.createnetherite.sent", count, total), true);
            }
            default -> { return; }
        }
        sendPage();
    }

    private void sendPage() {
        if (!(viewer instanceof ServerPlayer player)) return;
        WirelessTerminalData data = WirelessTerminalData.get(player.server);
        selected.removeIf(id -> data.get(id) == null || !data.get(id).visibleTo(player.getUUID()));
        var entries = currentPage(data).stream().map(t -> new WirelessPagePayload.Entry(t.id(), t.ownerName(), t.name(),
            t.dimension().location().toString(), t.pos(), t.shared(), t.owner().equals(player.getUUID()), selected.contains(t.id()))).toList();
        int pages = Math.max(1, (visible(data).size() + WirelessPagePayload.PAGE_SIZE - 1) / WirelessPagePayload.PAGE_SIZE);
        PacketDistributor.sendToPlayer(player, new WirelessPagePayload(containerId, page, pages, selected.size(), terminalId != null, entries));
        lastRevision = data.revision();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (viewer instanceof ServerPlayer player && (WirelessTerminalData.get(player.server).revision() != lastRevision || ++ticks % 20 == 0)) sendPage();
    }

    public WirelessPagePayload snapshot() { return snapshot; }
    public void receive(WirelessPagePayload page) { snapshot = page; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
