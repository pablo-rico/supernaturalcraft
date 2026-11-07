package org.papiricoh.supernaturalcraft.entity.boss.michaelvessel;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Calling Michael's vessel (v0.41): out in the open where a hunter told him no ({@link #summon}, the arena round them), or by
 * the rematch rite {@code summon_michael_vessel} once his vessel has fallen to the ritualist. The mindscape (the yes) is
 * {@code nephilim.michael.Mindscape}. By command, anywhere.
 */
public final class MichaelVesselSummoning {

    private MichaelVesselSummoning() {
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the world (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (player != null) player.displayClientMessage(busy, true);
            return null;
        }
        ArenaController arena = LuciferSummoning.openArena(level, at, MichaelVesselBalance.ARENA_RADIUS, ArenaTheme.VESSEL);
        if (arena == null) return null;
        MichaelVesselEntity boss = NephilimRegistry.MICHAEL_VESSEL.get().create(level);
        if (boss == null) return null;
        Vec3 c = Vec3.atBottomCenterOf(at);
        float yaw = player == null ? 0 : (float) Math.toDegrees(Math.atan2(-(player.getX() - c.x), player.getZ() - c.z));
        boss.moveTo(c.x, c.y, c.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.michael_vessel.no").withStyle(ChatFormatting.AQUA), false);
        }
        return boss;
    }

    /** Why the rematch rite fails here and now, or null. */
    public static @Nullable Component riteRefusal(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return Component.translatable("message.supernaturalcraft.michael_vessel.rite.elsewhere");
        return LuciferSummoning.refusal(level);
    }

    /** The rite {@code summon_michael_vessel} (the rematch). */
    public record Rite() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_michael_vessel");
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
