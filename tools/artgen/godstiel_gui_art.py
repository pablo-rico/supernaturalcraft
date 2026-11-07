"""Godstiel's HUD sheet (v0.39): textures/gui/godstiel_bar.png, one 256x128 sheet laid out as GodstielAssets.BAR_SHEET and
client.godstiel.GodstielGui read it. The style runs from light to goo: a stained-glass halo frame, a soul-blue fill that the
Leviathans' black goo eats away.

  frame    (0, 0)    256x32   the frame: a band of stained glass (white, pale gold, a little soul blue) in dark lead came,
                               a halo ring at each end, a small light-burst crest over the middle; the fill window at
                               (24, 12) 208x8 is clear (the fills are drawn under the frame, or the frame over them)
  light    (0, 32)   208x8    the light fill: soul blue, a bright white core along rows 3..4, a slow shimmer and motes
  goo      (0, 40)   208x8    the goo fill: black, an oily sheen on its upper rows, iridescent streaks
  drip     (0, 48)   8 frames of 16x12 at (16 * i, 48): the goo's leading edge (the goo fills x >= 8 of each frame, its
                               ragged front licks left over x 3..8, and a drop swells under it and falls); rows 0..7 match
                               the fill, rows 8..11 the drop hanging below the window
  cracks   (0, 64)   256x32   the frame's cracks, alpha only (black at varying alpha), laid exactly over the frame; drawn
                               over it from phase III, darker each phase
  pips     (0, 96)   16x16 each: lit at (16 * i, 96), corrupted at (16 * i, 112), i = phase - 1 (a little halo ring;
                               lit gold round a soul-blue glow, corrupted blackened, cracked and dripping goo); the five
                               differ only by the number of light points on the ring (i + 1)
  seal     (128, 96) 32x32    the title card's seal: a gold halo pierced top to bottom by a black drop of goo
"""

import math
import random

from chuck_art import h01
from common import save
from pixelkit import Tex, fbm, hexc, mix

W, H = 256, 128
WIN_X, WIN_Y, WIN_W, WIN_H = 24, 12, 208, 8

LEAD = [hexc(c) for c in ("#0e0e14", "#1c1c26", "#2c2c3a", "#40404f")]
GOLD = [hexc(c) for c in ("#5a4214", "#8a6a24", "#b8933c", "#dcbc66", "#f2dc9a", "#fff6d8")]
GLASS = {"w": [hexc(c) for c in ("#a8a8b4", "#cfd0da", "#e8e8f0", "#fbfbff")],
         "g": [hexc(c) for c in ("#a08850", "#c8ad6e", "#e4cf94", "#f6e8bc")],
         "b": [hexc(c) for c in ("#5f86b8", "#86a9d6", "#b2cdee", "#dcebff")]}
SOUL = [hexc(c) for c in ("#10306a", "#1f4f9c", "#3a7cd0", "#6fb2f2", "#b4dcff", "#ffffff")]
GOO = [hexc(c) for c in ("#020203", "#07070a", "#0e0e13", "#18181f", "#262632", "#3e4052")]
OIL = [hexc(c) for c in ("#3c5a6e", "#5a3e6e", "#4a6e4a", "#6a6a3a")]


def put(t, x, y, c, a=1.0):
    """Paints c over the pixel with coverage a."""
    x, y = int(x), int(y)
    if not (0 <= x < t.w and 0 <= y < t.h) or a <= 0:
        return
    d = t.rows[y][x]
    a = min(1.0, a)
    if d[3] == 0:
        t.rows[y][x] = (c[0], c[1], c[2], int(255 * a * c[3] / 255))
        return
    da = d[3] / 255
    oa = a + da * (1 - a)
    t.rows[y][x] = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3)) + (int(255 * oa),)


def ramp(r, v):
    return r[max(0, min(len(r) - 1, int(round(v))))]


# --- the frame ---------------------------------------------------------------------------------------------------------

def _ring_mask(cx, cy, r_out, r_in, x, y):
    d = math.hypot(x - cx, y - cy)
    return r_in <= d <= r_out, d


def frame(t, ox, oy):
    """The stained-glass halo frame."""
    cxs = (12.0, 243.5)
    cy = 15.5
    # 1. The band: glass panes above and below the window (y 7..11 and 20..24), bounded and split by lead.
    for y in range(6, 26):
        for x in range(18, 238):
            if WIN_Y <= y < WIN_Y + WIN_H and WIN_X <= x < WIN_X + WIN_W:
                continue
            edge = y in (6, 25) or x in (18, 237)
            inner = y in (WIN_Y - 1, WIN_Y + WIN_H) and WIN_X - 1 <= x <= WIN_X + WIN_W or \
                x in (WIN_X - 1, WIN_X + WIN_W) and WIN_Y - 1 <= y <= WIN_Y + WIN_H
            if edge:
                put(t, ox + x, oy + y, GOLD[2] if y == 6 else GOLD[1])
                continue
            if inner:
                put(t, ox + x, oy + y, LEAD[0])
                continue
            # Panes 13 px wide, the lead between them slanting a little, the glass colour cycling w-g-w-b.
            k = (x - 18 + (y - 6) // 3) // 13
            seam = (x - 18 + (y - 6) // 3) % 13 == 0
            if seam or y in (7, 24):
                put(t, ox + x, oy + y, LEAD[1] if y not in (7, 24) else GOLD[3] if y == 7 else GOLD[1])
                continue
            kind = "wgwb"[k % 4] if y < WIN_Y else "gwbw"[k % 4]
            g = GLASS[kind]
            # Lit from above: the upper band brighter; a streak of light across each pane.
            n = fbm(x, y * 2, 3901 + k, 256, 32, 2, 8.0)
            v = 1.6 + (0.6 if y < WIN_Y else -0.2) + (n - 0.5) * 1.2
            if (x - y) % 7 == 0:
                v += 0.8
            put(t, ox + x, oy + y, ramp(g, v))
    # 2. The halo rings at either end, over the band's ends: gold rings, glass inside, a soft light.
    for cx in cxs:
        for y in range(0, 32):
            for x in range(int(cx) - 13, int(cx) + 14):
                if not 0 <= x < 256:
                    continue
                on, d = _ring_mask(cx, cy, 11.6, 8.4, x, y)
                if on:
                    lit = -((x - cx) + (y - cy)) / 12
                    v = 3.2 + lit + (0.8 if 9.6 < d < 10.6 else 0)
                    put(t, ox + x, oy + y, ramp(GOLD, v))
                    if d > 11.0 or d < 9.0:
                        put(t, ox + x, oy + y, GOLD[1], 0.6)
                elif d < 8.4:
                    # Glass inside the ring: a rose of four panes round a bright centre, leaded in a cross.
                    if abs(x - cx) < 0.6 or abs(y - cy) < 0.6:
                        put(t, ox + x, oy + y, LEAD[1])
                    elif d < 2.2:
                        put(t, ox + x, oy + y, SOUL[5])
                    else:
                        q = (x > cx) + 2 * (y > cy)
                        g = GLASS["wbgw"[q]]
                        put(t, ox + x, oy + y, ramp(g, 2.6 - d / 4 + (0.5 if (x + y) % 5 == 0 else 0)))
                elif d < 13.0:
                    # A halo of light round the ring.
                    put(t, ox + x, oy + y, GOLD[5], 0.35 * (13.0 - d) / 1.4)
    # 3. The crest: a small burst of light over the middle, rays of gold.
    mx = 127.5
    for i in range(9):
        a = math.radians(-90 + (i - 4) * 18)
        ln = 6 if i % 2 == 0 else 4
        for s in range(ln * 2):
            r = 2.5 + s * 0.5
            x, y = mx + math.cos(a) * r, 6 + math.sin(a) * r
            put(t, ox + x, oy + y, GOLD[4] if s < ln else GOLD[3], 1.0 - s / (ln * 2) * 0.6)
    for y in range(3, 10):
        for x in range(123, 133):
            d = math.hypot(x - mx, (y - 6) * 1.2)
            if d < 3.4:
                put(t, ox + x, oy + y, SOUL[5] if d < 1.6 else GOLD[4] if d < 2.6 else GOLD[2])
    # 4. Little gold studs along the lead, top and bottom.
    for x in range(30, 230, 26):
        for y in (6, 25):
            put(t, ox + x, oy + y, GOLD[5])
            put(t, ox + x + 1, oy + y, GOLD[3])


def frame_mask(t, ox, oy):
    return {(x, y) for y in range(32) for x in range(256) if t.rows[oy + y][ox + x][3] > 0}


# --- the fills ---------------------------------------------------------------------------------------------------------

def light_fill(t, ox, oy, w=WIN_W, h=WIN_H):
    for y in range(h):
        for x in range(w):
            core = 1 - abs(y - 3.5) / 3.5            # 1 on the middle rows
            n = fbm(x, y * 3, 3910, 256, 32, 3, 10.0)
            wave = 0.5 + 0.5 * math.sin(x * 0.19 + y * 0.6)
            v = 1.4 + core * 3.0 + (n - 0.5) * 1.0 + 0.4 * wave
            c = ramp(SOUL, v)
            if y == 0:
                c = mix(c, SOUL[1], 0.5)
            put(t, ox + x, oy + y, c)
    rng = random.Random(3911)
    for _ in range(26):
        x, y = rng.randrange(w), rng.randrange(1, h - 1)
        put(t, ox + x, oy + y, SOUL[5])
        put(t, ox + x + 1, oy + y, SOUL[4], 0.6)


def goo_fill(t, ox, oy, w=WIN_W, h=WIN_H, seed=3920):
    for y in range(h):
        for x in range(w):
            n = fbm(x, y * 2, seed, 256, 32, 3, 9.0)
            v = 1.2 + (n - 0.5) * 1.6 + (0.9 if y == 1 else 0.5 if y == 2 else 0) - (0.6 if y >= h - 2 else 0)
            c = ramp(GOO, v)
            # Oily iridescent streaks, longer than they are tall.
            s = fbm(x * 0.4, y * 3, seed + 1, 256, 32, 2, 6.0)
            if s > 0.6 and y < h - 2:
                c = mix(c, OIL[int(h01("oil", x // 9) * 4)], 0.35 + (s - 0.6) * 1.6)
            if y == 1 and (x * 7) % 11 < 4:
                c = mix(c, GOO[5], 0.7)
            if y == 2 and (x * 5) % 13 < 2:
                c = mix(c, OIL[(x // 13) % 4], 0.6)
            put(t, ox + x, oy + y, c)


def drip_frames(t, ox, oy, fw=16, fh=12, frames=8):
    """The goo's leading edge: goo for x >= 8, its ragged front licking left; a drop swells under it and falls."""
    for f in range(frames):
        x0 = ox + f * fw
        ph = f / frames * 2 * math.pi
        for y in range(8):
            front = 7.0 - 2.4 * (0.5 + 0.5 * math.sin(y * 1.1 + ph)) - 1.2 * (0.5 + 0.5 * math.sin(y * 2.3 - ph * 2))
            for x in range(fw):
                if x < front:
                    continue
                n = fbm(x + f * 3, y * 2, 3930, 64, 32, 2, 5.0)
                v = 1.2 + (n - 0.5) * 1.6 + (0.9 if y == 1 else 0) - (0.5 if y >= 6 else 0)
                c = ramp(GOO, v)
                if x - front < 1.0:
                    c = mix(c, GOO[5], 0.55)       # the wet rim of the front
                put(t, x0 + x, oy + y, c)
        # The drop: swells under the front (frames 0..4), stretches and falls (5..7).
        k = f / frames
        cx = 9.5
        if f < 5:
            r = 0.8 + 1.2 * k / 0.6
            cy = 7.4 + r * 0.6
            neck = 0
        else:
            r = 1.6
            cy = 8.0 + (f - 4) * 1.3
            neck = 1
        for y in range(6, fh):
            for x in range(6, 14):
                d = math.hypot(x - cx, (y - cy) * (1.0 if y >= cy else 0.7))
                if d <= r:
                    c = GOO[1] if d > r - 0.7 else GOO[2]
                    if x < cx and y < cy:
                        c = GOO[5]
                    put(t, x0 + x, oy + y, c)
                elif neck and abs(x - cx) < 0.6 and 7 <= y < cy - r:
                    put(t, x0 + x, oy + y, GOO[1], 0.8)


# --- the cracks --------------------------------------------------------------------------------------------------------

def crack_overlay(t, ox, oy, mask, seed=3940):
    """Black cracks at varying alpha over the frame's own pixels: spidering from a few impact points."""
    rng = random.Random(seed)
    starts = [(40, 8), (90, 23), (128, 6), (170, 9), (212, 22), (12, 6), (243, 25), (60, 24), (150, 23), (196, 8)]
    for sx, sy in starts:
        for _ in range(rng.randint(2, 4)):
            x, y = float(sx), float(sy)
            ang = rng.uniform(0, 2 * math.pi)
            ln = rng.randint(6, 16)
            a = 0.95
            for _ in range(ln):
                ix, iy = int(round(x)), int(round(y))
                if (ix, iy) in mask:
                    put(t, ox + ix, oy + iy, (0, 0, 0, 255), a)
                ang += rng.uniform(-0.6, 0.6)
                x += math.cos(ang)
                y += math.sin(ang) * 0.7
                a *= 0.93
                if rng.random() < 0.12:
                    bx, by, ba = x, y, ang + rng.choice((-1, 1)) * 0.9
                    for _ in range(rng.randint(2, 5)):
                        bx += math.cos(ba)
                        by += math.sin(ba) * 0.7
                        if (int(bx), int(by)) in mask:
                            put(t, ox + int(bx), oy + int(by), (0, 0, 0, 255), a * 0.7)


# --- the pips and the seal ---------------------------------------------------------------------------------------------

def pip(t, ox, oy, i, corrupt):
    cx, cy = 7.5, 8.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, (y - cy) * 1.7)          # an ellipse: a ring seen a little from above
            if 4.3 <= d <= 6.6:
                lit = -((x - cx) + (y - cy)) / 8
                if corrupt:
                    c = ramp(GOO, 2.4 + lit * 1.5 + (1.4 if 5.2 < d < 5.8 and y < cy else 0))
                else:
                    c = ramp(GOLD, 3.0 + lit + (1.0 if 5.0 < d < 5.9 else 0))
                put(t, ox + x, oy + y, c)
            elif d < 4.3 and not corrupt:
                put(t, ox + x, oy + y, mix(SOUL[5], SOUL[3], d / 4.3), 0.95 - 0.35 * d / 4.3)
            elif 6.6 < d < 8.0 and not corrupt:
                put(t, ox + x, oy + y, GOLD[5], 0.3 * (8.0 - d) / 1.4)
    # Light points on the ring: one per phase.
    for k in range(i + 1):
        a = math.radians(-90 + (k - i / 2) * 32)
        x, y = cx + math.cos(a) * 5.5, cy + math.sin(a) * 5.5 / 1.7
        if corrupt:
            put(t, ox + x, oy + y, GOO[5])
        else:
            put(t, ox + x, oy + y, SOUL[5])
            put(t, ox + x, oy + y - 1, SOUL[4], 0.6)
    if corrupt:
        # A crack through it and goo dripping off the bottom.
        for s in range(6):
            put(t, ox + 4 + s, oy + 5 + (s % 2) + s // 2, SOUL[1], 0.8)
        for j, (dx, ln) in enumerate(((5, 4), (9, 6), (11, 3))):
            for y in range(int(cy + 3), int(cy + 3 + ln)):
                if oy + y < oy + 16:
                    put(t, ox + dx, oy + y, GOO[1] if y < cy + 2 + ln else GOO[3])
            put(t, ox + dx, oy + min(15, int(cy + 2 + ln)), GOO[4])


def seal(t, ox, oy):
    """A gold halo pierced by a black drop of goo, falling through it top to bottom."""
    cx, cy = 15.5, 17.0
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, (y - cy) * 1.8)
            if 9.0 <= d <= 13.6:
                lit = -((x - cx) + (y - cy)) / 12
                v = 3.0 + lit + (1.2 if 10.6 < d < 12.0 else 0)
                put(t, ox + x, oy + y, ramp(GOLD, v))
            elif 13.6 < d < 15.8:
                put(t, ox + x, oy + y, GOLD[5], 0.3 * (15.8 - d) / 2.2)
            elif d < 9.0:
                put(t, ox + x, oy + y, SOUL[4], 0.18 + 0.25 * (1 - d / 9.0))
    # The drop: a teardrop, point up, from the top of the card down through the ring's middle (in front of its near edge).
    for y in range(1, 32):
        for x in range(32):
            ty = (y - 1) / 30.0                         # 0 at the point .. 1 at the bottom
            if ty < 0.62:
                half = 5.6 * (ty / 0.62) ** 1.3
            else:
                u = (ty - 0.62) / 0.38
                half = 5.6 * math.sqrt(max(0.0, 1 - u * u))
            dx = abs(x - cx)
            if dx <= half:
                edge = half - dx
                c = GOO[1] if edge > 1.0 else GOO[3]
                if x < cx - half * 0.3 and 0.45 < ty < 0.8 and edge > 0.6:
                    c = GOO[5]                          # the wet highlight
                if 0.3 < ty < 0.95 and (x + y) % 9 == 0 and edge > 1.4:
                    c = mix(c, OIL[(x + y) % 4], 0.5)
                put(t, ox + x, oy + y, c)
    # Where the drop pierces the ring the gold is burnt black.
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, (y - cy) * 1.8)
            if 13.6 < d < 15.4 and abs(x - cx) < 6:
                put(t, ox + x, oy + y, GOO[2], 0.5)


def generate():
    t = Tex(W, H, 3900)
    for y in range(H):
        for x in range(W):
            t.rows[y][x] = (0, 0, 0, 0)
    frame(t, 0, 0)
    mask = frame_mask(t, 0, 0)
    light_fill(t, 0, 32)
    goo_fill(t, 0, 40)
    drip_frames(t, 0, 48)
    crack_overlay(t, 0, 64, mask)
    for i in range(5):
        pip(t, 16 * i, 96, i, False)
        pip(t, 16 * i, 112, i, True)
    seal(t, 128, 96)
    save(t, "gui", "godstiel_bar")
    return t


if __name__ == "__main__":
    generate()
