package io.github.boosterproject.booster.registry;

import io.github.boosterproject.booster.Booster;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class BoosterFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Booster.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Booster.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Booster.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Booster.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> ESSENCE_TYPE = TYPES.register("nether_essence", () -> new FluidType(
        FluidType.Properties.create().descriptionId("fluid.createnetherite.nether_essence").density(1500).viscosity(3000)
            .lightLevel(6).canConvertToSource(false)) {});
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> NETHER_ESSENCE = FLUIDS.register("nether_essence", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_NETHER_ESSENCE = FLUIDS.register("flowing_nether_essence", () -> new BaseFlowingFluid.Flowing(properties()));
    public static final DeferredBlock<LiquidBlock> ESSENCE_BLOCK = BLOCKS.register("nether_essence", () -> new LiquidBlock(
        NETHER_ESSENCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(state -> 6)));
    public static final DeferredItem<BucketItem> ESSENCE_BUCKET = ITEMS.register("nether_essence_bucket", () -> new BucketItem(
        NETHER_ESSENCE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).fireResistant()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(ESSENCE_TYPE, NETHER_ESSENCE, FLOWING_NETHER_ESSENCE)
            .bucket(ESSENCE_BUCKET).block(ESSENCE_BLOCK).tickRate(20);
    }

    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); BLOCKS.register(bus); ITEMS.register(bus); }
    private BoosterFluids() {}
}
