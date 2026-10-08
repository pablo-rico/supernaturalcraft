package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Map;

/**
 * Every mod sound event points at re-pitched vanilla events for now. A resource pack (or a later
 * version) replaces an entry with real .ogg files without touching code.
 */
public class SNSoundDefinitions extends SoundDefinitionsProvider {

    static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("entity.metatron.ambient", "Metatron murmurs"),
            Map.entry("entity.metatron.hurt", "Metatron hurts"),
            Map.entry("entity.metatron.death", "Metatron falls silent"),
            Map.entry("entity.metatron.rise", "Metatron rises"),
            Map.entry("entity.metatron.word", "The Word is spoken"),
            Map.entry("entity.metatron.fall", "An angel falls"),
            Map.entry("entity.metatron.rewrite", "The ground is rewritten"),
            Map.entry("entity.scribe_hand.slam", "The Hand strikes"),
            Map.entry("entity.scribe_hand.write", "The quill writes"),
            Map.entry("entity.scribe_book.slam", "The Book slams"),
            Map.entry("entity.scribe_book.pages", "Pages storm"),
            Map.entry("item.angel_tablet.use", "The Tablet glows"),
            Map.entry("music.metatron", "Music plays"),
            Map.entry("entity.lilith.ambient", "Lilith hums"),
            Map.entry("entity.lilith.laugh", "Lilith laughs"),
            Map.entry("entity.lilith.hurt", "Lilith hurts"),
            Map.entry("entity.lilith.death", "Lilith's light goes out"),
            Map.entry("entity.lilith.light_charge", "White light gathers"),
            Map.entry("entity.lilith.light_burst", "White light bursts"),
            Map.entry("entity.lilith.contract_sign", "A contract is signed"),
            Map.entry("entity.lilith.contract_burn", "A contract burns"),
            Map.entry("item.hound_whistle.blow", "Hellhound answers"),
            Map.entry("music.lilith", "Music plays"),
            Map.entry("entity.azazel.ambient", "Azazel murmurs"),
            Map.entry("entity.azazel.laugh", "Azazel laughs"),
            Map.entry("entity.azazel.gaze", "Yellow eyes burn"),
            Map.entry("entity.azazel.hurt", "Azazel hurts"),
            Map.entry("entity.azazel.death", "Azazel is torn out"),
            Map.entry("entity.azazel.smoke", "Yellow smoke rushes"),
            Map.entry("block.colt_rail.snap", "Colt's rails snap shut"),
            Map.entry("block.colt_rail.recharge", "Colt's rails charge"),
            Map.entry("music.azazel", "Music plays"),
            Map.entry("entity.demon.ambient", "Demon mutters"),
            Map.entry("entity.demon.hurt", "Demon hurts"),
            Map.entry("entity.demon.death", "Demon dies"),
            Map.entry("entity.demon.smoke", "Demon smokes out"),
            Map.entry("spell.cast", "Sigil flares"),
            Map.entry("spell.fizzle", "Sigil fizzles"),
            Map.entry("spell.learn", "Sigil learned"),
            Map.entry("ritual.channel", "Ritual hums"),
            Map.entry("ritual.complete", "Ritual completes"),
            Map.entry("ritual.backlash", "Ritual backlash"),
            Map.entry("entity.lucifer.emerge", "Lucifer rises"),
            Map.entry("entity.lucifer.ambient", "Lucifer breathes"),
            Map.entry("entity.lucifer.hurt", "Lucifer hurts"),
            Map.entry("entity.lucifer.deflect", "Lucifer shrugs it off"),
            Map.entry("entity.lucifer.snap", "Lucifer snaps his fingers"),
            Map.entry("entity.lucifer.roar", "Lucifer roars"),
            Map.entry("entity.lucifer.transform", "Lucifer transforms"),
            Map.entry("entity.lucifer.wings", "Wings beat"),
            Map.entry("entity.lucifer.charge", "Grace gathers"),
            Map.entry("entity.lucifer.smite", "Lucifer smites"),
            Map.entry("entity.lucifer.death", "Lucifer is cast down"),
            Map.entry("arena.barrier", "Cage barrier repels"),
            Map.entry("item.amulet.warm", "Amulet warms"),
            Map.entry("item.colt.shot", "The Colt fires"),
            Map.entry("item.colt.cock", "Hammer cocks"),
            Map.entry("item.colt.click", "Hammer clicks on empty"),
            Map.entry("item.colt.lever", "Loading lever"),
            Map.entry("item.colt.reload", "Round seated"),
            Map.entry("item.colt.spin", "Revolver spins"),
            Map.entry("item.colt.execute", "Demon burns out"),
            Map.entry("music.lucifer", "Music plays"),
            Map.entry("entity.amara.emerge", "The Darkness rises"),
            Map.entry("entity.amara.ambient", "The Darkness breathes"),
            Map.entry("entity.amara.hurt", "The Darkness shrieks"),
            Map.entry("entity.amara.deflect", "The Darkness shrugs it off"),
            Map.entry("entity.amara.roar", "The Darkness roars"),
            Map.entry("entity.amara.snuff", "Lights gutter out"),
            Map.entry("entity.amara.lance", "Black fire tears the ground"),
            Map.entry("entity.amara.part_break", "Something of hers breaks"),
            Map.entry("entity.amara.death", "The Darkness fades"),
            Map.entry("music.amara", "Music plays"),
            Map.entry("music.chorus", "Music plays"),
            Map.entry("entity.chorus.emerge", "The Chorus descends"),
            Map.entry("entity.chorus.ambient", "The Chorus hums"),
            Map.entry("entity.chorus.sing", "A face of the Chorus sings"),
            Map.entry("entity.chorus.hymn", "The Chorus begins its Hymn"),
            Map.entry("entity.chorus.gaze", "An eye opens"),
            Map.entry("entity.chorus.judgment", "An eye passes judgment"),
            Map.entry("entity.chorus.hurt", "The Chorus rings"),
            Map.entry("entity.chorus.deflect", "Blow glances off"),
            Map.entry("entity.chorus.part_break", "Something of the Chorus shatters"),
            Map.entry("entity.chorus.kneel", "The Chorus kneels"),
            Map.entry("entity.chorus.transform", "The Chorus breaks apart"),
            Map.entry("entity.chorus.death", "The Chorus falls silent"),
            Map.entry("entity.choir_echo.sing", "Echo sings"),
            Map.entry("block.choir_bell.ring", "Choir Bell rings"),
            Map.entry("block.spell_bowl.pour", "Liquid pours into a bowl"),
            Map.entry("block.spell_bowl.light", "Spell bowl flares"),
            Map.entry("block.spell_bowl.recite", "Incantation spoken"),
            Map.entry("block.spell_bowl.typo", "Incantation falters"),
            Map.entry("block.spell_bowl.cast", "Spell takes hold"),
            Map.entry("block.spell_bowl.backlash", "Spell backfires"),
            Map.entry("block.spell_bowl.smoke_trail", "Smoke drifts away"),
            Map.entry("item.spell_bowl.spill", "Bowl spills"),
            Map.entry("item.spell_page.learn", "Spell learned"),
            Map.entry("item.hex_bag.curse", "A curse takes hold"),
            Map.entry("entity.ghost.ambient", "Ghost moans"),
            Map.entry("entity.ghost.whisper", "Something whispers"),
            Map.entry("entity.ghost.wail", "Ghost wails"),
            Map.entry("entity.ghost.hurt", "Ghost flickers"),
            Map.entry("entity.ghost.disperse", "Ghost disperses"),
            Map.entry("block.grave_bones.burn", "Bones burn"),
            Map.entry("entity.crossroads_demon.ambient", "Crossroads demon chuckles"),
            Map.entry("entity.crossroads_demon.seal", "A deal is sealed"),
            Map.entry("entity.crossroads_demon.arrive", "Someone answers"),
            Map.entry("ambient.crossroads.howl", "Hellhounds howl"));

    public SNSoundDefinitions(PackOutput output, ExistingFileHelper helper) {
        super(output, SupernaturalCraft.MODID, helper);
    }

    static String subtitleKey(String path) {
        return "subtitles.supernaturalcraft." + path;
    }

    @Override
    public void registerSounds() {
        map(AllSounds.DEMON_AMBIENT, "entity.evoker.ambient", 0.6f, 1.0f);
        map(AllSounds.DEMON_HURT, "entity.vindicator.hurt", 0.7f, 1.0f);
        map(AllSounds.DEMON_DEATH, "entity.evoker.death", 0.55f, 1.0f);
        map(AllSounds.DEMON_SMOKE, "entity.blaze.ambient", 0.5f, 1.0f);
        map(AllSounds.SPELL_CAST, "entity.evoker.cast_spell", 1.25f, 0.8f);
        map(AllSounds.SPELL_FIZZLE, "block.fire.extinguish", 1.4f, 0.6f);
        map(AllSounds.SIGIL_LEARN, "block.enchantment_table.use", 0.8f, 1.0f);
        map(AllSounds.RITUAL_CHANNEL, "block.beacon.ambient", 0.6f, 1.0f);
        map(AllSounds.RITUAL_COMPLETE, "block.beacon.activate", 0.7f, 1.0f);
        map(AllSounds.RITUAL_BACKLASH, "entity.lightning_bolt.thunder", 1.3f, 0.5f);
        map(AllSounds.LUCIFER_EMERGE, "entity.wither.spawn", 0.6f, 1.0f);
        map(AllSounds.LUCIFER_AMBIENT, "entity.warden.listening", 1.2f, 0.8f);
        map(AllSounds.LUCIFER_HURT, "entity.warden.hurt", 1.4f, 1.0f);
        map(AllSounds.LUCIFER_DEFLECT, "item.shield.block", 0.6f, 1.0f);
        map(AllSounds.LUCIFER_SNAP, "entity.evoker_fangs.attack", 1.4f, 1.0f);
        map(AllSounds.LUCIFER_ROAR, "entity.warden.roar", 1.2f, 1.0f);
        map(AllSounds.LUCIFER_TRANSFORM, "entity.elder_guardian.curse", 0.6f, 1.0f);
        map(AllSounds.LUCIFER_WINGS, "entity.ender_dragon.flap", 0.7f, 1.0f);
        map(AllSounds.LUCIFER_CHARGE, "entity.warden.sonic_charge", 0.8f, 1.0f);
        map(AllSounds.LUCIFER_SMITE, "entity.warden.sonic_boom", 0.7f, 1.0f);
        map(AllSounds.LUCIFER_DEATH, "entity.ender_dragon.death", 1.2f, 1.0f);
        map(AllSounds.ARENA_BARRIER, "block.respawn_anchor.deplete", 1.6f, 0.8f);
        map(AllSounds.AMULET_WARM, "block.amethyst_block.chime", 0.6f, 1.0f);
        map(AllSounds.COLT_SHOT, "entity.firework_rocket.large_blast", 0.6f, 1.0f);
        layer(AllSounds.COLT_SHOT_BODY, "entity.generic.explode", 1.6f, 0.6f);
        layer(AllSounds.COLT_SHOT_TAIL, "entity.lightning_bolt.thunder", 2.0f, 0.35f);
        map(AllSounds.COLT_COCK, "block.lever.click", 1.7f, 0.8f);
        map(AllSounds.COLT_CLICK, "block.tripwire.click_off", 1.8f, 1.0f);
        map(AllSounds.COLT_LEVER, "block.iron_trapdoor.open", 2.0f, 0.4f);
        map(AllSounds.COLT_RELOAD, "block.chain.place", 1.6f, 0.8f);
        map(AllSounds.COLT_SPIN, "entity.player.attack.sweep", 2.0f, 0.4f);
        map(AllSounds.COLT_EXECUTE, "block.redstone_torch.burnout", 0.5f, 1.0f);
        layer(AllSounds.COLT_EXECUTE_ZAP, "entity.lightning_bolt.impact", 1.8f, 0.3f);
        map(AllSounds.MUSIC_LUCIFER, "music.dragon", 1.0f, 1.0f);
        map(AllSounds.AMARA_EMERGE, "entity.warden.emerge", 0.5f, 1.0f);
        map(AllSounds.AMARA_AMBIENT, "ambient.soul_sand_valley.mood", 0.6f, 1.0f);
        map(AllSounds.AMARA_HURT, "block.sculk_shrieker.shriek", 1.6f, 0.6f);
        map(AllSounds.AMARA_DEFLECT, "block.amethyst_block.resonate", 0.5f, 1.0f);
        map(AllSounds.AMARA_ROAR, "entity.warden.roar", 0.6f, 1.0f);
        map(AllSounds.AMARA_SNUFF, "block.candle.extinguish", 0.5f, 1.0f);
        map(AllSounds.AMARA_LANCE, "entity.warden.sonic_boom", 0.7f, 1.0f);
        map(AllSounds.AMARA_PART_BREAK, "block.respawn_anchor.deplete", 0.6f, 1.0f);
        map(AllSounds.AMARA_DEATH, "entity.wither.death", 0.5f, 1.0f);
        map(AllSounds.MUSIC_AMARA, "music.end", 0.8f, 1.0f);
        map(AllSounds.MUSIC_CHORUS, "music.credits", 1.0f, 1.0f);
        map(AllSounds.CHORUS_EMERGE, "block.beacon.activate", 0.5f, 1.0f);
        map(AllSounds.CHORUS_AMBIENT, "block.amethyst_block.resonate", 0.5f, 1.0f);
        map(AllSounds.CHORUS_SING, "block.amethyst_block.resonate", 1.0f, 1.0f);
        map(AllSounds.CHORUS_HYMN, "block.beacon.power_select", 0.5f, 1.0f);
        map(AllSounds.CHORUS_GAZE, "block.beacon.ambient", 1.8f, 1.0f);
        map(AllSounds.CHORUS_JUDGMENT, "block.beacon.deactivate", 1.6f, 1.0f);
        map(AllSounds.CHORUS_HURT, "block.amethyst_block.hit", 0.6f, 1.0f);
        map(AllSounds.CHORUS_DEFLECT, "block.anvil.place", 1.8f, 0.8f);
        map(AllSounds.CHORUS_PART_BREAK, "block.glass.break", 0.5f, 1.0f);
        map(AllSounds.CHORUS_KNEEL, "block.anvil.land", 0.5f, 1.0f);
        map(AllSounds.CHORUS_TRANSFORM, "entity.wither.spawn", 1.4f, 1.0f);
        map(AllSounds.CHORUS_DEATH, "entity.wither.death", 1.5f, 1.0f);
        map(AllSounds.CHOIR_ECHO_SING, "block.amethyst_block.chime", 1.2f, 1.0f);
        map(AllSounds.CHOIR_BELL_RING, "block.bell.use", 1.0f, 1.0f);
        map(AllSounds.MUSIC_UNCAGED, "music.dragon", 0.8f, 1.0f);
        map(AllSounds.HELLHOUND_GROWL, "entity.wolf.growl", 0.45f, 1.4f);
        map(AllSounds.HELLHOUND_BARK, "entity.ravager.roar", 1.3f, 0.8f);
        map(AllSounds.HELLHOUND_HURT, "entity.wolf.hurt", 0.5f, 1.2f);
        map(AllSounds.HELLHOUND_DEATH, "entity.wolf.death", 0.45f, 1.2f);
        map(AllSounds.HELLHOUND_BITE, "entity.evoker_fangs.attack", 0.7f, 1.0f);
        map(AllSounds.UNCAGED_CHAINS, "block.chain.break", 0.5f, 2.0f);
        map(AllSounds.UNCAGED_STAR, "entity.generic.explode", 0.7f, 1.6f);
        map(AllSounds.AZAZEL_AMBIENT, "entity.evoker.ambient", 0.8f, 0.6f);
        map(AllSounds.AZAZEL_LAUGH, "entity.witch.celebrate", 0.55f, 1.0f);
        map(AllSounds.AZAZEL_GAZE, "entity.evoker.prepare_attack", 0.7f, 1.0f);
        map(AllSounds.AZAZEL_HURT, "entity.evoker.hurt", 0.6f, 1.0f);
        map(AllSounds.AZAZEL_DEATH, "entity.wither.death", 1.3f, 0.8f);
        map(AllSounds.AZAZEL_SMOKE, "entity.blaze.shoot", 0.5f, 0.9f);
        map(AllSounds.RAIL_TRAP_SNAP, "block.anvil.land", 0.6f, 1.0f);
        map(AllSounds.RAIL_TRAP_RECHARGE, "block.beacon.power_select", 1.6f, 0.8f);
        map(AllSounds.MUSIC_AZAZEL, "music.nether.basalt_deltas", 0.8f, 0.8f);
        map(AllSounds.LILITH_AMBIENT, "entity.allay.ambient_without_item", 0.6f, 0.7f);
        map(AllSounds.LILITH_LAUGH, "entity.witch.celebrate", 1.3f, 1.0f);
        map(AllSounds.LILITH_HURT, "entity.witch.hurt", 1.2f, 1.0f);
        map(AllSounds.LILITH_DEATH, "entity.wither.death", 1.6f, 0.7f);
        map(AllSounds.LILITH_LIGHT_CHARGE, "block.beacon.activate", 1.4f, 1.0f);
        map(AllSounds.LILITH_LIGHT_BURST, "entity.generic.explode", 1.8f, 1.0f);
        map(AllSounds.CONTRACT_SIGN, "item.book.page_turn", 0.7f, 1.0f);
        map(AllSounds.CONTRACT_BURN, "entity.blaze.shoot", 1.5f, 0.9f);
        map(AllSounds.HOUND_WHISTLE, "entity.wolf.howl", 0.6f, 0.9f);
        map(AllSounds.MUSIC_LILITH, "music.nether.soul_sand_valley", 0.9f, 0.8f);
        map(AllSounds.METATRON_AMBIENT, "entity.villager.ambient", 0.7f, 0.8f);
        map(AllSounds.METATRON_HURT, "entity.villager.hurt", 0.7f, 1.0f);
        map(AllSounds.METATRON_DEATH, "entity.wither.death", 1.8f, 0.7f);
        map(AllSounds.METATRON_RISE, "block.beacon.power_select", 0.7f, 1.0f);
        map(AllSounds.METATRON_WORD, "block.bell.resonate", 0.6f, 1.0f);
        map(AllSounds.METATRON_FALL, "entity.blaze.shoot", 0.6f, 1.0f);
        map(AllSounds.METATRON_REWRITE, "block.end_portal_frame.fill", 0.6f, 1.0f);
        map(AllSounds.SCRIBE_HAND_SLAM, "entity.ravager.stunned", 0.6f, 1.0f);
        map(AllSounds.SCRIBE_HAND_WRITE, "item.book.page_turn", 0.5f, 1.0f);
        map(AllSounds.SCRIBE_BOOK_SLAM, "entity.iron_golem.damage", 0.5f, 1.0f);
        map(AllSounds.SCRIBE_BOOK_PAGES, "item.book.page_turn", 1.2f, 1.0f);
        map(AllSounds.ANGEL_TABLET_USE, "block.beacon.activate", 1.4f, 1.0f);
        map(AllSounds.MUSIC_METATRON, "music.end", 0.9f, 0.8f);
        map(AllSounds.BOWL_POUR, "item.bottle.empty", 0.9f, 1.0f);
        map(AllSounds.BOWL_LIGHT, "item.firecharge.use", 0.8f, 0.8f);
        map(AllSounds.RECITE_LETTER, "item.book.page_turn", 1.6f, 0.25f);
        map(AllSounds.RECITE_TYPO, "block.fire.extinguish", 1.6f, 0.4f);
        map(AllSounds.BOWL_CAST, "entity.evoker.cast_spell", 0.8f, 1.0f);
        map(AllSounds.BOWL_BACKLASH, "entity.generic.explode", 1.4f, 0.6f);
        map(AllSounds.SMOKE_TRAIL, "block.fire.ambient", 0.6f, 1.0f);
        map(AllSounds.BOWL_SPILL, "item.bucket.empty", 1.3f, 0.7f);
        map(AllSounds.SPELL_PAGE_LEARN, "block.enchantment_table.use", 0.7f, 1.0f);
        map(AllSounds.HEX_CURSE, "entity.witch.ambient", 0.6f, 0.8f);
        map(AllSounds.GHOST_AMBIENT, "ambient.soul_sand_valley.mood", 1.4f, 0.6f);
        map(AllSounds.GHOST_WHISPER, "ambient.cave", 1.2f, 0.6f);
        map(AllSounds.GHOST_WAIL, "entity.ghast.scream", 0.6f, 0.8f);
        map(AllSounds.GHOST_HURT, "entity.vex.hurt", 0.6f, 1.0f);
        map(AllSounds.GHOST_DISPERSE, "entity.vex.death", 0.5f, 1.0f);
        map(AllSounds.GRAVE_BURN, "entity.ghast.death", 0.6f, 1.0f);
        map(AllSounds.CROSSROADS_AMBIENT, "entity.evoker.ambient", 0.75f, 0.8f);
        map(AllSounds.CROSSROADS_SEAL, "entity.evoker.prepare_summon", 0.6f, 1.0f);
        map(AllSounds.CROSSROADS_ARRIVE, "entity.evoker.prepare_wololo", 0.7f, 1.0f);
        map(AllSounds.DEBT_HOWL, "entity.wolf.howl", 0.5f, 1.0f);
        map(AllSounds.TORMENT_WHISPER, "ambient.soul_sand_valley.additions", 0.7f, 0.8f);
        org.papiricoh.supernaturalcraft.datagen.chuck.ChuckAssetData.sounds((event, definition) -> add(event.get(), definition));
        org.papiricoh.supernaturalcraft.datagen.horsemen.HorsemenAssetData.sounds((event, definition) -> add(event.get(), definition));
        org.papiricoh.supernaturalcraft.datagen.michael.MichaelAssetData.sounds((event, definition) -> add(event.get(), definition));
        org.papiricoh.supernaturalcraft.datagen.allegiance.AllegianceAssetData.sounds((event, definition) -> add(event.get(), definition));
        org.papiricoh.supernaturalcraft.datagen.gabriel.GabrielAssetData.sounds((event, definition) -> add(event.get(), definition));
    }

    /** A sound only ever played together with another, so it has no subtitle of its own. */
    private void layer(DeferredHolder<SoundEvent, SoundEvent> event, String vanillaEvent, float pitch, float volume) {
        add(event, SoundDefinition.definition()
                .with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace(vanillaEvent), SoundDefinition.SoundType.EVENT)
                        .pitch(pitch).volume(volume)));
    }

    private void map(DeferredHolder<SoundEvent, SoundEvent> event, String vanillaEvent, float pitch, float volume) {
        add(event, SoundDefinition.definition()
                .subtitle(subtitleKey(event.getId().getPath()))
                .with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace(vanillaEvent), SoundDefinition.SoundType.EVENT)
                        .pitch(pitch).volume(volume)));
    }
}
