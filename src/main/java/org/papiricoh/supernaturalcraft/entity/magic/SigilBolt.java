package org.papiricoh.supernaturalcraft.entity.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.magic.spell.form.SpellForms;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/** The Bolt form in flight. Weightless; breaks on the first entity or block it meets. */
public class SigilBolt extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SigilBolt.class, EntityDataSerializers.INT);
    private static final int MAX_AGE = 100;

    private SpellCarrier carrier = new SpellCarrier(Spell.EMPTY, 1, 1, 0, 0xF2E6B0);
    private final java.util.Set<java.util.UUID> pierced = new java.util.HashSet<>();

    public SigilBolt(EntityType<? extends SigilBolt> type, Level level) {
        super(type, level);
    }

    public SigilBolt(Level level, LivingEntity caster, Spell spell, SpellContext ctx) {
        this(level, caster, SpellCarrier.of(spell, ctx));
    }

    public SigilBolt(Level level, LivingEntity caster, SpellCarrier carrier) {
        super(AllEntities.SIGIL_BOLT.get(), caster, level);
        this.carrier = carrier;
        entityData.set(COLOR, carrier.color());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xF2E6B0);
    }

    public int getColor() {
        return entityData.get(COLOR);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(SpellForms.dust(getColor(), 1.0f), getX(), getY(), getZ(), 0, 0, 0);
            if (tickCount % 3 == 0) {
                level().addParticle(AllParticles.SIGIL.get(), getX(), getY(), getZ(), 0, 0.01, 0);
            }
        } else if (tickCount > MAX_AGE) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && !pierced.contains(target.getUUID());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server) || !(getOwner() instanceof LivingEntity caster)) {
            if (!level().isClientSide) discard();
            return;
        }
        ResolvedSpell spell = ResolvedSpell.resolve(server.registryAccess(),
                    caster instanceof net.minecraft.world.entity.player.Player pl ? org.papiricoh.supernaturalcraft.legacy.Legacies.archive(pl) : null, carrier.spell());
        if (spell != null) {
            SpellContext ctx = new SpellContext(server, caster);
            carrier.applyTo(ctx);
            if (result instanceof EntityHitResult ehr) {
                spell.applyTo(ctx, ehr.getEntity());
                if (ctx.area > 0) SpellForms.splash(ctx, spell, ehr.getLocation(), ehr.getEntity());
                if (pierced.size() < carrier.traits().boltPierce()) {
                    // A piercing bolt marks what it went through and keeps flying.
                    pierced.add(ehr.getEntity().getUUID());
                    server.sendParticles(AllParticles.SIGIL.get(), getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.02);
                    return;
                }
            } else if (result instanceof BlockHitResult bhr) {
                spell.applyToBlock(ctx, bhr.getBlockPos(), bhr.getDirection());
                if (ctx.area > 0) SpellForms.splash(ctx, spell, bhr.getLocation(), null);
            }
        }
        server.sendParticles(AllParticles.SIGIL.get(), getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.05);
        server.sendParticles(SpellForms.dust(getColor(), 1.4f), getX(), getY(), getZ(), 16, 0.3, 0.3, 0.3, 0);
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        carrier.save(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        carrier = SpellCarrier.load(tag);
        entityData.set(COLOR, carrier.color());
    }
}
