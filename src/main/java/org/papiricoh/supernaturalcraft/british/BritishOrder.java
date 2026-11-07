package org.papiricoh.supernaturalcraft.british;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;

import java.util.function.UnaryOperator;

/** v0.41: the server's door to a hunter's {@link British} standing (their answer to Hess, Ketch's hunt, the flags). */
public final class BritishOrder {

    private BritishOrder() {
    }

    public static British get(Player player) {
        return player.getData(BritishRegistry.BRITISH);
    }

    public static void set(Player player, British b) {
        player.setData(BritishRegistry.BRITISH, b);
    }

    public static British update(Player player, UnaryOperator<British> f) {
        British b = f.apply(get(player));
        set(player, b);
        return b;
    }

    public static BritishRules.Choice choice(Player player) {
        return get(player).stance();
    }

    public static long today(ServerPlayer p) {
        return Prophecies.today(p.server);
    }

    /** Whether Ketch is beaten for {@code p} (by the flag his fall sets, or by his advancement). */
    public static boolean ketchBeaten(ServerPlayer p) {
        return get(p).has(British.KETCH_BEATEN) || AuthorWorld.done(p, BossProgression.Boss.KETCH.advancement);
    }

    /** Whether Hess is beaten for {@code p}. */
    public static boolean hessBeaten(ServerPlayer p) {
        return get(p).has(British.HESS_BEATEN) || AuthorWorld.done(p, BossProgression.Boss.HESS.advancement);
    }

    /** Whether Dagon is beaten for {@code p}. */
    public static boolean dagonBeaten(ServerPlayer p) {
        return get(p).has(British.DAGON_BEATEN) || AuthorWorld.done(p, BossProgression.Boss.DAGON.advancement);
    }

    /** Whether Ketch is after {@code p} now. */
    public static boolean hunted(ServerPlayer p) {
        return BritishRules.hunted(choice(p), ketchBeaten(p));
    }
}
