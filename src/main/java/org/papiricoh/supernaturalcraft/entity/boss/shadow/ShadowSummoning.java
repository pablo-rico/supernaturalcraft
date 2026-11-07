package org.papiricoh.supernaturalcraft.entity.boss.shadow;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.empty.EmptyWorld;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Waking the Shadow (v0.41): in the Empty, at the floor's centre, when a hunter whose Jack has fallen has been there a few
 * seconds ({@code EmptyWorld}); the rematch rite {@code summon_shadow} (in the Empty, once it has fallen to the ritualist).
 * By command, anywhere.
 */
public final class ShadowSummoning {

    private ShadowSummoning() {
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the world (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        if (LuciferSummoning.refusal(level) != null) return null;
        ArenaController arena = LuciferSummoning.openArena(level, at, ShadowBalance.ARENA_RADIUS, ArenaTheme.THE_EMPTY);
        if (arena == null) return null;
        ShadowEntity boss = NephilimRegistry.SHADOW.get().create(level);
        if (boss == null) return null;
        Vec3 c = Vec3.atBottomCenterOf(at);
        float yaw = player == null ? 0 : (float) Math.toDegrees(Math.atan2(-(player.getX() - c.x), player.getZ() - c.z));
        boss.moveTo(c.x, c.y, c.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        if (player != null) player.displayClientMessage(Component.translatable("message.supernaturalcraft.shadow.wakes").withStyle(ChatFormatting.DARK_PURPLE), false);
        return boss;
    }

    public static @Nullable Component riteRefusal(@Nullable ServerPlayer ritualist, ServerLevel level) {
        if (ritualist == null || !EmptyWorld.in(ritualist)) return Component.translatable("message.supernaturalcraft.shadow.rite.elsewhere");
        return LuciferSummoning.refusal(level);
    }

    /** The rite {@code summon_shadow} (the rematch, in the Empty). */
    public record Rite() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_shadow");
        public static final MapCodec<Rite> CODEC = MapCodec.unit(new Rite());

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            return riteRefusal(ritualist, level);
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            if (riteRefusal(ritualist, level) != null) return false;
            return summon(level, EmptyWorld.centre(level).above(), ritualist) != null;
        }
    }
}
