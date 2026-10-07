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
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
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
    public static final float HEIGHT = 3.0f;

    private SpellCarrier carrier = new SpellCarrier(Spell.EMPTY, 1, 1, 3, 0xF2E6B0);
    private UUID ownerId;
    /** Wards stop hostile projectiles; lingering zones (from a catalysed Burst) only pulse. */
    private boolean shielding = true;

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

    private WardEntity(EntityType<? extends WardEntity> type, Level level, LivingEntity owner, SpellCarrier carrier, int lifetime,
                       boolean shielding) {
        this(type, level);
        this.shielding = shielding;
        this.ownerId = owner.getUUID();
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

    /** True if {@code entity} stands inside a ward cast by itself or a friend. */
    public static boolean isSheltered(LivingEntity entity) {
        for (WardEntity ward : entity.level().getEntitiesOfClass(WardEntity.class, entity.getBoundingBox().inflate(12))) {
            if (ward.shielding && ward.contains(entity) && ward.getOwner() instanceof LivingEntity owner && ResolvedSpell.isFriend(owner, entity)) {
                return true;
            }
        }
        return false;
    }

    public Entity getOwner() {
        return ownerId != null && level() instanceof ServerLevel server ? server.getEntity(ownerId) : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 4 == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = getRadius();
                level().addParticle(AllParticles.SIGIL.get(), getX() + Math.cos(a) * r, getY() + 0.1, getZ() + Math.sin(a) * r, 0, 0.03, 0);
            }
            return;
        }
        if (tickCount >= getLifetime() || !(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }
        ServerLevel server = (ServerLevel) level();
        double r = getRadius();
        AABB box = new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + HEIGHT, getZ() + r);
        for (Projectile p : shielding ? server.getEntitiesOfClass(Projectile.class, box.inflate(1), this::contains) : List.<Projectile>of()) {
            Entity shooter = p.getOwner();
            if (!(p instanceof SigilBolt) && (shooter == null || !ResolvedSpell.isFriend(owner, shooter))) {
                server.sendParticles(SpellForms.dust(getColor(), 1.5f), p.getX(), p.getY(), p.getZ(), 12, 0.2, 0.2, 0.2, 0);
                p.discard();
            }
        }
        if (tickCount % 20 == 0) {
            ResolvedSpell spell = ResolvedSpell.resolve(server.registryAccess(), carrier.spell());
            if (spell != null) {
                SpellContext ctx = new SpellContext(server, owner);
                carrier.applyTo(ctx);
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box, this::contains)) {
                    spell.applyTo(ctx, e);
                }
            }
            if (tickCount % 60 == 0) {
                playSound(AllSounds.RITUAL_CHANNEL.get(), 0.4f, 1.6f);
            }
        }
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        carrier.save(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putFloat("Radius", getRadius());
        tag.putInt("Lifetime", getLifetime());
        tag.putInt("Age", tickCount);
        tag.putBoolean("Shielding", shielding);
    }
}
