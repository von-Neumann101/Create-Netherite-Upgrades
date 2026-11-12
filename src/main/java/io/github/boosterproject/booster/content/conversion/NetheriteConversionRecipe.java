package io.github.boosterproject.booster.content.conversion;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import io.github.boosterproject.booster.registry.BoosterRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;

public class NetheriteConversionRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    private int essenceAmount = 1000;
    public NetheriteConversionRecipe(ProcessingRecipeParams params) { super(BoosterRecipeTypes.CONVERSION, params); }
    public int getEssenceAmount() { return essenceAmount; }
    @Override protected int getMaxInputCount() { return 1; }
    @Override protected int getMaxOutputCount() { return 1; }
    public boolean upgradesWrenchActivation() {
        return !getRollableResults().isEmpty() && WrenchData.isWrench(getRollableResults().getFirst().getStack())
            && WrenchData.activationLimit(getRollableResults().getFirst().getStack()) == 2;
    }
    @Override public boolean matches(SingleRecipeInput input, Level level) {
        if (ingredients.size() != 1 || !ingredients.getFirst().test(input.item())) return false;
        return !upgradesWrenchActivation() || WrenchData.isWrench(input.item()) && WrenchData.activationLimit(input.item()) < 2;
    }

    public static final class Serializer implements RecipeSerializer<NetheriteConversionRecipe> {
        private final StandardProcessingRecipe.Serializer<NetheriteConversionRecipe> base = new StandardProcessingRecipe.Serializer<>(NetheriteConversionRecipe::new);
        private final MapCodec<NetheriteConversionRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            base.codec().forGetter(recipe -> recipe),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("fluid_amount", 1000).forGetter(NetheriteConversionRecipe::getEssenceAmount)
        ).apply(instance, (recipe, cost) -> { recipe.essenceAmount = cost; return recipe; }));
        private final StreamCodec<RegistryFriendlyByteBuf, NetheriteConversionRecipe> streamCodec = StreamCodec.of((buf, recipe) -> {
            base.streamCodec().encode(buf, recipe); buf.writeVarInt(recipe.essenceAmount);
        }, buf -> {
            var recipe = base.streamCodec().decode(buf);
            recipe.essenceAmount = buf.readVarInt();
            if (recipe.essenceAmount < 1) throw new IllegalArgumentException("Essence amount must be positive");
            return recipe;
        });
        @Override public MapCodec<NetheriteConversionRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, NetheriteConversionRecipe> streamCodec() { return streamCodec; }
    }
}
