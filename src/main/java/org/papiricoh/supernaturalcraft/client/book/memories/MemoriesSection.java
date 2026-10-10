package org.papiricoh.supernaturalcraft.client.book.memories;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.journal.Ink;
import org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven;
import org.papiricoh.supernaturalcraft.client.heaven.HeavenText;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemoryKind;
import org.papiricoh.supernaturalcraft.memory.MemoryLog;
import org.papiricoh.supernaturalcraft.memory.MemorySets;
import org.papiricoh.supernaturalcraft.memory.scenes.Figure;
import org.papiricoh.supernaturalcraft.memory.scenes.MemoryScenes;
import org.papiricoh.supernaturalcraft.memory.scenes.Scene;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Hunter's Book's Memories (v0.18): the hunter's story as their Heaven keeps it. The index shows on the left how far each
 * collection has been gathered (victories, crossroads, cases, kin, companions, sightings) and on the right a card for every
 * memory, gathered ones warm and the rest pale, newest first. A memory's page has its title, when it happened and what it was
 * on the left, and on the right its scene as the shrine stages it, drawn from above at an angle in its blocks' map colours
 * (from {@link MemoryScenes#scene}, pure), with who stands in it. Drawn by hand in book space, clipped with
 * {@code book.scissor}.
 */
public class MemoriesSection extends BookSection {

    private static final int L = BookStyle.LEFT_X, R = BookStyle.RIGHT_X, PW = BookStyle.PAGE_W, TOP = BookStyle.PAGE_Y;
    private static final int LIST_Y = 40, LIST_BOTTOM = 248, CARD_W = 88, CARD_H = 32, CARD_GAP = 4, NAV_Y = 237;
    private static final int SET_ROW = 29;
    /** Thumbnails kept at once (each a small texture). */
    private static final int THUMBS = 12;

    /** A memory the book opens at next time (one just gathered). */
    private static @Nullable String pending;

    private @Nullable String open;
    private double scroll;
    private int seenVersion = -1;
    private List<Memory> cards = List.of();
    private final Map<String, Thumb> thumbs = new LinkedHashMap<>();
    private final Map<String, Scene> scenes = new HashMap<>();

    /** A scene drawn into a texture. */
    private record Thumb(ResourceLocation id, DynamicTexture texture, int w, int h) {
    }

    /** The next time the book's Memories open, they open at this memory's page. */
    public static void focusNext(String memoryId) {
        pending = memoryId;
    }

    // --- data --------------------------------------------------------------------------------------------------------

    private void refresh() {
        if (seenVersion == ClientHeaven.logVersion()) return;
        seenVersion = ClientHeaven.logVersion();
        // Newest first: the log is a timeline (the backfill is sorted into it), so its order read backwards.
        List<Memory> all = ClientHeaven.log().entries();
        List<Memory> ordered = new ArrayList<>(all.size());
        for (int i = all.size() - 1; i >= 0; i--) ordered.add(all.get(i));
        cards = ordered;
    }

    private static boolean gathered(Memory m) {
        return ClientHeaven.log().collected().contains(m.id());
    }

    /** Gathered memories counting towards a collection, and what it needs. */
    static int[] progress(MemoryLog log, MemorySets.Set set) {
        return new int[]{Math.min(MemorySets.progress(set, log.entries(), log.collected()), set.goal()), Math.max(1, set.goal())};
    }

    public static ItemStack icon(MemoryKind kind) {
        return switch (kind) {
            case BOSS_VICTORY -> new ItemStack(AllItems.MORNINGSTAR_TROPHY.get());
            case CROSSROADS_DEAL -> new ItemStack(AllItems.CROSSROADS_BOX.get());
            case CASE_SOLVED, CASE_LOST -> new ItemStack(AllItems.CASE_FILE.get());
            case ASCENSION, HEEDED_CALL -> new ItemStack(Items.FEATHER);
            case LEGACY_RANK -> new ItemStack(AllItems.MEN_OF_LETTERS_EMBLEM.get());
            case PET_LOST -> new ItemStack(Items.BONE);
            case FIRST_SIGHTING -> new ItemStack(Items.SPYGLASS);
            case FAVOURITE_PREY -> new ItemStack(Items.IRON_SWORD);
        };
    }

    static ItemStack setIcon(MemorySets.Set set) {
        return switch (set) {
            case VICTORIES -> new ItemStack(AllItems.MORNINGSTAR_TROPHY.get());
            case ALL_VICTORIES -> new ItemStack(Items.NETHER_STAR);
            case CROSSROADS -> new ItemStack(AllItems.CROSSROADS_BOX.get());
            case CASES -> new ItemStack(AllItems.CASE_FILE.get());
            case KIN -> new ItemStack(Items.FEATHER);
            case COMPANIONS -> new ItemStack(Items.BONE);
            case SIGHTINGS -> new ItemStack(Items.SPYGLASS);
        };
    }

    /** When it happened: the date if the world remembers it, else the day of the world. */
    static Component when(Memory m) {
        if (m.realTime() > 0) {
            String d = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ROOT).format(Instant.ofEpochMilli(m.realTime()).atZone(ZoneId.systemDefault()));
            return Component.literal(d);
        }
        if (m.gameTime() > 0) return Component.translatable("screen.supernaturalcraft.book.memories.day", m.gameTime() / 24000L + 1);
        return Component.translatable("screen.supernaturalcraft.book.memories.long_ago");
    }

    /** The scene this memory stages (the same seed for the same hunter and memory). */
    private Scene scene(Memory m) {
        return scenes.computeIfAbsent(m.id(), id -> {
            long seed = (mc.player == null ? 0L : mc.player.getUUID().getMostSignificantBits()) ^ id.hashCode() * 0x9E3779B97F4A7C15L;
            try {
                return MemoryScenes.scene(m, seed);
            } catch (RuntimeException e) {
                SupernaturalCraft.LOGGER.warn("Memories: no scene for {} ({})", id, e.toString());
                return Scene.empty("");
            }
        });
    }

    private @Nullable Thumb thumb(Memory m) {
        Thumb t = thumbs.get(m.id());
        if (t != null) return t;
        IsoThumbnail.Image img = IsoThumbnail.draw(voxels(scene(m)));
        if (img == null) return null;
        NativeImage ni = new NativeImage(img.width(), img.height(), true);
        for (int y = 0; y < img.height(); y++) {
            for (int x = 0; x < img.width(); x++) {
                int argb = img.at(x, y);
                // NativeImage keeps ABGR.
                ni.setPixelRGBA(x, y, (argb & 0xFF00FF00) | (argb >> 16 & 0xFF) | (argb & 0xFF) << 16);
            }
        }
        DynamicTexture tex = new DynamicTexture(ni);
        ResourceLocation id = SupernaturalCraft.asResource("dynamic/memory_" + Integer.toHexString(m.id().hashCode()));
        mc.getTextureManager().register(id, tex);
        t = new Thumb(id, tex, img.width(), img.height());
        thumbs.put(m.id(), t);
        while (thumbs.size() > THUMBS) {
            String oldest = thumbs.keySet().iterator().next();
            mc.getTextureManager().release(thumbs.remove(oldest).id());
        }
        return t;
    }

    /** The scene's blocks with a colour (air and colourless blocks left out). */
    static List<IsoThumbnail.Voxel> voxels(Scene scene) {
        List<IsoThumbnail.Voxel> out = new ArrayList<>();
        Map<String, Integer> colours = new HashMap<>();
        for (ArenaCell c : scene.cells()) {
            int rgb = colours.computeIfAbsent(c.block(), MemoriesSection::mapColour);
            if (rgb >= 0) out.add(new IsoThumbnail.Voxel(c.dx(), c.dy(), c.dz(), rgb));
        }
        return out;
    }

    private static int mapColour(String state) {
        try {
            BlockState s = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), state, false).blockState();
            if (s.isAir()) return -1;
            MapColor c = s.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            return c == MapColor.NONE ? -1 : c.col;
        } catch (Exception e) {
            return -1;
        }
    }

    // --- lifecycle ---------------------------------------------------------------------------------------------------

    @Override
    public void init() {
        refresh();
        if (pending != null) {
            String id = pending;
            pending = null;
            if (ClientHeaven.memory(id) != null) open = id;
        }
    }

    @Override
    public void hidden() {
        for (Thumb t : thumbs.values()) mc.getTextureManager().release(t.id());
        thumbs.clear();
        scenes.clear();
    }

    // --- drawing -----------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        refresh();
        decorate(g);
        Memory m = open == null ? null : ClientHeaven.memory(open);
        if (m != null) renderPage(g, m, mx, my);
        else {
            open = null;
            renderIndex(g, mx, my);
        }
    }

    /** Heaven's light over the pages: a warm wash from the top, gold rules. */
    private static void decorate(GuiGraphics g) {
        for (int x0 : new int[]{L, R}) {
            g.fillGradient(x0 - 2, TOP, x0 + PW + 2, TOP + 80, 0x30FFE6A0, 0x00FFE6A0);
            g.fill(x0 - 2, TOP - 1, x0 + PW + 2, TOP, 0x80C9A24A);
            g.fill(x0 - 2, TOP + BookStyle.PAGE_H, x0 + PW + 2, TOP + BookStyle.PAGE_H + 1, 0x80C9A24A);
        }
    }

    private void renderIndex(GuiGraphics g, int mx, int my) {
        MemoryLog log = ClientHeaven.log();
        Ink.heading(g, font, Component.translatable("screen.supernaturalcraft.book.memories"), L, TOP + 2, PW, BookStyle.INK);
        Ink.centred(g, font, Component.translatable("screen.supernaturalcraft.book.memories.gathered", log.collected().size(), log.entries().size()),
                L + PW / 2, TOP + 22, PW, BookStyle.FADED);
        MemorySets.Set[] sets = MemorySets.Set.values();
        for (int i = 0; i < sets.length; i++) {
            MemorySets.Set set = sets[i];
            int y = LIST_Y + 4 + i * SET_ROW;
            int[] p = progress(log, set);
            g.renderFakeItem(setIcon(set), L + 2, y);
            Ink.left(g, font, HeavenText.set(set), L + 22, y, PW - 60, p[0] > 0 ? BookStyle.INK : BookStyle.FADED);
            Ink.right(g, font, Component.literal(p[0] + "/" + p[1]), L + PW - 2, y, p[0] >= p[1] ? BookStyle.GOLD : BookStyle.FADED);
            int bx = L + 22, bw = PW - 24, by = y + 11;
            g.fill(bx, by, bx + bw, by + 3, 0x223B2A1A);
            if (p[1] > 0) g.fill(bx, by, bx + Math.round(bw * p[0] / (float) p[1]), by + 3, BookStyle.GOLD);
            Component bonus = Component.translatable(set.key() + ".bonus").withStyle(ChatFormatting.ITALIC);
            Ink.left(g, font, bonus, L + 22, y + 16, PW - 24, BookStyle.FADED);
            if (Ink.over(mx, my, L, y - 2, PW, SET_ROW - 2)) {
                book.tooltip(List.of(HeavenText.set(set), Component.translatable(set.key() + ".bonus").withStyle(ChatFormatting.GRAY)));
            }
        }

        // The cards.
        Ink.heading(g, font, Component.translatable("screen.supernaturalcraft.book.memories.cards"), R, TOP + 2, PW, BookStyle.INK);
        if (cards.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("screen.supernaturalcraft.book.memories.none")
                    .withStyle(ChatFormatting.ITALIC), PW - 10);
            int y = LIST_Y + 10;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, R + (PW - font.width(line)) / 2, y, BookStyle.FADED, false);
                y += 10;
            }
            return;
        }
        int rows = (cards.size() + 1) / 2, max = Math.max(0, rows * (CARD_H + CARD_GAP) - (LIST_BOTTOM - LIST_Y));
        scroll = Mth.clamp(scroll, 0, max);
        book.scissor(g, R - 1, LIST_Y, R + PW + 1, LIST_BOTTOM);
        boolean inList = my >= LIST_Y && my < LIST_BOTTOM;
        for (int i = 0; i < cards.size(); i++) {
            int cx = R + (i % 2) * (CARD_W + CARD_GAP), cy = LIST_Y + (i / 2) * (CARD_H + CARD_GAP) - (int) scroll;
            if (cy + CARD_H < LIST_Y || cy > LIST_BOTTOM) continue;
            card(g, cards.get(i), cx, cy, inList && Ink.over(mx, my, cx, cy, CARD_W, CARD_H));
        }
        g.disableScissor();
        if (max > 0) {
            int track = LIST_BOTTOM - LIST_Y, content = rows * (CARD_H + CARD_GAP);
            int thumb = Math.max(12, track * track / content);
            int ty = LIST_Y + (int) ((track - thumb) * (scroll / max));
            g.fill(R + PW + 2, LIST_Y, R + PW + 3, LIST_BOTTOM, 0x223B2A1A);
            g.fill(R + PW + 1, ty, R + PW + 4, ty + thumb, BookStyle.FADED);
        }
    }

    private void card(GuiGraphics g, Memory m, int x, int y, boolean over) {
        boolean got = gathered(m);
        BookAtlas.panel(g, BookAtlas.CARD, x, y, CARD_W, CARD_H);
        if (got) g.fillGradient(x + 2, y + 2, x + CARD_W - 2, y + CARD_H - 2, 0x40FFE6A0, 0x10FFE6A0);
        else g.fill(x + 2, y + 2, x + CARD_W - 2, y + CARD_H - 2, 0x30F4EEE0);
        if (over) Ink.hover(g, x + 1, y + 1, CARD_W - 2, CARD_H - 2);
        if (got) g.renderFakeItem(icon(m.kind()), x + 3, y + 3);
        else Ink.silhouette(g, icon(m.kind()), x + 3, y + 3);
        List<FormattedCharSequence> lines = font.split(HeavenText.title(m), CARD_W - 24);
        int ink = got ? BookStyle.INK : BookStyle.FADED;
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), x + 21, y + 4 + i * 9, ink, false);
        Ink.left(g, font, got ? when(m) : Component.translatable("screen.supernaturalcraft.book.memories.unseen"), x + 4, y + CARD_H - 10,
                CARD_W - 8, got ? BookStyle.GOLD : BookStyle.FADED);
        if (over) book.tooltip(List.of(HeavenText.title(m), HeavenText.set(m.kind().set).copy().withStyle(ChatFormatting.GRAY)));
    }

    private void renderPage(GuiGraphics g, Memory m, int mx, int my) {
        boolean got = gathered(m);
        int y = TOP + 2;
        List<FormattedCharSequence> title = font.split(HeavenText.title(m), PW);
        for (int i = 0; i < Math.min(3, title.size()); i++) {
            g.drawString(font, title.get(i), L + (PW - font.width(title.get(i))) / 2, y, BookStyle.INK, false);
            y += 10;
        }
        BookAtlas.FLOURISH.draw(g, L + (PW - BookAtlas.FLOURISH.w()) / 2, y);
        y += 14;
        g.renderFakeItem(icon(m.kind()), L + 2, y - 2);
        Ink.left(g, font, HeavenText.set(m.kind().set), L + 22, y, PW - 22, BookStyle.GOLD);
        Ink.left(g, font, when(m), L + 22, y + 10, PW - 22, BookStyle.FADED);
        y += 28;
        Component line = HeavenText.line(m);
        if (!line.getString().isEmpty()) {
            for (FormattedCharSequence l : font.split(line, PW - 4)) {
                g.drawString(font, l, L + 2, y, BookStyle.INK, false);
                y += 10;
            }
            y += 6;
        }
        Component state = Component.translatable(got ? "screen.supernaturalcraft.book.memories.page_gathered"
                : "screen.supernaturalcraft.book.memories.page_unseen").withStyle(ChatFormatting.ITALIC);
        for (FormattedCharSequence l : font.split(state, PW - 4)) {
            g.drawString(font, l, L + 2, y, got ? BookStyle.FADED : BookStyle.BLOOD, false);
            y += 10;
        }

        // The scene, on the right page.
        Ink.heading(g, font, Component.translatable("screen.supernaturalcraft.book.memories.scene"), R, TOP + 2, PW, BookStyle.INK);
        int fx = R + 2, fy = TOP + 26, fw = PW - 4, fh = 150;
        g.fill(fx - 1, fy - 1, fx + fw + 1, fy + fh + 1, 0x80C9A24A);
        g.fillGradient(fx, fy, fx + fw, fy + fh, 0xFFDDEBFA, 0xFFFFF6E2);
        Scene scene = scene(m);
        Thumb t = got ? thumb(m) : null;
        if (t != null) {
            float s = Math.min((fw - 8) / (float) t.w(), (fh - 8) / (float) t.h());
            int dw = Math.round(t.w() * s), dh = Math.round(t.h() * s);
            book.scissor(g, fx, fy, fx + fw, fy + fh);
            g.blit(t.id(), fx + (fw - dw) / 2, fy + (fh - dh) / 2, dw, dh, 0, 0, t.w(), t.h(), t.w(), t.h());
            g.disableScissor();
        } else {
            // Not yet relived (or a scene of light alone): a haze with the shrine's promise.
            for (int i = 0; i < 6; i++) {
                int inset = i * 10;
                g.fill(fx + inset, fy + inset, fx + fw - inset, fy + fh - inset, 0x14FFFFFF);
            }
            Component hint = Component.translatable(got ? "screen.supernaturalcraft.book.memories.light"
                    : "screen.supernaturalcraft.book.memories.haze").withStyle(ChatFormatting.ITALIC);
            List<FormattedCharSequence> lines = font.split(hint, fw - 20);
            int ty = fy + fh / 2 - lines.size() * 5;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, fx + (fw - font.width(l)) / 2, ty, BookStyle.FADED, false);
                ty += 10;
            }
        }
        if (got) {
            Set<Component> who = new LinkedHashSet<>();
            for (Figure f : scene.figures()) who.add(figureName(f.figure()));
            if (!who.isEmpty()) {
                Component list = Component.translatable("screen.supernaturalcraft.book.memories.who",
                        net.minecraft.network.chat.ComponentUtils.formatList(who, Component.literal(", ")));
                int wy = fy + fh + 6;
                for (FormattedCharSequence l : font.split(list, PW - 4)) {
                    if (wy > NAV_Y - 10) break;
                    g.drawString(font, l, R + 2, wy, BookStyle.FADED, false);
                    wy += 10;
                }
            }
        }

        Component back = Component.translatable("screen.supernaturalcraft.book.journal.back");
        boolean overBack = overBack(mx, my);
        Ink.centred(g, font, back, L + PW / 2, NAV_Y + 2, PW - 50, overBack ? BookStyle.GOLD : BookStyle.FADED);
        if (overBack) Ink.rule(g, L + (PW - font.width(back)) / 2, NAV_Y + 11, font.width(back), BookStyle.GOLD);
    }

    /** Who a scene's figure is, in words. */
    static Component figureName(String figure) {
        if (figure.equals("@owner")) return Component.translatable("screen.supernaturalcraft.book.memories.you");
        if (figure.startsWith("@ally:")) return Component.literal(HeavenText.pretty(figure.substring(6)));
        return HeavenText.creature(figure);
    }

    private boolean overBack(double mx, double my) {
        Component back = Component.translatable("screen.supernaturalcraft.book.journal.back");
        int w = font.width(back);
        return Ink.over(mx, my, L + (PW - w) / 2 - 4, NAV_Y, w + 8, 12);
    }

    // --- input -------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (open != null) {
            if (overBack(mx, my)) {
                open = null;
                click();
                return true;
            }
            return false;
        }
        if (my < LIST_Y || my >= LIST_BOTTOM) return false;
        for (int i = 0; i < cards.size(); i++) {
            int cx = R + (i % 2) * (CARD_W + CARD_GAP), cy = LIST_Y + (i / 2) * (CARD_H + CARD_GAP) - (int) scroll;
            if (Ink.over(mx, my, cx, cy, CARD_W, CARD_H)) {
                open = cards.get(i).id();
                click();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (open != null || mx < R) return false;
        scroll -= sy * (CARD_H + CARD_GAP) / 2.0;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (open != null && (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT)) {
            open = null;
            return true;
        }
        return false;
    }

    private void click() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    // --- previews ----------------------------------------------------------------------------------------------------

    /** Opens a memory's page (previews; the book). */
    public void openMemory(@Nullable String id) {
        open = id;
    }

    @Override
    public List<PreviewShot> previewShots() {
        List<PreviewShot> out = new ArrayList<>();
        out.add(new PreviewShot("index", b -> b.memories().openMemory(null)));
        out.add(new PreviewShot("page", b -> {
            for (Memory m : ClientHeaven.log().entries()) {
                if (gathered(m)) {
                    b.memories().openMemory(m.id());
                    return;
                }
            }
        }));
        out.add(new PreviewShot("page_unseen", b -> {
            for (Memory m : ClientHeaven.log().entries()) {
                if (!gathered(m)) {
                    b.memories().openMemory(m.id());
                    return;
                }
            }
        }));
        return out;
    }
}
