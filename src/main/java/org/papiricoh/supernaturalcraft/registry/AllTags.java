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
        /** Lesser things the Colt kills outright: demons and the bosses' summons. */
        public static final TagKey<EntityType<?>> COLT_EXECUTES = tag("colt_executes");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, SupernaturalCraft.asResource(name));
        }
    }

    public static class Items {
        /** Weapons that bypass Lucifer's resistance to mundane harm. */
        public static final TagKey<Item> HOLY_WEAPONS = tag("holy_weapons");
        /** Weapons with a bonus against {@link Entities#DEMONS}. */
        public static final TagKey<Item> DEMON_BANE = tag("demon_bane");
        public static final TagKey<Item> SALT = tag("salt");
        /** Carried in the Darkness's arena, these shed light around their bearer. */
        public static final TagKey<Item> HELD_LIGHT_SOURCES = tag("held_light_sources");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, SupernaturalCraft.asResource(name));
        }
    }

    public static class Structures {
        /** Spires the Hymnal Map can lead to. */
        public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> HYMNAL_SPIRES =
                TagKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("hymnal_spires"));
    }

    public static class Blocks {
        /** Never rewritten by the boss arena: altars, containers, bedrock. */
        public static final TagKey<Block> ARENA_IMMUNE = tag("arena_immune");
        public static final TagKey<Block> CHALK_LINES = tag("chalk_lines");
        /** Light sources the Darkness can put out (removed until her arena is restored). */
        public static final TagKey<Block> SNUFFABLE = tag("snuffable");

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
