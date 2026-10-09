package org.papiricoh.supernaturalcraft.client.book.journal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy;
import org.papiricoh.supernaturalcraft.client.legacy.LegacyText;
import org.papiricoh.supernaturalcraft.journal.JournalBlock;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.JournalEntry;
import org.papiricoh.supernaturalcraft.journal.Unlock;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedFormula;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedRite;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchMath;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The Archive's pages written from the hunter's own research (v0.17): every generated formula and rite, every identified
 * artifact, every creature file and every solved case, one page each, after the fixed pages of the chapter. Their text is lang
 * templates ({@code journal.supernaturalcraft.archive.*}) filled in here; generated content never sits in the assets.
 *
 * <p>Page text with arguments travels through {@link JournalBlock.Text} as {@code key␟arg␟arg…} ({@link #text}); an argument
 * starting with {@code #} is itself a lang key. {@code PageBlocks} reads it back with {@link #parse}.
 */
public final class ArchivePages {

    static final char SEP = '\u001f';
    static final String PREFIX = "archive_";
    private static final String T = "journal.supernaturalcraft.archive.";

    private ArchivePages() {
    }

    /** A text block's string: a template and its arguments. */
    public static String text(String key, Object... args) {
        StringBuilder b = new StringBuilder(key);
        for (Object a : args) b.append(SEP).append(a instanceof Component c ? c.getString() : String.valueOf(a));
        return b.toString();
    }

    /** A text block's string back to a component (a plain lang key stays a plain key). */
    public static MutableComponent parse(String text) {
        if (text.indexOf(SEP) < 0) return Component.translatable(text);
        String[] parts = text.split(String.valueOf(SEP), -1);
        Object[] args = new Object[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            String s = parts[i];
            args[i - 1] = s.startsWith("#") ? Component.translatable(s.substring(1)) : s;
        }
        return Component.translatable(parts[0], args);
    }

    /** Every generated page, in order: formulas, rites, artifacts, creature files, cases. */
    static List<JournalPages.Page> pages() {
        if (!ClientLegacy.member()) return List.of();
        Archive a = ClientLegacy.archive();
        List<JournalPages.Page> out = new ArrayList<>();
        for (GeneratedFormula f : a.formulas()) out.add(formula(a, f));
        for (GeneratedRite r : a.rites()) out.add(rite(r));
        for (ArtifactData d : a.artifacts()) out.add(artifact(d));
        Map<String, Integer> files = new TreeMap<>(a.files());
        files.forEach((id, level) -> {
            if (level > 0) out.add(creature(id, level));
        });
        for (CaseFile c : ClientLegacy.legacy().cases()) if (c.state() == CaseFile.SOLVED) out.add(solvedCase(c));
        return out;
    }

    /** The Archive page a finished research topic writes (or adds to), or null if it writes none of its own. */
    @Nullable
    public static ResourceLocation pageFor(String topic) {
        var kind = org.papiricoh.supernaturalcraft.legacy.research.TopicKind.of(topic);
        String subject = org.papiricoh.supernaturalcraft.legacy.research.TopicKind.subject(topic);
        if (kind == null) return null;
        String path = switch (kind) {
            case LORE -> "archive_lore_" + subject;
            case FORMULA -> PREFIX + "formula_" + subject;
            case RITE -> PREFIX + "rite_" + subject;
            case CASE -> PREFIX + "case_" + subject;
            case ARTIFACT -> {
                try {
                    yield PREFIX + "artifact_" + Long.toUnsignedString(Long.parseLong(subject), 36);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            case CREATURE -> {
                ResourceLocation rl = ResourceLocation.tryParse(subject);
                yield rl == null ? null : PREFIX + "creature_" + (rl.getNamespace() + "_" + rl.getPath()).replace('/', '_');
            }
            case BOSS -> {
                ResourceLocation rl = ResourceLocation.tryParse(subject);
                var boss = rl == null ? null : org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.byEntity(rl.getPath());
                yield boss == null ? null : "archive_boss_" + boss.id();
            }
        };
        return path == null ? null : SupernaturalCraft.asResource(path);
    }

    @Nullable
    static JournalPages.Page find(ResourceLocation id) {
        if (!id.getNamespace().equals(SupernaturalCraft.MODID) || !id.getPath().startsWith(PREFIX)) return null;
        for (JournalPages.Page p : pages()) if (p.id().equals(id)) return p;
        return null;
    }

    private static JournalPages.Page page(String id, Component title, ItemStack icon, List<JournalBlock> blocks) {
        JournalEntry entry = new JournalEntry(JournalChapter.ARCHIVE, 2000, SupernaturalCraft.asResource("field_notes"), Unlock.ALWAYS,
                Optional.empty(), blocks);
        return new JournalPages.Page(SupernaturalCraft.asResource(PREFIX + id), entry, title, icon);
    }

    private static JournalPages.Page formula(Archive a, GeneratedFormula f) {
        SigilComponent s = f.sigil();
        StringBuilder params = new StringBuilder();
        s.params().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            if (!params.isEmpty()) params.append(", ");
            params.append(e.getKey().replace('_', ' ')).append(' ').append(fmt(e.getValue()));
        });
        List<JournalBlock> blocks = new ArrayList<>();
        blocks.add(new JournalBlock.Text(text(T + "formula", f.name(), "#" + SigilComponent.translationKey(f.base()), LegacyText.roman(s.tier()),
                fmt(s.manaCost()), fmt(s.cooldown() / 20f), params.isEmpty() ? "-" : params.toString())));
        List<ResourceLocation> reagents = new ArrayList<>();
        for (SigilComponent.Reagent r : s.reagents()) r.item().unwrapKey().ifPresent(k -> reagents.add(k.location()));
        if (!reagents.isEmpty()) blocks.add(new JournalBlock.Items(reagents, Optional.of(T + "formula.reagents")));
        blocks.add(new JournalBlock.Text(T + "formula.use"));
        return page("formula_" + f.index(), Component.literal(f.name()), new ItemStack(Items.ENCHANTED_BOOK), blocks);
    }

    private static JournalPages.Page rite(GeneratedRite r) {
        List<String> liquids = new ArrayList<>();
        for (String l : r.liquids()) {
            BowlLiquid liquid = BowlLiquid.fromId(l);
            liquids.add(Component.translatable("bowl_liquid.supernaturalcraft." + (liquid != null ? liquid.id() : l)).getString());
        }
        String effect = Component.translatableWithFallback(T + "effect." + r.effect().type().getPath(), r.effect().type().getPath().replace('_', ' '))
                .getString();
        List<JournalBlock> blocks = new ArrayList<>();
        blocks.add(new JournalBlock.Text(text(T + "rite", r.name(), String.join(", ", liquids), r.incantation(), fmt(r.manaCost()), effect)));
        if (!r.ingredients().isEmpty()) blocks.add(new JournalBlock.Items(r.ingredients(), Optional.of(T + "rite.ingredients")));
        blocks.add(new JournalBlock.Text(T + "rite.use"));
        return page("rite_" + r.index(), Component.literal(r.name()), new ItemStack(Items.WRITABLE_BOOK), blocks);
    }

    private static JournalPages.Page artifact(ArtifactData d) {
        List<JournalBlock> blocks = new ArrayList<>();
        blocks.add(new JournalBlock.Text(text(T + "artifact", d.name(), "#artifact.supernaturalcraft.form." + d.form(),
                "#artifact.supernaturalcraft.rarity." + d.rarity())));
        for (String boon : d.boons()) {
            blocks.add(new JournalBlock.Text(text(T + "artifact.boon", "#artifact.supernaturalcraft.trait." + boon,
                    "#artifact.supernaturalcraft.trait." + boon + ".desc")));
        }
        if (!d.curse().isEmpty()) {
            blocks.add(new JournalBlock.Text(text(T + "artifact.curse", "#artifact.supernaturalcraft.trait." + d.curse(),
                    "#artifact.supernaturalcraft.trait." + d.curse() + ".desc")));
        } else {
            blocks.add(new JournalBlock.Text(T + "artifact.clean"));
        }
        ItemStack icon = org.papiricoh.supernaturalcraft.legacy.artifact.Artifacts.of(d);
        return page("artifact_" + Long.toUnsignedString(d.seed(), 36), Component.literal(d.name()), icon, blocks);
    }

    private static JournalPages.Page creature(String id, int level) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Component name = LegacyText.entityName(id);
        int bonus = (int) Math.round(ResearchMath.creatureDamage(level) * 100);
        List<JournalBlock> blocks = new ArrayList<>();
        blocks.add(new JournalBlock.Text(text(T + "creature", name, level, bonus)));
        if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) blocks.add(new JournalBlock.Entity(rl, Optional.empty(), 1f));
        // One more of the order's notes for every level of the file.
        for (int i = 1; i <= Math.min(level, CREATURE_NOTES); i++) blocks.add(new JournalBlock.Text(text(T + "creature.note." + i, name)));
        String path = rl == null ? id.replace(':', '_') : rl.getNamespace() + "_" + rl.getPath();
        ItemStack icon = org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem.stack("creature:" + id, 1);
        return page("creature_" + path.replace('/', '_'), Component.translatable(T + "creature.title", name), icon, blocks);
    }

    /** How many creature-file notes there are ({@code journal.supernaturalcraft.archive.creature.note.<n>}). */
    public static final int CREATURE_NOTES = 6;

    private static JournalPages.Page solvedCase(CaseFile c) {
        String twist = c.twist().isEmpty() ? "none" : c.twist();
        List<JournalBlock> blocks = new ArrayList<>();
        blocks.add(new JournalBlock.Text(text(T + "case", c.index() + 1, "#legacy.supernaturalcraft.case.scenario." + c.scenario(),
                LegacyText.entityName(c.monster().toString()), "#legacy.supernaturalcraft.case.twist." + twist, c.site().getX(), c.site().getZ(),
                LegacyText.roman(c.tier()))));
        boolean written = ClientLegacy.researched("case:" + c.index());
        blocks.add(new JournalBlock.Text(written ? T + "case.written" : T + "case.unwritten"));
        return page("case_" + c.index(), Component.translatable("legacy.supernaturalcraft.case.scenario." + c.scenario()),
                new ItemStack(AllItems.CASE_FILE.get()), blocks);
    }

    private static String fmt(float v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "");
    }
}
