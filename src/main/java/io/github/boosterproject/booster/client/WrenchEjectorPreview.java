package io.github.boosterproject.booster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchLaunch;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchModule;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Immediate, depth-tested lines; no particles or fading outlines survive a frame. */
public final class WrenchEjectorPreview {
    private static final int MAX_SEGMENTS = 80;
    private static Frame previewFrame;

    private WrenchEjectorPreview() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WrenchEjectorPreview::renderLevel);
        NeoForge.EVENT_BUS.addListener(WrenchEjectorPreview::renderGui);
    }

    private static boolean visible(Minecraft mc) {
        return mc.level != null && mc.player != null && mc.screen == null && !mc.isPaused()
            && !mc.options.hideGui && mc.player.isAlive() && !mc.player.isSpectator() && !mc.player.isPassenger()
            && WrenchData.active(mc.player) == WrenchModule.EJECTOR
            && (Screen.hasShiftDown() || WrenchLaunch.isCharging(mc.player));
    }

    private static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        previewFrame = null;
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;

        Player player = mc.player;
        float power = WrenchLaunch.isCharging(player)
            ? WrenchLaunch.powerForTime(WrenchLaunch.chargeTicks(player)) : 1;
        WrenchLaunch.Preview preview = WrenchLaunch.predict(player, power);
        previewFrame = new Frame(player, preview, power);

        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RenderType.lines());
        float red = preview.blocked() ? 1 : .2F;
        float green = preview.blocked() ? .25F : 1;
        float blue = preview.blocked() ? .2F : .75F;
        // In 21.1.219 AFTER_PARTICLES, LevelRenderer already applied getModelViewMatrix()
        // to RenderSystem's model-view stack. Applying it to this pose would rotate twice.
        // Subtract the camera in double precision before any vertex is converted to float.
        double height = player.getBbHeight() * .5;
        drawPath(poses.last(), vertices, preview.points(), camera, height, red, green, blue);
        drawTarget(poses, vertices, player, preview, camera, height, red, green, blue);
        // Flush before returning so this geometry cannot fade or accumulate between frames.
        // RenderType.lines retains LEQUAL depth testing, including behind solid terrain.
        buffers.endBatch(RenderType.lines());
    }

    private static void drawPath(PoseStack.Pose pose, VertexConsumer vertices, List<Vec3> points,
                                 Vec3 camera, double height, float red, float green, float blue) {
        int last = points.size() - 1;
        int segments = Math.min(MAX_SEGMENTS, last);
        if (segments <= 0) return;
        Vec3 from = points.getFirst().subtract(camera).add(0, height, 0);
        for (int step = 1; step <= segments; step++) {
            Vec3 to = points.get((int) ((long) step * last / segments)).subtract(camera).add(0, height, 0);
            line(pose, vertices, from, to, red, green, blue);
            from = to;
        }
    }

    private static void drawTarget(PoseStack poses, VertexConsumer vertices, Player player,
                                   WrenchLaunch.Preview preview, Vec3 camera, double height,
                                   float red, float green, float blue) {
        // Prediction uses feet coordinates. Lift the footprint a little above the landing surface.
        Vec3 target = preview.target().subtract(camera).add(0, .04, 0);
        double radius = Math.max(.3, player.getBbWidth() * .5);
        LevelRenderer.renderLineBox(poses, vertices, new AABB(target.x - radius, target.y, target.z - radius,
            target.x + radius, target.y + .08, target.z + radius), red, green, blue, 1);
        PoseStack.Pose pose = poses.last();
        line(pose, vertices, target.add(-radius, .04, -radius), target.add(radius, .04, radius), red, green, blue);
        line(pose, vertices, target.add(-radius, .04, radius), target.add(radius, .04, -radius), red, green, blue);
        if (!preview.points().isEmpty())
            line(pose, vertices, preview.points().getLast().subtract(camera).add(0, height, 0), target, red, green, blue);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer vertices, Vec3 from, Vec3 to,
                             float red, float green, float blue) {
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0e-8) return;
        Vec3 normal = delta.normalize();
        vertices.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
            .setColor(red, green, blue, 1).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        vertices.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
            .setColor(red, green, blue, 1).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void renderGui(RenderGuiEvent.Post event) {
        Frame frame = previewFrame;
        previewFrame = null;
        Minecraft mc = Minecraft.getInstance();
        if (frame == null || frame.player() != mc.player || !visible(mc)) return;

        var gui = event.getGuiGraphics();
        var font = mc.font;
        int distance = (int) Math.round(frame.player().position().distanceTo(frame.preview().target()));
        Component charge = WrenchLaunch.isCharging(frame.player())
            ? Component.translatable("wrench.createnetherite.ejector_charge", Math.round(frame.power() * 100), distance)
            : Component.translatable("wrench.createnetherite.ejector_preview", distance);
        Component hint = Component.translatable("wrench.createnetherite.ejector_aim");
        int x = gui.guiWidth() / 2;
        int y = gui.guiHeight() / 2 + 24;
        int halfWidth = Math.max(font.width(charge), font.width(hint)) / 2 + 5;
        gui.fill(x - halfWidth, y - 3, x + halfWidth, y + font.lineHeight * 2 + 3, 0x90000000);
        gui.drawCenteredString(font, charge, x, y, frame.preview().blocked() ? 0xFFFF6655 : 0xFF55FFBB);
        gui.drawCenteredString(font, hint, x, y + font.lineHeight + 2, 0xFFE0E0E0);
    }

    private record Frame(Player player, WrenchLaunch.Preview preview, float power) {}
}
