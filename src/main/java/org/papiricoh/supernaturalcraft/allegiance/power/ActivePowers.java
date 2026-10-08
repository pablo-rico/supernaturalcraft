package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.allegiance.HostAllyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.LightSpearEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.BoundHellhoundEntity;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelBladeItem;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What every active power does (v0.13); {@link PowerCaster} has already checked and will pay. Each returns a {@link Result}
 * (what was struck, where, how long it lasts: the {@code CAST} fx carries these) or null if there was nothing to do, which
 * costs nothing. The powers that run on (true form, smoke, telekinesis, possession, the throne) are ticked from
 * {@link #tick}.
 */
public final class ActivePowers {

    /** What a cast did, for its fx. */
    public record Result(@Nullable LivingEntity target, Vec3 point, int duration) {
    }

    public static final float HEAL = 8f, TOUCH_BURN = 8f;
    public static final int BLADE_TICKS = 1200, RADIO_PINGS = 4, RADIO_GAP = 100, RADIO_LIMIT = 24;
    public static final float SMITE_LESSER = 60f, SMITE_BOSS = 20f, SMITE_PLAYER = 14f, SMITE_OTHER = 10f;
    public static final int TRUE_FORM_TICKS = 200, SQUAD = 5;
    public static final float TRUE_FORM_BURN = 4f, LANCE_SPEED = 2.6f;
    public static final int SMOKE_TICKS = 6, HOLD_TICKS = 80, THROWN_TICKS = 40, POSSESS_TICKS = 400, THRONE_TICKS = 160;
    public static final float THROW_DAMAGE = 8f, THROW_SPEED = 2.2f;
    /** A mob heavier than this can't be ridden or seized. */
    public static final float POSSESS_MAX_HEALTH = 60f;
    /** Item custom data: game time a summoned Angel Blade vanishes at. */
    public static final String SUMMONED_TAG = "sn_summoned_until";

    private record Dash(Vec3 from, Vec3 to, long start) {
    }

    private record Hold(int entity, long until) {
    }

    private record Thrown(UUID thrower, long until) {
    }

    private static final Map<UUID, Long> TRUE_FORM = new ConcurrentHashMap<>();
    private static final Map<UUID, Dash> SMOKE = new ConcurrentHashMap<>();
    private static final Map<UUID, Hold> HELD = new ConcurrentHashMap<>();
    private static final Map<Integer, Thrown> THROWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Hold> RIDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> THRONE = new ConcurrentHashMap<>();

    private ActivePowers() {
    }

    static @Nullable Result perform(ServerPlayer p, Power power, @Nullable LivingEntity target) {
        return switch (power) {
            case TELEPORT -> teleport(p, power.range);
            case HEALING_TOUCH -> healingTouch(p, target);
            case ANGEL_BLADE -> angelBlade(p);
            case ANGEL_RADIO -> angelRadio(p, power.range);
            case SMITE -> smite(p, target);
            case TRUE_FORM -> trueForm(p);
            case HOST_SQUAD -> hostSquad(p);
            case LIGHT_LANCE -> lightLance(p);
            case SMOKE -> smoke(p, power.range);
            case SUMMON_HOUND -> summonHound(p);
            case TELEKINESIS -> seize(p, target);
            case POSSESS -> possess(p, target);
            case THRONE -> throne(p);
            default -> null;
        };
    }

    // --- Angel ---------------------------------------------------------------------------------------------------------

    /** Where a blink along the gaze lands: short of the first wall, on a spot the body fits. */
    static @Nullable Vec3 landing(ServerPlayer p, int range) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 aim = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(range)) : hit.getLocation().subtract(look.scale(0.6));
        Vec3 feet = new Vec3(aim.x, aim.y - p.getEyeHeight() * 0.5, aim.z);
        for (int dy : new int[]{0, 1, -1, 2, -2, 3}) {
            Vec3 c = new Vec3(feet.x, Math.floor(feet.y) + dy, feet.z);
            if (level.noCollision(p, p.getBoundingBox().move(c.subtract(p.position())))) {
                return c.distanceToSqr(p.position()) < 2.25 ? null : c;
            }
        }
        return null;
    }

    static @Nullable Result teleport(ServerPlayer p, int range) {
        Vec3 to = landing(p, range);
        if (to == null) return null;
        ServerLevel level = p.serverLevel();
        level.sendParticles(AllParticles.GRACE.get(), p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        level.sendParticles(AllParticles.GRACE.get(), to.x, to.y + 1, to.z, 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, p.blockPosition(), AllSounds.ALLEGIANCE_TELEPORT.get(), SoundSource.PLAYERS, 1f, 1f);
        return new Result(null, to, 10);
    }

    /** Two fingers to the brow: heals (and cures poisons); a demon burns instead. No target: oneself. */
    static Result healingTouch(ServerPlayer p, @Nullable LivingEntity target) {
        LivingEntity t = target == null ? p : target;
        ServerLevel level = p.serverLevel();
        if (t != p && Kin.isDemon(t)) {
            t.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), TOUCH_BURN);
            level.sendParticles(ParticleTypes.FLAME, t.getX(), t.getEyeY(), t.getZ(), 12, 0.2, 0.2, 0.2, 0.02);
        } else {
            t.heal(HEAL);
            t.removeEffect(MobEffects.POISON);
            t.removeEffect(MobEffects.WITHER);
            t.removeEffect(MobEffects.HUNGER);
            t.clearFire();
            level.sendParticles(AllParticles.GRACE.get(), t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 16, 0.3, 0.4, 0.3, 0.03);
        }
        level.playSound(null, t.blockPosition(), AllSounds.ALLEGIANCE_HEAL.get(), SoundSource.PLAYERS, 1f, 1f);
        return new Result(t, t.getEyePosition(), 20);
    }

    public static boolean summonedBlade(ItemStack stack) {
        if (!stack.is(AllItems.ANGEL_BLADE.get())) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(SUMMONED_TAG);
    }

    /** A blade from the sleeve (bound, it vanishes after a minute); with one already in hand, a dash. */
    static @Nullable Result angelBlade(ServerPlayer p) {
        if (summonedBlade(p.getMainHandItem())) {
            AngelBladeItem.dash(p, 1f);
            return new Result(null, p.position(), AngelBladeItem.DASH_TICKS);
        }
        ItemStack blade = new ItemStack(AllItems.ANGEL_BLADE.get());
        CompoundTag tag = new CompoundTag();
        tag.putLong(SUMMONED_TAG, p.serverLevel().getGameTime() + BLADE_TICKS);
        blade.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (p.getMainHandItem().isEmpty()) p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, blade);
        else if (!p.getInventory().add(blade)) return null;
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.MICHAEL_LANCE_THROW.get(), SoundSource.PLAYERS, 0.6f, 1.8f);
        return new Result(null, p.position(), BLADE_TICKS);
    }

    /** Angel radio: demons and bosses within {@code range} whisper where they are, every few seconds for 20 s. */
    static Result angelRadio(ServerPlayer p, int range) {
        for (int i = 0; i < RADIO_PINGS; i++) {
            int delay = i * RADIO_GAP;
            if (delay == 0) ping(p, range);
            else org.papiricoh.supernaturalcraft.util.ServerScheduler.schedule(delay, () -> {
                if (p.isAlive() && !p.isRemoved()) ping(p, range);
            });
        }
        return new Result(null, p.position(), RADIO_PINGS * RADIO_GAP);
    }

    /** One round of whispers ({@link AllegianceFxPayload#RADIO_PING}: arg 0 a demon, 1 a boss). @return how many */
    public static int ping(ServerPlayer p, int range) {
        List<LivingEntity> found = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range),
                e -> e != p && e.isAlive() && (Kin.isDemon(e) || e.getType().is(AllTags.Entities.BOSSES)) && e.distanceTo(p) <= range));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int n = Math.min(RADIO_LIMIT, found.size());
        for (int i = 0; i < n; i++) {
            LivingEntity e = found.get(i);
            AllegianceFx.toSelf(p, AllegianceFxPayload.RADIO_PING, e.getType().is(AllTags.Entities.BOSSES) ? 1 : 0, e.getId(), e.position(), RADIO_GAP + 10);
        }
        p.playNotifySound(AllSounds.ALLEGIANCE_RADIO.get(), SoundSource.PLAYERS, 0.6f, 1f);
        return n;
    }

    /** A palm to the face: holy fire through the eyes. Deadly to a lesser demon. */
    static @Nullable Result smite(ServerPlayer p, @Nullable LivingEntity t) {
        if (t == null) return null;
        ServerLevel level = p.serverLevel();
        float dmg = t.getType().is(AllTags.Entities.BOSSES) ? SMITE_BOSS : t instanceof Player ? SMITE_PLAYER
                : Kin.isDemon(t) ? SMITE_LESSER : SMITE_OTHER;
        t.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), dmg);
        if (Kin.isDemon(t)) t.igniteForSeconds(3);
        level.sendParticles(ParticleTypes.FLASH, t.getX(), t.getEyeY(), t.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(AllParticles.GRACE.get(), t.getX(), t.getEyeY(), t.getZ(), 24, 0.2, 0.2, 0.2, 0.1);
        level.playSound(null, t.blockPosition(), AllSounds.ALLEGIANCE_SMITE.get(), SoundSource.PLAYERS, 1.2f, 1f);
        return new Result(t, t.getEyePosition(), 20);
    }

    /** True form: {@link #TRUE_FORM_TICKS} of light (pulses in {@link #tick}). */
    static Result trueForm(ServerPlayer p) {
        TRUE_FORM.put(p.getUUID(), p.serverLevel().getGameTime() + TRUE_FORM_TICKS);
        Allegiances.setFlag(p, Allegiances.TRUE_FORM, true);
        AllegianceFx.around(p, AllegianceFxPayload.TRUE_FORM, 0, 0, p.getEyePosition(), TRUE_FORM_TICKS);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.ALLEGIANCE_TRUE_FORM.get(), SoundSource.PLAYERS, 2f, 1f);
        return new Result(null, p.getEyePosition(), TRUE_FORM_TICKS);
    }

    /** Everything near that sees the light is blinded; demons burn. */
    public static int trueFormPulse(ServerPlayer p, List<? extends LivingEntity> near) {
        ServerLevel level = p.serverLevel();
        boolean pvp = level.getServer().isPvpAllowed();
        int n = 0;
        for (LivingEntity e : near) {
            if (e == p || !e.isAlive() || e.distanceTo(p) > Power.TRUE_FORM.range) continue;
            if (e instanceof Player && !pvp) continue;
            if (Kin.isAngel(e) || e instanceof HostAllyEntity) continue;
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false), p);
            if (Kin.isDemon(e)) {
                e.igniteForSeconds(3);
                e.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), TRUE_FORM_BURN);
            }
            n++;
        }
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, 1.5, 1.2, 1.5, 0.08);
        return n;
    }

    /** A captain and four of the Host, for a minute. */
    static Result hostSquad(ServerPlayer p) {
        for (int i = 0; i < SQUAD; i++) {
            double a = i * Math.PI * 2 / SQUAD + p.getYRot() * Math.PI / 180;
            Vec3 at = i == 0 ? p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(2)) : p.position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5);
            HostAllyEntity.summon(p.serverLevel(), p, at, i == 0);
        }
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.MICHAEL_TRUMPET.get(), SoundSource.PLAYERS, 1.2f, 1.1f);
        return new Result(null, p.position(), HostAllyEntity.LIFETIME);
    }

    /** A lance of light, thrown along the gaze (its damage scaled in {@code AllegianceCombat}). */
    static Result lightLance(ServerPlayer p) {
        LightSpearEntity s = new LightSpearEntity(AllEntities.LIGHT_SPEAR.get(), p.level());
        s.setOwner(p);
        Vec3 eye = p.getEyePosition();
        s.setPos(eye.x, eye.y - 0.1, eye.z);
        Vec3 look = p.getLookAngle();
        s.shoot(look.x, look.y, look.z, LANCE_SPEED, 0f);
        p.level().addFreshEntity(s);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.ALLEGIANCE_LANCE.get(), SoundSource.PLAYERS, 1f, 1f);
        return new Result(null, eye, 20);
    }

    // --- Demon ---------------------------------------------------------------------------------------------------------

    /** Black smoke: pour out and rush along the gaze, untouchable on the way. */
    static @Nullable Result smoke(ServerPlayer p, int range) {
        Vec3 to = landing(p, range);
        if (to == null) return null;
        SMOKE.put(p.getUUID(), new Dash(p.position(), to, p.serverLevel().getGameTime()));
        Allegiances.setFlag(p, Allegiances.SMOKE, true);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.ALLEGIANCE_SMOKE.get(), SoundSource.PLAYERS, 1f, 1f);
        return new Result(null, to, SMOKE_TICKS);
    }

    static @Nullable Result summonHound(ServerPlayer p) {
        Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot() + 90).scale(1.5));
        BoundHellhoundEntity hound = BoundHellhoundEntity.call(p.serverLevel(), p, at.x, p.getY(), at.z);
        if (hound == null) return null;
        p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, at.x, p.getY() + 0.5, at.z, 20, 0.4, 0.3, 0.4, 0.02);
        return new Result(hound, at, BoundHellhoundEntity.LIFETIME);
    }

    /** What telekinesis and possession may take: not a player, not a boss, nothing too great. */
    static boolean takeable(@Nullable LivingEntity e) {
        return e instanceof Mob && !(e instanceof Player) && !e.getType().is(AllTags.Entities.BOSSES) && e.getMaxHealth() <= POSSESS_MAX_HEALTH;
    }

    /** Telekinesis, first half: seize what is looked at; it hangs before the caster until hurled. */
    static @Nullable Result seize(ServerPlayer p, @Nullable LivingEntity t) {
        if (!takeable(t)) return null;
        HELD.put(p.getUUID(), new Hold(t.getId(), p.serverLevel().getGameTime() + HOLD_TICKS));
        p.serverLevel().playSound(null, t.blockPosition(), AllSounds.ALLEGIANCE_TELEKINESIS.get(), SoundSource.PLAYERS, 1f, 1f);
        return new Result(t, t.position(), HOLD_TICKS);
    }

    public static boolean holding(ServerPlayer p) {
        return HELD.containsKey(p.getUUID());
    }

    public static @Nullable Entity held(ServerPlayer p) {
        Hold h = HELD.get(p.getUUID());
        return h == null ? null : p.serverLevel().getEntity(h.entity());
    }

    /** Telekinesis, second half: hurl it along the gaze. */
    public static void hurl(ServerPlayer p) {
        Hold h = HELD.remove(p.getUUID());
        if (h == null || !(p.serverLevel().getEntity(h.entity()) instanceof LivingEntity e) || !e.isAlive()) return;
        Vec3 v = p.getLookAngle().scale(THROW_SPEED).add(0, 0.2, 0);
        e.setDeltaMovement(v);
        e.hurtMarked = true;
        THROWN.put(e.getId(), new Thrown(p.getUUID(), p.serverLevel().getGameTime() + THROWN_TICKS));
        AllegianceFx.around(p, AllegianceFxPayload.CAST, Power.TELEKINESIS.ordinal(), e.getId(), e.position(), 10);
    }

    /** Ride a mob: it fights for the demon, who waits inside it as smoke. */
    static @Nullable Result possess(ServerPlayer p, @Nullable LivingEntity t) {
        if (!takeable(t) || RIDING.containsKey(p.getUUID())) return null;
        RIDING.put(p.getUUID(), new Hold(t.getId(), p.serverLevel().getGameTime() + POSSESS_TICKS));
        Allegiances.setFlag(p, Allegiances.POSSESSING | Allegiances.SMOKE, true);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, POSSESS_TICKS, 0, false, false), p);
        if (t instanceof Mob m) m.setTarget(null);
        p.serverLevel().sendParticles(AllParticles.DEMON_SMOKE.get(), t.getX(), t.getEyeY(), t.getZ(), 40, 0.2, 0.4, 0.2, 0.05);
        return new Result(t, t.position(), POSSESS_TICKS);
    }

    /** The mob {@code p} is riding, if any. */
    public static @Nullable LivingEntity ridden(ServerPlayer p) {
        Hold h = RIDING.get(p.getUUID());
        return h == null ? null : p.serverLevel().getEntity(h.entity()) instanceof LivingEntity l ? l : null;
    }

    /** Whether {@code mob} is ridden by {@code player}. */
    public static boolean rides(Entity player, Entity mob) {
        Hold h = RIDING.get(player.getUUID());
        return h != null && h.entity() == mob.getId();
    }

    /** Whether some demon rides {@code mob}; returns the rider's UUID or null. */
    public static @Nullable UUID rider(Entity mob) {
        for (var e : RIDING.entrySet()) if (e.getValue().entity() == mob.getId()) return e.getKey();
        return null;
    }

    /** The throne: every mob near goes to its knees for {@link #THRONE_TICKS}. */
    static Result throne(ServerPlayer p) {
        THRONE.put(p.getUUID(), p.serverLevel().getGameTime() + THRONE_TICKS);
        kneel(p);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.ALLEGIANCE_THRONE.get(), SoundSource.PLAYERS, 2f, 1f);
        return new Result(null, p.position(), THRONE_TICKS);
    }

    public static int kneel(ServerPlayer p) {
        int n = 0;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(Power.THRONE.range),
                m -> m.isAlive() && !m.getType().is(AllTags.Entities.BOSSES) && m.distanceTo(p) <= Power.THRONE.range)) {
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, false, false), p);
            m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 2, false, false), p);
            m.setTarget(null);
            m.getNavigation().stop();
            n++;
        }
        p.serverLevel().sendParticles(AllParticles.HELLFIRE.get(), p.getX(), p.getY() + 0.2, p.getZ(), 20, Power.THRONE.range / 3.0, 0.1,
                Power.THRONE.range / 3.0, 0.01);
        return n;
    }

    /** Untouchable: pouring through the air as smoke, or waiting inside a ridden mob. */
    public static boolean untouchable(Entity p) {
        return SMOKE.containsKey(p.getUUID()) || RIDING.containsKey(p.getUUID());
    }

    // --- ticking -------------------------------------------------------------------------------------------------------

    static void tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        UUID id = p.getUUID();
        Long form = TRUE_FORM.get(id);
        if (form != null) {
            if (now >= form || !p.isAlive()) {
                TRUE_FORM.remove(id);
                Allegiances.setFlag(p, Allegiances.TRUE_FORM, false);
            } else if ((form - now) % 20 == 0) {
                trueFormPulse(p, level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(Power.TRUE_FORM.range), p::hasLineOfSight));
            }
        }
        Dash dash = SMOKE.get(id);
        if (dash != null) {
            float t = Math.min(1f, (now - dash.start()) / (float) SMOKE_TICKS);
            Vec3 at = dash.from().lerp(dash.to(), t);
            p.teleportTo(at.x, at.y, at.z);
            p.fallDistance = 0;
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), at.x, at.y + 1, at.z, 10, 0.3, 0.5, 0.3, 0.02);
            if (t >= 1f) {
                SMOKE.remove(id);
                if (!RIDING.containsKey(id)) Allegiances.setFlag(p, Allegiances.SMOKE, false);
            }
        }
        Hold hold = HELD.get(id);
        if (hold != null) {
            if (!(level.getEntity(hold.entity()) instanceof LivingEntity e) || !e.isAlive() || !p.isAlive()) {
                HELD.remove(id);
            } else if (now >= hold.until()) {
                hurl(p);
            } else {
                Vec3 anchor = p.getEyePosition().add(p.getLookAngle().scale(3)).subtract(0, e.getBbHeight() / 2, 0);
                e.setDeltaMovement(anchor.subtract(e.position()).scale(0.5));
                e.hurtMarked = true;
                e.fallDistance = 0;
                if (e instanceof Mob m) m.setTarget(null);
            }
        }
        Hold ride = RIDING.get(id);
        if (ride != null) tickRide(p, level, ride, now);
        Long throne = THRONE.get(id);
        if (throne != null) {
            if (now >= throne) THRONE.remove(id);
            else if ((throne - now) % 20 == 0) kneel(p);
        }
        tickThrown(level, now);
        if (now % 20 == 0) expireBlades(p, now);
    }

    private static void tickRide(ServerPlayer p, ServerLevel level, Hold ride, long now) {
        if (!(level.getEntity(ride.entity()) instanceof LivingEntity mob) || !mob.isAlive() || !p.isAlive() || now >= ride.until()) {
            release(p);
            return;
        }
        p.teleportTo(mob.getX(), mob.getY(), mob.getZ());
        p.fallDistance = 0;
        if (mob instanceof Mob m && now % 10 == 0) {
            // It fights for its rider: whatever hostile thing is nearest that is not of the rider's side.
            LivingEntity foe = null;
            double best = 16 * 16;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16),
                    e -> e != mob && e != p && e.isAlive() && e instanceof net.minecraft.world.entity.monster.Enemy && !Kin.sameSide(p, e))) {
                double d = e.distanceToSqr(mob);
                if (d < best) {
                    best = d;
                    foe = e;
                }
            }
            if (foe != null) m.setTarget(foe);
        }
        if (now % 5 == 0) level.sendParticles(AllParticles.DEMON_SMOKE.get(), mob.getX(), mob.getEyeY(), mob.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
    }

    /** Lets go of a ridden mob (time up, it died, the rider was cured…). */
    public static void release(ServerPlayer p) {
        Hold ride = RIDING.remove(p.getUUID());
        if (ride == null) return;
        if (p.serverLevel().getEntity(ride.entity()) instanceof LivingEntity mob) {
            mob.removeEffect(MobEffects.GLOWING);
            if (mob instanceof Mob m) m.setTarget(null);
            p.serverLevel().sendParticles(AllParticles.DEMON_SMOKE.get(), mob.getX(), mob.getEyeY(), mob.getZ(), 30, 0.2, 0.4, 0.2, 0.05);
        }
        Allegiances.setFlag(p, Allegiances.POSSESSING | (SMOKE.containsKey(p.getUUID()) ? 0 : Allegiances.SMOKE), false);
    }

    private static void tickThrown(ServerLevel level, long now) {
        if (THROWN.isEmpty()) return;
        for (var e : List.copyOf(THROWN.entrySet())) {
            Thrown t = e.getValue();
            Entity thrown = level.getEntity(e.getKey());
            if (thrown == null) {
                if (now >= t.until()) THROWN.remove(e.getKey());
                continue;
            }
            if (now >= t.until() || thrown.horizontalCollision || thrown.onGround()) {
                THROWN.remove(e.getKey());
                Player by = level.getPlayerByUUID(t.thrower());
                thrown.hurt(by != null ? level.damageSources().playerAttack(by) : level.damageSources().generic(), THROW_DAMAGE);
            }
        }
    }

    /** A summoned Angel Blade is gone when its minute is up. */
    static void expireBlades(ServerPlayer p, long now) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!summonedBlade(s)) continue;
            CustomData data = s.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().getLong(SUMMONED_TAG) <= now) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        release(p);
        TRUE_FORM.remove(id);
        SMOKE.remove(id);
        HELD.remove(id);
        THRONE.remove(id);
        Allegiances.setFlag(p, Allegiances.TRUE_FORM | Allegiances.SMOKE | Allegiances.POSSESSING, false);
    }
}
