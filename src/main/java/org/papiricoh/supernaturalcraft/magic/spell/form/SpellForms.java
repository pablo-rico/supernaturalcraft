package org.papiricoh.supernaturalcraft.magic.spell.form;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;
import org.papiricoh.supernaturalcraft.entity.magic.SpellCarrier;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehavior;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehaviors;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/** The four ways a spell can travel. */
public final class SpellForms {

    private SpellForms() {
    }

    /** A split Bolt's sigils each carry this share of the potency. */
    public static final float SPLIT_POTENCY = 0.6f, SPLIT_DEGREES = 8f, LINGER_POTENCY = 0.4f, CHAIN_RANGE = 4f;

    /** Touch, catalysed: the spell jumps on to the nearest other foes. */
    static void chain(SpellContext ctx, ResolvedSpell spell, Entity from) {
        int left = ctx.traits.touchChain();
        if (left <= 0) return;
        List<LivingEntity> near = new ArrayList<>(ctx.level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(CHAIN_RANGE),
                e -> e != from && e != ctx.caster && !ResolvedSpell.isFriend(ctx.caster, e)));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        Vec3 a = from.position().add(0, from.getBbHeight() / 2, 0);
        for (LivingEntity e : near.subList(0, Math.min(left, near.size()))) {
            spell.applyTo(ctx, e);
            Vec3 b = e.position().add(0, e.getBbHeight() / 2, 0);
            for (int i = 0; i <= 8; i++) {
                Vec3 p = a.lerp(b, i / 8.0);
                ctx.level.sendParticles(dust(ctx.color, 0.8f), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            a = b;
        }
    }

    public static DustParticleOptions dust(int color, float size) {
        return new DustParticleOptions(new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f), size);
    }

    /** Whatever is in arm's reach: an entity, else a block, else (for purely helpful spells) yourself. */
    public static final SpellBehavior.Form TOUCH = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.range = sigil.param("range", 4.0f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            LivingEntity caster = ctx.caster;
            Vec3 from = caster.getEyePosition();
            Vec3 to = from.add(caster.getLookAngle().scale(ctx.range));
            BlockHitResult block = ctx.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
            Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(caster, from, end,
                    caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(ctx.range)).inflate(1.0),
                    e -> e.isPickable() && !e.isSpectator(), ctx.range * ctx.range);
            Vec3 at;
            if (entity != null) {
                at = entity.getLocation();
                spell.applyTo(ctx, entity.getEntity());
                if (ctx.area > 0) splash(ctx, spell, at, entity.getEntity());
                chain(ctx, spell, entity.getEntity());
            } else if (block.getType() == HitResult.Type.BLOCK) {
                at = block.getLocation();
                spell.applyToBlock(ctx, block.getBlockPos(), block.getDirection());
                if (ctx.area > 0) splash(ctx, spell, at, null);
            } else if (spell.allBeneficial()) {
                at = caster.position().add(0, 1, 0);
                spell.applyTo(ctx, caster);
            } else {
                at = end;
            }
            ctx.level.sendParticles(AllParticles.SIGIL.get(), at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.02);
            ctx.level.sendParticles(dust(ctx.color, 1.0f), at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0);
        }
    };

    /** A thrown sigil that flies straight and breaks on the first thing it meets. */
    public static final SpellBehavior.Form BOLT = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.range = sigil.param("speed", 1.6f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            int n = Math.max(1, ctx.traits.boltSplit());
            SpellContext each = ctx.copy();
            if (n > 1) each.potency *= SPLIT_POTENCY;
            each.area += ctx.traits.boltBurstArea();
            Vec3 look = ctx.caster.getLookAngle();
            for (int i = 0; i < n; i++) {
                double offset = Math.toRadians((i - (n - 1) / 2.0) * SPLIT_DEGREES);
                Vec3 dir = new Vec3(look.x * Math.cos(offset) - look.z * Math.sin(offset), look.y,
                        look.x * Math.sin(offset) + look.z * Math.cos(offset));
                SigilBolt bolt = new SigilBolt(ctx.level, ctx.caster, spell.spell(), each);
                bolt.setPos(ctx.caster.getX() + dir.x * 0.6, ctx.caster.getEyeY() - 0.15, ctx.caster.getZ() + dir.z * 0.6);
                bolt.shoot(dir.x, dir.y, dir.z, ctx.range, 0.0f);
                ctx.level.addFreshEntity(bolt);
            }
        }
    };

    /** A pulse outward from the caster. */
    public static final SpellBehavior.Form BURST = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.area = sigil.param("radius", 3.5f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            Vec3 c = ctx.caster.position();
            splash(ctx, spell, c.add(0, 1, 0), null);
            spell.applyTo(ctx, ctx.caster);
            int points = (int) (ctx.area * 10);
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2 * i / points;
                ctx.level.sendParticles(dust(ctx.color, 1.2f), c.x + Math.cos(a) * ctx.area, c.y + 0.2,
                        c.z + Math.sin(a) * ctx.area, 1, 0, 0.05, 0, 0);
            }
            ctx.level.sendParticles(AllParticles.SIGIL.get(), c.x, c.y + 1, c.z, 12, ctx.area / 3, 0.3, ctx.area / 3, 0.02);
            if (ctx.traits.burstLingerTicks() > 0) {
                WardEntity zone = WardEntity.lingering(ctx.level, ctx.caster,
                        SpellCarrier.of(spell.spell(), ctx).withPotency(ctx.potency * LINGER_POTENCY), ctx.traits.burstLingerTicks());
                zone.setPos(c.x, c.y, c.z);
                ctx.level.addFreshEntity(zone);
            }
        }
    };

    /** A circle drawn at your feet that keeps working for a while and stops hostile projectiles. */
    public static final SpellBehavior.Form WARD = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.area = sigil.param("radius", 3.0f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            float scale = ctx.traits.wardScale();
            int duration = Math.round(ctx.duration(Math.round(spell.form().param("duration", 200f))) * scale);
            ctx.area *= scale;
            WardEntity ward = new WardEntity(ctx.level, ctx.caster, spell.spell(), ctx, duration);
            ward.setPos(ctx.caster.getX(), ctx.caster.getY(), ctx.caster.getZ());
            ctx.level.addFreshEntity(ward);
        }
    };

    /** Applies the spell to everything (except {@code skip}) within {@code ctx.area} of {@code at}. */
    public static void splash(SpellContext ctx, ResolvedSpell spell, Vec3 at, Entity skip) {
        double r = ctx.area;
        for (Entity e : ctx.level.getEntities(ctx.caster, new AABB(at, at).inflate(r))) {
            if (e != skip && e instanceof LivingEntity && e.position().distanceToSqr(at) <= r * r * 1.4) {
                spell.applyTo(ctx, e);
            }
        }
    }

    public static void registerAll() {
        SpellBehaviors.register(SupernaturalCraft.asResource("touch"), TOUCH);
        SpellBehaviors.register(SupernaturalCraft.asResource("bolt"), BOLT);
        SpellBehaviors.register(SupernaturalCraft.asResource("burst"), BURST);
        SpellBehaviors.register(SupernaturalCraft.asResource("ward"), WARD);
    }
}
