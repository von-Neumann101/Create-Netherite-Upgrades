package io.github.boosterproject.booster.content.kinetics.portable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Authority for each reusable item, including while its block or holder is unloaded. */
public final class PortableStressData extends SavedData {
    public static final int RUN_TICKS = 2400;
    public static final int COOLDOWN_TICKS = 2400;
    public record Deployment(UUID token, ResourceKey<Level> dimension, BlockPos pos, long expiresAt) {}
    public record Entry(Deployment deployment, long readyAt) {}
    private final Map<UUID, Entry> entries = new HashMap<>();

    public static PortableStressData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new Factory<>(PortableStressData::new, PortableStressData::load), "createnetherite_portable_stress");
    }
    public static long now(MinecraftServer server) { return server.overworld().getGameTime(); }
    public Entry entry(UUID id) { return entries.get(id); }
    public Map<UUID, Entry> entries() { return Map.copyOf(entries); }
    public boolean available(UUID id, long now) {
        Entry entry = entries.get(id);
        return entry == null || entry.deployment() == null && now >= entry.readyAt();
    }
    public Deployment deploy(UUID id, ResourceKey<Level> dimension, BlockPos pos, long now) {
        if (!available(id, now)) return null;
        Deployment deployment = new Deployment(UUID.randomUUID(), dimension, pos.immutable(), now + RUN_TICKS);
        entries.put(id, new Entry(deployment, 0));
        setDirty();
        return deployment;
    }
    public boolean active(UUID id, UUID token, ResourceKey<Level> dimension, BlockPos pos, long now) {
        Entry entry = entries.get(id);
        if (entry == null || entry.deployment() == null) return false;
        Deployment deployment = entry.deployment();
        return deployment.token().equals(token) && deployment.dimension().equals(dimension)
            && deployment.pos().equals(pos) && now < deployment.expiresAt();
    }
    public boolean finish(UUID id, UUID token, long now) {
        Entry entry = entries.get(id);
        if (entry == null || entry.deployment() == null || !entry.deployment().token().equals(token)) return false;
        // The deadline starts cooldown even when the block wasn't ticking at expiry.
        long endedAt = Math.min(now, entry.deployment().expiresAt());
        entries.put(id, new Entry(null, endedAt + COOLDOWN_TICKS));
        setDirty();
        return true;
    }
    public void prune(long now) {
        if (entries.values().removeIf(entry -> entry.deployment() == null && now >= entry.readyAt())) setDirty();
    }
    public static PortableStressData load(CompoundTag tag, HolderLookup.Provider registries) {
        PortableStressData data = new PortableStressData();
        for (Tag element : tag.getList("Devices", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) element;
            if (!saved.hasUUID("Id")) continue;
            Deployment deployment = null;
            if (saved.hasUUID("Token")) {
                ResourceLocation dimension = ResourceLocation.tryParse(saved.getString("Dimension"));
                if (dimension == null) continue;
                deployment = new Deployment(saved.getUUID("Token"), ResourceKey.create(Registries.DIMENSION, dimension),
                    BlockPos.of(saved.getLong("Pos")), saved.getLong("ExpiresAt"));
            }
            data.entries.put(saved.getUUID("Id"), new Entry(deployment, saved.getLong("ReadyAt")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag devices = new ListTag();
        entries.forEach((id, entry) -> {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("Id", id);
            saved.putLong("ReadyAt", entry.readyAt());
            if (entry.deployment() != null) {
                Deployment deployment = entry.deployment();
                saved.putUUID("Token", deployment.token());
                saved.putString("Dimension", deployment.dimension().location().toString());
                saved.putLong("Pos", deployment.pos().asLong());
                saved.putLong("ExpiresAt", deployment.expiresAt());
            }
            devices.add(saved);
        });
        tag.put("Devices", devices);
        return tag;
    }
}
