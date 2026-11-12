package io.github.boosterproject.booster.content.equipment.wrench;

import io.github.boosterproject.booster.Booster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class WrenchNetworking {
    public static final int OPEN = 0, TOGGLE_FAN = 1, SELECT_FLUID = 2;
    private WrenchNetworking() {}
    public record Action(int action, int value) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "wrench_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.action); buf.writeVarInt(p.value); },
            buf -> new Action(buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Launch(Vec3 velocity) implements CustomPacketPayload {
        public static final Type<Launch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "wrench_launch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Launch> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeDouble(p.velocity.x); buf.writeDouble(p.velocity.y); buf.writeDouble(p.velocity.z); },
            buf -> new Launch(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Action.TYPE, Action.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handle(player, payload);
        }));
        registrar.playToClient(Launch.TYPE, Launch.CODEC, (payload, context) -> context.enqueueWork(() -> {
            context.player().setDeltaMovement(payload.velocity());
            context.player().hasImpulse = true;
            context.player().setOnGround(false);
            context.player().fallDistance = 0;
        }));
    }
    public static void handle(ServerPlayer player, Action payload) {
        var stack = player.getMainHandItem();
        if (!WrenchData.isWrench(stack) || !player.isAlive() || player.isSpectator()
            || player.containerMenu != player.inventoryMenu) return;
        switch (payload.action()) {
            case OPEN -> { return; } // Loading modules now requires an assembly table.
            case TOGGLE_FAN -> {
                if (!WrenchData.has(stack, WrenchModule.FAN)) return;
                WrenchData.setFanPull(stack, !WrenchData.fanPull(stack));
                player.displayClientMessage(Component.translatable(WrenchData.fanPull(stack) ? "wrench.createnetherite.pull" : "wrench.createnetherite.push"), true);
            }
            case SELECT_FLUID -> {
                if (WrenchData.active(stack) != WrenchModule.TANK) return;
                int index = payload.value();
                if (index < 0 || index >= WrenchData.fluids(stack, player.registryAccess()).size()) return;
                WrenchData.selectFluid(stack, index);
            }
            default -> { return; }
        }
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
    }
    public static void sendLaunch(ServerPlayer player, Vec3 velocity) {
        PacketDistributor.sendToPlayer(player, new Launch(velocity));
    }
}
