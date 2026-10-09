package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Tags are the extension points: other mods join the fight by tagging their content. */
public class AllTags {

    public static class Entities {
        /** Black-eyed demons and their kin: blocked by salt, held by devil's traps, burned by holy water. */
        public static final TagKey<EntityType<?>> DEMONS = tag("demons");
        /** Anything the Hunter's Amulet warms up near. */
        public static final TagKey<EntityType<?>> SUPERNATURAL = tag("supernatural");
        /** Lucifer and his summons: unhurt by the arena's own hellfire and frost. */
        public static final TagKey<EntityType<?>> CAGE_DWELLERS = tag("cage_dwellers");
        /** Amara and what she makes: her own void never harms them. */
        public static final TagKey<EntityType<?>> DARKNESS = tag("darkness");
        /** Bosses: the Colt's rounds hit them for exact damage (see BossDamage), never execute them. */
        public static final TagKey<EntityType<?>> BOSSES = tag("bosses");
        /**
         * Lesser things the Colt kills outright: demons and the bosses' summons. Since v0.15 every living thing but a player,
         * a boss and {@link #COLT_IMMUNE} dies to one round, so this only documents (and guarantees) the old list.
         */
        public static final TagKey<EntityType<?>> COLT_EXECUTES = tag("colt_executes");
        /**
         * Archangels and above that are not this mod's great enemies (other mods' bosses through {@code #c:bosses}, the caged
         * Lucifer, Heaven's messenger): never executed by the Colt; a round takes its hard cap of them, at least
         * {@code colt.otherDamage}.
         */
        public static final TagKey<EntityType<?>> COLT_IMMUNE = tag("colt_immune");

        /** Spirits of the dead: ghosts. Salt holds them, iron scatters them, Second Sight shows them. */
        public static final TagKey<EntityType<?>> SPIRITS = tag("spirits");
        /** Angels (not bosses): Concealment hides from them as from demons. */
        public static final TagKey<EntityType<?>> ANGELS = tag("angels");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, SupernaturalCraft.asResource(name));
        }
    }

    public static class Items {
        /** Weapons that bypass Lucifer's resistance to mundane harm. */
        public static final TagKey<Item> HOLY_WEAPONS = tag("holy_weapons");
        /** Weapons with a bonus against {@link Entities#DEMONS}. */
        public static final TagKey<Item> DEMON_BANE = tag("demon_bane");
        public static final TagKey<Item> SALT = tag("salt");        /** Cold iron: scatters a ghost for a while (it cannot be killed, only laid to rest). */
        public static final TagKey<Item> GHOST_BANE = tag("ghost_bane");
        /** Items a spell bowl refuses as ingredients (they do something else to it). */
        public static final TagKey<Item> BOWL_REJECTS = tag("bowl_rejects");

        /** Carried in the Darkness's arena, these shed light around their bearer. */
        public static final TagKey<Item> HELD_LIGHT_SOURCES = tag("held_light_sources");

        /** Blades that take a head (v0.17): only a killing blow with one of these keeps a vampire down. */
        public static final TagKey<Item> BEHEADING = tag("beheading");
        /** Silver (v0.17): what kills a werewolf for good and shows a shapeshifter for what it is. */
        public static final TagKey<Item> SILVER = tag("silver");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, SupernaturalCraft.asResource(name));
        }
    }

    public static class Structures {
        /** Spires the Hymnal Map can lead to. */
        public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> HYMNAL_SPIRES =
                TagKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("hymnal_spires"));
        /** Graves with a restless ghost: the Locating spell can find them. */
        public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> GRAVES =
                TagKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("graves"));
        /** Lucifer's Cage in Hell (one, at the origin). */
        public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> LUCIFERS_CAGE =
                TagKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("lucifers_cage"));
    }

    public static class Blocks {
        /** Never rewritten by the boss arena: altars, containers, bedrock. */
        public static final TagKey<Block> ARENA_IMMUNE = tag("arena_immune");
        public static final TagKey<Block> CHALK_LINES = tag("chalk_lines");
        /** Light sources the Darkness can put out (removed until her arena is restored). */
        public static final TagKey<Block> SNUFFABLE = tag("snuffable");
        /** Holy ground for Grace (v0.13): within a few blocks of one of these counts as consecrated. */
        public static final TagKey<Block> CONSECRATED = tag("consecrated");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, SupernaturalCraft.asResource(name));
        }
    }

    public static class DamageTypes {
        /** Counts as holy against Lucifer's damage policy. */
        public static final TagKey<DamageType> HOLY = tag("holy");
        public static final TagKey<DamageType> LUCIFER_IMMUNE = tag("lucifer_immune");
        /** Skips bosses' multipliers and per-hit caps (never their phase floors): see BossDamage. */
        public static final TagKey<DamageType> EXACT_BOSS_DAMAGE = tag("exact_boss_damage");

        private static TagKey<DamageType> tag(String name) {
            return TagKey.create(Registries.DAMAGE_TYPE, SupernaturalCraft.asResource(name));
        }
    }
}
