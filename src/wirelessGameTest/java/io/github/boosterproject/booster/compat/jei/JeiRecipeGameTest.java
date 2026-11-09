package io.github.boosterproject.booster.compat.jei;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import io.github.boosterproject.booster.registry.BoosterItems;
import io.github.boosterproject.booster.registry.BoosterFluids;
import io.github.boosterproject.booster.content.conversion.NetheriteConversionRecipe;
import io.github.boosterproject.booster.content.processing.burner.NetheriteBlazeBurnerData;
import io.github.boosterproject.booster.Booster;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class JeiRecipeGameTest {
    @GameTest(templateNamespace = Booster.MOD_ID, template = "wireless_empty", timeoutTicks = 40)
    public static void customRecipeCategoriesAreRegistered(GameTestHelper helper) throws Exception {
        IModPlugin plugin;
        try {
            plugin = (IModPlugin) Class.forName("io.github.boosterproject.booster.compat.jei.BoosterJeiPlugin").getConstructor().newInstance();
        } catch (ClassNotFoundException missing) {
            helper.fail("custom mixing and conversion need a discoverable JEI plugin");
            return;
        }
        List<IRecipeCategory<?>> categories = new ArrayList<>();
        var registration = (IRecipeCategoryRegistration) Proxy.newProxyInstance(JeiRecipeGameTest.class.getClassLoader(),
            new Class<?>[] {IRecipeCategoryRegistration.class}, (proxy, method, args) -> {
                if (method.getName().equals("addRecipeCategories")) categories.addAll(List.of((IRecipeCategory<?>[]) args[0]));
                return null;
            });
        plugin.registerCategories(registration);
        helper.assertTrue(categories.size() == 2, "both custom recipe types need categories");
        helper.assertTrue(categories.stream().anyMatch(category -> category.getRecipeType().getUid().toString().equals("createnetherite:netherite_mixing"))
            && categories.stream().anyMatch(category -> category.getRecipeType().getUid().toString().equals("createnetherite:netherite_conversion")),
            "custom recipes must be indexed separately from ordinary Create mixing");
        helper.succeed();
    }

    @GameTest(templateNamespace = Booster.MOD_ID, template = "wireless_empty", timeoutTicks = 40)
    public static void liveRecipesExposeEssenceAndAllConversions(GameTestHelper helper) throws Exception {
        Setup setup = setup(helper);
        helper.assertTrue(setup.recipes.get("createnetherite:netherite_mixing").size() == 1
            && setup.recipes.get("createnetherite:netherite_conversion").size() == 18,
            "JEI must receive the real exclusive mixing recipe and all eighteen live conversions");
        List<Slot> mixing = layout(setup, "netherite_mixing", "mixing/nether_essence");
        helper.assertTrue(hasItem(mixing, RecipeIngredientRole.INPUT, Items.BLAZE_POWDER)
            && hasItem(mixing, RecipeIngredientRole.INPUT, Items.NETHER_WART)
            && fluid(mixing, RecipeIngredientRole.INPUT, Fluids.LAVA) == 1000
            && fluid(mixing, RecipeIngredientRole.OUTPUT, BoosterFluids.NETHER_ESSENCE.get()) == 1000,
            "the actual JEI layout indexes both ingredients and the exact lava/essence quantities");
        List<Slot> conversion = layout(setup, "netherite_conversion", "conversion/netherite_netherite_wrench");
        helper.assertTrue(hasItem(conversion, RecipeIngredientRole.INPUT, BoosterItems.NETHERITE_WRENCH.get())
            && hasItem(conversion, RecipeIngredientRole.OUTPUT, BoosterItems.NETHERITE_NETHERITE_WRENCH.get())
            && fluid(conversion, RecipeIngredientRole.INPUT, BoosterFluids.NETHER_ESSENCE.get()) == 250,
            "advanced wrench conversion is searchable with its basic wrench input and 250mB fluid");
        var advanced = (NetheriteConversionRecipe) setup.recipes.get("createnetherite:netherite_conversion").stream()
            .filter(recipe -> recipe.id().getPath().equals("conversion/netherite_netherite_wrench")).findFirst().orElseThrow().value();
        helper.assertTrue(advanced.getNetheriteCost() == 16
            && !hasItem(conversion, RecipeIngredientRole.INPUT, BoosterItems.NETHERITE_SHEET.get())
            && hasItem(conversion, RecipeIngredientRole.RENDER_ONLY, BoosterItems.NETHERITE_SHEET.get()),
            "stored value cost remains sixteen; point sources must not be misrepresented as per-recipe item consumption");
        helper.succeed();
    }

    @GameTest(templateNamespace = Booster.MOD_ID, template = "wireless_empty", timeoutTicks = 40)
    public static void allCurrentItemsHaveRecipesAndBucketFilling(GameTestHelper helper) {
        Set<Item> obtainable = new HashSet<>();
        for (RecipeHolder<?> recipe : helper.getLevel().getRecipeManager().getRecipes())
            obtainable.add(recipe.value().getResultItem(helper.getLevel().registryAccess()).getItem());
        for (var item : BoosterItems.ITEMS.getEntries()) {
            helper.assertTrue(obtainable.contains(item.get()), "missing acquisition recipe for " + item.getId());
        }
        ItemStack bucket = new ItemStack(Items.BUCKET);
        FluidStack essence = new FluidStack(BoosterFluids.NETHER_ESSENCE.get(), 1000);
        helper.assertTrue(GenericItemFilling.canItemBeFilled(helper.getLevel(), bucket)
            && GenericItemFilling.getRequiredAmountForItem(helper.getLevel(), bucket, essence) == 1000,
            "the essence bucket qualifies for native Create generic filling and its JEI-generated route");
        ItemStack filled = GenericItemFilling.fillItem(helper.getLevel(), 1000, bucket, essence);
        helper.assertTrue(filled.is(BoosterFluids.ESSENCE_BUCKET.get()) && bucket.isEmpty() && essence.isEmpty(),
            "actual filling consumes one empty bucket and exactly 1000mB to produce an essence bucket");
        helper.succeed();
    }

    @GameTest(templateNamespace = Booster.MOD_ID, template = "wireless_empty", timeoutTicks = 40)
    @SuppressWarnings("unchecked")
    public static void catalystsAndEmptyBurnerSubtypeAreAccurate(GameTestHelper helper) throws Exception {
        Setup setup = setup(helper);
        Map<Item, Set<String>> catalysts = new HashMap<>();
        setup.plugin.registerRecipeCatalysts(proxy(IRecipeCatalystRegistration.class, (instance, method, args) -> {
            if (method.getName().equals("addRecipeCatalyst")) {
                Item item = args[0] instanceof ItemStack stack ? stack.getItem() : ((ItemLike) args[0]).asItem();
                for (RecipeType<?> type : (RecipeType<?>[]) args[1]) catalysts.computeIfAbsent(item, key -> new HashSet<>()).add(type.getUid().toString());
            }
            return null;
        }));
        helper.assertTrue(catalysts.get(BoosterItems.NETHERITE_MECHANICAL_MIXER.get()).containsAll(Set.of("createnetherite:netherite_mixing", "create:mixing", "create:automatic_shapeless", "create:automatic_brewing"))
            && catalysts.get(BoosterItems.NETHERITE_CONVERTER.get()).contains("createnetherite:netherite_conversion"), "upgraded processing machines open their supported categories");
        helper.assertTrue(!catalysts.getOrDefault(com.simibubi.create.AllBlocks.MECHANICAL_MIXER.asItem(), Set.of()).contains("createnetherite:netherite_mixing"),
            "ordinary mixers must not be advertised for exclusive essence recipes");
        helper.assertTrue(catalysts.get(BoosterItems.NETHERITE_MECHANICAL_PRESS.get()).containsAll(Set.of("create:pressing", "create:packing", "create:automatic_packing"))
            && catalysts.get(BoosterItems.NETHERITE_ENCASED_FAN.get()).size() == 4
            && catalysts.get(BoosterItems.NETHERITE_MECHANICAL_SAW.get()).containsAll(Set.of("create:sawing", "create:block_cutting"))
            && catalysts.get(BoosterItems.NETHERITE_DEPLOYER.get()).contains("create:deploying"), "press, saw, deployer and all four fan modes have recipe entry points");
        List<ISubtypeInterpreter<ItemStack>> interpreters = new ArrayList<>();
        setup.plugin.registerItemSubtypes(proxy(ISubtypeRegistration.class, (instance, method, args) -> {
            if (method.getName().equals("registerSubtypeInterpreter")) interpreters.add((ISubtypeInterpreter<ItemStack>) args[1]);
            return null;
        }));
        ItemStack empty = new ItemStack(BoosterItems.NETHERITE_BLAZE_BURNER.get()), full = empty.copy(), four = empty.copy();
        NetheriteBlazeBurnerData.setCount(empty, 0); NetheriteBlazeBurnerData.setCount(full, 1); NetheriteBlazeBurnerData.setCount(four, 4);
        var interpreter = interpreters.getFirst();
        helper.assertTrue(!interpreter.getSubtypeData(empty, UidContext.Recipe).equals(interpreter.getSubtypeData(full, UidContext.Recipe))
            && interpreter.getSubtypeData(full, UidContext.Recipe).equals(interpreter.getSubtypeData(four, UidContext.Recipe)),
            "empty burner recipes stay separate while filled counts share their obtainable recipe");
        helper.succeed();
    }

    private record Setup(IModPlugin plugin, List<IRecipeCategory<?>> categories, Map<String, List<RecipeHolder<?>>> recipes) {}

    private static Setup setup(GameTestHelper helper) throws Exception {
        IModPlugin plugin = (IModPlugin) Class.forName("io.github.boosterproject.booster.compat.jei.BoosterJeiPlugin").getConstructor().newInstance();
        List<IRecipeCategory<?>> categories = new ArrayList<>();
        plugin.registerCategories(proxy(IRecipeCategoryRegistration.class, (instance, method, args) -> {
            if (method.getName().equals("addRecipeCategories")) categories.addAll(List.of((IRecipeCategory<?>[]) args[0]));
            return null;
        }));
        Map<String, List<RecipeHolder<?>>> recipes = new HashMap<>();
        IRecipeRegistration registration = proxy(IRecipeRegistration.class, (instance, method, args) -> {
            if (method.getName().equals("addRecipes")) {
                List<RecipeHolder<?>> values = new ArrayList<>();
                for (Object recipe : (List<?>) args[1]) values.add((RecipeHolder<?>) recipe);
                recipes.put(((RecipeType<?>) args[0]).getUid().toString(), values);
            }
            return null;
        });
        plugin.getClass().getMethod("registerRecipesFromManager", IRecipeRegistration.class, RecipeManager.class)
            .invoke(plugin, registration, helper.getLevel().getRecipeManager());
        return new Setup(plugin, categories, recipes);
    }

    private record Slot(RecipeIngredientRole role, List<Object> ingredients) {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<Slot> layout(Setup setup, String type, String path) {
        String uid = "createnetherite:" + type;
        IRecipeCategory category = setup.categories.stream().filter(value -> value.getRecipeType().getUid().toString().equals(uid)).findFirst().orElseThrow();
        RecipeHolder<?> recipe = setup.recipes.get(uid).stream().filter(value -> value.id().getPath().equals(path)).findFirst().orElseThrow();
        List<Slot> slots = new ArrayList<>();
        var builder = proxy(IRecipeLayoutBuilder.class, (instance, method, args) -> {
            if (method.getName().equals("addSlot")) {
                Slot slot = new Slot((RecipeIngredientRole) args[0], new ArrayList<>()); slots.add(slot);
                return proxy(IRecipeSlotBuilder.class, (slotProxy, slotMethod, slotArgs) -> {
                    switch (slotMethod.getName()) {
                        case "addIngredients" -> {
                            if (slotArgs[0] instanceof Ingredient ingredient) slot.ingredients.addAll(List.of(ingredient.getItems()));
                            else slot.ingredients.addAll((List<?>) slotArgs[1]);
                        }
                        case "addItemStacks" -> slot.ingredients.addAll((List<?>) slotArgs[0]);
                        case "addItemStack" -> slot.ingredients.add(slotArgs[0]);
                        case "addIngredient" -> slot.ingredients.add(slotArgs[1]);
                    }
                    return slotProxy;
                });
            }
            return null;
        });
        category.setRecipe(builder, recipe, null);
        return slots;
    }

    private static boolean hasItem(List<Slot> slots, RecipeIngredientRole role, Item item) {
        return slots.stream().filter(slot -> slot.role == role).flatMap(slot -> slot.ingredients.stream()).anyMatch(value -> value instanceof ItemStack stack && stack.is(item));
    }

    private static int fluid(List<Slot> slots, RecipeIngredientRole role, net.minecraft.world.level.material.Fluid fluid) {
        return slots.stream().filter(slot -> slot.role == role).flatMap(slot -> slot.ingredients.stream())
            .filter(value -> value instanceof FluidStack stack && stack.is(fluid)).mapToInt(value -> ((FluidStack) value).getAmount()).sum();
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(JeiRecipeGameTest.class.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
