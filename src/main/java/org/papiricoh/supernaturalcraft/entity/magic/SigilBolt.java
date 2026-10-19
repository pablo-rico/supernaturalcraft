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

/** The Bolt form in flight. Weightless; breaks on the first entity or block it meets. */
public class SigilBolt extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SigilBolt.class, EntityDataSerializers.INT);
    /** Ticks a bolt flies before it fades. */
    public static final int MAX_AGE = 100;

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
        // The trail is drawn by the client from the BOLT_TRAIL payload (v0.21).
        if (!level().isClientSide && tickCount > MAX_AGE) {
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
            ctx.lingerTicks = spell.lingerTicks();
            int budget = ctx.impactBudget;
            if (result instanceof EntityHitResult ehr) {
                spell.applyTo(ctx, ehr.getEntity());
                if (ctx.area > 0) SpellForms.splash(ctx, spell, ehr.getLocation(), ehr.getEntity());
                if (pierced.size() < carrier.traits().boltPierce()) {
                    // A piercing bolt marks what it went through and keeps flying.
                    pierced.add(ehr.getEntity().getUUID());
                    return;
                }
            } else if (result instanceof BlockHitResult bhr) {
                spell.applyToBlock(ctx, bhr.getBlockPos(), bhr.getDirection());
                if (ctx.area > 0) SpellForms.splash(ctx, spell, bhr.getLocation(), null);
            }
            // Nothing took hold: the bolt still breaks visibly (a generic burst in the spell's colours).
            if (ctx.impactBudget == budget) {
                org.papiricoh.supernaturalcraft.magic.fx.MagicFx.impact(server, spell.school(), position(), position().subtract(getDeltaMovement()),
                        -1, 0, null, ResolvedSpell.IMPACT_TICKS);
            }
        }
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
