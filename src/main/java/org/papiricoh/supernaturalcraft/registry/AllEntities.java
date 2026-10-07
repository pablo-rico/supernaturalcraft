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

    public static void init() {
    }
}
