package org.papiricoh.supernaturalcraft.entity.boss.alastair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseSite;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseWorld;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Calling Alastair (v0.40). At the warehouse the fight begins in place ({@link #begin}): a hunter who has not beaten him yet
 * walks up to the angels' trap and finds him chained in it, his guards round him. Beaten once, he comes back only by the rite
 * ({@code summon_alastair}), at the warehouse and unchained. By command, anywhere ({@link #summon}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AlastairSummoning {

    /** The arena round a free summoning; how close to the trap a hunter starts the fight; the rite's reach from the trap. */
    public static final int FREE_RADIUS = 20;
    public static final double TRIGGER = 9, RITE_REACH = 8;

    private AlastairSummoning() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        WarehouseWorld.tick(overworld);
        if (overworld.getGameTime() % 20 == 9) tick(overworld);
    }

    /** Whether an Alastair fight is on (only one in the world). */
    public static boolean fighting(ServerLevel level) {
        for (ArenaController a : ArenaSavedData.get(level).all()) if (a.isActive() && a.theme() == ArenaTheme.WAREHOUSE) return true;
        return false;
    }

    /** Once a second: a hunter who has not beaten him, at the trap, starts the fight. */
    static void tick(ServerLevel overworld) {
        if (!WarehouseWorld.built(overworld.getServer()) || fighting(overworld)) return;
        WarehouseSite site = WarehouseWorld.site(overworld.getServer());
        if (site == null) return;
        Vec3 trap = Vec3.atBottomCenterOf(site.trap());
        for (ServerPlayer p : overworld.players()) {
            if (p.isSpectator() || p.isCreative() || p.position().distanceToSqr(trap) > TRIGGER * TRIGGER) continue;
            if (AuthorWorld.done(p, BossProgression.Boss.ALASTAIR.advancement)) continue;
            begin(overworld, p, site, true);
            return;
        }
    }

    /** The fight at the warehouse: {@code chained} the first one (the trap and the angels), else a rematch. */
    public static @Nullable AlastairEntity begin(ServerLevel level, @Nullable ServerPlayer hunter, WarehouseSite site, boolean chained) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (hunter != null) hunter.displayClientMessage(busy, true);
            return null;
        }
        BlockPos centre = site.point(WarehouseAnchors.ARENA_CENTRE);
        ArenaController arena = LuciferSummoning.openArena(level, centre != null ? centre : site.trap(), WarehouseAnchors.ARENA_RADIUS, ArenaTheme.WAREHOUSE);
        if (arena == null) return null;
        AlastairEntity boss = AllEntities.ALASTAIR.get().create(level);
        if (boss == null) return null;
        Vec3 at = Vec3.atBottomCenterOf(site.trap());
        Vec3 face = hunter != null ? hunter.position() : site.floor(WarehouseAnchors.ENTRY);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindWarehouse(site.origin(), site.q());
        boss.bindArena(arena);
        boss.chain(chained);
        if (chained) WarehouseWorld.paintTrap(level, site.trap());
        level.addFreshEntity(boss);
        if (chained) boss.guardedBy(stageAngels(level, site, boss));
        // The loading doors barred for the fight.
        for (BlockPos p : site.doors()) {
            if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) arena.mutate(level, p, Blocks.IRON_BARS.defaultBlockState(), 0);
        }
        boss.beginEmergence();
        if (hunter != null && chained) {
            hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.alastair.found").withStyle(ChatFormatting.DARK_RED), false);
        }
        return boss;
    }

    /** The angels who hold him: they stand still round the trap, can't be hurt, and die when he breaks free. */
    static List<Entity> stageAngels(ServerLevel level, WarehouseSite site, AlastairEntity boss) {
        List<Entity> out = new ArrayList<>();
        for (Vec3 s : site.angels()) {
            Mob a = AllEntities.ANGEL.get().create(level);
            if (a == null) continue;
            float yaw = (float) Math.toDegrees(Math.atan2(-(boss.getX() - s.x), boss.getZ() - s.z));
            a.moveTo(s.x, s.y, s.z, yaw, 0);
            a.setYHeadRot(yaw);
            a.setNoAi(true);
            a.setInvulnerable(true);
            a.setPersistenceRequired();
            level.addFreshEntity(a);
            out.add(a);
        }
        return out;
    }

    /** The rematch rite, at the warehouse. @return whether he answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        WarehouseSite site = WarehouseWorld.site(level.getServer());
        return site != null && begin(level, ritualist, site, false) != null;
    }

    /** Why the rite fails here and now, or null: only at the warehouse, by the trap. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        WarehouseSite site = WarehouseWorld.site(level.getServer());
        if (site == null || level.dimension() != Level.OVERWORLD || site.trap().distSqr(altar) > RITE_REACH * RITE_REACH) {
            return Component.translatable("message.supernaturalcraft.alastair.rite.elsewhere");
        }
        return LuciferSummoning.refusal(level);
    }

    /** Anywhere (commands, tests): a plain arena round {@code at}, unchained. */
    public static @Nullable AlastairEntity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer facing) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.WAREHOUSE);
        if (arena == null) {
            if (facing != null) {
                Component why = LuciferSummoning.refusal(level);
                facing.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.alastair.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        AlastairEntity boss = AllEntities.ALASTAIR.get().create(level);
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 180, 0);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
