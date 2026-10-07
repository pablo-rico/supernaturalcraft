package org.papiricoh.supernaturalcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.forge.HellforgeMenu;

/** The Hellforge: weapon on the left, runes to grave beside it, what it costs (or why it can't). */
public class HellforgeScreen extends AbstractContainerScreen<HellforgeMenu> {

    private static final ResourceLocation BG = SupernaturalCraft.asResource("textures/gui/hellforge.png");
    private Button inscribe, purge;

    public HellforgeScreen(HellforgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        inscribe = addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.hellforge.inscribe"),
                b -> click(HellforgeMenu.INSCRIBE)).bounds(leftPos + 62, topPos + 56, 62, 16).build());
        purge = addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.hellforge.purge"),
                b -> click(HellforgeMenu.PURGE)).bounds(leftPos + 126, topPos + 56, 44, 16).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        inscribe.active = menu.inscribeProblem() == null;
        purge.active = !menu.graved().runes().isEmpty();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int free = menu.freeSlots();
        for (int i = 0; i < HellforgeMenu.RUNE_SLOTS; i++) {
            int x = leftPos + 61 + i * 20, y = topPos + 34;
            if (i >= free) g.fill(x, y, x + 18, y + 18, 0xC0201010);
        }
        int x = leftPos + 8;
        for (Rune r : menu.graved().runes()) {
            g.fill(x, topPos + 58, x + 6, topPos + 64, 0xFF000000 | r.color);
            x += 8;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xE8D9A8, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB0A080, false);
        String problem = menu.inscribeProblem();
        Component line;
        int color;
        if (problem == null) {
            line = Component.translatable("screen.supernaturalcraft.hellforge.cost", menu.inscribeCost());
            color = 0x7FE07A;
        } else if (problem.equals("no_weapon") || problem.equals("no_runes")) {
            line = Component.translatable("screen.supernaturalcraft.hellforge." + problem);
            color = 0xB0A080;
        } else {
            line = Component.translatable("screen.supernaturalcraft.hellforge." + problem);
            color = 0xFF6A5A;
        }
        g.drawString(font, line, 62, 22, color, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }
}
