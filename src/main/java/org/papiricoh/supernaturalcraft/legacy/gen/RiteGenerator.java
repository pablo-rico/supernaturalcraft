package org.papiricoh.supernaturalcraft.legacy.gen;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generated bowl spells ("rites", v0.17), pure: a catalogued {@code BowlSpellEffect} with safe params, 1–3 liquids, 2–5
 * ingredients from a catalogue of things a hunter can get, a Latin name and incantation. The effect is written as the JSON a
 * bowl recipe would hold, so the server decodes it with {@code BowlSpellEffect.CODEC} like any recipe.
 */
public final class RiteGenerator {

    /** A helpful mob effect a rite may lay: id, lowest research tier, highest amplifier, duration range (ticks). */
    public record Boon(String effect, int minTier, int maxAmp, int minTicks, int maxTicks) {
    }

    /** A catalogued ingredient: item id and the lowest research tier that asks for it. */
    public record Ingredient(String item, int minTier) {
    }

    /** A worked-out rite. {@code effect} is the effect's JSON (with its {@code type}). */
    public record Spec(String name, List<String> liquids, List<String> ingredients, String incantation, float manaCost,
                       JsonObject effect, int smokeColor, int tier) {
    }

    public static final List<Boon> BOONS = List.of(
            new Boon("minecraft:night_vision", 1, 0, 3600, 9600),
            new Boon("minecraft:water_breathing", 1, 0, 2400, 6000),
            new Boon("minecraft:jump_boost", 1, 1, 1200, 3600),
            new Boon("minecraft:slow_falling", 1, 0, 1200, 3600),
            new Boon("minecraft:speed", 1, 1, 1200, 3600),
            new Boon("minecraft:haste", 2, 1, 1200, 3600),
            new Boon("minecraft:fire_resistance", 2, 0, 1800, 4800),
            new Boon("minecraft:luck", 2, 0, 2400, 6000),
            new Boon("minecraft:invisibility", 2, 0, 1200, 3600),
            new Boon("supernaturalcraft:second_sight", 2, 0, 1200, 3600),
            new Boon("minecraft:regeneration", 3, 0, 200, 600),
            new Boon("minecraft:resistance", 3, 0, 600, 1800),
            new Boon("minecraft:absorption", 3, 1, 1200, 2400),
            new Boon("supernaturalcraft:concealed", 3, 0, 1200, 3600));

    /** Effects other than mob effects: type and the lowest research tier. */
    public static final List<String> WARDS = List.of("supernaturalcraft:purify", "supernaturalcraft:bind", "supernaturalcraft:banish");
    static final int[] WARD_TIERS = {2, 4, 4};

    public static final List<Ingredient> INGREDIENTS = List.of(
            new Ingredient("minecraft:bone", 1), new Ingredient("minecraft:feather", 1), new Ingredient("minecraft:spider_eye", 1),
            new Ingredient("minecraft:sugar", 1), new Ingredient("minecraft:ink_sac", 1), new Ingredient("minecraft:redstone", 1),
            new Ingredient("minecraft:glowstone_dust", 1), new Ingredient("minecraft:gold_nugget", 1),
            new Ingredient("minecraft:iron_nugget", 1), new Ingredient("minecraft:candle", 1), new Ingredient("minecraft:string", 1),
            new Ingredient("supernaturalcraft:salt", 1), new Ingredient("minecraft:amethyst_shard", 2),
            new Ingredient("minecraft:rabbit_foot", 2), new Ingredient("minecraft:phantom_membrane", 2),
            new Ingredient("minecraft:lily_of_the_valley", 2), new Ingredient("minecraft:glow_berries", 2),
            new Ingredient("supernaturalcraft:sulfur", 2), new Ingredient("supernaturalcraft:grave_dirt", 2),
            new Ingredient("supernaturalcraft:ectoplasm", 3), new Ingredient("minecraft:blaze_powder", 3),
            new Ingredient("minecraft:ender_pearl", 3), new Ingredient("minecraft:nether_wart", 3),
            new Ingredient("minecraft:ghast_tear", 4), new Ingredient("supernaturalcraft:demon_blood", 4),
            new Ingredient("minecraft:echo_shard", 5));

    /** Liquids by the lowest research tier that uses them. */
    public static final List<String> LIQUIDS = List.of("water", "holy_water", "honey", "blood", "demon_blood");
    static final int[] LIQUID_TIERS = {1, 1, 2, 3, 4};

    public static final float MAX_MANA = 120;
    public static final int MAX_RADIUS = 10;

    private RiteGenerator() {
    }

    /**
     * The {@code index}-th rite of a hunter.
     *
     * @param salt the hunter's rite salt ({@link GenSeed#of}(world, player, "rite", 0)); {@code attempt} re-rolls everything but
     *             the name (the server retries when a mix clashes with a bowl recipe)
     * @param researchTier 1–5
     */
    public static Spec generate(long salt, int index, int researchTier, int attempt) {
        int tier = Math.max(1, Math.min(5, researchTier));
        GenSeed.Rng r = new GenSeed.Rng(GenSeed.mix(salt ^ (index + 1L) * 0x2545F4914F6CDD1DL ^ attempt * 0x9E3779B97F4A7C15L));
        JsonObject effect = new JsonObject();
        float weight;
        List<Integer> wards = new ArrayList<>();
        for (int i = 0; i < WARDS.size(); i++) if (WARD_TIERS[i] <= tier) wards.add(i);
        if (!wards.isEmpty() && r.chance(0.25)) {
            int w = r.pick(wards);
            effect.addProperty("type", WARDS.get(w));
            int radius = Math.min(MAX_RADIUS, r.range(4, 4 + tier));
            effect.addProperty("radius", radius);
            weight = 15 + radius * 2;
        } else {
            List<Boon> boons = BOONS.stream().filter(b -> b.minTier() <= tier).toList();
            Boon b = r.pick(boons);
            int amp = tier >= 4 ? r.range(0, b.maxAmp()) : 0;
            double share = Math.min(1, 0.4 + 0.15 * tier) * (0.75 + 0.25 * r.nextDouble());
            int ticks = (int) Math.round(b.minTicks() + (b.maxTicks() - b.minTicks()) * share);
            effect.addProperty("type", "supernaturalcraft:apply_effect");
            effect.addProperty("effect", b.effect());
            effect.addProperty("duration", ticks);
            effect.addProperty("amplifier", amp);
            int radius = tier >= 2 && r.chance(0.4) ? r.range(3, Math.min(8, 3 + tier)) : 0;
            effect.addProperty("radius", (double) radius);
            weight = 8 + b.minTier() * 4 + amp * 10 + radius * 2 + ticks / 600f;
        }
        int liquidCount = r.range(1, Math.min(3, 1 + tier / 2));
        List<String> liquidPool = new ArrayList<>();
        for (int i = 0; i < LIQUIDS.size(); i++) if (LIQUID_TIERS[i] <= tier) liquidPool.add(LIQUIDS.get(i));
        List<String> liquids = new ArrayList<>();
        for (int i = 0; i < liquidCount; i++) liquids.add(r.pick(liquidPool));
        liquids.sort(Comparator.naturalOrder());
        int ingredientCount = r.range(2, Math.min(5, 2 + tier));
        List<Ingredient> pool = new ArrayList<>(INGREDIENTS.stream().filter(i -> i.minTier() <= tier).toList());
        List<String> ingredients = new ArrayList<>();
        // The first is always of the rite's own tier when it has any: deeper rites ask for rarer things.
        List<Ingredient> own = pool.stream().filter(i -> i.minTier() == tier).toList();
        if (!own.isEmpty()) {
            Ingredient first = r.pick(own);
            ingredients.add(first.item());
            pool.remove(first);
        }
        while (ingredients.size() < ingredientCount && !pool.isEmpty()) ingredients.add(pool.remove(r.nextInt(pool.size())).item());
        ingredients.sort(Comparator.naturalOrder());
        float mana = Math.min(MAX_MANA, Math.round(15 + 8 * tier + weight));
        String incantation = LatinNames.incantation(r.nextLong(), r.range(2, 2 + tier / 2));
        int smoke = FormulaGenerator.hsb((float) r.nextDouble(), 0.45f, 0.9f);
        return new Spec(LatinNames.name(salt ^ 0x5EEDL, index), List.copyOf(liquids), List.copyOf(ingredients), incantation, mana,
                effect, smoke, tier);
    }

    /** Research tier of the {@code index}-th rite (and formula): two per tier, I…V. */
    public static int tierOf(int index) {
        return Math.min(5, 1 + Math.max(0, index) / 2);
    }
}
