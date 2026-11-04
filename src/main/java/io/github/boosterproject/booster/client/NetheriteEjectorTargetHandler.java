package io.github.boosterproject.booster.client;

import com.simibubi.create.AllItems;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEjectorBlockEntity;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEjectorItem;
import io.github.boosterproject.booster.content.logistics.depot.NetheriteEntityLauncher;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class NetheriteEjectorTargetHandler {
    private NetheriteEjectorTargetHandler() {}
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()
            || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() == HitResult.Type.MISS) return;
        ItemStack held = mc.player.getMainHandItem();
        BlockPos source;
        BlockPos target;
        if (held.getItem() instanceof NetheriteEjectorItem) {
            target = NetheriteEjectorItem.selectedTarget(held, mc.level);
            if (target == null) return;
            source = mc.level.getBlockEntity(hit.getBlockPos()) instanceof NetheriteEjectorBlockEntity
                ? hit.getBlockPos() : new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, held, hit).getClickedPos();
        } else if (AllItems.WRENCH.isIn(held) && mc.level.getBlockEntity(hit.getBlockPos()) instanceof NetheriteEjectorBlockEntity ejector) {
            source = ejector.getBlockPos();
            target = ejector.getTargetPosition();
        } else return;
        boolean valid = NetheriteEjectorBlockEntity.isValidOffset(target.subtract(source));
        int color = valid ? 0x9ede73 : 0xff7171;
        Outliner.getInstance().showAABB("createnetherite_ejector_target", new AABB(target)).colored(color).lineWidth(1 / 16F);
        Outliner.getInstance().showAABB("createnetherite_ejector_source", new AABB(source)).colored(color).lineWidth(1 / 16F);
        if (!valid) return;
        var trajectory = new NetheriteEntityLauncher(target.subtract(source));
        double total = trajectory.getTotalFlyingTicks();
        int segments = Math.min(64, Math.max(8, (int) Math.ceil(total / 2)));
        var dust = new DustParticleOptions(new Color(color).asVectorF(), 1);
        for (int segment = 0; segment <= segments; segment++) {
            var point = trajectory.getGlobalPos(total * segment / segments, Direction.NORTH, source);
            mc.level.addParticle(dust, point.x, point.y, point.z, 0, 0, 0);
        }
    }
}
