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

    public static void init() {
    }
}
