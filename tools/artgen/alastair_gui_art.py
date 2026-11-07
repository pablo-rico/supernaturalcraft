"""Alastair's bar (v0.40): textures/gui/alastair_bar.png, one 256x128 sheet laid out as client/apocalypse/alastair/AlastairHud
reads it (the constants there mirror these):

  plate    (0, 0)     256x40   a plate of rusted black iron, riveted, bolted at the corners; a recessed trough for the bar
                                (x 34..221, y 22..31) and an empty band above it for his name (drawn by the code at y 5..15)
  fill     (0, 40)    188x10   his health: a taut chain soaked in blood, wet sheen along its top (drawn cropped from the left)
  drained  (0, 50)    188x10   the trough where the chain has run out: scratched rusty iron, old stains
  hooks    (0, 64)    24x32 each: on its chain (x 0), bloody and swinging (x 24), the chain snapped, the hook gone (x 48)
  drip     (96, 64)   6 frames of 8x16 (x = 96 + 8 * frame): blood dripping from the bar's cut end (frame = time)
"""

import math

from chuck_art import h01
from common import save
from pixelkit import Tex, fbm, hexc, mix

W, H = 256, 128
IRON = [hexc(c) for c in ("#09090a", "#141416", "#1f1f22", "#2c2c30", "#3c3c42", "#56565e")]
RUST = [hexc(c) for c in ("#2a1208", "#45200e", "#643016", "#86421e", "#a65a2a", "#c47a40")]
BLOOD = [hexc(c) for c in ("#200204", "#3e0508", "#5e0a0c", "#7e1212", "#a01c18", "#c8382c")]
STEEL = [hexc(c) for c in ("#24272c", "#454a52", "#6b717a", "#9198a2", "#bac0c8", "#e6eaee")]

TROUGH = (34, 22, 188, 10)


def put(t, x, y, c, a=1.0):
    x, y = int(x), int(y)
    if not (0 <= x < t.w and 0 <= y < t.h) or a <= 0:
        return
    d = t.rows[y][x]
    a = min(1.0, a)
    if d[3] == 0:
        t.rows[y][x] = (c[0], c[1], c[2], int(255 * a))
        return
    da = d[3] / 255
    oa = a + da * (1 - a)
    t.rows[y][x] = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3)) + (int(255 * oa),)


def ramp(r, v):
    return r[max(0, min(len(r) - 1, int(round(v))))]


def iron(x, y, seed, base=2.6):
    n = fbm(x, y, seed, 256, 64, 3, 14.0)
    c = ramp(IRON, base + (n - 0.5) * 1.6)
    r = fbm(x * 2 + 7, y * 2, seed + 1, 256, 64, 2, 9.0)
    if r > 0.74:
        c = mix(c, ramp(RUST, 1.0 + (r - 0.74) * 10), 0.45)
    if h01("scr", seed, x // 3, y) < 0.02:
        c = mix(c, IRON[5], 0.5)
    return c


def rivet(t, cx, cy):
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            d = math.hypot(dx, dy)
            if d <= 1.3:
                put(t, cx + dx, cy + dy, IRON[5] if (dx < 0 or dy < 0) and d > 0.5 else (IRON[4] if d < 0.5 else IRON[1]))


def plate(t):
    x0, y0, w, h = 0, 0, 256, 40
    for y in range(h):
        for x in range(w):
            # A slightly ragged outline: corners clipped, a few rust bites out of the edge.
            e = min(x, w - 1 - x, y, h - 1 - y)
            if min(x, w - 1 - x) + min(y, h - 1 - y) < 3:
                continue
            if e == 0 and h01("bite", x, y) < 0.08:
                continue
            c = iron(x, y, 40901)
            if e == 0:
                c = IRON[0]
            elif e == 1:
                c = mix(c, IRON[5] if (y < h / 2) else IRON[0], 0.5)
            put(t, x0 + x, y0 + y, c)
    # Rivets along the top and bottom, bolts in the corners.
    for x in range(10, 250, 20):
        rivet(t, x, 3)
        rivet(t, x, 36)
    for cx, cy in ((5, 6), (250, 6), (5, 33), (250, 33)):
        rivet(t, cx, cy)
    # The trough: recessed, a dark lip above, a lit lip below.
    tx, ty, tw, th = TROUGH
    for x in range(tx - 1, tx + tw + 1):
        put(t, x, ty - 1, IRON[0])
        put(t, x, ty + th, IRON[4])
    for y in range(ty - 1, ty + th + 1):
        put(t, tx - 1, y, IRON[0])
        put(t, tx + tw, y, IRON[4])
    for y in range(ty, ty + th):
        for x in range(tx, tx + tw):
            put(t, x, y, IRON[0])
    # Old blood run down from the trough in streaks.
    for i in range(14):
        sx = tx + int(h01("streak", i) * tw)
        L = 2 + int(h01("len", i) * 5)
        for k in range(L):
            put(t, sx, ty + th + 1 + k, BLOOD[1], 0.75 - k * 0.1)


def chain_fill(t, ox, oy, w=188, h=10):
    """A taut chain of oval links soaked in blood: alternating flat and edge-on links, a wet sheen on top."""
    for y in range(h):
        for x in range(w):
            put(t, ox + x, oy + y, BLOOD[1])
    for x in range(w):
        link = x % 8
        flat = (x // 8) % 2 == 0
        for y in range(h):
            yy = y - (h - 1) / 2
            if flat:
                # A flat link: an oval ring.
                d = math.hypot((link - 3.5) / 3.8, yy / 3.6)
                if 0.62 < d < 1.0:
                    c = ramp(BLOOD, 3.4 - yy * 0.35)
                    if yy < -1.5:
                        c = mix(c, STEEL[3], 0.35)
                    put(t, ox + x, oy + y, c)
            else:
                # An edge-on link: a thick bar through the middle.
                if abs(yy) < 1.6:
                    put(t, ox + x, oy + y, ramp(BLOOD, 3.8 - abs(yy) * 0.8))
        # The wet sheen.
        if h01("sheen", x // 3) < 0.6:
            put(t, ox + x, oy + 1, BLOOD[5], 0.55)
        # Drops along the bottom.
        if h01("drop", x) < 0.08:
            put(t, ox + x, oy + h - 1, BLOOD[4])


def drained(t, ox, oy, w=188, h=10):
    for y in range(h):
        for x in range(w):
            c = iron(x * 1.5, y * 3 + 90, 40911, 1.6)
            if h01("stain", x // 4, y // 3) < 0.15:
                c = mix(c, BLOOD[1], 0.6)
            if (x + y * 3) % 23 == 0:
                c = mix(c, IRON[5], 0.4)            # scratches
            put(t, ox + x, oy + y, c)


def hook_frame(t, ox, oy, state):
    """A rusty hook on a short chain hanging from the plate's lower edge (24x32): 0 clean, 1 bloody, 2 the chain snapped."""
    cx = ox + 12
    chain_end = 14 if state < 2 else 6
    for y in range(chain_end):
        if y % 4 in (0, 3):
            put(t, cx, oy + y, IRON[4])
            put(t, cx + 1, oy + y, IRON[3])
        else:
            put(t, cx - 1, oy + y, IRON[4])
            put(t, cx + 2, oy + y, IRON[2])
    if state == 2:
        # The broken link, splayed.
        put(t, cx - 1, oy + 6, IRON[4])
        put(t, cx - 2, oy + 7, IRON[3])
        put(t, cx + 2, oy + 6, IRON[4])
        return
    # The hook: a shank down, a curve to the left, the point back up.
    pts = []
    for k in range(10):
        pts.append((cx, oy + 14 + k))
    for a in range(0, 181, 12):
        r = math.radians(a)
        pts.append((cx - 4 + 4 * math.cos(r), oy + 24 + 4 * math.sin(r)))
    for k in range(5):
        pts.append((cx - 8, oy + 24 - k))
    for i, (x, y) in enumerate(pts):
        c = RUST[3] if h01("rh", i) < 0.4 else IRON[4]
        put(t, x, y, c)
        put(t, x + 1, y, IRON[1])
    # The point.
    put(t, cx - 8, oy + 19, STEEL[4])
    put(t, cx - 9, oy + 20, STEEL[3])
    if state == 1:
        for i, (x, y) in enumerate(pts[10:]):
            if h01("bl", i) < 0.7:
                put(t, x, y, BLOOD[3])
        for k in range(3):
            put(t, cx - 4, oy + 29 + k, BLOOD[2], 0.9 - 0.25 * k)


def drip(t, ox, oy, frame):
    """The bar's cut end bleeding: a bead swelling under it, falling and splashing (8x16)."""
    for y in range(3):
        for x in range(2, 6):
            put(t, ox + x, oy + y, BLOOD[3] if y else BLOOD[4])
    phase = frame / 6.0
    if frame < 3:
        r = 1 + frame * 0.6
        for y in range(4):
            for x in range(8):
                if math.hypot(x - 3.5, y - 1 - r * 0.5) < r:
                    put(t, ox + x, oy + 3 + y, BLOOD[3])
    else:
        fy = 4 + (frame - 3) * 4
        for y in range(3):
            for x in range(3):
                if (x - 1) ** 2 + (y - 1) ** 2 <= 1:
                    put(t, ox + 2 + x, oy + fy + y, BLOOD[4] if y == 0 else BLOOD[3])
        for k in range(fy - 3):
            put(t, ox + 3, oy + 3 + k, BLOOD[2], 0.4)
    _ = phase


def build():
    t = Tex(W, H, 40900)
    plate(t)
    chain_fill(t, 0, 40)
    drained(t, 0, 50)
    for s in range(3):
        hook_frame(t, s * 24, 64, s)
    for f in range(6):
        drip(t, 96 + 8 * f, 64, f)
    return t


def generate():
    save(build(), "gui", "alastair_bar")


if __name__ == "__main__":
    generate()
