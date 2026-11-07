package org.papiricoh.supernaturalcraft.entity.boss.asmodeus;

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
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Calling Asmodeus (v0.41): his rite ({@code summon_asmodeus}), in the Overworld, once the ritualist's Jack is born (he
 * comes for the nephilim, and for the hunter who keeps him). The same rite is the rematch. By command, anywhere.
 */
public final class AsmodeusSummoning {

    private AsmodeusSummoning() {
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the world (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (player != null) player.displayClientMessage(busy, true);
            return null;
        }
        ArenaController arena = LuciferSummoning.openArena(level, at, AsmodeusBalance.ARENA_RADIUS, ArenaTheme.PRINCES);
        if (arena == null) return null;
        AsmodeusEntity boss = NephilimRegistry.ASMODEUS.get().create(level);
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

    /** Why the rite fails here and now, or null. */
    public static @Nullable Component riteRefusal(ServerLevel level, @Nullable ServerPlayer ritualist) {
        if (level.dimension() != Level.OVERWORLD) return Component.translatable("message.supernaturalcraft.asmodeus.rite.elsewhere");
        if (ritualist != null && !Nephilims.step(ritualist).atLeast(NephilimRules.Step.BORN)) {
            return Component.translatable("message.supernaturalcraft.asmodeus.rite.unborn");
        }
        return LuciferSummoning.refusal(level);
    }

    /** The rite {@code summon_asmodeus}. */
    public record Rite() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_asmodeus");
        public static final MapCodec<Rite> CODEC = MapCodec.unit(new Rite());

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            return riteRefusal(level, ritualist);
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            Component why = riteRefusal(level, ritualist);
            if (why != null) {
                if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
                return false;
            }
            return summon(level, altar.north(4), ritualist) != null;
        }
    }
}
