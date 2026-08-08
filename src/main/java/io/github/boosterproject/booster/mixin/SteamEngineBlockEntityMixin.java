package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.boosterproject.booster.content.kinetics.steamEngine.NetheriteSteamEngineBlockEntity;
import io.github.boosterproject.booster.registry.BoosterBlocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SteamEngineBlockEntity.class, remap = false)
public abstract class SteamEngineBlockEntityMixin {
    @Redirect(
        method = { "tick", "getTargetAngle" },
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = false
        )
    )
    private boolean booster$recognizeNetheriteSteamEngine(BlockEntry<?> entry, BlockState state) {
        return entry.has(state) || (Object) this instanceof NetheriteSteamEngineBlockEntity
            && state.is(BoosterBlocks.NETHERITE_STEAM_ENGINE.get());
    }
}
