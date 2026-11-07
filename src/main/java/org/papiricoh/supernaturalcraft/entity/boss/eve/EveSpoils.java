package org.papiricoh.supernaturalcraft.entity.boss.eve;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.purgatory.PurgatorySavedData;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladePlan;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladeWorld;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRift;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRifts;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * What Eve leaves each hunter who fought her (v0.39), on top of the shards, hearts and credit every boss gives: the first time,
 * the Mother's Heartwood (her own heart, gone to wood: it hushes her children, {@link MothersHeartwoodItem}) and a handful of soul
 * amber; a rematch, soul amber only. Her fall also lets them go: Purgatory no longer holds them ({@link PurgatoryWorld#escaped}),
 * and a rift home opens in her glade ({@link #RIFT_TICKS}).
 */
public final class EveSpoils {

    public static final int FIRST_AMBER = 4, REMATCH_AMBER = 3;
    public static final int RIFT_TICKS = PurgatoryRifts.GLADE_TICKS;

    private EveSpoils() {
    }

    public static void give(ServerLevel level, EveEntity eve, List<LivingEntity> hunters, @Nullable ArenaController arena) {
        for (LivingEntity e : hunters) {
            if (!(e instanceof ServerPlayer p)) continue;
            for (ItemStack s : share(p)) spawn(level, p.position(), s);
            PurgatoryWorld.escaped(p);
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.eve.way_open").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
        PurgatorySavedData.get(level.getServer()).fought();
        openTheWayHome(level, eve, arena);
    }

    public static List<ItemStack> share(ServerPlayer hunter) {
        return PurgatoryWorld.has(hunter, BossProgression.Boss.EVE.advancement) ? rematch() : first();
    }

    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.MOTHERS_HEARTWOOD.get()));
        out.add(new ItemStack(AllItems.SOUL_AMBER.get(), FIRST_AMBER));
        return out;
    }

    public static List<ItemStack> rematch() {
        return List.of(new ItemStack(AllItems.SOUL_AMBER.get(), REMATCH_AMBER));
    }

    /** The rift home: at the glade's rift point when she fell in her glade, else a little south of where the fight was. */
    public static PurgatoryRift openTheWayHome(ServerLevel level, EveEntity eve, @Nullable ArenaController arena) {
        BlockPos at;
        if (eve.inGlade()) {
            at = EveGladeWorld.point(level, EveGladePlan.RIFT);
        } else {
            BlockPos c = arena != null ? arena.center() : eve.blockPosition();
            at = PurgatoryRifts.groundNear(level, c.south(6), c.getY());
        }
        return PurgatoryRifts.open(level, at, Direction.Axis.X, PurgatoryRift.Kind.HOME, RIFT_TICKS);
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
        item.setNoPickUpDelay();
        level.addFreshEntity(item);
    }
}
