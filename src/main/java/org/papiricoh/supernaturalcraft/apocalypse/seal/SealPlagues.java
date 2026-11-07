package org.papiricoh.supernaturalcraft.apocalypse.seal;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.network.SealFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.List;

/**
 * The plagues in the world (v0.40): every block a plague turns goes through {@link #turn}, which writes it in the ledger
 * ({@link PlagueLedger}) first, so all of it comes back: the water turned to blood, crops withered, grass gone to dead earth,
 * a graveyard's broken ground. Only in loaded chunks, never a block entity, never inside an arena, never past the ledger's cap.
 * {@link #revert} turns back at most {@link PlagueLedger#REVERT_PER_TICK} a tick (everything, in an echo).
 */
public final class SealPlagues {

    /** How long the plagues of an event, and those of the stage, stay (clock ticks). */
    public static final long EVENT_LASTS = SealRules.DAY * 2, AMBIENT_LASTS = SealRules.DAY * 3;
    /** How far a plague reaches round its hex, and how many blocks it turns a step. */
    public static final int RADIUS = 10, PER_STEP = 6, AMBIENT_RADIUS = 6, AMBIENT_BLOCKS = 10;

    private SealPlagues() {
    }

    /**
     * Turns {@code pos} into {@code to}, written in the ledger to come back at {@code revertAt} (clock ticks).
     * @return whether it turned
     */
    public static boolean turn(ServerLevel level, BlockPos pos, BlockState to, long revertAt) {
        if (!level.isLoaded(pos) || level.getBlockEntity(pos) != null) return false;
        if (ArenaSavedData.get(level).at(Vec3.atCenterOf(pos)) != null) return false;
        BlockState was = level.getBlockState(pos);
        if (was.equals(to)) return false;
        SealSavedData data = SealSavedData.get(level.getServer());
        if (!data.ledger().add(pos.asLong(), BlockStateParser.serialize(was), BlockStateParser.serialize(to), revertAt)) return false;
        data.ledgerChanged();
        level.setBlock(pos, to, Block.UPDATE_ALL);
        return true;
    }

    /** One step of an event's plague round its hex. */
    static void step(ServerLevel level, SealEvent e) {
        long until = WorldClock.day(level.getServer()) + EVENT_LASTS;
        int turned = spread(level, e.kind, e.pos, RADIUS, PER_STEP, until, level.random);
        if (turned > 0) {
            e.plagueSteps++;
            Send.near(level, null, e.pos.getX() + 0.5, e.pos.getY() + 1, e.pos.getZ() + 0.5, 64,
                    new SealFxPayload(-1, SealFxPayload.PLAGUE, RADIUS, 0, Vec3.atCenterOf(e.pos), 40));
        }
    }

    /** Turns up to {@code max} blocks of {@code kind}'s plague within {@code radius} of {@code at}. @return how many */
    static int spread(ServerLevel level, SealKind kind, BlockPos at, int radius, int max, long until, RandomSource r) {
        int turned = 0;
        for (int tries = 0; tries < max * 6 && turned < max; tries++) {
            int x = at.getX() + r.nextInt(radius * 2 + 1) - radius, z = at.getZ() + r.nextInt(radius * 2 + 1) - radius;
            BlockPos top = SealStaging.ground(level, x, z);
            if (top == null || Math.abs(top.getY() - at.getY()) > 8) continue;
            BlockPos ground = top.below();
            BlockState g = level.getBlockState(ground), above = level.getBlockState(top);
            boolean ok = switch (kind) {
                case BLOOD_WATER -> {
                    // The water turns first; on dry land, pools of blood.
                    BlockPos water = waterAt(level, top);
                    if (water != null) yield turn(level, water, AllBlocks.BLOOD_POOL.get().defaultBlockState(), until);
                    yield above.isAir() && g.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP) && r.nextInt(3) == 0
                            && turn(level, top, AllBlocks.BLOOD_POOL.get().defaultBlockState(), until);
                }
                case WITHERED_CROPS -> {
                    if (above.getBlock() instanceof CropBlock crop && crop.getAge(above) > 0) yield turn(level, top, crop.getStateForAge(0), until);
                    if (g.is(Blocks.GRASS_BLOCK)) yield turn(level, ground, (r.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.PODZOL).defaultBlockState(), until);
                    if (above.is(Blocks.SHORT_GRASS) || above.is(Blocks.FERN)) yield turn(level, top, Blocks.DEAD_BUSH.defaultBlockState(), until);
                    yield false;
                }
                case CEMETERY -> g.is(Blocks.GRASS_BLOCK) && turn(level, ground, (r.nextInt(3) == 0 ? Blocks.ROOTED_DIRT : Blocks.COARSE_DIRT).defaultBlockState(), until);
                default -> false;
            };
            if (ok) turned++;
        }
        return turned;
    }

    /** Surface water at the column of {@code top} (a source with open air above), or null. */
    static @Nullable BlockPos waterAt(ServerLevel level, BlockPos top) {
        for (int dy = 0; dy >= -2; dy--) {
            BlockPos p = top.offset(0, dy, 0);
            if (level.getFluidState(p).isSource() && level.getBlockState(p).is(Blocks.WATER) && level.getBlockState(p.above()).isAir()) return p;
        }
        return null;
    }

    /** A graveyard opens: a few headstones (cobblestone walls) round the hex, all in the ledger. */
    static void graves(ServerLevel level, SealEvent e) {
        long until = WorldClock.day(level.getServer()) + EVENT_LASTS;
        for (int i = 0; i < 5; i++) {
            net.minecraft.world.phys.Vec3 at = SealStaging.ring(level, e.pos, 5, i, 5);
            BlockPos p = BlockPos.containing(at);
            if (level.getBlockState(p).canBeReplaced() && level.getFluidState(p).isEmpty()) {
                turn(level, p, (i % 2 == 0 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState(), until);
            }
        }
    }

    /** A plague of the stage, out in the world near a hunter (blood on the water, a withered field). */
    static void ambient(ServerLevel level, BlockPos at, RandomSource r) {
        if (!SealStaging.fits(level, at)) return;
        SealKind kind = r.nextBoolean() ? SealKind.BLOOD_WATER : SealKind.WITHERED_CROPS;
        long until = WorldClock.day(level.getServer()) + AMBIENT_LASTS;
        if (spread(level, kind, at, AMBIENT_RADIUS, AMBIENT_BLOCKS, until, r) > 0) {
            Send.near(level, null, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 64,
                    new SealFxPayload(-1, SealFxPayload.PLAGUE, AMBIENT_RADIUS, 1, Vec3.atCenterOf(at), 40));
        }
    }

    /** Each tick: the plagues whose time has come turn back (every one of them, in an echo). */
    public static void revert(MinecraftServer server) {
        revert(server, false);
    }

    /** Turns back up to {@link PlagueLedger#REVERT_PER_TICK} plagues now, whatever their time (commands, tests). */
    public static void revertAll(MinecraftServer server) {
        revert(server, true);
    }

    static void revert(MinecraftServer server, boolean all) {
        SealSavedData data = SealSavedData.get(server);
        PlagueLedger ledger = data.ledger();
        if (ledger.isEmpty()) return;
        ServerLevel level = server.overworld();
        long now = WorldClock.day(server);
        List<PlagueLedger.Entry> due = all || data.echo() ? ledger.take(PlagueLedger.REVERT_PER_TICK) : ledger.due(now, PlagueLedger.REVERT_PER_TICK);
        if (due.isEmpty()) return;
        for (PlagueLedger.Entry e : due) {
            BlockPos p = BlockPos.of(e.pos());
            if (!level.isLoaded(p)) {
                // Back when its chunk is: an hour from now.
                ledger.add(e.pos(), e.original(), e.placed(), now + 1000);
                continue;
            }
            BlockState current = level.getBlockState(p);
            if (!PlagueLedger.untouched(e, BlockStateParser.serialize(current))) continue;
            BlockState original = parse(level, e.original());
            if (original != null) level.setBlock(p, original, Block.UPDATE_ALL);
        }
        data.ledgerChanged();
    }

    /** Every plague within {@code radius} of {@code at} turns back now (the cleansing rite). @return how many were due */
    public static int cleanse(ServerLevel level, BlockPos at, int radius) {
        SealSavedData data = SealSavedData.get(level.getServer());
        int n = data.ledger().hasten(at.getX(), at.getZ(), radius, WorldClock.day(level.getServer()));
        if (n > 0) data.ledgerChanged();
        return n;
    }

    static @Nullable BlockState parse(ServerLevel level, String state) {
        try {
            return BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), state, false).blockState();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }
}
