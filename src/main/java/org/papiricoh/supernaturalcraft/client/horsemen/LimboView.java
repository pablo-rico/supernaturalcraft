package org.papiricoh.supernaturalcraft.client.horsemen;

import net.minecraft.client.Minecraft;
import org.papiricoh.supernaturalcraft.client.horsemen.fx.ClientHorsemen;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathClock;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.LimboExitEntity;

/** Who sees what in Death's fight: the reapers (a clock nearly out, limbo, the world of the dead) and one's own light. */
public final class LimboView {

    private LimboView() {
    }

    public static boolean sees(LimboExitEntity exit) {
        var player = Minecraft.getInstance().player;
        return player != null && exit.owner().map(player.getUUID()::equals).orElse(false);
    }

    public static boolean reapersVisible() {
        if (ClientHorsemen.inLimbo() || ClientHorsemen.deadWorld()) return true;
        return ClientHorsemen.clockShown() && ClientHorsemen.clockLeft() < DeathClock.REAPERS_SEEN_BELOW;
    }
}
