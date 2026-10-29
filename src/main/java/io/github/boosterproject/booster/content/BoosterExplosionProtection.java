package io.github.boosterproject.booster.content;

import io.github.boosterproject.booster.Booster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class BoosterExplosionProtection {
    private BoosterExplosionProtection() {
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> BuiltInRegistries.BLOCK
            .getKey(event.getLevel().getBlockState(pos).getBlock())
            .getNamespace().equals(Booster.MOD_ID));
    }

    public static void onEntityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)
            && event.getEntity() instanceof ItemEntity itemEntity
            && BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem())
                .getNamespace().equals(Booster.MOD_ID)) {
            event.setInvulnerable(true);
        }
    }
}
