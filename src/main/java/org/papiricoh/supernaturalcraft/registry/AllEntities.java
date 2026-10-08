package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferIllusion;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.projectile.BossShard;
import org.papiricoh.supernaturalcraft.entity.demon.DemonOccultist;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.entity.projectile.HellfireBolt;
import org.papiricoh.supernaturalcraft.entity.projectile.HolyWaterProjectile;

public class AllEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SupernaturalCraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HolyWaterProjectile>> HOLY_WATER =
            ENTITY_TYPES.register("holy_water", () -> EntityType.Builder
                    .<HolyWaterProjectile>of(HolyWaterProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10)
                    .build("holy_water"));

    public static final DeferredHolder<EntityType<?>, EntityType<HellfireBolt>> HELLFIRE_BOLT =
            ENTITY_TYPES.register("hellfire_bolt", () -> EntityType.Builder
                    .<HellfireBolt>of(HellfireBolt::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(2)
                    .build("hellfire_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<SigilBolt>> SIGIL_BOLT =
            ENTITY_TYPES.register("sigil_bolt", () -> EntityType.Builder
                    .<SigilBolt>of(SigilBolt::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(2)
                    .build("sigil_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<WardEntity>> WARD =
            ENTITY_TYPES.register("ward", () -> EntityType.Builder
                    .<WardEntity>of(WardEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f).clientTrackingRange(8).updateInterval(20).fireImmune()
                    .build("ward"));

    public static final DeferredHolder<EntityType<?>, EntityType<WardEntity>> LINGERING_ZONE =
            ENTITY_TYPES.register("lingering_zone", () -> EntityType.Builder
                    .<WardEntity>of(WardEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f).clientTrackingRange(8).updateInterval(20).fireImmune()
                    .build("lingering_zone"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackEyedDemon>> BLACK_EYED_DEMON =
            ENTITY_TYPES.register("black_eyed_demon", () -> EntityType.Builder
                    .of(BlackEyedDemon::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).clientTrackingRange(8)
                    .build("black_eyed_demon"));

    public static final DeferredHolder<EntityType<?>, EntityType<DemonOccultist>> DEMON_OCCULTIST =
            ENTITY_TYPES.register("demon_occultist", () -> EntityType.Builder
                    .of(DemonOccultist::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).clientTrackingRange(8)
                    .build("demon_occultist"));

    public static final DeferredHolder<EntityType<?>, EntityType<LuciferEntity>> LUCIFER =
            ENTITY_TYPES.register("lucifer", () -> EntityType.Builder
                    .of(LuciferEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.0f).eyeHeight(1.75f).fireImmune().clientTrackingRange(16).updateInterval(1)
                    .build("lucifer"));

    public static final DeferredHolder<EntityType<?>, EntityType<LuciferIllusion>> LUCIFER_ILLUSION =
            ENTITY_TYPES.register("lucifer_illusion", () -> EntityType.Builder
                    .of(LuciferIllusion::new, MobCategory.MONSTER)
                    .sized(0.9f, 2.6f).fireImmune().clientTrackingRange(12)
                    .build("lucifer_illusion"));

    public static final DeferredHolder<EntityType<?>, EntityType<TelegraphMarker>> TELEGRAPH =
            ENTITY_TYPES.register("telegraph", () -> EntityType.Builder
                    .<TelegraphMarker>of(TelegraphMarker::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f).clientTrackingRange(16).updateInterval(100).fireImmune()
                    .build("telegraph"));

    public static final DeferredHolder<EntityType<?>, EntityType<BossShard>> BOSS_SHARD =
            ENTITY_TYPES.register("boss_shard", () -> EntityType.Builder
                    .<BossShard>of(BossShard::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1)
                    .build("boss_shard"));

    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.projectile.SoulCrescent>> SOUL_CRESCENT =
            ENTITY_TYPES.register("soul_crescent", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.projectile.SoulCrescent>of(org.papiricoh.supernaturalcraft.entity.projectile.SoulCrescent::new, MobCategory.MISC)
                    .sized(1.8f, 0.6f).clientTrackingRange(6).updateInterval(1)
                    .build("soul_crescent"));

    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.hazard.VoidZone>> VOID_ZONE =
            ENTITY_TYPES.register("void_zone", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.hazard.VoidZone>of(org.papiricoh.supernaturalcraft.entity.hazard.VoidZone::new, MobCategory.MISC)
                    .sized(1.0f, 0.2f).clientTrackingRange(8).updateInterval(20).fireImmune().build("void_zone"));
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity>> AMARA =
            ENTITY_TYPES.register("amara", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity>of(org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity::new, MobCategory.MONSTER)
                    .sized(6.0f, 8.0f).fireImmune().clientTrackingRange(20).updateInterval(1).build("amara"));
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraShade>> AMARA_SHADE =
            ENTITY_TYPES.register("amara_shade", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraShade>of(org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraShade::new, MobCategory.MONSTER)
                    .sized(0.6f, 2.2f).clientTrackingRange(10).build("amara_shade"));
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail>> FLAME_TRAIL =
            ENTITY_TYPES.register("flame_trail", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail>of(org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail::new, MobCategory.MISC)
                    .sized(1.0f, 0.2f).clientTrackingRange(6).updateInterval(20).fireImmune()
                    .build("flame_trail"));

    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity>> BROKEN_CHORUS =
            ENTITY_TYPES.register("broken_chorus", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity::new, MobCategory.MONSTER)
                    .sized(3.0f, 12.0f).fireImmune().clientTrackingRange(20).updateInterval(1).build("broken_chorus"));
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chorus.ChoirEchoEntity>> CHOIR_ECHO =
            ENTITY_TYPES.register("choir_echo", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chorus.ChoirEchoEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chorus.ChoirEchoEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.9f).fireImmune().clientTrackingRange(10).build("choir_echo"));

    // --- Hell ----------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity>> HELLHOUND =
            ENTITY_TYPES.register("hellhound", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 1.2f).eyeHeight(1.0f).fireImmune().clientTrackingRange(10).build("hellhound"));
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.uncaged.LuciferUncagedEntity>> LUCIFER_UNCAGED =
            ENTITY_TYPES.register("lucifer_uncaged", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.boss.uncaged.LuciferUncagedEntity::new, MobCategory.MONSTER)
                    .sized(0.75f, 2.1f).eyeHeight(1.85f).fireImmune().clientTrackingRange(20).updateInterval(1)
                    .build("lucifer_uncaged"));
    /** Lucifer as he waits in the closed Cage: chained, still, untouchable. Only for the eye. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.hell.cage.CagedLuciferEntity>> CAGED_LUCIFER =
            ENTITY_TYPES.register("caged_lucifer", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.hell.cage.CagedLuciferEntity::new, MobCategory.MISC)
                    .sized(0.75f, 2.1f).fireImmune().clientTrackingRange(16).updateInterval(20).build("caged_lucifer"));

    // --- Azazel ---------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelEntity>> AZAZEL =
            ENTITY_TYPES.register("azazel", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).fireImmune().clientTrackingRange(16).updateInterval(1)
                    .build("azazel"));
    /** A block Azazel tore out of the ground and threw: drawn as the block, never placed. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.azazel.HurledDebris>> HURLED_DEBRIS =
            ENTITY_TYPES.register("hurled_debris", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.azazel.HurledDebris>of(org.papiricoh.supernaturalcraft.entity.boss.azazel.HurledDebris::new, MobCategory.MISC)
                    .sized(0.9f, 0.9f).clientTrackingRange(8).updateInterval(1).build("hurled_debris"));

    // --- Lilith ---------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity>> LILITH =
            ENTITY_TYPES.register("lilith", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(16).updateInterval(1)
                    .build("lilith"));
    /** A hellhound answering Lilith's Whistle: on its holder's side for a minute. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.hellhound.BoundHellhoundEntity>> BOUND_HELLHOUND =
            ENTITY_TYPES.register("bound_hellhound", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.hellhound.BoundHellhoundEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.2f).eyeHeight(1.0f).fireImmune().clientTrackingRange(10).build("bound_hellhound"));

    // --- Metatron -------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronEntity>> METATRON =
            ENTITY_TYPES.register("metatron", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(20).updateInterval(1)
                    .build("metatron"));
    /** The Hand of God with its quill: one of Metatron's two constructs. Untouchable. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeHandEntity>> SCRIBE_HAND =
            ENTITY_TYPES.register("scribe_hand", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeHandEntity>of(org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeHandEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.5f).fireImmune().clientTrackingRange(20).updateInterval(1).build("scribe_hand"));
    /** The Book, held open by Metatron's will. Untouchable. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeBookEntity>> SCRIBE_BOOK =
            ENTITY_TYPES.register("scribe_book", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeBookEntity>of(org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeBookEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.5f).fireImmune().clientTrackingRange(20).updateInterval(1).build("scribe_book"));

    // --- v0.8 ------------------------------------------------------------------------------------
    /** A vengeful spirit bound to the bones in its grave. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity>> GHOST =
            ENTITY_TYPES.register("ghost", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(10).build("ghost"));
    /** The crossroads demon: answers a bowl at night, and makes deals. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity>> CROSSROADS_DEMON =
            ENTITY_TYPES.register("crossroads_demon", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).clientTrackingRange(10).build("crossroads_demon"));

    // --- The Author (v0.10) ------------------------------------------------------------------------
    /** Chuck, the Author: the last boss. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity>> CHUCK =
            ENTITY_TYPES.register("chuck", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(24).updateInterval(1)
                    .build("chuck"));
    /** The Author at home in his cabin: talks, cannot be hurt. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.author.AuthorNpcEntity>> AUTHOR_NPC =
            ENTITY_TYPES.register("author_npc", () -> EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.author.AuthorNpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(10).build("author_npc"));
    /** One of the Author's giant hands. Untouchable. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorHandEntity>> AUTHOR_HAND =
            ENTITY_TYPES.register("author_hand", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorHandEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorHandEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f).fireImmune().clientTrackingRange(24).updateInterval(1).build("author_hand"));
    /** A manuscript page or a ring's weak point: what holds his script together. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity>> AUTHOR_TARGET =
            ENTITY_TYPES.register("author_target", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity::new, MobCategory.MISC)
                    .sized(1.6f, 1.6f).fireImmune().clientTrackingRange(24).updateInterval(1).build("author_target"));
    /** An old enemy written back in ink for one attack. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chuck.InkEchoEntity>> INK_ECHO =
            ENTITY_TYPES.register("ink_echo", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chuck.InkEchoEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chuck.InkEchoEntity::new, MobCategory.MISC)
                    .sized(0.8f, 2.2f).fireImmune().clientTrackingRange(24).updateInterval(1).build("ink_echo"));
    /** A word written into the air. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chuck.FloatingWordEntity>> FLOATING_WORD =
            ENTITY_TYPES.register("floating_word", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chuck.FloatingWordEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chuck.FloatingWordEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).fireImmune().clientTrackingRange(24).updateInterval(1).build("floating_word"));
    /** A giant typewriter key falling from the sky. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chuck.TypewriterKeyEntity>> TYPEWRITER_KEY =
            ENTITY_TYPES.register("typewriter_key", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chuck.TypewriterKeyEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chuck.TypewriterKeyEntity::new, MobCategory.MISC)
                    .sized(1.2f, 0.8f).fireImmune().clientTrackingRange(24).updateInterval(1).build("typewriter_key"));
    /** Dean, Sam or Castiel, at the very end. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.chuck.HunterAllyEntity>> HUNTER_ALLY =
            ENTITY_TYPES.register("hunter_ally", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.chuck.HunterAllyEntity>of(org.papiricoh.supernaturalcraft.entity.boss.chuck.HunterAllyEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.9f).fireImmune().clientTrackingRange(24).updateInterval(1).build("hunter_ally"));

    // --- The Four Horsemen (v0.11) ---------------------------------------------------------------
    /** War, in a red suit, with a sword and a red horse. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarEntity>> WAR =
            ENTITY_TYPES.register("war", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(20).updateInterval(1).build("war"));
    /** Famine, an old man in a wheelchair who is always hungry. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.FamineEntity>> FAMINE =
            ENTITY_TYPES.register("famine", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.FamineEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.FamineEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(20).updateInterval(1).build("famine"));
    /** Pestilence, coughing, with a cane and glasses. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.PestilenceEntity>> PESTILENCE =
            ENTITY_TYPES.register("pestilence", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.PestilenceEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.PestilenceEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(20).updateInterval(1).build("pestilence"));
    /** Death: thin, in a black suit, with a cane; older than God. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathEntity>> DEATH =
            ENTITY_TYPES.register("death", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(20).updateInterval(1).build("death"));
    /** A Horseman's horse, left behind: a better horse, tameable like any other. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanSteedEntity>> HORSEMAN_STEED =
            ENTITY_TYPES.register("horseman_steed", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanSteedEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanSteedEntity::new, MobCategory.CREATURE)
                    .sized(1.3964844f, 1.6f).eyeHeight(1.52f).passengerAttachments(1.72f).clientTrackingRange(10).build("horseman_steed"));
    /** One of War's standards: while it stands, his fury grows. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarStandardEntity>> WAR_STANDARD =
            ENTITY_TYPES.register("war_standard", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarStandardEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarStandardEntity::new, MobCategory.MISC)
                    .sized(0.8f, 3.0f).fireImmune().clientTrackingRange(16).build("war_standard"));
    /** An illusion of War's: hostile, or an innocent wearing a demon's face. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarMirageEntity>> WAR_MIRAGE =
            ENTITY_TYPES.register("war_mirage", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarMirageEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarMirageEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(16).build("war_mirage"));
    /** One of Famine's starving thralls, shuffling to him to be eaten. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.HungryThrallEntity>> HUNGRY_THRALL =
            ENTITY_TYPES.register("hungry_thrall", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.HungryThrallEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.HungryThrallEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(16).build("hungry_thrall"));
    /** A cloud of Pestilence's flies. Fire disperses it. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.FlySwarmEntity>> FLY_SWARM =
            ENTITY_TYPES.register("fly_swarm", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.FlySwarmEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.FlySwarmEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 1.0f).clientTrackingRange(16).build("fly_swarm"));
    /** A reaper: only seen when your time is nearly up. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.ReaperEntity>> REAPER =
            ENTITY_TYPES.register("reaper", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.ReaperEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.ReaperEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.4f).fireImmune().clientTrackingRange(16).build("reaper"));
    /** The way out of a hunter's limbo: a light only they can see. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.LimboExitEntity>> LIMBO_EXIT =
            ENTITY_TYPES.register("limbo_exit", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.LimboExitEntity>of(org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.LimboExitEntity::new, MobCategory.MISC)
                    .sized(1.0f, 2.0f).fireImmune().clientTrackingRange(16).build("limbo_exit"));

    // --- The Archangel Michael (v0.12) -----------------------------------------------------------
    /** Michael, the Sword of Heaven: his vessel (0.6 x 1.9); his true form grows to 1.6 x 4.6 (MichaelEntity). */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity>> MICHAEL =
            ENTITY_TYPES.register("michael", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity>of(org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(24).updateInterval(1).build("michael"));
    /** A soldier of the Host of Heaven (one captain per company). */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity>> HOST_ANGEL =
            ENTITY_TYPES.register("host_angel", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity>of(org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).fireImmune().clientTrackingRange(16).build("host_angel"));
    /** The Lance of Michael, thrown by him. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.MichaelLanceEntity>> MICHAEL_LANCE =
            ENTITY_TYPES.register("michael_lance", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.MichaelLanceEntity>of(org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.MichaelLanceEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(16).updateInterval(1).build("michael_lance"));
    /** The Lance of Michael, thrown by a hunter. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.reward.michael.ThrownLanceEntity>> THROWN_LANCE =
            ENTITY_TYPES.register("thrown_lance", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.reward.michael.ThrownLanceEntity>of(org.papiricoh.supernaturalcraft.reward.michael.ThrownLanceEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(20).build("thrown_lance"));
    /** One of his steel feathers. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.SteelFeatherEntity>> STEEL_FEATHER =
            ENTITY_TYPES.register("steel_feather", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.SteelFeatherEntity>of(org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.SteelFeatherEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(12).updateInterval(1).build("steel_feather"));
    /** A spear of his halo. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.LightSpearEntity>> LIGHT_SPEAR =
            ENTITY_TYPES.register("light_spear", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.LightSpearEntity>of(org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.LightSpearEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(12).updateInterval(1).build("light_spear"));

    // --- Allegiance (v0.13) ---------------------------------------------------------------------------------------------
    /** Heaven's messenger, who offers a hunter Grace. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity>> MESSENGER =
            ENTITY_TYPES.register("messenger", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity>of(org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(12).build("messenger"));
    /** A rival hunter, who hunts angels and demons. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity>> RIVAL_HUNTER =
            ENTITY_TYPES.register("rival_hunter", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity>of(org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).clientTrackingRange(10).build("rival_hunter"));
    /** A soldier of the Host who answers a General of the Host. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.allegiance.HostAllyEntity>> HOST_ALLY =
            ENTITY_TYPES.register("host_ally", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.allegiance.HostAllyEntity>of(org.papiricoh.supernaturalcraft.entity.allegiance.HostAllyEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).fireImmune().clientTrackingRange(16).build("host_ally"));

    // --- Gabriel, the Trickster (v0.14) ---------------------------------------------------------------------------------
    /** Gabriel: a short man in an olive jacket (or the channel's costume). */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielEntity>> GABRIEL =
            ENTITY_TYPES.register("gabriel", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielEntity>of(org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).eyeHeight(1.55f).fireImmune().clientTrackingRange(24).updateInterval(1).build("gabriel"));
    /** One of his doubles: an extra, a nurse or a spokesman. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity>> GABRIEL_DOUBLE =
            ENTITY_TYPES.register("gabriel_double", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity>of(org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).eyeHeight(1.55f).fireImmune().clientTrackingRange(16).build("gabriel_double"));
    /** A cream pie he throws. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.gabriel.PieProjectile>> GABRIEL_PIE =
            ENTITY_TYPES.register("gabriel_pie", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.gabriel.PieProjectile>of(org.papiricoh.supernaturalcraft.entity.boss.gabriel.PieProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.3f).clientTrackingRange(12).updateInterval(1).build("gabriel_pie"));

    // --- Raphael, the archangel of the storm (v0.16) ---------------------------------------------------------------------
    /** Raphael: his season-five vessel, a tall man in a dark suit. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelEntity>> RAPHAEL =
            ENTITY_TYPES.register("raphael", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelEntity>of(org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).fireImmune().clientTrackingRange(24).updateInterval(1).build("raphael"));
    /** An angel of his garrison, holding a thread of grace to him. */
    public static final DeferredHolder<EntityType<?>, EntityType<org.papiricoh.supernaturalcraft.entity.boss.raphael.GarrisonAngelEntity>> GARRISON_ANGEL =
            ENTITY_TYPES.register("garrison_angel", () -> EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.entity.boss.raphael.GarrisonAngelEntity>of(org.papiricoh.supernaturalcraft.entity.boss.raphael.GarrisonAngelEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).fireImmune().clientTrackingRange(16).build("garrison_angel"));

    public static void init() {
    }
}
