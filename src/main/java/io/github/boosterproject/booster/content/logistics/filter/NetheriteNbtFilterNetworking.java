package io.github.boosterproject.booster.content.logistics.filter;

import io.github.boosterproject.booster.Booster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetheriteNbtFilterNetworking {
    private NetheriteNbtFilterNetworking() {}
    public record Action(int containerId, int referenceHash, int action, int field, int operator, String first, String second) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "nbt_filter_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeVarInt(value.containerId); buf.writeInt(value.referenceHash); buf.writeVarInt(value.action);
                buf.writeVarInt(value.field); buf.writeVarInt(value.operator); buf.writeUtf(value.first, 32767); buf.writeUtf(value.second, 32767); },
            buf -> new Action(buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(32767), buf.readUtf(32767)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Action.TYPE, Action.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handle(player, payload);
        }));
    }
    public static void handle(ServerPlayer player, Action payload) {
        if (player.isAlive() && !player.isSpectator() && player.containerMenu instanceof NetheriteNbtFilterMenu menu && menu.stillValid(player)) menu.apply(payload);
    }
}
