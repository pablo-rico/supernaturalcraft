package org.papiricoh.supernaturalcraft.client.legacy.table;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainTile;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTable;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlockEntity;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableServerHandlers;

import java.util.List;

/**
 * The war table's hologram (v0.19), drawn from its centre over all nine pieces: the land around the table (the smallest of
 * 1024, 2048 or 3200 blocks that holds the open case), a faint grid, a scan line, a dashed line to the open case, and a beam of
 * light over each case and pin (cases off the map wait at the rim with an arrow). Looking at the glass shows a cursor and the
 * label of the mark under it.
 */
public class WarTableRenderer implements BlockEntityRenderer<WarTableBlockEntity> {

    static final ResourceLocation FX = SupernaturalCraft.asResource("textures/misc/war_table_fx.png");
    /** The glass inside the brass rim, in blocks from the centre block's north-west corner. */
    static final float MIN = -1 + 2 / 16f, MAX = 2 - 2 / 16f, SIZE = MAX - MIN, MID = 0.5f;
    static final float Y = 13.05f / 16f;
    static final int[] VIEWS = {1024, 2048, 3200};
    private static final int FULL = LightTexture.FULL_BRIGHT;

    private final Font font;

    public WarTableRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(WarTableBlockEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !WarTable.intact(mc.level, table.getBlockPos())) return;
        BlockPos centre = table.getBlockPos();
        List<ClientWarTable.Marker> markers = ClientWarTable.markers(centre);
        int view = view(centre, markers);
        double cx = centre.getX() + 0.5, cz = centre.getZ() + 0.5;
        long ms = Util.getMillis();
        Projection p = new Projection(cx, cz, view);

        pose.pushPose();
        boolean any = tiles(pose, buffers, centre, p);
        grid(pose, buffers, p, any ? 0x30 : (int) (0x30 + 0x30 * Math.sin(ms / 160.0)));
        scan(pose, buffers, ms);
        for (ClientWarTable.Marker m : markers) {
            if (m.kind() == ClientWarTable.Kind.CASE_OPEN) line(pose, buffers, p, m);
        }
        for (ClientWarTable.Marker m : markers) mark(pose, buffers, p, m, ms);
        hover(table, pose, buffers, p, markers);
        pose.popPose();
    }

    /** The smallest view (blocks across) that holds the open case. */
    static int view(BlockPos centre, List<ClientWarTable.Marker> markers) {
        double far = 0;
        for (ClientWarTable.Marker m : markers) {
            if (m.kind() == ClientWarTable.Kind.CASE_OPEN) far = Math.max(Math.abs(m.x() - centre.getX()), Math.abs(m.z() - centre.getZ()));
        }
        for (int v : VIEWS) if (far <= v * 0.45) return v;
        return VIEWS[VIEWS.length - 1];
    }

    /** World columns to the glass: local x/z (from the centre block's corner) of a world x/z. */
    record Projection(double cx, double cz, int view) {
        float x(double wx) {
            return (float) (MID + (wx - cx) / view * SIZE);
        }

        float z(double wz) {
            return (float) (MID + (wz - cz) / view * SIZE);
        }

        double worldX(float lx) {
            return cx + (lx - MID) / SIZE * view;
        }

        double worldZ(float lz) {
            return cz + (lz - MID) / SIZE * view;
        }

        boolean inside(float lx, float lz) {
            return lx >= MIN && lx <= MAX && lz >= MIN && lz <= MAX;
        }
    }

    private boolean tiles(PoseStack pose, MultiBufferSource buffers, BlockPos centre, Projection p) {
        int lod = p.view() <= VIEWS[0] ? TerrainTile.NEAR : TerrainTile.FAR;
        double half = p.view() / 2.0;
        double x0 = p.cx() - half, z0 = p.cz() - half, x1 = p.cx() + half, z1 = p.cz() + half;
        long[] keys = TerrainTile.covering(lod, (int) Math.floor(x0), (int) Math.floor(z0), (int) Math.ceil(x1), (int) Math.ceil(z1));
        ClientWarTable.want(centre, keys);
        boolean any = false;
        int span = TerrainTile.span(lod);
        for (long key : keys) {
            ResourceLocation tex = ClientWarTable.texture(key);
            if (tex == null) continue;
            any = true;
            double ox = TerrainTile.origin(lod, TerrainTile.tx(key)), oz = TerrainTile.origin(lod, TerrainTile.tz(key));
            double ax = Math.max(ox, x0), az = Math.max(oz, z0), bx = Math.min(ox + span, x1), bz = Math.min(oz + span, z1);
            if (ax >= bx || az >= bz) continue;
            float u0 = (float) ((ax - ox) / span), v0 = (float) ((az - oz) / span), u1 = (float) ((bx - ox) / span), v1 = (float) ((bz - oz) / span);
            quad(buffers.getBuffer(RenderType.entityTranslucentEmissive(tex)), pose, p.x(ax), p.z(az), p.x(bx), p.z(bz), Y, u0, v0, u1, v1, 0xFFFFFFFF);
        }
        return any;
    }

    private void grid(PoseStack pose, MultiBufferSource buffers, Projection p, int alpha) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(FX));
        int step = p.view() <= 1024 ? 128 : 256;
        int colour = (Math.max(0, Math.min(255, alpha)) << 24) | 0x40E0FF;
        double half = p.view() / 2.0;
        for (double x = Math.ceil((p.cx() - half) / step) * step; x <= p.cx() + half; x += step) {
            float lx = p.x(x);
            quad(vc, pose, lx - 0.004f, MIN, lx + 0.004f, MAX, Y + 0.002f, SOLID_U0, 0, SOLID_U1, 1, colour);
        }
        for (double z = Math.ceil((p.cz() - half) / step) * step; z <= p.cz() + half; z += step) {
            float lz = p.z(z);
            quad(vc, pose, MIN, lz - 0.004f, MAX, lz + 0.004f, Y + 0.002f, SOLID_U0, 0, SOLID_U1, 1, colour);
        }
    }

    private void scan(PoseStack pose, MultiBufferSource buffers, long ms) {
        float f = (ms % 5000) / 5000f;
        float z = MIN + f * SIZE;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(FX));
        quad(vc, pose, MIN, Math.max(MIN, z - 0.25f), MAX, z, Y + 0.008f, SCAN_U0, 0, SCAN_U1, 1, 0xA0FFFFFF);
    }

    private void line(PoseStack pose, MultiBufferSource buffers, Projection p, ClientWarTable.Marker m) {
        float x0 = MID, z0 = MID;
        float x1 = clamp(p.x(m.x() + 0.5)), z1 = clamp(p.z(m.z() + 0.5));
        float len = (float) Math.hypot(x1 - x0, z1 - z0);
        if (len < 0.01f) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(FX));
        float nx = -(z1 - z0) / len * 0.012f, nz = (x1 - x0) / len * 0.012f;
        int dashes = (int) (len / 0.08f);
        for (int i = 0; i < dashes; i += 2) {
            float a = i / (float) dashes, b = Math.min(1, (i + 1) / (float) dashes);
            float ax = x0 + (x1 - x0) * a, az = z0 + (z1 - z0) * a, bx = x0 + (x1 - x0) * b, bz = z0 + (z1 - z0) * b;
            Matrix4f mat = pose.last().pose();
            float y = Y + 0.004f;
            vertex(vc, pose, mat, ax + nx, y, az + nz, SOLID_U0, 0, 0xD0FF4A3A);
            vertex(vc, pose, mat, bx + nx, y, bz + nz, SOLID_U1, 0, 0xD0FF4A3A);
            vertex(vc, pose, mat, bx - nx, y, bz - nz, SOLID_U1, 1, 0xD0FF4A3A);
            vertex(vc, pose, mat, ax - nx, y, az - nz, SOLID_U0, 1, 0xD0FF4A3A);
        }
    }

    private void mark(PoseStack pose, MultiBufferSource buffers, Projection p, ClientWarTable.Marker m, long ms) {
        float lx = p.x(m.x() + 0.5), lz = p.z(m.z() + 0.5);
        int colour = ClientWarTable.colour(m.kind());
        if (!p.inside(lx, lz)) {
            if (m.kind() != ClientWarTable.Kind.CASE_OPEN) return;
            float ex = clamp(lx), ez = clamp(lz);
            float angle = (float) Math.toDegrees(Math.atan2(lx - MID, -(lz - MID)));
            icon(pose, buffers, WarTableScreen.ICON_EDGE, ex, ez, 0.16f, angle, colour);
            return;
        }
        switch (m.kind()) {
            case TABLE -> icon(pose, buffers, WarTableScreen.ICON_TABLE, lx, lz, 0.14f, 0, 0xFFFFFFFF);
            case CASE_OPEN -> {
                float pulse = (ms % 1500) / 1500f;
                icon(pose, buffers, WarTableScreen.ICON_RING, lx, lz, 0.14f + pulse * 0.25f, 0, ((int) ((1 - pulse) * 220) << 24) | (colour & 0xFFFFFF));
                icon(pose, buffers, WarTableScreen.ICON_CASE, lx, lz, 0.16f, 0, 0xFFFFFFFF);
                beam(pose, buffers, lx, lz, 0.9f + 0.1f * (float) Math.sin(ms / 300.0), colour);
            }
            case CASE_SOLVED, CASE_LOST -> {
                icon(pose, buffers, m.kind() == ClientWarTable.Kind.CASE_SOLVED ? WarTableScreen.ICON_SOLVED : WarTableScreen.ICON_LOST, lx, lz, 0.12f, 0, 0xFFFFFFFF);
                beam(pose, buffers, lx, lz, 0.2f, colour);
            }
            case PIN -> {
                icon(pose, buffers, WarTableScreen.ICON_PIN, lx, lz, 0.13f, 0, 0xFFFFFFFF);
                beam(pose, buffers, lx, lz, 0.4f, colour);
            }
            case OFFICER_PIT, OFFICER_HOST, SEAT_PIT, SEAT_HOST, SIGHTING -> {
                boolean chosen = ClientWarTable.highlighted(m) && m.kind() != ClientWarTable.Kind.SIGHTING;
                boolean seat = m.kind() == ClientWarTable.Kind.SEAT_PIT || m.kind() == ClientWarTable.Kind.SEAT_HOST;
                icon(pose, buffers, seat ? WarTableScreen.ICON_SEAT : WarTableScreen.ICON_OFFICER, lx, lz, chosen ? 0.14f : 0.1f, 0,
                        chosen ? colour : (0x90 << 24) | (colour & 0xFFFFFF));
                if (chosen) beam(pose, buffers, lx, lz, 0.7f, colour);
            }
        }
    }

    /** Where the hunter's eye meets the glass: a cursor, and the label of the mark under it (or the place's coordinates). */
    private void hover(WarTableBlockEntity table, PoseStack pose, MultiBufferSource buffers, Projection p, List<ClientWarTable.Marker> markers) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || hit.getDirection() != Direction.UP) return;
        BlockPos at = hit.getBlockPos();
        if (!table.getBlockPos().equals(WarTable.centre(at, mc.level.getBlockState(at)))) return;
        Vec3 loc = hit.getLocation().subtract(Vec3.atLowerCornerWithOffset(table.getBlockPos(), 0, 0, 0));
        float lx = (float) loc.x, lz = (float) loc.z;
        if (!p.inside(lx, lz)) return;
        ClientWarTable.Marker best = null;
        float bestD = 0.12f * 0.12f;
        for (ClientWarTable.Marker m : markers) {
            float dx = p.x(m.x() + 0.5) - lx, dz = p.z(m.z() + 0.5) - lz;
            if (dx * dx + dz * dz < bestD) {
                bestD = dx * dx + dz * dz;
                best = m;
            }
        }
        if (best != null) {
            float mx = p.x(best.x() + 0.5), mz = p.z(best.z() + 0.5);
            icon(pose, buffers, WarTableScreen.ICON_RING, mx, mz, 0.22f, 0, 0xFFFFFFFF);
            label(pose, buffers, mx, Y + 0.45f, mz, best.label(), ClientWarTable.colour(best.kind()));
            if (best.kind() != ClientWarTable.Kind.TABLE) {
                double bearing = CompassMath.bearing(p.cx(), p.cz(), best.x(), best.z());
                int dist = (int) Math.hypot(best.x() - p.cx(), best.z() - p.cz());
                label(pose, buffers, mx, Y + 0.36f, mz, Component.translatable("gui.supernaturalcraft.war_table.distance", dist,
                        Component.translatable("screen.supernaturalcraft.case.dir." + CompassMath.point(bearing))), WarTableScreen.MUTED);
            }
        } else {
            icon(pose, buffers, WarTableScreen.ICON_RING, lx, lz, 0.1f, 0, 0xC0FFFFFF);
            label(pose, buffers, lx, Y + 0.25f, lz, Component.translatable("gui.supernaturalcraft.war_table.coords",
                    (int) Math.floor(p.worldX(lx)), (int) Math.floor(p.worldZ(lz))), WarTableScreen.TEXT);
        }
    }

    private void label(PoseStack pose, MultiBufferSource buffers, float x, float y, float z, Component text, int argb) {
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.0125f, -0.0125f, 0.0125f);
        float w = -font.width(text) / 2f;
        font.drawInBatch(text, w, 0, argb, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x60000000, FULL);
        pose.popPose();
    }

    private void icon(PoseStack pose, MultiBufferSource buffers, int icon, float x, float z, float size, float angle, int argb) {
        pose.pushPose();
        pose.translate(x, Y + 0.006f, z);
        if (angle != 0) pose.mulPose(Axis.YN.rotationDegrees(angle));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(WarTableScreen.ICONS));
        float h = size / 2;
        quad(vc, pose, -h, -h, h, h, 0, icon / (float) WarTableScreen.ICON_COUNT, 0, (icon + 1) / (float) WarTableScreen.ICON_COUNT, 1, argb);
        pose.popPose();
    }

    private void beam(PoseStack pose, MultiBufferSource buffers, float x, float z, float height, int argb) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(FX));
        Matrix4f m = pose.last().pose();
        float w = 0.025f, y0 = Y, y1 = Y + height;
        int c = (0xB0 << 24) | (argb & 0xFFFFFF);
        for (int i = 0; i < 2; i++) {
            float dx = i == 0 ? w : 0, dz = i == 0 ? 0 : w;
            vertex(vc, pose, m, x - dx, y0, z - dz, BEAM_U0, 1, c);
            vertex(vc, pose, m, x + dx, y0, z + dz, BEAM_U1, 1, c);
            vertex(vc, pose, m, x + dx, y1, z + dz, BEAM_U1, 0, c);
            vertex(vc, pose, m, x - dx, y1, z - dz, BEAM_U0, 0, c);
        }
    }

    /** Strips of the fx sheet: a beam fading upwards, a scan line fading back, solid white. */
    static final float BEAM_U0 = 0, BEAM_U1 = 0.25f, SCAN_U0 = 0.25f, SCAN_U1 = 0.5f, SOLID_U0 = 0.75f, SOLID_U1 = 1f;

    private static void quad(VertexConsumer vc, PoseStack pose, float x0, float z0, float x1, float z1, float y,
                             float u0, float v0, float u1, float v1, int argb) {
        Matrix4f m = pose.last().pose();
        vertex(vc, pose, m, x0, y, z0, u0, v0, argb);
        vertex(vc, pose, m, x0, y, z1, u0, v1, argb);
        vertex(vc, pose, m, x1, y, z1, u1, v1, argb);
        vertex(vc, pose, m, x1, y, z0, u1, v0, argb);
    }

    private static void vertex(VertexConsumer vc, PoseStack pose, Matrix4f m, float x, float y, float z, float u, float v, int argb) {
        vc.addVertex(m, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(pose.last(), 0, 1, 0);
    }

    private static float clamp(float v) {
        return Math.max(MIN + 0.05f, Math.min(MAX - 0.05f, v));
    }

    @Override
    public boolean shouldRenderOffScreen(WarTableBlockEntity table) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return (int) WarTableServerHandlers.SIGHT;
    }

    @Override
    public AABB getRenderBoundingBox(WarTableBlockEntity table) {
        return table.renderBox();
    }
}
