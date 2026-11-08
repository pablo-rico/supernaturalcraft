package org.papiricoh.supernaturalcraft.datagen.darkness;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.datagen.slice.SliceData;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * v0.42 datagen hooks for season 11's core: the Darkness let out, the fog and Superior, Amara's visits, the Cage and Casifer,
 * Chuck, the dying sun and Amara's sounds. The English text is in {@code DarknessLang}; the Spanish in {@code es_es.json}.
 */
public final class DarknessData implements SliceData {

    @Override
    public void lang(BiConsumer<String, String> add) {
        DarknessLang.add(add);
    }

    /** A fogged villager, now and then, a little of what took it. */
    @Override
    public void entityLoot(BiConsumer<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.level.storage.loot.LootTable.Builder> add) {
        add.accept(DarknessRegistry.FOGGED_VILLAGER.get(), net.minecraft.world.level.storage.loot.LootTable.lootTable()
                .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                        .when(net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition.randomChance(0.08f))
                        .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(org.papiricoh.supernaturalcraft.registry.AllItems.VOID_ESSENCE.get()))));
    }

    /** English names by registry id where the title-cased id is wrong (the Spanish ones are in es_es.json). */
    @Override
    public Map<String, String> names() {
        return Map.of("chuck_ally", "Chuck", "amara_visitor", "Amara", "amaras_light", "Amara's Light", "eclipse_sight", "The Sight");
    }

    /** Vanilla stand-ins (event, pitch) until {@code tools/soundgen} has written the OGGs ({@code sounds/<event with / for .>.ogg}). */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("amara.whisper", new Object[]{"ambient.soul_sand_valley.mood", 1.4f}),
            Map.entry("amara.giggle", new Object[]{"entity.allay.ambient_without_item", 0.7f}),
            Map.entry("amara.child_scream", new Object[]{"entity.ghast.scream", 1.6f}),
            Map.entry("amara.hum", new Object[]{"block.beacon.ambient", 0.5f}),
            Map.entry("amara.tk", new Object[]{"entity.illusioner.cast_spell", 0.6f}),
            Map.entry("amara.drain", new Object[]{"entity.warden.heartbeat", 0.7f}),
            Map.entry("amara.strike", new Object[]{"entity.warden.attack_impact", 1.2f}),
            Map.entry("amara.wound", new Object[]{"block.beacon.deactivate", 0.5f}),
            Map.entry("amara.wall", new Object[]{"entity.warden.sonic_charge", 0.6f}),
            Map.entry("amara.crumble", new Object[]{"block.sculk_shrieker.shriek", 0.4f}),
            Map.entry("amara.island", new Object[]{"block.deepslate.break", 0.5f}),
            Map.entry("amara.pull", new Object[]{"entity.warden.sonic_boom", 0.5f}),
            Map.entry("amara.starfall", new Object[]{"entity.firework_rocket.large_blast_far", 0.6f}),
            Map.entry("amara.countdown", new Object[]{"block.bell.resonate", 0.5f}),
            Map.entry("amara.heartbeat", new Object[]{"entity.warden.heartbeat", 0.6f}),
            Map.entry("amara.enrage", new Object[]{"entity.warden.roar", 0.6f}),
            Map.entry("amara.sun_die", new Object[]{"block.beacon.deactivate", 0.3f}),
            Map.entry("amara.reconcile", new Object[]{"block.beacon.activate", 0.8f}),
            Map.entry("amara.dawn", new Object[]{"ui.toast.challenge_complete", 0.8f}),
            Map.entry("amara.visit", new Object[]{"ambient.basalt_deltas.mood", 1.2f}),
            Map.entry("amara.feed", new Object[]{"entity.warden.sniff", 0.6f}),
            Map.entry("music.amara.phase1", new Object[]{"music.end", 1f}),
            Map.entry("music.amara.phase2", new Object[]{"music.end", 1.1f}),
            Map.entry("music.amara.phase3", new Object[]{"music.dragon", 0.9f}),
            Map.entry("music.amara.phase4", new Object[]{"music.dragon", 1f}),
            Map.entry("music.amara.phase5", new Object[]{"music.end", 0.8f}),
            Map.entry("music.amara.phase6", new Object[]{"music.dragon", 0.8f}),
            Map.entry("casifer.snap", new Object[]{"entity.evoker.cast_spell", 1.3f}),
            Map.entry("casifer.laugh", new Object[]{"entity.witch.celebrate", 0.7f}),
            Map.entry("darkness.fog", new Object[]{"ambient.soul_sand_valley.loop", 0.6f}),
            Map.entry("darkness.release", new Object[]{"entity.wither.spawn", 0.5f}),
            Map.entry("darkness.fog_infect", new Object[]{"entity.zombie_villager.converted", 0.6f}),
            Map.entry("darkness.cure", new Object[]{"entity.zombie_villager.cure", 1.2f}),
            Map.entry("darkness.visit", new Object[]{"ambient.cave", 0.8f}));

    @Override
    public void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (var group : DarknessRegistry.soundGroups()) {
            group.forEach((event, holder) -> {
                ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
                boolean music = event.startsWith("music.");
                SoundDefinition def = SoundDefinition.definition();
                if (!music) def.subtitle("subtitles.supernaturalcraft." + event);
                if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                    SoundDefinition.Sound s = SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(32);
                    if (music) s.stream();
                    def.with(s);
                } else {
                    Object[] f = FALLBACK.getOrDefault(event, new Object[]{"ambient.cave", 1f});
                    def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                            .pitch((Float) f[1]));
                }
                add.accept(holder, def);
            });
        }
    }

    @Override
    public Map<String, String> subtitles() {
        return Map.ofEntries(
                Map.entry("amara.whisper", "Amara whispers"), Map.entry("amara.giggle", "A child giggles"),
                Map.entry("amara.child_scream", "A child screams"), Map.entry("amara.hum", "The Darkness hums"),
                Map.entry("amara.tk", "Debris lifts"), Map.entry("amara.drain", "A soul is drawn out"),
                Map.entry("amara.strike", "Amara strikes"), Map.entry("amara.wound", "God's light tears"),
                Map.entry("amara.wall", "A wall of smoke rises"), Map.entry("amara.crumble", "The ground unmakes"),
                Map.entry("amara.island", "An island sinks"), Map.entry("amara.pull", "The black sun pulls"),
                Map.entry("amara.starfall", "Stars fall"), Map.entry("amara.countdown", "The sun dims"),
                Map.entry("amara.heartbeat", "A dying heartbeat"), Map.entry("amara.enrage", "Amara rages"),
                Map.entry("amara.sun_die", "The sun gutters"), Map.entry("amara.reconcile", "Light meets Darkness"),
                Map.entry("amara.dawn", "Dawn breaks"), Map.entry("amara.visit", "Amara is near"),
                Map.entry("amara.feed", "Amara feeds"),
                Map.entry("casifer.snap", "Casifer snaps"), Map.entry("casifer.laugh", "Casifer laughs"),
                Map.entry("darkness.fog", "Black fog rolls"), Map.entry("darkness.release", "The Darkness is loose"),
                Map.entry("darkness.fog_infect", "A villager is taken by the fog"), Map.entry("darkness.cure", "The fog lets go"),
                Map.entry("darkness.visit", "Something stirs"));
    }

    @Override
    public void itemModels(org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider p, ExistingFileHelper existing) {
        p.basicItem(DarknessRegistry.AMARAS_LIGHT.get());
    }
}
