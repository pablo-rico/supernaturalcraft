package org.papiricoh.supernaturalcraft.entity.boss.soulless;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Calling soulless Jack (v0.41): he comes down at night where his hunter stands, the night after he left
 * ({@link #ambush}, driven by {@code NephilimEvents}); the rematch rite {@code summon_soulless_jack} once he has fallen to the
 * ritualist. By command, anywhere.
 */
public final class SoullessJackSummoning {

    private SoullessJackSummoning() {
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the world (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        if (LuciferSummoning.refusal(level) != null) return null;
        ArenaController arena = LuciferSummoning.openArena(level, at, SoullessJackBalance.ARENA_RADIUS, ArenaTheme.NEPHILIM);
        if (arena == null) return null;
        SoullessJackEntity boss = NephilimRegistry.SOULLESS_JACK.get().create(level);
        if (boss == null) return null;
        Vec3 c = Vec3.atBottomCenterOf(at);
        float yaw = player == null ? 0 : (float) Math.toDegrees(Math.atan2(-(player.getX() - c.x), player.getZ() - c.z));
        boss.moveTo(c.x, c.y, c.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }

    /** He comes for {@code hunter}, a few steps in front of them. @return whether he came */
    public static boolean ambush(ServerPlayer hunter) {
        ServerLevel level = hunter.serverLevel();
        Vec3 ahead = hunter.position().add(Vec3.directionFromRotation(0, hunter.getYRot()).scale(SoullessJackBalance.AMBUSH_DISTANCE));
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(ahead));
        if (Math.abs(at.getY() - hunter.getY()) > 6) at = hunter.blockPosition();
        if (summon(level, at, hunter) == null) return false;
        hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.soulless_jack.comes").withStyle(ChatFormatting.YELLOW), false);
        return true;
    }

    public static @Nullable Component riteRefusal(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return Component.translatable("message.supernaturalcraft.soulless_jack.rite.elsewhere");
        return LuciferSummoning.refusal(level);
    }

    /** The rite {@code summon_soulless_jack} (the rematch). */
    public record Rite() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_soulless_jack");
        public static final MapCodec<Rite> CODEC = MapCodec.unit(new Rite());

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            return riteRefusal(level);
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            if (riteRefusal(level) != null) return false;
            return summon(level, altar.north(4), ritualist) != null;
        }
    }
}
