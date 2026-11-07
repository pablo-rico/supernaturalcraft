package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Bobby's Panic Room on the server (v0.40): checking a room ({@link PanicRoomRules}), locking a drinker in from the inside,
 * and the cure ({@link BloodRules#PANIC_DAYS} of day-time inside, then the blood and the dependency are gone). Leaving the
 * room, or the door going missing, breaks the cure off.
 */
public final class PanicRoom {

    private PanicRoom() {
    }

    /** The room behind {@code door} on {@code side} of it. */
    public static PanicRoomRules.Room room(ServerLevel level, BlockPos door, Direction side) {
        BlockPos start = door.relative(side);
        return PanicRoomRules.check(new PanicRoomRules.World() {
            @Override
            public boolean wall(int x, int y, int z) {
                BlockPos p = new BlockPos(x, y, z);
                return !level.isLoaded(p) || level.getBlockState(p).is(AllTags.Blocks.PANIC_ROOM_WALLS);
            }

            @Override
            public boolean ceilingTrap(int x, int y, int z) {
                BlockState s = level.getBlockState(new BlockPos(x, y, z));
                return s.getBlock() instanceof DevilsTrapBlock && DevilsTrapBlock.onCeiling(s);
            }
        }, start.getX(), start.getY(), start.getZ());
    }

    /** The room {@code p} stands in, on either side of the door (lower half at {@code door}), or null. */
    public static @Nullable PanicRoomRules.Room roomOf(ServerLevel level, BlockPos door, ServerPlayer p) {
        BlockState s = level.getBlockState(door);
        if (!(s.getBlock() instanceof DoorBlock)) return null;
        Direction facing = s.getValue(DoorBlock.FACING);
        for (Direction side : new Direction[]{facing, facing.getOpposite()}) {
            PanicRoomRules.Room r = room(level, door, side);
            BlockPos at = p.blockPosition();
            if (r.ok() && r.contains(at.getX(), at.getY(), at.getZ())) return r;
        }
        return null;
    }

    /**
     * {@code p} uses the door (lower half {@code door}). @return true if it did something: locked them in, or told them why it
     * would not. An unlocked door with nobody needing it just opens and shuts by hand.
     */
    public static boolean use(ServerLevel level, BlockPos door, BlockState state, ServerPlayer p) {
        if (!(level.getBlockEntity(door) instanceof PanicRoomDoorBlockEntity be)) return false;
        if (be.locked()) {
            String key = p.getUUID().equals(be.occupant()) ? "message.supernaturalcraft.panic_room.waiting" : "message.supernaturalcraft.panic_room.locked";
            p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        BloodData d = Bloods.get(p);
        if (d.dependency() <= 0 || state.getValue(DoorBlock.OPEN)) return false;
        PanicRoomRules.Room r = roomOf(level, door, p);
        if (r == null) {
            // Standing outside a sound room just walks through; a room that leaks or lacks its trap says which.
            Direction facing = state.getValue(DoorBlock.FACING);
            PanicRoomRules.Room a = room(level, door, facing), b = room(level, door, facing.getOpposite());
            if (a.ok() || b.ok()) return false;
            boolean noTrap = a.verdict() == PanicRoomRules.Verdict.NO_TRAP || b.verdict() == PanicRoomRules.Verdict.NO_TRAP;
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.panic_room." + (noTrap ? "no_trap" : "leaks"))
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        long until = org.papiricoh.supernaturalcraft.util.WorldClock.day(level.getServer()) + BloodRules.PANIC_DAYS * BloodRules.DAY_TICKS;
        be.lock(p.getUUID(), until);
        Bloods.update(p, x -> x.withPanic(door.asLong(), until));
        level.playSound(null, door, AllSounds.apocalypse("blood.door_lock"), SoundSource.BLOCKS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.panic_room.locked_in").withStyle(ChatFormatting.DARK_RED), false);
        return true;
    }

    /** Once a second for every player (from {@link BloodEvents}). */
    public static void tick(ServerPlayer p) {
        BloodData d = Bloods.get(p);
        if (!d.inPanicRoom()) return;
        ServerLevel level = p.serverLevel();
        BlockPos door = BlockPos.of(d.panicDoor());
        if (!level.isLoaded(door)) return;
        boolean ours = level.getBlockEntity(door) instanceof PanicRoomDoorBlockEntity be && p.getUUID().equals(be.occupant());
        if (!ours || roomOf(level, door, p) == null) {
            release(level, door, p);
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.panic_room.broken").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        if (org.papiricoh.supernaturalcraft.util.WorldClock.day(level.getServer()) >= d.panicUntil()) {
            release(level, door, p);
            Bloods.cure(p);
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.panic_room.cured").withStyle(ChatFormatting.GOLD), false);
        }
    }

    private static void release(ServerLevel level, BlockPos door, ServerPlayer p) {
        if (level.getBlockEntity(door) instanceof PanicRoomDoorBlockEntity be && p.getUUID().equals(be.occupant())) be.unlock();
        Bloods.update(p, x -> x.withPanic(0L, -1L));
        level.playSound(null, door, AllSounds.apocalypse("blood.door_lock"), SoundSource.BLOCKS, 1f, 1.2f);
    }

    /** The lower half of the door at {@code pos}. */
    public static BlockPos lower(BlockPos pos, BlockState state) {
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
