package org.papiricoh.supernaturalcraft.legacy.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generated sigils ("formulae", v0.17), pure: a registered sigil's behaviour with its numbers worked over. Every formula keeps
 * its base's kind and behaviour (so the spell code needs nothing new) and mutates its params within hard caps:
 * <ul>
 *   <li>each strength param is scaled by a factor in [{@link #MIN_FACTOR}, {@link #maxFactor}(tier)], never past
 *       {@link #MAX_FACTOR} (the jump from one sigil tier to the next) nor its absolute cap in {@link #CAPS};</li>
 *   <li>the mana cost follows the mean strength (stronger costs more, never less than {@link #MIN_MANA_SHARE} of the base);</li>
 *   <li>the sigil tier is the base's or one more (max 3), and only bases of a tier the research tier allows are used.</li>
 * </ul>
 * Modifiers scale their bonus over 1 ({@code 1 + (m - 1)·f}); their {@code mana_multiplier} rises with it. Sigils without
 * params ({@code banishing}) and the echo are never bases.
 */
public final class FormulaGenerator {

    /** A registered sigil, as far as the generator cares. */
    public record Base(String id, String kind, String behavior, int tier, float manaCost, int cooldown, Map<String, Float> params,
                       int color) {
    }

    /** A worked-out formula: its base, name, numbers and colour. */
    public record Spec(String baseId, String name, int sigilTier, float manaCost, int cooldown, Map<String, Float> params, int color) {
    }

    /** Weakest a param gets (×). */
    public static final double MIN_FACTOR = 0.85;
    /** Strongest a param ever gets (×): one sigil tier's worth. */
    public static final double MAX_FACTOR = 1.35;
    /** A formula never costs less mana than this share of its base. */
    public static final double MIN_MANA_SHARE = 0.9;

    /** Absolute caps per param (after scaling). Anything not listed is capped by {@link #MAX_FACTOR} only. */
    public static final Map<String, Float> CAPS = Map.ofEntries(
            Map.entry("damage", 14f), Map.entry("duration", 400f), Map.entry("heal", 8f), Map.entry("burn_seconds", 8f),
            Map.entry("radius", 6f), Map.entry("range", 8f), Map.entry("speed", 2.5f), Map.entry("strength", 2.2f),
            Map.entry("lift", 0.55f), Map.entry("vs_unholy", 2.6f), Map.entry("potency_multiplier", 1.9f),
            Map.entry("duration_multiplier", 2.3f), Map.entry("range_multiplier", 2.3f), Map.entry("area_bonus", 3f));

    /** Params that are not strengths: kept as they are. */
    static final Set<String> FIXED = Set.of("echo", "mana_multiplier");

    /** Bases never used. */
    public static final Set<String> EXCLUDED = Set.of("supernaturalcraft:echo", "supernaturalcraft:banishing");

    private FormulaGenerator() {
    }

    /** The most a research tier (1–5) lets a param grow: ×1.07 per tier, at most {@link #MAX_FACTOR}. */
    public static double maxFactor(int researchTier) {
        return Math.min(MAX_FACTOR, 1 + 0.07 * Math.max(1, Math.min(5, researchTier)));
    }

    /** Bases a research tier may work from (sigil tier ≤ min(3, research tier), with params, not excluded), by id. */
    public static List<Base> usable(List<Base> bases, int researchTier) {
        int cap = Math.min(3, Math.max(1, researchTier));
        return bases.stream().filter(b -> b.tier() <= cap && !b.params().isEmpty() && !EXCLUDED.contains(b.id()))
                .filter(b -> b.params().keySet().stream().anyMatch(k -> !FIXED.contains(k)))
                .sorted(Comparator.comparing(Base::id)).toList();
    }

    /**
     * The {@code index}-th formula of a hunter.
     *
     * @param bases the registered sigils
     * @param salt the hunter's formula salt ({@link GenSeed#of}(world, player, "formula", 0)): names never repeat within it
     * @param researchTier 1–5
     * @return null if no base is usable
     */
    public static Spec generate(List<Base> bases, long salt, int index, int researchTier) {
        List<Base> pool = usable(bases, researchTier);
        if (pool.isEmpty()) return null;
        GenSeed.Rng r = new GenSeed.Rng(GenSeed.mix(salt ^ (index + 1L) * 0x632BE59BD9B4E019L));
        Base base = r.pick(pool);
        double hi = maxFactor(researchTier);
        boolean modifier = "modifier".equals(base.kind());
        Map<String, Float> params = new LinkedHashMap<>();
        double sum = 0;
        int n = 0;
        for (Map.Entry<String, Float> e : base.params().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            String key = e.getKey();
            float v = e.getValue();
            if (FIXED.contains(key)) {
                params.put(key, v);
                continue;
            }
            double f = MIN_FACTOR + r.nextDouble() * (hi - MIN_FACTOR);
            double out = modifier && key.endsWith("_multiplier") ? 1 + (v - 1) * f : v * f;
            Float cap = CAPS.get(key);
            double limit = modifier && key.endsWith("_multiplier") ? 1 + (v - 1) * MAX_FACTOR : v * MAX_FACTOR;
            if (cap != null) limit = Math.min(limit, Math.max(cap, v));
            out = Math.min(out, limit);
            params.put(key, floor(out, key.equals("duration") ? 0 : 2));
            sum += v == 0 ? 1 : out / v;
            n++;
        }
        double strength = n == 0 ? 1 : sum / n;
        if (modifier && base.params().containsKey("mana_multiplier")) {
            float mm = base.params().get("mana_multiplier");
            params.put("mana_multiplier", round(1 + (mm - 1) * Math.max(MIN_MANA_SHARE, Math.pow(strength, 1.2)), 2));
        }
        float mana = base.manaCost() <= 0 ? 0 : round(base.manaCost() * Math.max(MIN_MANA_SHARE, Math.pow(strength, 1.2)), 1);
        int cooldown = (int) Math.round(base.cooldown() * Math.max(1.0, strength));
        int sigilTier = Math.min(3, base.tier() + (strength > 1.15 && researchTier >= 3 ? 1 : 0));
        String name = LatinNames.name(salt, index);
        return new Spec(base.id(), name, sigilTier, mana, Math.min(1200, cooldown), params, tint(base.color(), r));
    }

    /** The base's colour, drawn 30 % towards a random hue. */
    static int tint(int color, GenSeed.Rng r) {
        float hue = (float) r.nextDouble();
        int other = hsb(hue, 0.55f, 0.95f);
        int rr = mix((color >> 16) & 0xFF, (other >> 16) & 0xFF), gg = mix((color >> 8) & 0xFF, (other >> 8) & 0xFF),
                bb = mix(color & 0xFF, other & 0xFF);
        return rr << 16 | gg << 8 | bb;
    }

    private static int mix(int a, int b) {
        return Math.round(a * 0.7f + b * 0.3f);
    }

    /** HSB → 0xRRGGBB (as {@code java.awt.Color.HSBtoRGB}, without AWT). */
    static int hsb(float hue, float sat, float bri) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float f = h - (float) Math.floor(h);
        float p = bri * (1 - sat), q = bri * (1 - sat * f), t = bri * (1 - sat * (1 - f));
        float r, g, b;
        switch ((int) h) {
            case 0 -> { r = bri; g = t; b = p; }
            case 1 -> { r = q; g = bri; b = p; }
            case 2 -> { r = p; g = bri; b = t; }
            case 3 -> { r = p; g = q; b = bri; }
            case 4 -> { r = t; g = p; b = bri; }
            default -> { r = bri; g = p; b = q; }
        }
        return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    static float floor(double v, int places) {
        double p = Math.pow(10, places);
        return (float) (Math.floor(v * p + 1e-9) / p);
    }

    static float round(double v, int places) {
        double p = Math.pow(10, places);
        return (float) (Math.round(v * p) / p);
    }

    /** Every base, sorted (for tests and the server). */
    public static List<Base> sorted(List<Base> bases) {
        List<Base> l = new ArrayList<>(bases);
        l.sort(Comparator.comparing(Base::id));
        return l;
    }
}
