package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.chain.OfficerSpawner;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.nationsandwar.api.NationsAndWarApi;
import org.papiricoh.nationsandwar.api.ProtectionBypass;

import java.util.HashSet;
import java.util.Set;

/**
 * No boss fight stops at a town's border: every arena (from its opening to the end of its restoration) is a Nations &amp; War
 * protection-free zone, so minions spawn, outsiders fight, and fire and blasts do what the fight needs; the arena restores
 * its own ground, so nothing of it goes in a war's restore log. Bosses, officers and war parties are exempt everywhere.
 * The ritual that opens a fight still follows the land's perms (only where one may build).
 */
final class ArenaBypass {
    static final String PREFIX = "supernaturalcraft:arena/";
    /** Below and above the arena's centre the zone reaches (themes dig and raise further than the dome). */
    private static final int BELOW = 48, ABOVE = 80;
    private static final Set<String> OPEN = new HashSet<>();

    private ArenaBypass() {
    }

    /** Opens a zone for every arena standing (also after a restart) and closes those of arenas that are gone. */
    static void tick(MinecraftServer server) {
        Set<String> seen = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            ArenaSavedData data = ArenaSavedData.get(level);
            if (data.all().isEmpty()) continue;
            String dim = NationsAndWarApi.dimension(level);
            for (ArenaController a : data.all()) {
                String key = PREFIX + a.id();
                seen.add(key);
                if (OPEN.add(key)) ProtectionBypass.openZone(key, dim, box(a), ProtectionBypass.ALL);
            }
        }
        for (String key : Set.copyOf(OPEN)) {
            if (seen.contains(key)) continue;
            ProtectionBypass.close(key);
            OPEN.remove(key);
        }
    }

    static AABB box(ArenaController a) {
        BlockPos c = a.center();
        int r = a.radius() + 2;
        return new AABB(c.getX() - r, c.getY() - BELOW, c.getZ() - r, c.getX() + r + 1, c.getY() + ABOVE, c.getZ() + r + 1);
    }

    static boolean exempt(Entity e) {
        return e.getType().is(AllTags.Entities.BOSSES) || OfficerSpawner.key(e) != null || e.getPersistentData().hasUUID(NWWars.FOR);
    }

    static void stopped() {
        ProtectionBypass.closeAll(PREFIX);
        OPEN.clear();
    }
}
