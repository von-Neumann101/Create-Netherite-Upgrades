package io.github.boosterproject.booster.content.logistics.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.mojang.brigadier.StringReader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

public final class NetheriteNbtFilterData {
    private static final String KEY = "NbtFilter";
    private static final int REFERENCE_SLOT = NetheriteFilterData.SLOT_COUNT;
    public record Field(List<String> path, Tag value) {
        public String label() { return String.join(" / ", path); }
        public String text() { return value instanceof NumericTag number ? number.getAsNumber().toString() : value.getAsString(); }
        public boolean numeric() { return value instanceof NumericTag; }
    }
    public record Condition(List<String> path, int operator, String first, String second, int kind) {}

    private NetheriteNbtFilterData() {}
    private static CompoundTag config(ItemStack filter) {
        return filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
    }
    public static boolean configured(ItemStack filter) {
        return filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(KEY);
    }
    private static void save(ItemStack filter, CompoundTag config) {
        CompoundTag data = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.put(KEY, config); filter.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
    public static ItemStack storedReference(ItemStack filter) {
        var contents = filter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return contents.getSlots() > REFERENCE_SLOT ? contents.getStackInSlot(REFERENCE_SLOT).copy() : ItemStack.EMPTY;
    }
    public static ItemStack reference(ItemStack filter, HolderLookup.Provider registries) {
        ItemStack stored = storedReference(filter);
        return stored.isEmpty() ? ItemStack.parseOptional(registries, config(filter).getCompound("Reference")) : stored;
    }
    public static boolean hasReference(ItemStack filter) {
        return !storedReference(filter).isEmpty() || config(filter).getCompound("Reference").contains("id", Tag.TAG_STRING);
    }
    public static void setReference(ItemStack filter, ItemStack reference) {
        var old = filter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> samples = NonNullList.withSize(REFERENCE_SLOT + 1, ItemStack.EMPTY);
        for (int slot = 0; slot < Math.min(old.getSlots(), REFERENCE_SLOT); slot++) samples.set(slot, old.getStackInSlot(slot).copy());
        samples.set(REFERENCE_SLOT, reference.isEmpty() ? ItemStack.EMPTY : reference.copyWithCount(1));
        filter.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(samples));
        CompoundTag config = config(filter); config.remove("Reference"); config.remove("Conditions");
        if (!config.contains("SameItem")) config.putBoolean("SameItem", true);
        save(filter, config);
    }
    public static boolean sameItem(ItemStack filter) { CompoundTag config = config(filter); return !config.contains("SameItem") || config.getBoolean("SameItem"); }
    public static boolean blacklist(ItemStack filter) { return config(filter).getBoolean("Blacklist"); }
    public static void toggleSameItem(ItemStack filter) { CompoundTag config = config(filter); config.putBoolean("SameItem", !sameItem(filter)); save(filter, config); }
    public static void toggleBlacklist(ItemStack filter) { CompoundTag config = config(filter); config.putBoolean("Blacklist", !blacklist(filter)); save(filter, config); }
    public static CompoundTag snapshot(ItemStack reference, HolderLookup.Provider registries) {
        if (reference.isEmpty()) return new CompoundTag();
        return (CompoundTag) DataComponentMap.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), reference.getComponents()).getOrThrow();
    }
    public static List<Field> fields(ItemStack reference, HolderLookup.Provider registries) {
        List<Field> result = new ArrayList<>();
        flatten(snapshot(reference, registries), List.of(), result); return result;
    }
    private static void flatten(Tag value, List<String> path, List<Field> fields) {
        if (!path.isEmpty() && (!(value instanceof CompoundTag compound) || compound.isEmpty())
            && (!(value instanceof CollectionTag<?> list) || list.isEmpty())) fields.add(new Field(path, value.copy()));
        if (value instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys().stream().sorted().toList()) {
                List<String> child = new ArrayList<>(path); child.add(key); flatten(compound.get(key), List.copyOf(child), fields);
            }
        } else if (value instanceof CollectionTag<?> list) {
            for (int i = 0; i < list.size(); i++) {
                List<String> child = new ArrayList<>(path); child.add(Integer.toString(i)); flatten(list.get(i), List.copyOf(child), fields);
            }
        }
    }
    public static List<Condition> conditions(ItemStack filter) {
        List<Condition> result = new ArrayList<>();
        for (Tag entry : config(filter).getList("Conditions", Tag.TAG_COMPOUND)) {
            CompoundTag condition = (CompoundTag) entry; List<String> path = new ArrayList<>();
            for (Tag segment : condition.getList("Path", Tag.TAG_STRING)) path.add(segment.getAsString());
            result.add(new Condition(List.copyOf(path), condition.getInt("Op"), condition.getString("First"), condition.getString("Second"), condition.getInt("Kind")));
        }
        return result;
    }
    public static Condition condition(ItemStack filter, Field field) {
        return conditions(filter).stream().filter(value -> value.path.equals(field.path)).findFirst().orElse(null);
    }
    public static boolean setCondition(ItemStack filter, Field field, int operator, String first, String second) {
        if (field.path.isEmpty() || operator < 0 || operator > 3 || (!field.numeric() && operator != 0)) return false;
        try {
            if (field.numeric()) {
                BigDecimal minimum = new BigDecimal(first);
                if (operator == 3 && minimum.compareTo(new BigDecimal(second)) > 0) return false;
            } else if (!(field.value instanceof StringTag)) parseValue(first);
        } catch (Exception invalid) { return false; }
        List<Condition> conditions = new ArrayList<>(conditions(filter)); conditions.removeIf(value -> value.path.equals(field.path));
        conditions.add(new Condition(field.path, operator, first, operator == 3 ? second : "", field.numeric() ? 0 : field.value.getId()));
        saveConditions(filter, conditions); return true;
    }
    public static void toggleCondition(ItemStack filter, Field field) {
        List<Condition> conditions = new ArrayList<>(conditions(filter));
        if (conditions.removeIf(value -> value.path.equals(field.path))) saveConditions(filter, conditions);
        else setCondition(filter, field, 0, field.text(), "");
    }
    public static void clearConditions(ItemStack filter) { saveConditions(filter, List.of()); }
    private static void saveConditions(ItemStack filter, List<Condition> conditions) {
        ListTag list = new ListTag();
        for (Condition value : conditions) {
            CompoundTag tag = new CompoundTag(); ListTag path = new ListTag();
            value.path.forEach(segment -> path.add(StringTag.valueOf(segment)));
            tag.put("Path", path); tag.putInt("Op", value.operator); tag.putString("First", value.first); tag.putString("Second", value.second); tag.putInt("Kind", value.kind); list.add(tag);
        }
        CompoundTag config = config(filter); config.put("Conditions", list); save(filter, config);
    }
    private static Tag parseValue(String value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        StringReader reader = new StringReader(value); Tag result = new TagParser(reader).readValue(); reader.skipWhitespace();
        if (reader.canRead()) throw new IllegalArgumentException("Trailing NBT input");
        return result;
    }
    private static Tag at(Tag root, List<String> path) {
        Tag result = root;
        for (String segment : path) {
            if (result instanceof CompoundTag compound) result = compound.get(segment);
            else if (result instanceof CollectionTag<?> list) {
                try { int index = Integer.parseInt(segment); result = index >= 0 && index < list.size() ? list.get(index) : null; }
                catch (NumberFormatException invalid) { return null; }
            } else return null;
            if (result == null) return null;
        }
        return result;
    }
    private static boolean matches(Tag target, Condition condition) {
        if (target == null || condition.path.isEmpty()) return false;
        try {
            if (condition.kind == 0) {
                if (!(target instanceof NumericTag number)) return false;
                BigDecimal actual = new BigDecimal(number.getAsNumber().toString()), first = new BigDecimal(condition.first);
                int comparison = actual.compareTo(first);
                return switch (condition.operator) {
                    case 0 -> comparison == 0;
                    case 1 -> comparison > 0;
                    case 2 -> comparison < 0;
                    case 3 -> comparison >= 0 && actual.compareTo(new BigDecimal(condition.second)) <= 0;
                    default -> false;
                };
            }
            return condition.operator == 0 && target.getId() == condition.kind
                && (target instanceof StringTag ? target.getAsString().equals(condition.first) : target.equals(parseValue(condition.first)));
        } catch (Exception invalid) { return false; }
    }
    public static boolean matches(ItemStack filter, ItemStack target, HolderLookup.Provider registries) {
        ItemStack reference = reference(filter, registries);
        if (reference.isEmpty() || target.isEmpty()) return false;
        boolean matches = !sameItem(filter) || target.is(reference.getItem());
        List<Condition> conditions = conditions(filter);
        if (matches && !conditions.isEmpty()) {
            CompoundTag data = snapshot(target, registries);
            for (Condition condition : conditions) if (!matches(at(data, condition.path), condition)) { matches = false; break; }
        }
        return blacklist(filter) ? !matches : matches;
    }
}
