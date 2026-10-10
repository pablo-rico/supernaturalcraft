package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Naomi's recalibration console (v0.18): while she stands at it healing, every blow on it counts (a swing, or a projectile that
 * strikes it); {@link NaomiBalance#CONSOLE_HITS} of them break her off it and she reels. Unbreakable; at any other time it is only
 * a console. The blows are also heard through {@code PlayerInteractEvent.LeftClickBlock} ({@code NaomiEvents}), so that a world
 * that refuses breaking in Heaven still lets hunters strike it; a short per-hunter cooldown keeps one swing one blow.
 */
public class ReprogrammingConsoleBlock extends Block {

    public ReprogrammingConsoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) struck(server, pos, player);
        super.attack(state, level, pos, player);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel server && projectile.getOwner() instanceof Player p) struck(server, hit.getBlockPos(), p);
    }

    /** A blow on the console at {@code pos}. @return whether Naomi felt it */
    public static boolean struck(ServerLevel level, BlockPos pos, Player by) {
        NaomiEntity n = NaomiEntity.atConsole(level, pos);
        return n != null && n.consoleHit(pos, by);
    }
}
