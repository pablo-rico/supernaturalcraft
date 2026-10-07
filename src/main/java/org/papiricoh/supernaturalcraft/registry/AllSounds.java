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
    public static final DeferredHolder<SoundEvent, SoundEvent> TORMENT_WHISPER = register("ambient.hell.whisper");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        DeferredHolder<SoundEvent, SoundEvent> holder = SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(SupernaturalCraft.asResource(name)));
        ALL.add(holder);
        return holder;
    }

    public static void init() {
    }
}
