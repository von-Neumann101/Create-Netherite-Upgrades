package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.Booster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** A wheel changes activation only, bound to the actual server-held stack. */
public final class WrenchWheelNetworking {
    public static final int OPEN = 0, TOGGLE = 1, CLOSE = 2;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static State clientState;
    private WrenchWheelNetworking() {}

    public record Action(UUID session, int action, int slot) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id("wrench_wheel_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.session); buf.writeVarInt(p.action); buf.writeVarInt(p.slot); },
            buf -> new Action(buf.readUUID(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(UUID session, int hotbar, List<ItemStack> modules, List<Integer> activeSlots,
                        int activationLimit, int rightClickSlot) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id("wrench_wheel_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeUUID(p.session); buf.writeVarInt(p.hotbar);
            for (int i = 0; i < WrenchData.SLOTS; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.modules.get(i));
            buf.writeVarInt(p.activeSlots.size());
            p.activeSlots.forEach(buf::writeVarInt);
            buf.writeVarInt(p.activationLimit); buf.writeVarInt(p.rightClickSlot);
        }, buf -> {
            UUID session = buf.readUUID(); int hotbar = buf.readVarInt();
            List<ItemStack> modules = new ArrayList<>();
            for (int i = 0; i < WrenchData.SLOTS; i++) modules.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            int count = buf.readVarInt();
            if (count < 0 || count > 2) throw new IllegalArgumentException("Invalid wrench activation count");
            List<Integer> active = new ArrayList<>();
            for (int i = 0; i < count; i++) active.add(buf.readVarInt());
            return new State(session, hotbar, List.copyOf(modules), List.copyOf(active), buf.readVarInt(), buf.readVarInt());
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private record Session(ServerPlayer owner, UUID nonce, int hotbar, ItemStack source) {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, path); }
    public static State clientState() { return clientState; }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Action.TYPE, Action.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handle(player, payload);
        }));
        registrar.playToClient(State.TYPE, State.CODEC, (payload, context) -> context.enqueueWork(() -> clientState = payload));
    }

    public static void handle(ServerPlayer player, Action payload) {
        Session session = SESSIONS.get(player.getUUID());
        if (payload.action == CLOSE) {
            if (session != null && session.owner == player && session.nonce.equals(payload.session)) SESSIONS.remove(player.getUUID());
            return;
        }
        if (payload.action == OPEN) {
            if (!validPlayer(player) || !WrenchData.isWrench(player.getMainHandItem())) {
                invalidate(player, payload.session); return;
            }
            player.stopUsingItem();
            session = new Session(player, payload.session, player.getInventory().selected, player.getMainHandItem());
            SESSIONS.put(player.getUUID(), session);
            send(player, session);
            return;
        }
        if (payload.action != TOGGLE) return;
        if (session == null || session.owner != player || !session.nonce.equals(payload.session)) {
            invalidate(player, payload.session); return;
        }
        if (!validPlayer(player) || player.getInventory().selected != session.hotbar || player.getMainHandItem() != session.source
            || !WrenchData.isWrench(session.source)) {
            SESSIONS.remove(player.getUUID()); invalidate(player, payload.session); return;
        }
        if (payload.slot >= 0 && payload.slot < WrenchData.SLOTS && WrenchModule.fromStack(WrenchData.modules(session.source).get(payload.slot)) != null) {
            player.stopUsingItem();
            WrenchData.toggleSlot(session.source, payload.slot);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        send(player, session);
    }

    private static boolean validPlayer(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.containerMenu == player.inventoryMenu;
    }
    private static void send(ServerPlayer player, Session session) {
        var stack = session.source;
        PacketDistributor.sendToPlayer(player, new State(session.nonce, session.hotbar, WrenchData.modules(stack),
            WrenchData.activeSlots(stack), WrenchData.activationLimit(stack), WrenchData.rightClickSlot(stack)));
    }
    private static void invalidate(ServerPlayer player, UUID nonce) {
        PacketDistributor.sendToPlayer(player, new State(nonce, -1, java.util.Collections.nCopies(WrenchData.SLOTS, ItemStack.EMPTY), List.of(), 0, -1));
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }
    public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) forget(player);
    }
    private static void forget(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.owner == player) SESSIONS.remove(player.getUUID());
    }
}
