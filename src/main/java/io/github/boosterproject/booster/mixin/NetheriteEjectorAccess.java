package io.github.boosterproject.booster.mixin;

import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.depot.EntityLauncher;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.data.IntAttached;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EjectorBlockEntity.class, remap = false)
public interface NetheriteEjectorAccess {
    @Accessor("launcher") void booster$setLauncher(EntityLauncher launcher);
    @Accessor("earlyTarget") Pair<Vec3, BlockPos> booster$getEarlyTarget();
    @Accessor("earlyTarget") void booster$setEarlyTarget(Pair<Vec3, BlockPos> target);
    @Accessor("earlyTargetTime") void booster$setEarlyTargetTime(float time);
    @Accessor("scanCooldown") void booster$setScanCooldown(int ticks);
    @Accessor("trackedItem") void booster$setTrackedItem(ItemStack stack);
    @Accessor("launchedItems") List<IntAttached<ItemStack>> booster$getLaunchedItems();
    @Accessor("powered") boolean booster$isPowered();
}
