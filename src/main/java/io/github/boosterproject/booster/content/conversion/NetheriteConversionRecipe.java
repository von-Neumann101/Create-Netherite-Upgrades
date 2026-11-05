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

public class NetheriteConversionRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    private int netheriteCost = 1;
    public NetheriteConversionRecipe(ProcessingRecipeParams params) { super(BoosterRecipeTypes.CONVERSION, params); }
    public int getNetheriteCost() { return netheriteCost; }
    @Override protected int getMaxInputCount() { return 1; }
    @Override protected int getMaxOutputCount() { return 1; }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredients.size() == 1 && ingredients.getFirst().test(input.item()); }

    public static final class Serializer implements RecipeSerializer<NetheriteConversionRecipe> {
        private final StandardProcessingRecipe.Serializer<NetheriteConversionRecipe> base = new StandardProcessingRecipe.Serializer<>(NetheriteConversionRecipe::new);
        private final MapCodec<NetheriteConversionRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            base.codec().forGetter(recipe -> recipe),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("netherite_cost", 1).forGetter(NetheriteConversionRecipe::getNetheriteCost)
        ).apply(instance, (recipe, cost) -> { recipe.netheriteCost = cost; return recipe; }));
        private final StreamCodec<RegistryFriendlyByteBuf, NetheriteConversionRecipe> streamCodec = StreamCodec.of((buf, recipe) -> {
            base.streamCodec().encode(buf, recipe); buf.writeVarInt(recipe.netheriteCost);
        }, buf -> {
            var recipe = base.streamCodec().decode(buf);
            recipe.netheriteCost = buf.readVarInt();
            if (recipe.netheriteCost < 1) throw new IllegalArgumentException("Netherite cost must be positive");
            return recipe;
        });
        @Override public MapCodec<NetheriteConversionRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, NetheriteConversionRecipe> streamCodec() { return streamCodec; }
    }
}
