package org.papiricoh.supernaturalcraft.journal;

import net.minecraft.resources.ResourceLocation;

/**
 * What a hunter has done, as the journal and the roadmap ask it: on the client it is read from
 * the last {@code HunterLogSyncPayload}; in tests it is a stub.
 */
public interface Progress {

    /** Whether the advancement with this id is done. */
    boolean done(ResourceLocation advancement);

    /** Whether this creature has been seen (or slain). */
    boolean seen(ResourceLocation entity);

    /** Whether this item has been held. */
    boolean has(ResourceLocation item);

    /** Whether this bowl spell has been learned. */
    default boolean knowsRite(ResourceLocation spell) {
        return false;
    }

    Progress NONE = new Progress() {
        @Override
        public boolean done(ResourceLocation advancement) {
            return false;
        }

        @Override
        public boolean seen(ResourceLocation entity) {
            return false;
        }

        @Override
        public boolean has(ResourceLocation item) {
            return false;
        }
    };
}
