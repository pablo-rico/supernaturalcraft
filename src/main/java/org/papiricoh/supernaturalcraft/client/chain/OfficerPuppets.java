package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.chain.ChainRoster;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;

import java.util.HashMap;
import java.util.Map;

/**
 * Stand-ins for the officers the book draws: one client-side mob per record (its own type, vessel or variant and rank),
 * never added to the level. Their tick count runs with the client so GeckoLib plays their idle.
 */
public final class OfficerPuppets {

    private record Key(String faction, int id, String type, int variant, String rank) {
    }

    private static final Map<Key, LivingEntity> PUPPETS = new HashMap<>();
    private static ClientLevel level;

    private OfficerPuppets() {
    }

    /** The officer's stand-in, or null if its type is not a living mob here. */
    @Nullable
    public static LivingEntity of(Entry e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        if (mc.level != level) clear();
        level = mc.level;
        Key key = new Key(e.faction(), e.id(), type(e), e.variant(), e.rank());
        return PUPPETS.computeIfAbsent(key, k -> create(k, mc.level));
    }

    /** Drawn as the spawner shows it ({@link ChainRoster#materialize}: a Lord in its side's plain vessel, the fallen as fallen). */
    private static String type(Entry e) {
        return ChainRoster.materialize(org.papiricoh.supernaturalcraft.chain.ChainFaction.byId(e.faction()),
                org.papiricoh.supernaturalcraft.chain.ChainFaction.Branch.valueOf(e.branch()), ChainRank.valueOf(e.rank()),
                e.entityType() == null ? "" : e.entityType());
    }

    @Nullable
    private static LivingEntity create(Key k, ClientLevel level) {
        ResourceLocation id = ResourceLocation.tryParse(k.type());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
        Entity made = BuiltInRegistries.ENTITY_TYPE.get(id).create(level);
        if (!(made instanceof LivingEntity living)) return null;
        int rank = ChainRank.valueOf(k.rank()).ordinal();
        if (living instanceof BlackEyedDemon d) {
            d.setVariant(k.variant());
            d.setOfficerRank(rank);
        }
        if (living instanceof AngelEntity a) {
            a.setVessel(k.variant());
            a.setOfficerRank(rank);
        }
        return living;
    }

    public static void tick() {
        // Records that left the chain are forgotten once the book has not asked for them in a while.
        if (PUPPETS.size() > 96) clear();
        for (LivingEntity p : PUPPETS.values()) if (p != null) p.tickCount++;
    }

    public static void clear() {
        PUPPETS.clear();
        level = null;
    }
}
