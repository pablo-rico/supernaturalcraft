package org.papiricoh.supernaturalcraft.entity.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.magic.spell.form.SpellForms;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.UUID;

/**
 * The Ward form: a glowing circle on the ground. Every second it applies its spell to whatever
 * stands inside (helpful effects to friends, harmful ones to the rest), and it unmakes hostile
 * projectiles that cross it. Standing in one is also the only way to live through Lucifer's
 * Archangel's Smite.
 */
public class WardEntity extends Entity {

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(WardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(WardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(WardEntity.class, EntityDataSerializers.INT);
    /** Its school's ordinal (synced so the client can tint glyphs and linger zones). */
    private static final EntityDataAccessor<Byte> SCHOOL = SynchedEntityData.defineId(WardEntity.class, EntityDataSerializers.BYTE);
    public static final float HEIGHT = 3.0f;

    private SpellCarrier carrier = new SpellCarrier(Spell.EMPTY, 1, 1, 3, 0xF2E6B0);
    private UUID ownerId;
    /** Whether its owner is a player: one who logs out leaves it standing (inert) until its time is up. */
    private boolean ownerIsPlayer;
    /** Wards stop hostile projectiles; lingering zones (from a catalysed Burst) only pulse. */
    private boolean shielding = true;
    /**
     * A glyph (v0.21, the {@code glyph} form): a small sigil on the floor that does nothing until something hostile to its owner
     * steps on it; then it applies its spell to whatever stands on it and is spent.
     */
    private boolean trap = false;

    public WardEntity(EntityType<? extends WardEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public WardEntity(Level level, LivingEntity owner, Spell spell, SpellContext ctx, int lifetime) {
        this(AllEntities.WARD.get(), level, owner, SpellCarrier.of(spell, ctx), lifetime, true);
    }

    /** A zone that keeps re-applying a spell but stops nothing. */
    public static WardEntity lingering(Level level, LivingEntity owner, SpellCarrier carrier, int lifetime) {
        return new WardEntity(AllEntities.LINGERING_ZONE.get(), level, owner, carrier, lifetime, false);
    }

    /**
     * A glyph trap (v0.21): planted at {@code pos}-level by the {@code glyph} form, lasting {@code lifetime} ticks. At most
     * {@link #MAX_GLYPHS} per owner: planting another removes their oldest.
     */
    public static WardEntity glyph(Level level, LivingEntity owner, SpellCarrier carrier, int lifetime) {
        WardEntity glyph = new WardEntity(AllEntities.SIGIL_GLYPH.get(), level, owner, carrier, lifetime, false);
        glyph.trap = true;
        glyph.entityData.set(RADIUS, GLYPH_RADIUS + Math.max(0, carrier.area()));
        return glyph;
    }

    /** How many glyphs one caster may have down at once, and how wide one is (blocks). */
    public static final int MAX_GLYPHS = 3;
    public static final float GLYPH_RADIUS = 1.25f;

    public boolean isTrap() {
        return trap;
    }

    public java.util.UUID ownerId() {
        return ownerId;
    }

    private WardEntity(EntityType<? extends WardEntity> type, Level level, LivingEntity owner, SpellCarrier carrier, int lifetime,
                       boolean shielding) {
        this(type, level);
        this.shielding = shielding;
        this.ownerId = owner.getUUID();
        this.ownerIsPlayer = owner instanceof net.minecraft.world.entity.player.Player;
        this.carrier = carrier;
        entityData.set(RADIUS, Math.max(1.5f, carrier.area()));
        entityData.set(COLOR, carrier.color());
        entityData.set(LIFETIME, lifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 3.0f);
        builder.define(COLOR, 0xF2E6B0);
        builder.define(LIFETIME, 200);
        builder.define(SCHOOL, (byte) School.NONE.ordinal());
    }

    public void setSchool(School school) {
        entityData.set(SCHOOL, (byte) (school == null ? School.NONE : school).ordinal());
    }

    /** Whose colours it is drawn in (v0.21; synced, so it holds on the client too). */
    public School getSchool() {
        int i = entityData.get(SCHOOL);
        School[] all = School.values();
        return i >= 0 && i < all.length ? all[i] : School.NONE;
    }

    private void pulse(int state) {
        if (level() instanceof ServerLevel server) {
            MagicFx.ward(server, getSchool(), this, position(), getRadius(), state, SpellForms.WARD_PULSE_TICKS);
        }
    }

    public float getRadius() {
        return entityData.get(RADIUS);
    }

    public int getColor() {
        return entityData.get(COLOR);
    }

    public int getLifetime() {
        return entityData.get(LIFETIME);
    }

    public boolean contains(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        double r = getRadius();
        return dx * dx + dz * dz <= r * r && e.getY() >= getY() - 1 && e.getY() <= getY() + HEIGHT;
    }

    /** Whether the point {@code p} lies inside the circle. */
    public boolean covers(net.minecraft.world.phys.Vec3 p) {
        double dx = p.x - getX(), dz = p.z - getZ();
        double r = getRadius();
        return dx * dx + dz * dz <= r * r && p.y >= getY() - 1 && p.y <= getY() + HEIGHT;
    }

    /**
     * The shielding ward of {@code owner} that covers {@code p} (v0.21: a Ward raised inside a ritual circle shields the rite from
     * its backlash), or null.
     */
    public static @org.jetbrains.annotations.Nullable WardEntity shelteringWard(ServerLevel level, java.util.UUID owner, net.minecraft.world.phys.Vec3 p) {
        if (owner == null) return null;
        for (WardEntity ward : level.getEntitiesOfClass(WardEntity.class, new AABB(p, p).inflate(12))) {
            if (ward.shielding && !ward.trap && owner.equals(ward.ownerId) && ward.covers(p)) return ward;
        }
        return null;
    }

    /** True if {@code entity} stands inside a ward cast by itself or a friend. */
    public static boolean isSheltered(LivingEntity entity) {
        for (WardEntity ward : entity.level().getEntitiesOfClass(WardEntity.class, entity.getBoundingBox().inflate(12))) {
            if (ward.shielding && ward.contains(entity) && ward.getOwner() instanceof LivingEntity owner && ResolvedSpell.isFriend(owner, entity)) {
                return true;
            }
        }
        return false;
    }

    /** Whether its owner is a player who is offline (the ward waits for them). */
    private boolean ownerAway() {
        return ownerIsPlayer && ownerId != null && level() instanceof ServerLevel server
                && server.getServer().getPlayerList().getPlayer(ownerId) == null;
    }

    public Entity getOwner() {
        return ownerId != null && level() instanceof ServerLevel server ? server.getEntity(ownerId) : null;
    }

    @Override
    public void tick() {
        super.tick();
        // The circle is drawn by the client (WardRenderer and the WARD_PULSE effects, v0.21).
        if (level().isClientSide) return;
        if (tickCount >= getLifetime()) {
            pulse(MagicFxPayload.WARD_BROKEN);
            discard();
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner)) {
            // A player who logged out leaves it standing, inert, until its time is up (they may come back to it); one who is
            // online elsewhere (another dimension), or a caster that is gone, takes it with them.
            if (!ownerAway()) {
                pulse(MagicFxPayload.WARD_BROKEN);
                discard();
            }
            return;
        }
        ServerLevel server = (ServerLevel) level();
        double r = getRadius();
        AABB box = new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + HEIGHT, getZ() + r);
        if (trap) {
            tickTrap(server, owner, box);
            return;
        }
        boolean struck = false;
        for (Projectile p : shielding ? server.getEntitiesOfClass(Projectile.class, box.inflate(1), this::contains) : List.<Projectile>of()) {
            Entity shooter = p.getOwner();
            if (!(p instanceof SigilBolt) && (shooter == null || !ResolvedSpell.isFriend(owner, shooter))) {
                p.discard();
                struck = true;
            }
        }
        if (struck) pulse(MagicFxPayload.WARD_HIT);
        if (tickCount % 20 == 0) {
            ResolvedSpell spell = ResolvedSpell.resolve(server.registryAccess(),
                    owner instanceof net.minecraft.world.entity.player.Player pl ? org.papiricoh.supernaturalcraft.legacy.Legacies.archive(pl) : null, carrier.spell());
            if (spell != null) {
                SpellContext ctx = new SpellContext(server, owner);
                carrier.applyTo(ctx);
                ctx.impactBudget = 0;
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box, this::contains)) {
                    spell.applyTo(ctx, e);
                }
            }
            if (tickCount % 60 == 0) {
                playSound(AllSounds.RITUAL_CHANNEL.get(), 0.4f, 1.6f);
            }
        }
    }

    /** A glyph springs when something hostile to its owner stands on it: the spell goes off on all on it, once. */
    private void tickTrap(ServerLevel server, LivingEntity owner, AABB box) {
        if (tickCount % 4 != 0) return;
        List<LivingEntity> on = server.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator() && contains(e));
        if (on.stream().noneMatch(e -> !ResolvedSpell.isFriend(owner, e))) return;
        ResolvedSpell spell = ResolvedSpell.resolve(server.registryAccess(),
                owner instanceof net.minecraft.world.entity.player.Player pl ? org.papiricoh.supernaturalcraft.legacy.Legacies.archive(pl) : null, carrier.spell());
        if (spell != null) {
            SpellContext ctx = new SpellContext(server, owner);
            carrier.applyTo(ctx);
            ctx.lingerTicks = spell.lingerTicks();
            for (LivingEntity e : on) spell.applyTo(ctx, e);
        }
        pulse(MagicFxPayload.WARD_HIT);
        discard();
    }

    /** Removes {@code owner}'s oldest glyphs so that one more leaves them at most {@link #MAX_GLYPHS}. */
    public static void makeRoomForGlyph(ServerLevel level, LivingEntity owner) {
        List<WardEntity> mine = new java.util.ArrayList<>();
        for (Entity e : level.getEntities(AllEntities.SIGIL_GLYPH.get(), w -> owner.getUUID().equals(w.ownerId))) {
            if (e instanceof WardEntity w && w.isAlive()) mine.add(w);
        }
        mine.sort(java.util.Comparator.comparingInt((WardEntity w) -> w.tickCount).reversed());
        for (int i = 0; i <= mine.size() - MAX_GLYPHS; i++) mine.get(i).discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        carrier = SpellCarrier.load(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        entityData.set(RADIUS, tag.getFloat("Radius"));
        entityData.set(COLOR, carrier.color());
        entityData.set(LIFETIME, tag.getInt("Lifetime"));
        tickCount = tag.getInt("Age");
        shielding = !tag.contains("Shielding") || tag.getBoolean("Shielding");
        setSchool(School.fromId(tag.getString("School")));
        ownerIsPlayer = tag.getBoolean("OwnerIsPlayer");
        trap = tag.getBoolean("Trap");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        carrier.save(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putFloat("Radius", getRadius());
        tag.putInt("Lifetime", getLifetime());
        tag.putInt("Age", tickCount);
        tag.putBoolean("Shielding", shielding);
        tag.putString("School", getSchool().id());
        tag.putBoolean("OwnerIsPlayer", ownerIsPlayer);
        tag.putBoolean("Trap", trap);
    }
}
