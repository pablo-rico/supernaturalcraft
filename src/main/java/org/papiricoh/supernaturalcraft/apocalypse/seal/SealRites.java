package org.papiricoh.supernaturalcraft.apocalypse.seal;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonSite;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonWorld;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * The four rites of the seals' spoils (v0.40), each a {@link RitualEffect} of its own (registered from {@link #bootstrap}):
 * <ul>
 *   <li>{@link WitnessWard} ({@code ward_against_witnesses}, Witness Shrouds): for a day, the Witnesses do not see the hunters in
 *   the circle (they will not target them, their wail does not darken them).</li>
 *   <li>{@link FeatherSight} ({@code feather_of_revelation}, Scorched Feathers): every angel within {@link FeatherSight#REACH},
 *   loyal or rebel, glows for a minute; a hunter Castiel has warned also learns the way to Uriel's garrison.</li>
 *   <li>{@link Cleansing} ({@code cleanse_the_plague}, Plague Ash): every plague round the altar turns back at once and the crops
 *   there grow a little.</li>
 *   <li>{@link MendSeal} ({@code mend_the_seal}, Sacrificial Ash): the next seal breaks a day later (it counts against the days the
 *   hunters can win: {@link SealRules#DELAY_CAP}).</li>
 * </ul>
 */
public final class SealRites {

    /** Persistent-data key of a hunter's Witness Ward: the game time it ends. */
    public static final String WARD_TAG = "sn_witness_ward";
    /** The ward's length and reach round the altar. */
    public static final int WARD_TICKS = (int) SealRules.DAY, WARD_REACH = 8;

    private static boolean booted;

    private SealRites() {
    }

    /** Registers the four effect types (once, as the mod is built: before any recipe is read). */
    public static synchronized void bootstrap() {
        if (booted) return;
        booted = true;
        RitualEffect.register(WitnessWard.ID, WitnessWard.CODEC);
        RitualEffect.register(FeatherSight.ID, FeatherSight.CODEC);
        RitualEffect.register(Cleansing.ID, Cleansing.CODEC);
        RitualEffect.register(MendSeal.ID, MendSeal.CODEC);
    }

    /** Whether the Witnesses cannot see {@code e} now. */
    public static boolean warded(@Nullable Entity e) {
        if (!(e instanceof Player p)) return false;
        long until = p.getPersistentData().getLong(WARD_TAG);
        return until > 0 && p.level().getGameTime() < until;
    }

    /** Wards {@code p} for {@link #WARD_TICKS} from now. */
    public static void ward(ServerPlayer p) {
        p.getPersistentData().putLong(WARD_TAG, p.level().getGameTime() + WARD_TICKS);
    }

    /** The ward against the Witnesses. */
    public record WitnessWard() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("witness_ward");
        public static final MapCodec<WitnessWard> CODEC = MapCodec.unit(new WitnessWard());

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            java.util.List<ServerPlayer> warded = new java.util.ArrayList<>(level.getEntitiesOfClass(ServerPlayer.class, new AABB(altar).inflate(WARD_REACH)));
            if (ritualist != null && !warded.contains(ritualist)) warded.add(ritualist);
            for (ServerPlayer p : warded) {
                ward(p);
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.seal.rite.ward").withStyle(ChatFormatting.GRAY), true);
            }
            for (WitnessEntity w : level.getEntitiesOfClass(WitnessEntity.class, new AABB(altar).inflate(WARD_REACH * 2))) w.setTarget(null);
            return true;
        }
    }

    /** The feather that shows the angels. */
    public record FeatherSight() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("feather_sight");
        public static final MapCodec<FeatherSight> CODEC = MapCodec.unit(new FeatherSight());
        public static final int REACH = 64, GLOW_TICKS = 1200;

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            int n = 0;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(altar).inflate(REACH),
                    e -> !(e instanceof Player) && e.isAlive() && (Kin.isAngel(e) || Kin.isFallen(e)))) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0));
                n++;
            }
            if (ritualist == null) return true;
            ritualist.sendSystemMessage(Component.translatable("message.supernaturalcraft.seal.rite.feather", n).withStyle(ChatFormatting.AQUA));
            if (SealWorld.record(ritualist).envoy() > 0) {
                GarrisonSite site = GarrisonWorld.site(level.getServer());
                if (site != null) {
                    Vec3 at = site.floor(org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonAnchors.URIEL);
                    int dist = (int) Math.round(Math.sqrt(ritualist.distanceToSqr(at)));
                    ritualist.sendSystemMessage(Component.translatable("message.supernaturalcraft.seal.rite.feather_garrison", dist,
                            Component.translatable(SealEvents.direction(ritualist.position(), at))).withStyle(ChatFormatting.GOLD));
                }
            }
            return true;
        }
    }

    /** The cleansing of the plagues. */
    public record Cleansing() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("cleanse_plague");
        public static final MapCodec<Cleansing> CODEC = MapCodec.unit(new Cleansing());
        public static final int REACH = 48, GROW_REACH = 8;

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            int n = SealPlagues.cleanse(level, altar, REACH);
            for (BlockPos p : BlockPos.betweenClosed(altar.offset(-GROW_REACH, -2, -GROW_REACH), altar.offset(GROW_REACH, 2, GROW_REACH))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && crop instanceof BonemealableBlock b
                        && b.isValidBonemealTarget(level, p, s) && level.random.nextInt(3) == 0) {
                    b.performBonemeal(level, level.random, p.immutable(), s);
                }
            }
            if (ritualist != null) {
                ritualist.sendSystemMessage(Component.translatable("message.supernaturalcraft.seal.rite.cleanse", n).withStyle(ChatFormatting.GREEN));
            }
            return true;
        }
    }

    /** The mending of the seal: a day more before the next breaks. */
    public record MendSeal() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("mend_seal");
        public static final MapCodec<MendSeal> CODEC = MapCodec.unit(new MendSeal());

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            SealSavedData data = SealSavedData.get(level.getServer());
            if (!data.started() || data.risen()) return Component.translatable("message.supernaturalcraft.seal.rite.mend_none");
            if (!SealRules.canDelay(data.delayDays(), data.broken())) return Component.translatable("message.supernaturalcraft.seal.rite.mend_cap");
            return null;
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            SealSavedData data = SealSavedData.get(level.getServer());
            if (precheck(level, altar, ritualist) != null || !data.delay()) return false;
            long next = data.nextBreak();
            int days = SealRules.daysLeft(WorldClock.day(level.getServer()), next);
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.translatable("message.supernaturalcraft.seal.rite.mend", days).withStyle(ChatFormatting.GOLD));
            }
            SealWorld.syncAll(level.getServer());
            return true;
        }
    }
}
