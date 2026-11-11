package io.github.boosterproject.booster.content.kinetics.portable;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class PortableStressService {
    private PortableStressService() {}
    public static void recall(MinecraftServer server, UUID id) {
        var data = PortableStressData.get(server);
        var entry = data.entry(id);
        if (entry == null || entry.deployment() == null) return;
        data.finish(id, entry.deployment().token(), PortableStressData.now(server));
        removeLoaded(server, id, entry.deployment());
    }
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var data = PortableStressData.get(server);
        long now = PortableStressData.now(server);
        for (var entry : data.entries().entrySet()) {
            var deployment = entry.getValue().deployment();
            if (deployment != null && now >= deployment.expiresAt()) {
                data.finish(entry.getKey(), deployment.token(), now);
                removeLoaded(server, entry.getKey(), deployment);
            }
        }
        data.prune(now);
    }
    private static void removeLoaded(MinecraftServer server, UUID id, PortableStressData.Deployment deployment) {
        var level = server.getLevel(deployment.dimension());
        // Unloaded expired/recalled blocks remove themselves before restoring their network.
        if (level != null && level.hasChunkAt(deployment.pos())
            && level.getBlockEntity(deployment.pos()) instanceof PortableStressDeviceBlockEntity device
            && device.matches(id, deployment.token())) level.removeBlock(deployment.pos(), false);
    }
}
