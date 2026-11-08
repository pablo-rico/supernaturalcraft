package org.papiricoh.supernaturalcraft.client.hands;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseRenderers;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.mob.BansheeEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChitterEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleOfficerEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleSoldierEntity;

/**
 * Season 11's creatures on the client (v0.42), each a rig of {@link S11Assets}, drawn only once its files have loaded (the
 * shared {@link ApocalypseRenderers.Rig}) with its glowmask lit ({@code <texture>_glowmask.png}, when the art has one). The banshee
 * is not drawn at all while she is unseen ({@link BansheeEntity#revealed}).
 */
public final class HandsRenderers {

    private HandsRenderers() {
    }

    public static class ThuleSoldier extends ApocalypseRenderers.Rig<ThuleSoldierEntity> {
        public ThuleSoldier(EntityRendererProvider.Context ctx) {
            super(ctx, S11Assets.THULE_SOLDIER.rig(), 0.5f, true);
            addRenderLayer(new ApocalypseRenderers.Glow<>(this, e -> true, -1));
        }
    }

    public static class ThuleOfficer extends ApocalypseRenderers.Rig<ThuleOfficerEntity> {
        public ThuleOfficer(EntityRendererProvider.Context ctx) {
            super(ctx, S11Assets.THULE_OFFICER.rig(), 0.5f, true);
            addRenderLayer(new ApocalypseRenderers.Glow<>(this, e -> true, -1));
        }
    }

    public static class Banshee extends ApocalypseRenderers.Rig<BansheeEntity> {
        public Banshee(EntityRendererProvider.Context ctx) {
            super(ctx, S11Assets.BANSHEE.rig(), 0f, true);
            addRenderLayer(new ApocalypseRenderers.Glow<>(this, BansheeEntity::revealed, -1));
        }

        @Override
        public void render(BansheeEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!e.revealed() && !e.isDeadOrDying()) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }
    }

    public static class Chitter extends ApocalypseRenderers.Rig<ChitterEntity> {
        public Chitter(EntityRendererProvider.Context ctx) {
            super(ctx, S11Assets.CHITTER.rig(), 0.45f, true);
            addRenderLayer(new ApocalypseRenderers.Glow<>(this, e -> true, -1));
        }
    }
}
