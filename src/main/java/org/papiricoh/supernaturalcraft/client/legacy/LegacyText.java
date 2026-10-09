package org.papiricoh.supernaturalcraft.client.legacy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.legacy.LegacyRules;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;
import org.papiricoh.supernaturalcraft.legacy.research.TopicKind;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/** Words for the Men of Letters' screens (v0.17): research titles from topic ids, ranks, tiers, durations, icons. */
public final class LegacyText {

    private LegacyText() {
    }

    /** A title key with its arguments ({@code #key} arguments are translated). */
    public static Component title(String key, List<String> args) {
        Object[] a = new Object[args.size()];
        for (int i = 0; i < a.length; i++) {
            String s = args.get(i);
            a[i] = s.startsWith("#") ? Component.translatable(s.substring(1)) : s;
        }
        return Component.translatable(key, a);
    }

    public static Component entityName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return Component.literal(id);
        return BuiltInRegistries.ENTITY_TYPE.getOptional(rl).<Component>map(t -> t.getDescription()).orElse(Component.literal(id));
    }

    /** A topic's title worked out from its id alone (running research, toasts). */
    public static Component topic(String topic) {
        TopicKind kind = TopicKind.of(topic);
        String subject = TopicKind.subject(topic);
        String t = "research.supernaturalcraft.topic.";
        if (kind == null) return Component.literal(topic);
        return switch (kind) {
            case FORMULA, RITE -> Component.translatable(t + kind.id(), plusOne(subject));
            case ARTIFACT -> {
                for (ArtifactData a : ClientLegacy.archive().artifacts()) {
                    if (String.valueOf(a.seed()).equals(subject)) yield Component.literal(a.name());
                }
                yield Component.translatable("research.supernaturalcraft.kind.artifact");
            }
            case CREATURE -> {
                ResourceLocation rl = ResourceLocation.tryParse(subject);
                int level = rl == null ? 0 : ClientLegacy.archive().fileLevel(rl);
                boolean running = ClientLegacy.archive().slots().stream().anyMatch(s -> s.topic().equals(topic));
                yield Component.translatable(t + "creature", entityName(subject), String.valueOf(Math.max(1, level + (running ? 1 : 0))));
            }
            case BOSS -> Component.translatable(t + "boss", entityName(subject));
            case CASE -> Component.translatable(t + "case.short", plusOne(subject));
            case LORE -> Component.translatable("research.supernaturalcraft.lore." + subject);
        };
    }

    private static String plusOne(String n) {
        try {
            return String.valueOf(Integer.parseInt(n) + 1);
        } catch (NumberFormatException e) {
            return n;
        }
    }

    public static Component kind(String topic) {
        TopicKind kind = TopicKind.of(topic);
        return kind == null ? Component.empty() : Component.translatable("research.supernaturalcraft.kind." + kind.id());
    }

    /** An icon for a topic: field notes of its family, a page, a file. */
    public static ItemStack icon(String topic) {
        TopicKind kind = TopicKind.of(topic);
        if (kind == null) return new ItemStack(AllItems.FIELD_NOTES.get());
        return switch (kind) {
            case FORMULA -> new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            case RITE -> new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
            case ARTIFACT -> new ItemStack(AllItems.CURSED_ARTIFACT.get());
            case CREATURE, BOSS -> org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem.stack(topic.replaceFirst("^(boss|creature):", "creature:"), 1);
            case CASE -> new ItemStack(AllItems.CASE_FILE.get());
            case LORE -> new ItemStack(AllItems.ARCHIVE_SHELF.get());
        };
    }

    public static Component rank(int rank) {
        return Component.translatable("legacy.supernaturalcraft.rank." + LegacyRules.title(rank));
    }

    public static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    /** {@code 12:05} or {@code 1:02:03} from ticks. */
    public static String clock(long ticks) {
        long s = Math.max(0, (ticks + 19) / 20);
        long h = s / 3600, m = s / 60 % 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
    }
}
