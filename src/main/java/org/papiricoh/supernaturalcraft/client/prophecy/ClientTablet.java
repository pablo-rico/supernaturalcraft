package org.papiricoh.supernaturalcraft.client.prophecy;

import net.minecraft.client.Minecraft;
import org.papiricoh.supernaturalcraft.network.TabletPayload;

/** Opens, updates or closes the tablet screen as the server says (v0.36). */
public final class ClientTablet {

    private ClientTablet() {
    }

    public static void handle(TabletPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TabletScreen open && open.book() == p.book()) {
            if (p.status() == 0) open.solved();
            else open.update(p);
            return;
        }
        if (p.status() != 0) mc.setScreen(new TabletScreen(p));
    }
}
