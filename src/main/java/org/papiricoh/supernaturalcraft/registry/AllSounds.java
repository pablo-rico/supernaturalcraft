package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's own sound events. Each one resolves (in the generated sounds.json) to re-pitched
 * vanilla sounds, so a resource pack can drop real recordings in under the same event names.
 */
public class AllSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, SupernaturalCraft.MODID);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ALL = new ArrayList<>();

    public static final DeferredHolder<SoundEvent, SoundEvent> DEMON_AMBIENT = register("entity.demon.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEMON_HURT = register("entity.demon.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEMON_DEATH = register("entity.demon.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEMON_SMOKE = register("entity.demon.smoke");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_CAST = register("spell.cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_FIZZLE = register("spell.fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIGIL_LEARN = register("spell.learn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_CHANNEL = register("ritual.channel");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_COMPLETE = register("ritual.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_BACKLASH = register("ritual.backlash");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_EMERGE = register("entity.lucifer.emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_AMBIENT = register("entity.lucifer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_HURT = register("entity.lucifer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_DEFLECT = register("entity.lucifer.deflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_SNAP = register("entity.lucifer.snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_ROAR = register("entity.lucifer.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_TRANSFORM = register("entity.lucifer.transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_WINGS = register("entity.lucifer.wings");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_CHARGE = register("entity.lucifer.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_SMITE = register("entity.lucifer.smite");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUCIFER_DEATH = register("entity.lucifer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_BARRIER = register("arena.barrier");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMULET_WARM = register("item.amulet.warm");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_SHOT = register("item.colt.shot");
    /** Layered under the shot (no subtitle of their own): the blast's body and its rolling tail. */
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_SHOT_BODY = register("item.colt.shot_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_SHOT_TAIL = register("item.colt.shot_tail");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_COCK = register("item.colt.cock");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_CLICK = register("item.colt.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_LEVER = register("item.colt.lever");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_RELOAD = register("item.colt.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_SPIN = register("item.colt.spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_EXECUTE = register("item.colt.execute");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLT_EXECUTE_ZAP = register("item.colt.execute_zap");
    /** Placeholder for a boss theme; points at the vanilla dragon fight music until replaced. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_LUCIFER = register("music.lucifer");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_EMERGE = register("entity.amara.emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_AMBIENT = register("entity.amara.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_HURT = register("entity.amara.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_DEFLECT = register("entity.amara.deflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_ROAR = register("entity.amara.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_SNUFF = register("entity.amara.snuff");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_LANCE = register("entity.amara.lance");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_PART_BREAK = register("entity.amara.part_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMARA_DEATH = register("entity.amara.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_AMARA = register("music.amara");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CHORUS = register("music.chorus");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_EMERGE = register("entity.chorus.emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_AMBIENT = register("entity.chorus.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_SING = register("entity.chorus.sing");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_HYMN = register("entity.chorus.hymn");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_GAZE = register("entity.chorus.gaze");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_JUDGMENT = register("entity.chorus.judgment");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_HURT = register("entity.chorus.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_DEFLECT = register("entity.chorus.deflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_PART_BREAK = register("entity.chorus.part_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_KNEEL = register("entity.chorus.kneel");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_TRANSFORM = register("entity.chorus.transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORUS_DEATH = register("entity.chorus.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOIR_ECHO_SING = register("entity.choir_echo.sing");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOIR_BELL_RING = register("block.choir_bell.ring");
    // Hell
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_UNCAGED = register("music.uncaged");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELLHOUND_GROWL = register("entity.hellhound.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELLHOUND_BARK = register("entity.hellhound.bark");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELLHOUND_HURT = register("entity.hellhound.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELLHOUND_DEATH = register("entity.hellhound.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELLHOUND_BITE = register("entity.hellhound.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCAGED_CHAINS = register("entity.lucifer_uncaged.chains");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNCAGED_STAR = register("entity.lucifer_uncaged.star");
    // Azazel
    public static final DeferredHolder<SoundEvent, SoundEvent> AZAZEL_AMBIENT = register("entity.azazel.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> AZAZEL_LAUGH = register("entity.azazel.laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> AZAZEL_GAZE = register("entity.azazel.gaze");
    public static final DeferredHolder<SoundEvent, SoundEvent> AZAZEL_HURT = register("entity.azazel.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> AZAZEL_DEATH = register("entity.azazel.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> AZAZEL_SMOKE = register("entity.azazel.smoke");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAIL_TRAP_SNAP = register("block.colt_rail.snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAIL_TRAP_RECHARGE = register("block.colt_rail.recharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_AZAZEL = register("music.azazel");
    // Lilith
    public static final DeferredHolder<SoundEvent, SoundEvent> LILITH_AMBIENT = register("entity.lilith.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LILITH_LAUGH = register("entity.lilith.laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> LILITH_HURT = register("entity.lilith.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LILITH_DEATH = register("entity.lilith.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LILITH_LIGHT_CHARGE = register("entity.lilith.light_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LILITH_LIGHT_BURST = register("entity.lilith.light_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTRACT_SIGN = register("entity.lilith.contract_sign");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTRACT_BURN = register("entity.lilith.contract_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUND_WHISTLE = register("item.hound_whistle.blow");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_LILITH = register("music.lilith");
    // Metatron
    public static final DeferredHolder<SoundEvent, SoundEvent> METATRON_AMBIENT = register("entity.metatron.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> METATRON_HURT = register("entity.metatron.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> METATRON_DEATH = register("entity.metatron.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> METATRON_RISE = register("entity.metatron.rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> METATRON_WORD = register("entity.metatron.word");
    public static final DeferredHolder<SoundEvent, SoundEvent> METATRON_FALL = register("entity.metatron.fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> METATRON_REWRITE = register("entity.metatron.rewrite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCRIBE_HAND_SLAM = register("entity.scribe_hand.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCRIBE_HAND_WRITE = register("entity.scribe_hand.write");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCRIBE_BOOK_SLAM = register("entity.scribe_book.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCRIBE_BOOK_PAGES = register("entity.scribe_book.pages");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANGEL_TABLET_USE = register("item.angel_tablet.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_METATRON = register("music.metatron");
    public static final DeferredHolder<SoundEvent, SoundEvent> TORMENT_WHISPER = register("ambient.hell.whisper");

    // The spell bowl, ghosts and the crossroads (v0.8)
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWL_POUR = register("block.spell_bowl.pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWL_LIGHT = register("block.spell_bowl.light");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECITE_LETTER = register("block.spell_bowl.recite");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECITE_TYPO = register("block.spell_bowl.typo");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWL_CAST = register("block.spell_bowl.cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWL_BACKLASH = register("block.spell_bowl.backlash");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWL_SPILL = register("item.spell_bowl.spill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_PAGE_LEARN = register("item.spell_page.learn");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEX_CURSE = register("item.hex_bag.curse");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMOKE_TRAIL = register("block.spell_bowl.smoke_trail");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_AMBIENT = register("entity.ghost.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_WHISPER = register("entity.ghost.whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_WAIL = register("entity.ghost.wail");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_HURT = register("entity.ghost.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_DISPERSE = register("entity.ghost.disperse");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVE_BURN = register("block.grave_bones.burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROSSROADS_AMBIENT = register("entity.crossroads_demon.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROSSROADS_SEAL = register("entity.crossroads_demon.seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROSSROADS_ARRIVE = register("entity.crossroads_demon.arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEBT_HOWL = register("ambient.crossroads.howl");

    // The Author (v0.10): generated recordings (tools/soundgen), see ChuckSoundDefinitions.
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_TYPE = register("entity.chuck.type");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_CARRIAGE = register("entity.chuck.carriage");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_BELL = register("entity.chuck.bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_SNAP = register("entity.chuck.snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_BACKSPACE = register("entity.chuck.backspace");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_KEY_IMPACT = register("entity.chuck.key_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_PAGE_TEAR = register("entity.chuck.page_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_REWRITE = register("entity.chuck.rewrite");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_HURT = register("entity.chuck.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_LAUGH = register("entity.chuck.laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_REVEAL = register("entity.chuck.reveal");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_CRACK = register("entity.chuck.crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_ERASE = register("entity.chuck.erase");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_WRITE = register("entity.chuck.write");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_ECHO = register("entity.chuck.echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUCK_APPROVE = register("entity.chuck.approve");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTER_ALLY_ARRIVE = register("entity.hunter_ally.arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> AUTHOR_NPC_AMBIENT = register("entity.author_npc.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEN_WRITE = register("item.authors_pen.write");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CHUCK = register("music.chuck");

    // The Four Horsemen (v0.11): Art makes the OGG files and their definitions (HorsemenAssetData).
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_AMBIENT = register("entity.war.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_HURT = register("entity.war.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_DEATH = register("entity.war.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_RAGE = register("entity.war.rage");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_SWORD_CLASH = register("entity.war.sword_clash");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_PARRY = register("entity.war.parry");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAMINE_AMBIENT = register("entity.famine.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAMINE_HURT = register("entity.famine.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAMINE_DEATH = register("entity.famine.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAMINE_DEVOUR = register("entity.famine.devour");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAMINE_HUNGER = register("entity.famine.hunger");
    public static final DeferredHolder<SoundEvent, SoundEvent> PESTILENCE_AMBIENT = register("entity.pestilence.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PESTILENCE_HURT = register("entity.pestilence.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PESTILENCE_DEATH = register("entity.pestilence.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PESTILENCE_COUGH = register("entity.pestilence.cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_AMBIENT = register("entity.death.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_HURT = register("entity.death.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_DEATH = register("entity.death.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_REAP = register("entity.death.reap");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_CLOCK_TICK = register("entity.death.clock_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_LIMBO_BELL = register("entity.death.limbo_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_WORLD_FLIP = register("entity.death.world_flip");
    public static final DeferredHolder<SoundEvent, SoundEvent> REAPER_AMBIENT = register("entity.reaper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> REAPER_ATTACK = register("entity.reaper.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLY_SWARM_BUZZ = register("entity.fly_swarm.buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEED_NEIGH = register("entity.horseman_steed.neigh");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEED_GALLOP = register("entity.horseman_steed.gallop");

    // The Archangel Michael (v0.12): Art makes the OGG files and their definitions (MichaelAssetData).
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_AMBIENT = register("entity.michael.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_HURT = register("entity.michael.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_DEATH = register("entity.michael.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_WINGS = register("entity.michael.wings");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_SMITE = register("entity.michael.smite");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_ASK_YES = register("entity.michael.ask_yes");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_TRUMPET = register("entity.michael.trumpet");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_TRANSFORM = register("entity.michael.transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_HALO_BREAK = register("entity.michael.halo_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_LANCE_THROW = register("entity.michael.lance_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_LANCE_IMPACT = register("entity.michael.lance_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_LANCE_RECALL = register("entity.michael.lance_recall");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_FEATHER_STORM = register("entity.michael.feather_storm");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_CHOIR = register("entity.michael.choir");
    public static final DeferredHolder<SoundEvent, SoundEvent> MICHAEL_DIVE = register("entity.michael.dive");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_AMBIENT = register("entity.host_angel.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_HURT = register("entity.host_angel.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_DEATH = register("entity.host_angel.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_MARCH = register("entity.host_angel.march");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_SHIELD = register("entity.host_angel.shield");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERAL_ARMOR_WARD = register("item.general_armor.ward");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRACE_FLIGHT = register("item.michaels_grace.flight");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        DeferredHolder<SoundEvent, SoundEvent> holder = SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(SupernaturalCraft.asResource(name)));
        ALL.add(holder);
        return holder;
    }

    public static void init() {
    }
}
