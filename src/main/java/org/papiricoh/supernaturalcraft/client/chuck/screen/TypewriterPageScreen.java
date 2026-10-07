package org.papiricoh.supernaturalcraft.client.chuck.screen;

import net.minecraft.network.chat.Component;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * The half-typed page in the Author's typewriter: a draft about whoever is reading it. One line for each great enemy
 * they have beaten ({@code typewriter.supernaturalcraft.page.<boss>}), then where the story stands
 * ({@code .unfinished}, {@code .waiting} once every boss before him is beaten, {@code .expected} once he expects
 * them), and the cursor, waiting.
 */
public class TypewriterPageScreen extends PaperScreen {

    private static final int W = 230, MARGIN = 20, TOP = 34, ROW = 10;
    private static final List<String> BEFORE_HIM = List.of("azazel", "lilith", "lucifer", "broken_chorus", "metatron", "amara", "lucifer_uncaged");

    private final List<String> rows;
    private int sounded;

    public TypewriterPageScreen(List<String> beaten, boolean expected) {
        super(Component.translatable("screen.supernaturalcraft.author.page"), W, 250);
        String name = net.minecraft.client.Minecraft.getInstance().player == null ? "the hunter"
                : net.minecraft.client.Minecraft.getInstance().player.getGameProfile().getName();
        StringBuilder text = new StringBuilder(Component.translatable("typewriter.supernaturalcraft.page.start", name).getString());
        for (String b : beaten) {
            if (!BEFORE_HIM.contains(b)) continue;
            text.append(' ').append(Component.translatable("typewriter.supernaturalcraft.page." + b, name).getString());
        }
        boolean all = beaten.containsAll(BEFORE_HIM);
        String end = expected ? "expected" : all ? "waiting" : beaten.isEmpty() ? "nothing" : "unfinished";
        text.append("\n\n").append(Component.translatable("typewriter.supernaturalcraft.page." + end, name).getString());
        this.rows = new ArrayList<>(Paper.wrap(net.minecraft.client.Minecraft.getInstance().font, text.toString(), W - MARGIN * 2));
        this.pageH = Math.max(150, TOP + rows.size() * ROW + 24);
    }

    private int letters() {
        int n = 0;
        for (String r : rows) n += r.length();
        return n;
    }

    @Override
    public void tick() {
        super.tick();
        int n = Math.min(age * 3, letters());
        if (n > sounded && age % 2 == 0) Paper.sound(AllSounds.CHUCK_TYPE.get(), 0.9f + (float) Math.random() * 0.2f, 0.35f);
        sounded = n;
    }

    @Override
    protected void drawPage(net.minecraft.client.gui.GuiGraphics g, double mx, double my, float partial) {
        String head = Component.translatable("typewriter.supernaturalcraft.page.title").getString();
        Paper.type(g, font, head, (W - Paper.width(font, head)) / 2, 14, head.length(), Paper.INK);
        g.fill((W - Paper.width(font, head)) / 2, 24, (W + Paper.width(font, head)) / 2, 25, Paper.INK);
        int left = (int) ((age + partial) * 3);
        int y = TOP, cx = MARGIN;
        for (String r : rows) {
            int n = Math.max(0, Math.min(left, r.length()));
            cx = Paper.type(g, font, r, MARGIN, y, n, Paper.INK);
            left -= r.length();
            if (left <= 0) break;
            y += ROW;
        }
        Paper.cursor(g, cx + 1, y, ROW);
    }

    @Override
    protected boolean clickPage(double x, double y, int button) {
        if (button == 0 && age * 3 < letters()) {
            age = letters() / 3 + 1;
            return true;
        }
        if (button == 0) {
            onClose();
            return true;
        }
        return false;
    }
}
