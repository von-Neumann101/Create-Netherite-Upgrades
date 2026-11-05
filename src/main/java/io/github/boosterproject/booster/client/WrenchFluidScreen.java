package io.github.boosterproject.booster.client;

import io.github.boosterproject.booster.content.equipment.wrench.WrenchData;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchMenu;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchModule;
import io.github.boosterproject.booster.content.equipment.wrench.WrenchNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;

/** Selection only; source changes and all fluid movement remain server-side. */
public final class WrenchFluidScreen extends Screen {
    private final ItemStack source;
    private List<FluidStack> fluids = List.of();
    private int page, left, top;
    public WrenchFluidScreen(ItemStack source) { super(WrenchScreen.text("fluid_title")); this.source = source; }
    @Override protected void init() {
        left = (width - 288) / 2; top = (height - 184) / 2;
        fluids = WrenchData.fluids(source, minecraft.level.registryAccess());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); }).bounds(left + 90, top + 160, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(Math.max(0, (fluids.size() - 1) / 8), page + 1); }).bounds(left + 176, top + 160, 22, 18).build());
    }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.player.getMainHandItem() != source || WrenchData.active(source) != WrenchModule.TANK) onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(WrenchScreen.BACKGROUND, left, top, 0, 0, 288, 179, 288, 286);
        graphics.drawCenteredString(font, title, left + 144, top + 6, 0xFFE1E3E8);
        graphics.drawCenteredString(font, WrenchScreen.text("capacity", WrenchData.totalFluid(source, minecraft.level.registryAccess())), left + 144, top + 83, 0xFFE1E3E8);
        graphics.drawCenteredString(font, Component.literal((page + 1) + "/" + Math.max(1, (fluids.size() + 7) / 8)), left + 144, top + 164, 0xFFE1E3E8);
        for (int slot = 0; slot < 8; slot++) {
            int index = page * 8 + slot;
            if (index >= fluids.size()) break;
            var fluid = fluids.get(index); var pos = WrenchMenu.POSITIONS[slot];
            var bucket = FluidUtil.getFilledBucket(fluid);
            graphics.renderItem(bucket, left + pos[0], top + pos[1]);
            if (index == WrenchData.selectedFluid(source)) graphics.renderOutline(left + pos[0] - 2, top + pos[1] - 2, 20, 20, 0xFFFFC76A);
            if (mouseX >= left + pos[0] && mouseX < left + pos[0] + 16 && mouseY >= top + pos[1] && mouseY < top + pos[1] + 16)
                graphics.renderTooltip(font, List.of(fluid.getHoverName(), Component.literal(fluid.getAmount() + " mB")), java.util.Optional.empty(), mouseX, mouseY);
        }
        if (fluids.isEmpty()) graphics.drawCenteredString(font, WrenchScreen.text("empty"), left + 144, top + 104, 0xFFD5C6AA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) for (int slot = 0; slot < 8; slot++) {
            var pos = WrenchMenu.POSITIONS[slot]; int index = page * 8 + slot;
            if (index < fluids.size() && x >= left + pos[0] && x < left + pos[0] + 16 && y >= top + pos[1] && y < top + pos[1] + 16) {
                PacketDistributor.sendToServer(new WrenchNetworking.Action(WrenchNetworking.SELECT_FLUID, index));
                onClose(); return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
}
