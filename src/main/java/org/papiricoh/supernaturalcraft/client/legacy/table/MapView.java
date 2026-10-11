package org.papiricoh.supernaturalcraft.client.legacy.table;

import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainTile;

/**
 * The war table screen's window on the world (v0.19), pure: a screen rectangle showing {@link #width} blocks around a world
 * centre, panned and zoomed about the cursor, never wandering further from its table than the table maps.
 */
public final class MapView {

    public static final double MIN_WIDTH = 128, MAX_WIDTH = 3200;

    private final double originX, originZ;
    private double left, top, w = 1, h = 1;
    private double cx, cz, width;

    public MapView(double originX, double originZ, double width) {
        this.originX = originX;
        this.originZ = originZ;
        this.cx = originX;
        this.cz = originZ;
        this.width = clampWidth(width);
    }

    /** Where on screen the map is drawn. */
    public void frame(double left, double top, double w, double h) {
        this.left = left;
        this.top = top;
        this.w = Math.max(1, w);
        this.h = Math.max(1, h);
        clamp();
    }

    public double centreX() {
        return cx;
    }

    public double centreZ() {
        return cz;
    }

    public double width() {
        return width;
    }

    /** Screen pixels per block. */
    public double scale() {
        return w / width;
    }

    public double screenX(double worldX) {
        return left + w / 2 + (worldX - cx) * scale();
    }

    public double screenY(double worldZ) {
        return top + h / 2 + (worldZ - cz) * scale();
    }

    public double worldX(double screenX) {
        return cx + (screenX - left - w / 2) / scale();
    }

    public double worldZ(double screenY) {
        return cz + (screenY - top - h / 2) / scale();
    }

    public boolean contains(double sx, double sy) {
        return sx >= left && sx < left + w && sy >= top && sy < top + h;
    }

    /** Drags the map by a screen offset. */
    public void pan(double dx, double dy) {
        cx -= dx / scale();
        cz -= dy / scale();
        clamp();
    }

    /** Zooms to {@code newWidth}, keeping the world point under (sx, sy) where it is. */
    public void zoomAt(double sx, double sy, double newWidth) {
        double wx = worldX(sx), wz = worldZ(sy);
        width = clampWidth(newWidth);
        cx = wx - (sx - left - w / 2) / scale();
        cz = wz - (sy - top - h / 2) / scale();
        clamp();
    }

    public void centreOn(double x, double z) {
        cx = x;
        cz = z;
        clamp();
    }

    /** The world box the map shows: {minX, minZ, maxX, maxZ}. */
    public double[] bounds() {
        return new double[]{worldX(left), worldZ(top), worldX(left + w), worldZ(top + h)};
    }

    /** Keeps the view's edges within the table's reach (or centred on it, zoomed out past that). */
    private void clamp() {
        double halfW = width / 2, halfH = width * h / w / 2;
        double limX = Math.max(0, TerrainTile.RANGE - halfW), limZ = Math.max(0, TerrainTile.RANGE - halfH);
        cx = Math.max(originX - limX, Math.min(originX + limX, cx));
        cz = Math.max(originZ - limZ, Math.min(originZ + limZ, cz));
    }

    private static double clampWidth(double width) {
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
    }
}
