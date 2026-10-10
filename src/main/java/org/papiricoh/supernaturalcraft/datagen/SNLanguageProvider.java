package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * English only. Item, block, entity and effect names default to the title-cased registry id;
 * {@link #NAMES} overrides the ones that need an apostrophe or a better phrase.
 */
public class SNLanguageProvider extends LanguageProvider {

    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        NAMES.put("rubys_knife", "Ruby's Knife");
        NAMES.put("azazel_blood", "Azazel's Blood");
        NAMES.put("hound_whistle", "Lilith's Whistle");
        NAMES.put("metatron_trophy", "Scribe's Bust");
        NAMES.put("scribe_hand", "Hand of God");
        NAMES.put("scribe_book", "The Book");
        NAMES.put("lilith_trophy", "White-Eyed Bust");
        NAMES.put("bound_hellhound", "Bound Hellhound");
        NAMES.put("azazel_trophy", "Yellow-Eyed Bust");
        NAMES.put("colt_rail", "Samuel Colt's Rail");
        NAMES.put("devils_trap", "Devil's Trap");
        NAMES.put("salt_line", "Salt Line");
        NAMES.put("lucifer", "Lucifer");
        NAMES.put("key_to_the_cage", "Key to the Cage");
        NAMES.put("lucifers_grace", "Lucifer's Grace");
        NAMES.put("hunters_amulet", "Hunter's Amulet");
        NAMES.put("the_colt", "The Colt");
        NAMES.put("endless_colt", "The Endless Colt");
        NAMES.put("michael", "Michael");
        NAMES.put("host_angel", "Soldier of the Host");
        NAMES.put("michael_lance", "Lance of Michael");
        NAMES.put("thrown_lance", "Lance of Michael");
        NAMES.put("borrowed_lance", "Michael's Lance");
        NAMES.put("michaels_grace", "Michael's Grace");
        NAMES.put("michael_trophy", "Bust of the Archangel");
        NAMES.put("general_helmet", "General's Helm");
        NAMES.put("general_chestplate", "General's Breastplate");
        NAMES.put("general_leggings", "General's Greaves");
        NAMES.put("general_boots", "General's Sabatons");
        NAMES.put("grace_favor", "Grace's Favour");
        NAMES.put("heavens_mark", "Heaven's Mark");
        NAMES.put("exorcists_mace", "Exorcist's Mace");
        NAMES.put("broken_chorus", "The Broken Chorus");
        NAMES.put("rack_hook", "Hook of the Rack");
        NAMES.put("caged_lucifer", "Lucifer");
        NAMES.put("hellfire_brazier", "Hellfire Brazier");
        NAMES.put("spell_bowl", "Spell Bowl");
        NAMES.put("crossroads_contract", "Crossroads Contract");
        NAMES.put("curse_bag", "Hex Bag of Cursing");
        NAMES.put("protection_bag", "Hex Bag of Protection");
        NAMES.put("grave_bones", "Restless Bones");
        NAMES.put("chuck", "Chuck");
        NAMES.put("author_npc", "The Author");
        NAMES.put("authors_pen", "The Author's Pen");
        NAMES.put("sams_amulet", "Sam's Amulet");
        NAMES.put("the_end_manuscript", "The End");
        NAMES.put("hunter_ally", "Hunter");
        NAMES.put("horseman_steed", "Horseman's Steed");
        NAMES.put("horseman_steed_spawn_egg", "Horseman's Steed Spawn Egg");
        NAMES.put("war_trophy", "Bust of War");
        NAMES.put("famine_trophy", "Bust of Famine");
        NAMES.put("pestilence_trophy", "Bust of Pestilence");
        NAMES.put("death_trophy", "Bust of Death");
        NAMES.put("war_mirage", "Mirage");
        NAMES.put("limbo_exit", "The Light Out");
        NAMES.put("messenger", "Heaven's Messenger");
        NAMES.put("host_ally", "Soldier of the Host");
        NAMES.put("gabriel", "Gabriel");
        NAMES.put("gabriel_double", "Gabriel?");
        NAMES.put("gabriel_pie", "Cream Pie");
        NAMES.put("gabriel_blade", "Gabriel's Blade");
        NAMES.put("gabriel_trophy", "The Trickster's Television");
        NAMES.put("raphaels_stormcaller", "Raphael's Stormcaller");
        NAMES.put("raphael_trophy", "Raphael's Bust");
        // v0.18
        NAMES.put("naomis_drill", "Naomi's Drill");
        NAMES.put("naomis_diadem", "Naomi's Diadem");
        NAMES.put("naomi_trophy", "Naomi's Bust");
        NAMES.put("zachariahs_blade", "Zachariah's Blade");
        NAMES.put("heavens_seal", "Heaven's Seal");
        NAMES.put("zachariah_trophy", "Zachariah's Bust");
        NAMES.put("clerk_angel", "Angel Clerk");
        NAMES.put("heaven_guard", "Angel Guard");
        NAMES.put("memo_projectile", "Memo");
        NAMES.put("dead_mans_blood", "Dead Man's Blood");
        NAMES.put("henrys_case", "Henry's Case");
        NAMES.put("spellwrights_spectacles", "Spellwright's Spectacles");
        NAMES.put("men_of_letters_ring", "Men of Letters Ring");
        NAMES.put("men_of_letters_emblem", "Men of Letters Emblem");
        NAMES.put("trickster_bait", "Trickster's Bait");
        NAMES.put("ascension_shard_1", "Ascension Shard I");
        NAMES.put("ascension_shard_2", "Ascension Shard II");
        NAMES.put("ascension_shard_3", "Ascension Shard III");
        NAMES.put("ascension_shard_4", "Ascension Shard IV");
        NAMES.put("ascension_shard_5", "Ascension Shard V");
        NAMES.put("hunters_cap", "Hunter's Cap");
        NAMES.put("hunters_jacket", "Hunter's Jacket");
        NAMES.put("hunters_jeans", "Hunter's Jeans");
        NAMES.put("hunters_boots", "Hunter's Boots");
        NAMES.put("trickster_remote", "Trickster's Remote");
    }

    public SNLanguageProvider(PackOutput output) {
        super(output, SupernaturalCraft.MODID, "en_us");
    }

    static String titleCase(String id) {
        if (NAMES.containsKey(id)) return NAMES.get(id);
        if (id.startsWith("rune_")) return titleCase(id.substring(5)) + " Rune";
        if (id.startsWith("choir_bell_")) return titleCase(id.substring(11)) + " Choir Bell";
        StringBuilder sb = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            boolean minor = sb.length() > 0 && (word.equals("of") || word.equals("the") || word.equals("to"));
            sb.append(minor ? word : word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        }
        return sb.toString();
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.supernaturalcraft", "SupernaturalCraft");

        for (DeferredHolder<Item, ? extends Item> holder : AllItems.ITEMS.getEntries()) {
            Item item = holder.get();
            // Name-block items (salt, chalk) are named after the item, not the block they place.
            if (item instanceof BlockItem bi && BuiltInRegistries.BLOCK.getKey(bi.getBlock()).getPath().equals(holder.getId().getPath())) {
                continue;
            }
            add(item, titleCase(holder.getId().getPath()));
        }
        AllBlocks.BLOCKS.getEntries().forEach(h -> add(h.get(), titleCase(h.getId().getPath())));
        AllEntities.ENTITY_TYPES.getEntries().forEach(h -> add(h.get(), titleCase(h.getId().getPath())));
        AllMobEffects.MOB_EFFECTS.getEntries().forEach(h -> add(h.get(), titleCase(h.getId().getPath())));

        // --- Tooltips -------------------------------------------------------------------------
        add("tooltip.supernaturalcraft.demon_bane", "×%s damage against demons");
        add("tooltip.supernaturalcraft.harvests_blood", "Demons slain with it always bleed");

        // --- Damage messages -----------------------------------------------------------------
        death("smite", "%1$s was smitten", "%1$s was smitten by %2$s");
        death("colt", "%1$s was shot", "%1$s was shot by %2$s");
        death("void", "%1$s was swallowed by the Darkness", "%1$s was unmade by %2$s");
        death("hellfire", "%1$s burned in hellfire", "%1$s was burned in hellfire by %2$s");
        death("holy_water", "%1$s was scalded by holy water", "%1$s was scalded by %2$s's holy water");
        death("grace", "%1$s was unmade by grace", "%1$s was unmade by %2$s");
        death("spell", "%1$s was undone by a sigil", "%1$s was undone by %2$s's sigil");
        death("judgment", "%1$s was judged", "%1$s was judged by %2$s");
        death("hymn", "%1$s was unmade by the Hymn", "%1$s heard %2$s sing");
        death("white_light", "%1$s was burned out by a white light", "%1$s saw %2$s's true face");
        death("divine_wrath", "%1$s was struck down by Divine Wrath", "%1$s felt the wrath of %2$s");
        death("arena_barrier", "%1$s tried to leave the Cage", "%1$s tried to flee from %2$s");

        // --- Sound subtitles -----------------------------------------------------------------
        AllSounds.ALL.forEach(h -> add(SNSoundDefinitions.subtitleKey(h.getId().getPath()),
                SNSoundDefinitions.SUBTITLES.getOrDefault(h.getId().getPath(),
                        org.papiricoh.supernaturalcraft.datagen.chuck.ChuckAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                org.papiricoh.supernaturalcraft.datagen.horsemen.HorsemenAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                        org.papiricoh.supernaturalcraft.datagen.michael.MichaelAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                                org.papiricoh.supernaturalcraft.datagen.allegiance.AllegianceAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                                        org.papiricoh.supernaturalcraft.datagen.gabriel.GabrielAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                                                org.papiricoh.supernaturalcraft.datagen.raphael.RaphaelAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                                                        org.papiricoh.supernaturalcraft.datagen.legacy.LegacyAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                                                                org.papiricoh.supernaturalcraft.datagen.heaven.HeavenAssetData.SUBTITLES.getOrDefault(h.getId().getPath(),
                                                                                        titleCase(h.getId().getPath().replace('.', '_')))))))))))));

        SNLang.addAll(this::add);
    }

    private void death(String type, String plain, String byPlayer) {
        add("death.attack.supernaturalcraft." + type, plain);
        add("death.attack.supernaturalcraft." + type + ".player", byPlayer);
    }
}
