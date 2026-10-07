package org.papiricoh.supernaturalcraft.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.item.SpellScrollItem;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBook;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

/**
 * A slim vertical mana vial to the left of the hotbar, with the open grimoire page's sigils beside
 * it. Hidden while mana is full and no magic item is in hand.
 */
public class ManaHudOverlay implements LayeredDraw.Layer {

    private static final ResourceLocation FRAME = SupernaturalCraft.asResource("textures/gui/mana_bar.png");
    private static final int BAR_W = 9, BAR_H = 22;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;
        ItemStack held = magicItem(player);
        float mana = ClientArcana.mana(), max = ClientArcana.maxMana();
        float sanity = ClientArcana.sanity();
        int x = g.guiWidth() / 2 - 91 - BAR_W - 3;
        int y = g.guiHeight() - BAR_H;
        if (sanity < 99.5f) {
            // Sanity: a thin pale column beside the vial that darkens as it drains.
            int sx = x - 5, inner = BAR_H - 4, fill = Math.round(inner * sanity / 100f);
            g.fill(sx, y + 1, sx + 3, y + BAR_H - 1, 0xFF14100F);
            int color = sanity < 30 ? 0xFF8A2A6A : 0xFFB8C8D8;
            g.fill(sx + 1, y + 2 + inner - fill, sx + 2, y + 2 + inner, color);
            x -= 6;
        }
        if (held.isEmpty() && mana >= max - 0.5f) return;
        x += sanity < 99.5f ? 6 : 0;
        // Frame (left half of the texture) and liquid (right half), filled bottom-up.
        g.blit(FRAME, x, y, 0, 0, BAR_W, BAR_H, 32, 32);
        int inner = BAR_H - 4;
        int fill = Math.round(inner * Math.min(1f, mana / Math.max(1f, max)));
        boolean cooling = player.level().getGameTime() < ClientArcana.cooldownUntil();
        int u = cooling ? 2 * BAR_W : BAR_W;
        g.blit(FRAME, x + 2, y + 2 + inner - fill, u + 2, 2 + inner - fill, BAR_W - 4, fill, 32, 32);

        if (held.isEmpty()) return;
        Spell spell = held.getItem() instanceof GrimoireItem
                ? held.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY).current()
                : held.get(AllDataComponents.SCROLL_SPELL);
        if (spell == null || spell.isEmpty()) return;
        int gx = x - 3;
        for (ResourceLocation id : GrimoireItem.allSigils(spell).reversed()) {
            gx -= 10;
            g.pose().pushPose();
            g.pose().translate(gx, y + 12, 0);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.blit(SigilComponent.glyphTexture(id), 0, 0, 0, 0, 16, 16, 16, 16);
            g.pose().popPose();
        }
        Font font = mc.font;
        Component name = GrimoireItem.spellName(spell);
        int w = font.width(name);
        g.drawString(font, name, x - 3 - w, y + 1, 0xE8D9A8, true);
    }

    private static ItemStack magicItem(Player player) {
        for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (s.getItem() instanceof GrimoireItem || s.getItem() instanceof SpellScrollItem) return s;
        }
        return ItemStack.EMPTY;
    }
}
