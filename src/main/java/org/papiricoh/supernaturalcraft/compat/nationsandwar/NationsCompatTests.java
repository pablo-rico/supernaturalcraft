package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Nations &amp; War bridge in a real server (v0.32): generated only when Nations &amp; War is loaded
 * ({@code ./gradlew runGameTestServer -Pnw}), so the suite without it is untouched. The bodies, which touch its API, live in
 * {@link NationsCompatTestBodies}; on the GameTest server the bridge stays off while the other tests run ({@link NWPresence#suspend}).
 */
@GameTestHolder(SupernaturalCraft.MODID)
public final class NationsCompatTests {
    private static final String FIELD = SupernaturalCraft.MODID + ":gametest/empty_11x6x11";

    private NationsCompatTests() {
    }

    @GameTestGenerator
    public static List<TestFunction> generate() {
        List<TestFunction> out = new ArrayList<>();
        if (!NWPresence.LOADED) return out;
        add(out, "nw_consecrate", NationsCompatTestBodies::consecrationOathAndStances);
        add(out, "nw_powers", NationsCompatTestBodies::powersRaiseTheirSeatAndEnclaves);
        add(out, "nw_incursion", NationsCompatTestBodies::anIncursionLostMakesAVassal);
        add(out, "nw_powers_war", NationsCompatTestBodies::thePowersWarIsANationsWar);
        add(out, "nw_arena", NationsCompatTestBodies::anArenaIsFreeOfTheTownAroundIt);
        add(out, "nw_liberation", NationsCompatTestBodies::aPowerFreesItsOccupiedLand);
        add(out, "nw_migrate_front", NationsCompatTestBodies::anOldFrontBecomesEnclaves);
        add(out, "nw_migrate_guild", NationsCompatTestBodies::anOldGuildIsConsecratedByItsLord);
        add(out, "nw_migrate_site", NationsCompatTestBodies::enclavesKeepOffPlayersLand);
        return out;
    }

    private static void add(List<TestFunction> out, String name, Consumer<GameTestHelper> body) {
        out.add(new TestFunction(name, "nationscompat." + name, FIELD, 200, 0L, true, h -> {
            boolean was = NWPresence.suspended();
            NWPresence.suspend(false);
            try {
                body.accept(h);
            } finally {
                NWPresence.suspend(was);
            }
        }));
    }
}
