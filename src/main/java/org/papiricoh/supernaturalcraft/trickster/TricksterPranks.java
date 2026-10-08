package org.papiricoh.supernaturalcraft.trickster;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity;
import org.papiricoh.supernaturalcraft.network.GabrielFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Trickster's pranks (v0.14), on the server: once Lucifer has fallen, at most one a world day per hunter, at a random
 * moment ({@link PrankRules}). None of them breaks, moves or takes anything: a candy wrapper at your feet, a party hat on a
 * creature nearby, a laugh track from nowhere, a villager with a line off the TV, a chest that opens and shuts on its own.
 * Each one noticed goes in the hunter's {@link TricksterLedger}; the third puts them on his trail
 * ({@code main/trickster_sighted}: the bait's rite, the journal's page). Also keeps his marks ({@link TricksterMarks}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class TricksterPranks {

    /** How many TV lines a villager may come out with ({@code message.supernaturalcraft.trickster.tv_line.<n>}). */
    public static final int TV_LINES = 8;
    /** A chest the prank opens shuts again this much later. */
    public static final int CHEST_OPEN_TICKS = 30;

    private record Shut(ResourceKey<Level> level, BlockPos pos, long at) {
    }

    private static final List<Shut> SHUTTING = new CopyOnWriteArrayList<>();
    private static final Random RANDOM = new Random();

    private TricksterPranks() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % 20 == 0) TricksterMarks.tick(server);
        shutChests(server);
        if (tick % PrankRules.CHECK_EVERY != 0 || !SNConfig.TRICKSTER_PRANKS.get()) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) consider(p, RANDOM);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer) TricksterMarks.onStartTracking(event.getTarget(), viewer);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.loadedFromDisk()) return;
        TricksterMarks.onJoin(event.getEntity());
    }

    /** The world day pranks are counted by (the Overworld's). */
    public static long day(MinecraftServer server) {
        return PrankRules.day(server.overworld().getDayTime());
    }

    /** One look at {@code p}: a prank, if one is due and it is the moment. @return the prank played, or null */
    public static PrankRules.@Nullable Prank consider(ServerPlayer p, Random random) {
        if (!p.isAlive() || p.isSpectator() || p.getServer() == null) return null;
        TricksterLedger ledger = p.getData(AllAttachments.TRICKSTER);
        if (!PrankRules.due(AuthorWorld.done(p, "main/devil_went_down"), ledger, day(p.getServer()))) return null;
        // Never in the middle of a great fight.
        if (ArenaSavedData.get(p.serverLevel()).at(p.position()) != null) return null;
        if (!PrankRules.now(random)) return null;
        return prank(p, random);
    }

    /** Plays a prank on {@code p}, whichever fits what is around. @return the prank */
    public static PrankRules.Prank prank(ServerPlayer p, Random random) {
        ServerLevel level = p.serverLevel();
        boolean mob = SNConfig.PARTY_HATS.get() && hatTarget(level, p) != null;
        PrankRules.Prank prank = PrankRules.pick(random, mob, villager(level, p) != null, chest(level, p) != null);
        if (!play(p, prank, random)) {
            prank = PrankRules.Prank.CANDY_WRAPPER;
            play(p, prank, random);
        }
        return prank;
    }

    /** Plays {@code prank} on {@code p} now, ignoring the once-a-day rule (previews, tests). */
    public static boolean play(ServerPlayer p, PrankRules.Prank prank) {
        return play(p, prank, RANDOM);
    }

    /**
     * Plays {@code prank} on {@code p} and has them notice it (the ledger, the PRANK payload, the advancement at the third).
     *
     * @return false if there was nothing to play it on (nothing is noticed then)
     */
    public static boolean play(ServerPlayer p, PrankRules.Prank prank, Random random) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        switch (prank) {
            case CANDY_WRAPPER -> {
                double a = random.nextDouble() * Math.PI * 2;
                at = p.position().add(Math.cos(a) * 0.8, 0.2, Math.sin(a) * 0.8);
                ItemEntity wrapper = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(AllItems.CANDY_WRAPPER.get()));
                wrapper.setDeltaMovement(0, 0.1, 0);
                wrapper.setPickUpDelay(20);
                level.addFreshEntity(wrapper);
                level.playSound(null, BlockPos.containing(at), SoundEvents.ITEM_PICKUP, SoundSource.AMBIENT, 0.4f, 0.6f);
            }
            case PARTY_HAT -> {
                Mob mob = SNConfig.PARTY_HATS.get() ? hatTarget(level, p) : null;
                if (mob == null) return false;
                TricksterMarks.hat(mob, PrankRules.HAT_TICKS);
                at = mob.position();
            }
            case LAUGH_TRACK -> {
                // Only the pranked hunter hears it: the client plays the laugh track at the PRANK's point.
                Vec3 back = p.getLookAngle().multiply(-1, 0, -1);
                if (back.lengthSqr() < 0.01) back = new Vec3(0, 0, 1);
                at = p.position().add(back.normalize().scale(PrankRules.RADIUS - 2));
            }
            case TV_LINE -> {
                Villager v = villager(level, p);
                if (v == null) return false;
                at = v.position();
                Component line = Component.translatable("message.supernaturalcraft.trickster.villager_says", v.getDisplayName(),
                        Component.translatable("message.supernaturalcraft.trickster.tv_line." + random.nextInt(TV_LINES)))
                        .withStyle(ChatFormatting.ITALIC);
                for (ServerPlayer near : level.players()) if (near.distanceToSqr(v) < 16 * 16) near.sendSystemMessage(line);
                v.playSound(SoundEvents.VILLAGER_AMBIENT, 1.0f, 1.3f);
            }
            case CHEST -> {
                BlockPos chest = chest(level, p);
                if (chest == null) return false;
                at = Vec3.atCenterOf(chest);
                BlockState state = level.getBlockState(chest);
                level.blockEvent(chest, state.getBlock(), 1, 1);
                level.playSound(null, chest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.6f, 0.9f);
                SHUTTING.add(new Shut(level.dimension(), chest.immutable(), level.getGameTime() + CHEST_OPEN_TICKS));
            }
        }
        notice(p, prank, at);
        return true;
    }

    /** {@code p} noticed one: it goes in their ledger, their HUD gets its hint, and the third puts them on his trail. */
    private static void notice(ServerPlayer p, PrankRules.Prank prank, Vec3 at) {
        TricksterLedger ledger = p.getData(AllAttachments.TRICKSTER).sighted(p.getServer() != null ? day(p.getServer()) : 0);
        p.setData(AllAttachments.TRICKSTER, ledger);
        PacketDistributor.sendToPlayer(p, new GabrielFxPayload(-1, GabrielFxPayload.PRANK, prank.ordinal(), ledger.sightings(), at, 60));
        if (ledger.onHisTrail()) ChorusRewards.award(p, "main/trickster_sighted");
    }

    private static void shutChests(MinecraftServer server) {
        if (SHUTTING.isEmpty()) return;
        for (Shut s : SHUTTING) {
            ServerLevel level = server.getLevel(s.level());
            if (level == null) {
                SHUTTING.remove(s);
                continue;
            }
            if (level.getGameTime() < s.at()) continue;
            SHUTTING.remove(s);
            BlockState state = level.getBlockState(s.pos());
            if (state.getBlock() instanceof ChestBlock) {
                level.blockEvent(s.pos(), state.getBlock(), 1, 0);
                level.playSound(null, s.pos(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.6f, 0.9f);
            }
        }
    }

    // --- what is around -----------------------------------------------------------------------------------------------

    private static AABB around(ServerPlayer p) {
        return p.getBoundingBox().inflate(PrankRules.RADIUS, 4, PrankRules.RADIUS);
    }

    /** The nearest creature that could wear a hat: no boss, no villager, no double, none with a hat on already. */
    public static @Nullable Mob hatTarget(ServerLevel level, ServerPlayer p) {
        return level.getEntitiesOfClass(Mob.class, around(p), m -> m.isAlive() && !m.getType().is(AllTags.Entities.BOSSES)
                        && !(m instanceof GabrielDoubleEntity) && !TricksterMarks.hatted(m))
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    public static @Nullable Villager villager(ServerLevel level, ServerPlayer p) {
        return level.getEntitiesOfClass(Villager.class, around(p), Villager::isAlive)
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    /** The nearest chest within reach of the prank, or null. */
    public static @Nullable BlockPos chest(ServerLevel level, ServerPlayer p) {
        BlockPos c = p.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int r = PrankRules.RADIUS;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (!level.isLoaded(m) || !(level.getBlockState(m).getBlock() instanceof ChestBlock)) continue;
                    double d = m.distSqr(c);
                    if (d < bestD) {
                        bestD = d;
                        best = m.immutable();
                    }
                }
            }
        }
        return best;
    }
}
