package org.papiricoh.supernaturalcraft.client.chuck.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.AuthorChoicePayload;
import org.papiricoh.supernaturalcraft.network.AuthorDialoguePayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * The conversation with the Author, on a typewritten page: what he says is struck letter by letter as he "types" it
 * (a click finishes it), then the hunter's possible answers appear as typed lines to click. Each answer goes to the
 * server ({@link AuthorChoicePayload}); his reply turns the page ({@link #turn}).
 */
public class AuthorDialogueScreen extends PaperScreen {

    private static final int W = 320, MARGIN = 22, TOP = 30, ROW = 10, OPTION_ROW = 11, MIN_H = 190;
    /** Letters per tick. */
    private static final float SPEED = 1.6f;

    private AuthorDialoguePayload page;
    private List<String> rows = List.of();
    private List<String> options = List.of();
    private int typedAt;
    private int shownSounded;
    private boolean waiting, closing;

    public AuthorDialogueScreen(AuthorDialoguePayload page) {
        super(Component.translatable("screen.supernaturalcraft.author"), W, MIN_H);
        this.page = page;
    }

    public int npc() {
        return page.npc();
    }

    /** His answer arrived: a new page. */
    public void turn(AuthorDialoguePayload next) {
        this.page = next;
        waiting = false;
        layout();
        Paper.sound(AllSounds.CHUCK_CARRIAGE.get(), 1.0f, 0.5f);
    }

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        String name = minecraft != null && minecraft.player != null ? minecraft.player.getGameProfile().getName() : "hunter";
        StringBuilder text = new StringBuilder();
        for (int i = 0; ; i++) {
            String key = "dialogue.supernaturalcraft.author." + page.node() + "." + i;
            if (!net.minecraft.client.resources.language.I18n.exists(key) || i > 12) break;
            if (i > 0) text.append("\n\n");
            text.append(Component.translatable(key, name).getString());
        }
        rows = Paper.wrap(font, text.toString(), W - MARGIN * 2);
        options = new ArrayList<>();
        for (String o : page.options()) options.add("> " + Component.translatable("dialogue.supernaturalcraft.author.option." + o).getString());
        pageH = Math.max(MIN_H, TOP + rows.size() * ROW + 14 + options.size() * OPTION_ROW + 18);
        typedAt = age;
        shownSounded = 0;
        super.init();
    }

    private int letters() {
        int n = 0;
        for (String r : rows) n += r.length();
        return n;
    }

    private int shown(float partial) {
        return (int) ((age - typedAt + partial) * SPEED);
    }

    private boolean typing() {
        return shown(0) < letters();
    }

    @Override
    public void tick() {
        super.tick();
        int n = Math.min(shown(0), letters());
        if (n > shownSounded) {
            if (n - shownSounded >= 1 && age % 2 == 0) Paper.sound(AllSounds.CHUCK_TYPE.get(), 0.85f + (float) Math.random() * 0.3f, 0.45f);
            shownSounded = n;
            if (n >= letters()) Paper.sound(AllSounds.CHUCK_BELL.get(), 1.0f, 0.5f);
        }
        Entity him = minecraft == null || minecraft.level == null ? null : minecraft.level.getEntity(page.npc());
        if (him == null || !him.isAlive() || minecraft.player == null || minecraft.player.distanceTo(him) > 12) {
            closing = true;
            onClose();
        }
    }

    @Override
    protected void drawPage(net.minecraft.client.gui.GuiGraphics g, double mx, double my, float partial) {
        String head = Component.translatable("screen.supernaturalcraft.author.header").getString();
        Paper.type(g, font, head, W - MARGIN - Paper.width(font, head), 12, head.length(), Paper.FADED);
        int left = shown(partial);
        int y = TOP;
        int cx = MARGIN;
        for (String r : rows) {
            int n = Math.max(0, Math.min(left, r.length()));
            cx = Paper.type(g, font, r, MARGIN, y, n, Paper.INK);
            left -= r.length();
            if (left <= 0) break;
            y += ROW;
        }
        if (typing()) {
            Paper.cursor(g, cx + 1, y, ROW);
            return;
        }
        if (waiting) return;
        int oy = TOP + rows.size() * ROW + 14;
        int hover = optionAt(mx, my);
        for (int i = 0; i < options.size(); i++) {
            String o = options.get(i);
            int color = i == hover ? Paper.RED : Paper.INK;
            int end = Paper.type(g, font, o, MARGIN + 6, oy + i * OPTION_ROW, o.length(), color);
            if (i == hover) g.fill(MARGIN + 6, oy + i * OPTION_ROW + 9, end, oy + i * OPTION_ROW + 10, Paper.RED);
        }
    }

    private int optionAt(double mx, double my) {
        if (typing() || waiting) return -1;
        int oy = TOP + rows.size() * ROW + 14;
        int i = (int) Math.floor((my - oy + 1) / OPTION_ROW);
        if (my < oy - 1 || i < 0 || i >= options.size()) return -1;
        int w = Paper.width(font, options.get(i));
        return mx >= MARGIN + 2 && mx <= MARGIN + 10 + w ? i : -1;
    }

    @Override
    protected boolean clickPage(double x, double y, int button) {
        if (button != 0) return false;
        if (typing()) {
            typedAt = age - (int) Math.ceil(letters() / SPEED) - 1;
            return true;
        }
        int i = optionAt(x, y);
        if (i < 0) return false;
        waiting = true;
        Paper.sound(AllSounds.CHUCK_TYPE.get(), 1.1f, 0.7f);
        PacketDistributor.sendToServer(new AuthorChoicePayload(page.npc(), page.options().get(i)));
        return true;
    }

    /** The server closed the page. */
    public void closedByServer() {
        closing = true;
        onClose();
    }

    @Override
    public void onClose() {
        if (!closing) PacketDistributor.sendToServer(new AuthorChoicePayload(page.npc(), "leave"));
        closing = true;
        super.onClose();
    }

    /** For previews: everything typed at once. */
    public void finishTyping() {
        typedAt = age - (int) Math.ceil(letters() / SPEED) - 1;
    }
}
