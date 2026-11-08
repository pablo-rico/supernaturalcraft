"""Amara's HUD sheet (v0.42): textures/gui/amara_hud.png, 256x256, "the closing eclipse". Laid out exactly as
entity.boss.amara.AmaraHudSheet (client.amara.AmaraHud reads it); everything outside the regions is transparent.

  skin(i)      ((i % 4) * 64, (i / 4) * 64)  64x64   the sun's face in phase i + 1, full colour, a round disc
                                                     SUN_R px in radius centred in the cell, soft edge, clear outside:
                                                       0 smoke     a sun veiled in curling grey-violet smoke
                                                       1 crayon    a child's drawing: scribbled yellow sun, smiley, rays
                                                       2 filigree  art-deco gold and black fan rays (Gatsby)
                                                       3 two suns  white-gold and black-violet, interlocked (Chuck, Amara)
                                                       4 stars     a sun made of a starfield and a nebula swirl
                                                       5 dying     deep red-orange, cracked, embers, dim
  CORONA       (128, 64)   64x64     white-alpha corona round a disc hole (the code tints it and wipes it radially):
                                     brightest at the hole's edge, streamers wisping out (longest toward the corners)
  DISC         (192, 64)   64x64     Amara's disc: black, the suns' size, an ink edge with two small drip bulges, a faint
                                     violet rim, the faintest star specks
  pip(i, s)    (i * 12, 128 + s * 12)  12x12  lunar phases i = 0..5 (new, crescent, half, gibbous, full, black moon);
                                     s = 0 dark (dim grey outline), 1 lit (silver-white), 2 current (violet-white, halo)
  WORLD_FRAME  (0, 168)    182x7     worn gold frame; hollow inside (the 178x3 fill sits at frame x+2, y+2)
  WORLD_FILL   (0, 176)    178x3     white, a brighter core row (tinted from dawn gold to ember red)
  CHUCK_FRAME  (0, 184)    120x7     ink-and-paper frame: cream paper, black pen lines; hollow inside (fill at x+2, y+2)
  CHUCK_FILL   (0, 192)    116x3     white
  SEAL         (192, 176)  48x48     the title card's seal: an eclipse with a closed eye (lid and lashes across the disc)
  drip(f)      (f * 16, 208)  16x16 x8  ink falling off the disc's edge, a loop: a bead swells at the top edge, stretches,
                                     pinches off, falls and splashes out of the cell
"""

import math
import random

from common import save
from pixelkit import Tex, fbm, hexc, mix

W = H = 256
SUN_R = 28.5          # every sun face, the disc and the corona's hole share this radius
CELL_C = 32.0         # centre of a 64x64 cell


def skin_region(i):
    return (i % 4) * 64, (i // 4) * 64, 64, 64


CORONA = (128, 64, 64, 64)
DISC = (192, 64, 64, 64)
SEAL = (192, 176, 48, 48)
WORLD_FRAME = (0, 168, 182, 7)
WORLD_FILL = (0, 176, 178, 3)
CHUCK_FRAME = (0, 184, 120, 7)
CHUCK_FILL = (0, 192, 116, 3)


def pip_region(i, s):
    return i * 12, 128 + s * 12, 12, 12


def drip_region(f):
    return f * 16, 208, 16, 16


def regions():
    out = [("skin_%d" % i, skin_region(i)) for i in range(6)]
    out += [("corona", CORONA), ("disc", DISC)]
    out += [("pip_%d_%d" % (i, s), pip_region(i, s)) for i in range(6) for s in range(3)]
    out += [("seal", SEAL), ("world_frame", WORLD_FRAME), ("world_fill", WORLD_FILL), ("chuck_frame", CHUCK_FRAME),
            ("chuck_fill", CHUCK_FILL)]
    out += [("drip_%d" % f, drip_region(f)) for f in range(8)]
    return out


# --- drawing -------------------------------------------------------------------------------------------------------

def rgb(h):
    c = hexc(h)
    return (float(c[0]), float(c[1]), float(c[2]))


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[k] + (b[k] - a[k]) * t for k in range(len(a)))


def ramp(cols, t):
    """Sample a list of rgb colours at t in 0..1."""
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(len(cols) - 2, int(t))
    return lerp(cols[i], cols[i + 1], t - i)


def smooth(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def cell(t, region, fn, ss=3):
    """Rasterises fn(x, y) -> (r, g, b, a in 0..1) or None over a region, ss x ss samples a pixel (soft edges)."""
    ox, oy, w, h = region
    for py in range(h):
        for px in range(w):
            r = g = b = a = 0.0
            for sy in range(ss):
                for sx in range(ss):
                    c = fn(px + (sx + 0.5) / ss, py + (sy + 0.5) / ss)
                    if c is None or c[3] <= 0:
                        continue
                    r += c[0] * c[3]
                    g += c[1] * c[3]
                    b += c[2] * c[3]
                    a += c[3]
            if a <= 0:
                continue
            n = ss * ss
            alpha = a / n
            t.set(ox + px, oy + py, (int(min(255, r / a)), int(min(255, g / a)), int(min(255, b / a)), int(round(255 * min(1.0, alpha)))))


def disc_alpha(d, r=SUN_R, soft=1.0):
    return 1.0 - smooth(r - soft, r + 0.2, d)


def warp(x, y, seed, amt=6.0, scale=3.0):
    """Domain warp for curling smoke and swirls."""
    wx = fbm(x * 2, y * 2, seed, 128, 128, 2, scale) - 0.5
    wy = fbm(x * 2 + 50, y * 2 + 30, seed + 1, 128, 128, 2, scale) - 0.5
    return x + wx * amt * 2, y + wy * amt * 2


def h01(*parts):
    return random.Random(repr(parts)).random()


# --- the six sun faces ----------------------------------------------------------------------------------------------

SUN = [rgb("#fffdf2"), rgb("#fff1c2"), rgb("#ffd879"), rgb("#f2aa3c"), rgb("#c96e1f")]


def sun_body(d, r=SUN_R):
    """A sun's limb-darkened disc colour at distance d from its centre."""
    k = d / r
    return ramp(SUN, k ** 1.6)


def skin_smoke(x, y):
    dx, dy = x - CELL_C, y - CELL_C
    d = math.hypot(dx, dy)
    a = disc_alpha(d)
    if a <= 0:
        return None
    c = sun_body(d * 0.92)
    # Smoke curling across the sun: twice-warped noise cut into wisps, dense and dark at their hearts, thin and lit
    # gold by the sun at their edges, drifting up from the lower left.
    wx, wy = x, y
    # Curls: the smoke is twisted round a few eddies before the noise is read.
    for ex, ey, turn in ((22, 24, 2.6), (41, 37, -2.2), (27, 45, 1.8), (43, 18, -1.6)):
        rx, ry = wx - ex, wy - ey
        rr = math.hypot(rx, ry)
        th = turn * math.exp(-rr / 7.0)
        wx, wy = ex + rx * math.cos(th) - ry * math.sin(th), ey + rx * math.sin(th) + ry * math.cos(th)
    wx, wy = warp(wx, wy, 701, 4.0, 3.0)
    n = fbm(wx * 1.4, wy * 1.4, 702, 128, 128, 3, 4.0)
    n += (dy - dx) / SUN_R * 0.12
    dens = smooth(0.5, 0.6, n)
    heart = smooth(0.6, 0.76, n)
    smoke = lerp(rgb("#6d6080"), rgb("#2e2540"), heart)
    edge = dens * (1 - dens) * 4                          # 1 at a wisp's edge
    smoke = lerp(smoke, rgb("#f0c27a"), edge * 0.45)
    # A darker line just inside each wisp's edge: the curl of its rolled-over lip.
    lip = smooth(0.54, 0.57, n) * (1 - smooth(0.57, 0.61, n))
    smoke = lerp(smoke, rgb("#241c32"), lip * 0.6)
    c = lerp(c, smoke, dens * 0.9)
    # A dim violet rim where the smoke wraps the edge.
    c = lerp(c, rgb("#4a3a66"), smooth(SUN_R - 3, SUN_R, d) * 0.55)
    return c + (a,)


def _wobble_circle(x, y, cx, cy, r, seed, amp=0.9):
    ang = math.atan2(y - cy, x - cx)
    return math.hypot(x - cx, y - cy) - (r + math.sin(ang * 5 + seed) * amp * 0.6 + math.sin(ang * 11 + seed * 2) * amp * 0.4)


def _grain(x, y, seed):
    """Paper tooth showing through crayon: a coarse, slightly diagonal dropout."""
    return fbm(x * 6 + y * 2, y * 6, seed, 256, 256, 2, 32.0)


def _seg_dist(px, py, ax, ay, bx, by):
    vx, vy = bx - ax, by - ay
    wx, wy = px - ax, py - ay
    t = max(0.0, min(1.0, (wx * vx + wy * vy) / max(1e-6, vx * vx + vy * vy)))
    return math.hypot(px - (ax + vx * t), py - (ay + vy * t)), t


def skin_crayon(x, y):
    dx, dy = x - CELL_C, y - CELL_C
    d = math.hypot(dx, dy)
    a = disc_alpha(d)
    if a <= 0:
        return None
    # The page: cream paper, a sky of blue crayon scribbled round the edge.
    paper = lerp(rgb("#f6edd6"), rgb("#ead9b4"), fbm(x * 3, y * 3, 711, 128, 128, 2, 8.0))
    c = paper
    sky = smooth(SUN_R - 9, SUN_R - 3, d)
    hatch = math.sin((x + y * 0.35) * 1.9) > -0.2 and _grain(x, y, 712) > 0.33
    if sky > 0 and hatch:
        c = lerp(c, rgb("#7fb3e6"), sky * 0.85)
    sx, sy, sr = CELL_C - 0.5, CELL_C + 0.5, 12.5
    ang = math.atan2(y - sy, x - sx)
    # Rays: wobbly crayon strokes, long and short in turn, orange and yellow.
    for k in range(12):
        a0 = k / 12 * math.tau + 0.13 + math.sin(k * 2.1) * 0.06
        r0 = sr + 2.5
        r1 = sr + (11.5 if k % 2 == 0 else 7.5) + math.sin(k * 1.7) * 1.2
        ax, ay = sx + math.cos(a0) * r0, sy + math.sin(a0) * r0
        bx, by = sx + math.cos(a0 + 0.05) * r1, sy + math.sin(a0 + 0.05) * r1
        dist, t = _seg_dist(x, y, ax, ay, bx, by)
        wob = math.sin(t * 9 + k) * 0.35
        if dist + wob < 1.05 and _grain(x, y, 713) > 0.22:
            c = rgb("#f08a1c") if k % 2 == 0 else rgb("#f7c41a")
    e = _wobble_circle(x, y, sx, sy, sr, 3.0, 1.0)
    if e < 0:
        # The fill: back-and-forth yellow scribble that leaves paper in the gaps.
        scrib = math.sin((x * 0.8 - y * 1.1) * 1.7 + math.sin(y * 0.6) * 1.5)
        if scrib > -0.55 and _grain(x, y, 714) > 0.25:
            c = lerp(rgb("#ffe03a"), rgb("#ffc81e"), fbm(x * 4, y * 4, 715, 128, 128, 2, 8.0))
        else:
            c = lerp(paper, rgb("#fff1a0"), 0.5)
    if -1.3 < e < 0.4 and _grain(x, y, 716) > 0.2:
        c = rgb("#e8741a")
    # The face: two dots for eyes, a smile, round cheeks.
    for ex in (-4.3, 4.0):
        if math.hypot(x - (sx + ex), (y - (sy - 3.2)) * 0.8) < 1.35:
            c = rgb("#3a2a22")
    for ex in (-7.0, 6.8):
        if math.hypot(x - (sx + ex), y - (sy + 2.2)) < 1.7 and _grain(x, y, 717) > 0.35:
            c = lerp(c, rgb("#f07a7a"), 0.7)
    sm = math.hypot(x - sx, y - (sy - 1.5))
    if 5.2 < sm < 6.6 and y > sy + 1.2 and abs(x - sx) < 5.4 and _grain(x, y, 718) > 0.15:
        c = rgb("#3a2a22")
    return c + (a,)


GOLD = [rgb("#3a2606"), rgb("#7d5410"), rgb("#b07d18"), rgb("#d9a92b"), rgb("#f2d257"), rgb("#fff2a8")]


def _gold(v):
    return ramp(GOLD, v)


def skin_deco(x, y):
    dx, dy = x - CELL_C, y - CELL_C
    d = math.hypot(dx, dy)
    a = disc_alpha(d, soft=0.8)
    if a <= 0:
        return None
    ang = math.atan2(dy, dx)
    black = rgb("#0c0a0e")
    # Light falls from the upper left across the metal.
    sheen = 0.5 + 0.3 * (-(dx + dy) / (SUN_R * 1.4))
    c = black
    # The rim: a gold band with a black line and a bead row.
    if d > SUN_R - 2.6:
        c = _gold(sheen + 0.2)
        if SUN_R - 1.6 < d < SUN_R - 1.0:
            c = black
        return c + (a,)
    if SUN_R - 5.2 < d <= SUN_R - 2.6:
        bead = (ang / math.tau * 48) % 1.0
        c = _gold(sheen + 0.1) if abs(bead - 0.5) < 0.28 else black
        return c + (a,)
    # Fan rays: 24 wedges, alternately long gold blades and stepped gold chevrons on black.
    k = (ang / math.tau + 0.5) * 24
    frac = k % 1.0
    wedge = int(k) % 2
    if 9.5 < d <= SUN_R - 5.2:
        width = 0.32 if wedge == 0 else 0.18
        if abs(frac - 0.5) < width * (1.0 - (d - 9.5) / 40):
            c = _gold(sheen + (0.15 if wedge == 0 else -0.05) + (d - 9.5) / 60)
        elif wedge == 1 and abs(frac - 0.5) < 0.42 and int(d) % 4 == 0:
            c = _gold(sheen - 0.15)                  # little steps between the blades
        return c + (a,)
    # The medallion: rings and a stepped ziggurat sun at the centre.
    if 7.8 < d <= 9.5:
        c = _gold(sheen + 0.25) if d > 8.6 else black
        return c + (a,)
    if 5.5 < d <= 7.8:
        k2 = (ang / math.tau + 0.5) * 16
        c = _gold(sheen) if abs(k2 % 1.0 - 0.5) < 0.3 else black
        return c + (a,)
    step = max(abs(dx), abs(dy) * 1.0)
    if step < 2.0 or (abs(dx) + abs(dy) < 5.4 and (abs(dx) + abs(dy)) % 1.8 < 1.0):
        c = _gold(sheen + 0.35)
    return c + (a,)


def skin_two(x, y):
    dx, dy = x - CELL_C, y - CELL_C
    d = math.hypot(dx, dy)
    a = disc_alpha(d)
    if a <= 0:
        return None
    r = SUN_R
    # The S between them: two half-size circles stacked, as in the old sign; turned a little so it reads as a sun.
    th = math.radians(-35)
    u = dx * math.cos(th) - dy * math.sin(th)
    v = dx * math.sin(th) + dy * math.cos(th)
    top = math.hypot(u, v + r / 2)
    bot = math.hypot(u, v - r / 2)
    light = u > 0
    if top < r / 2:
        light = True
    if bot < r / 2:
        light = False
    edge = min(abs(top - r / 2) if v < 0 else 99, abs(bot - r / 2) if v >= 0 else 99,
               abs(u) if (top >= r / 2 and bot >= r / 2) else 99)
    # The eyes: a dot of each in the other.
    if math.hypot(u, v + r / 2) < 3.6:
        light = False
    if math.hypot(u, v - r / 2) < 3.6:
        light = True
    if light:
        c = ramp([rgb("#ffffff"), rgb("#fff6d8"), rgb("#ffdf8a"), rgb("#e9a83c")], (d / r) ** 1.4)
        if math.hypot(u, v - r / 2) < 3.6:
            c = rgb("#fffbe8")
    else:
        n = fbm(x * 3, y * 3, 731, 128, 128, 3, 6.0)
        c = ramp([rgb("#05030a"), rgb("#140a26"), rgb("#2e1650"), rgb("#4a2a80")], n * 0.7 + (d / r) * 0.3)
        if h01("two", int(x * 3), int(y * 3)) < 0.008:
            c = rgb("#e8deff")
        if math.hypot(u, v + r / 2) < 3.6:
            c = lerp(rgb("#000000"), rgb("#3a2160"), math.hypot(u, v + r / 2) / 3.6)
    # Where they meet, light bleeds a thin violet-gold seam.
    if edge < 0.8:
        c = lerp(c, rgb("#d8b8ff") if not light else rgb("#fff0c0"), 0.6)
    c = lerp(c, rgb("#2a1a48") if not light else rgb("#d98a2a"), smooth(r - 2, r, d) * 0.5)
    return c + (a,)


def skin_stars(x, y):
    dx, dy = x - CELL_C, y - CELL_C
    d = math.hypot(dx, dy)
    a = disc_alpha(d)
    if a <= 0:
        return None
    r = SUN_R
    ang = math.atan2(dy, dx)
    # A spiral nebula: two arms winding into a bright core.
    spiral = math.sin(2 * ang - d * 0.32 + fbm(x * 2, y * 2, 741, 128, 128, 2, 4.0) * 3.0)
    arm = smooth(0.2, 1.0, spiral) * (1 - smooth(r * 0.55, r, d) * 0.6)
    dust = fbm(x * 3, y * 3, 742, 128, 128, 3, 6.0)
    base = ramp([rgb("#04040e"), rgb("#0d0d2a"), rgb("#1c1450")], dust)
    neb = ramp([rgb("#3a1a7a"), rgb("#7a3ab8"), rgb("#c46ad8"), rgb("#ffd0f0")], arm * (0.6 + dust * 0.6))
    c = lerp(base, neb, arm * 0.85)
    c = lerp(c, rgb("#2a5ab8"), smooth(0.55, 0.8, dust) * (1 - arm) * 0.5)
    core = 1 - smooth(0, 7.5, d)
    c = lerp(c, rgb("#fff4e0"), core ** 1.5)
    # Stars: many small, a few large with a cross.
    gx, gy = int(x * 2), int(y * 2)
    s = h01("st", gx, gy)
    if s < 0.025 * (1.2 - d / r * 0.4):
        c = lerp(c, rgb("#ffffff") if s < 0.01 else rgb("#c8d8ff"), 0.9)
    for sx, sy, sz in ((-11, -8, 2.2), (9, -13, 1.8), (13, 7, 2.0), (-6, 14, 1.6), (-18, 4, 1.4)):
        ddx, ddy = abs(x - CELL_C - sx), abs(y - CELL_C - sy)
        if (ddx < 0.5 and ddy < sz * 1.8) or (ddy < 0.5 and ddx < sz * 1.8) or math.hypot(ddx, ddy) < sz * 0.6:
            c = rgb("#ffffff")
    c = lerp(c, rgb("#7a8cff"), smooth(r - 2.5, r, d) * 0.6)
    return c + (a,)


def _voronoi_edge(x, y, seed, cell_size=7.0):
    """Distance to the nearest crack: the second-nearest minus nearest feature point."""
    gx, gy = int(math.floor(x / cell_size)), int(math.floor(y / cell_size))
    ds = []
    for oy in (-1, 0, 1):
        for ox in (-1, 0, 1):
            cx, cy = gx + ox, gy + oy
            rng = random.Random(f"{seed}:{cx}:{cy}")
            px, py = (cx + rng.random()) * cell_size, (cy + rng.random()) * cell_size
            ds.append(math.hypot(x - px, y - py))
    ds.sort()
    return ds[1] - ds[0]


def skin_dying(x, y):
    dx, dy = x - CELL_C, y - CELL_C
    d = math.hypot(dx, dy)
    a = disc_alpha(d)
    if a <= 0:
        return None
    r = SUN_R
    n = fbm(x * 2, y * 2, 751, 128, 128, 3, 5.0)
    # Dim and deep: a dull red-orange crust, darker at the limb, mottled.
    c = ramp([rgb("#ff9a3a"), rgb("#d8521c"), rgb("#9a2a10"), rgb("#4a0e08"), rgb("#1a0604")], (d / r) ** 1.3 * 0.7 + (1 - n) * 0.35)
    e = _voronoi_edge(x, y, 752, 7.5)
    crack = 1 - smooth(0.4, 1.3, e)
    if crack > 0:
        # The cracks are dark seams; some still glow molten at their hearts.
        hot = smooth(0.55, 0.8, fbm(x * 3, y * 3, 753, 128, 128, 2, 5.0))
        c = lerp(c, rgb("#120302"), crack * 0.85)
        c = lerp(c, rgb("#ffb24a") if e < 0.35 else rgb("#ff6a1a"), crack * hot)
    # Embers flaking off.
    if h01("ember", int(x * 1.5), int(y * 1.5)) < 0.02 and d > r * 0.4:
        c = rgb("#ffd27a")
    c = lerp(c, rgb("#0a0202"), smooth(r - 4, r, d) * 0.55)
    return c + (a,)


SKINS = [skin_smoke, skin_crayon, skin_deco, skin_two, skin_stars, skin_dying]


# --- corona, disc, seal --------------------------------------------------------------------------------------------

def corona(x, y, cx=CELL_C, cy=CELL_C, hole=SUN_R - 1.0, bound=31.9, seed=761):
    dx, dy = x - cx, y - cy
    d = math.hypot(dx, dy)
    if d < hole:
        return None
    ang = math.atan2(dy, dx)
    # Room to the cell's edge along this angle: streamers fade out before they reach it (longest toward the corners).
    room = bound / max(abs(math.cos(ang)), abs(math.sin(ang))) - hole
    t = (d - hole) / max(1.0, room)
    if t >= 1:
        return None
    k = ang / math.tau + 0.5
    # Streamers: each angle gets a length from two octaves of noise round the circle, and a fine combing of rays.
    st = 0.55 * fbm(k * 96, 0, seed, 96, 4, 2, 12.0) + 0.45 * fbm(k * 96, 2, seed + 2, 96, 4, 2, 28.0)
    length = 0.4 + 0.6 * smooth(0.3, 0.75, st) ** 1.1
    fine = fbm(k * 512 + t * 6, t * 4, seed + 1, 512, 16, 2, 64.0)
    body = max(0.0, 1 - t / length) ** 1.1 * (0.6 + 0.4 * fine)
    inner = math.exp(-((d - hole) / 1.8) ** 2)          # the bright ring hugging the disc
    alpha = min(1.0, inner + body * 1.25)
    if alpha < 0.04:
        return None
    return (255.0, 255.0, 255.0, alpha)


def disc(x, y):
    dx, dy = x - CELL_C, y - CELL_C
    d = math.hypot(dx, dy)
    ang = math.atan2(dy, dx)
    # An ink edge: slightly irregular, with two small drips bulging from its lower edge.
    edge = SUN_R + (fbm((ang / math.tau + 0.5) * 64, 0, 771, 64, 8, 3, 8.0) - 0.5) * 1.2
    for da, sz in ((math.radians(108), 2.2), (math.radians(64), 1.5)):
        diff = math.atan2(math.sin(ang - da), math.cos(ang - da))
        edge += sz * math.exp(-(diff / 0.07) ** 2)
    a = 1 - smooth(edge - 0.6, edge + 0.4, d)
    if a <= 0:
        return None
    c = ramp([rgb("#000000"), rgb("#05030a"), rgb("#0d0718")], fbm(x * 2, y * 2, 772, 128, 128, 2, 4.0) * 0.6)
    rim = smooth(edge - 2.4, edge - 0.3, d)
    lit = 0.6 + 0.4 * max(0.0, -(dx + dy) / (d + 1e-6))      # the rim catches more light up-left
    c = lerp(c, rgb("#6a48b8"), rim * 0.55 * lit)
    if h01("disc", int(x), int(y)) < 0.006 and d < edge - 4:
        c = rgb("#3a2e5a")
    # A wet sheen arc, up and left.
    if abs(d - (edge - 4.5)) < 0.6 and -2.6 < ang < -1.6:
        c = lerp(c, rgb("#2a2048"), 0.8)
    return c + (a,)


def seal(x, y):
    cx = cy = 24.0
    dx, dy = x - cx, y - cy
    d = math.hypot(dx, dy)
    r = 14.0
    if d >= r:
        co = corona(x, y, cx, cy, hole=r - 0.5, bound=23.9, seed=781)
        if co is None:
            return None
        t = smooth(r, r + 9, d)
        col = lerp(rgb("#fff6dc"), rgb("#e0a24a"), t)
        return col + (co[3],)
    a = 1 - smooth(r - 0.8, r, d)
    c = ramp([rgb("#000000"), rgb("#0a0614"), rgb("#1e1236")], (d / r) ** 2)
    # The closed eye: the lid a gold arc sagging across the disc, lashes hanging from it.
    lid_y = cy + 1.0 + (1 - (dx / 10.5) ** 2) * 3.6 if abs(dx) < 10.5 else None
    if lid_y is not None:
        if abs(y - lid_y) < 0.7:
            c = rgb("#ffe7a8")
        elif abs(y - lid_y) < 1.2:
            c = lerp(c, rgb("#c98f2a"), 0.7)
        for lx in (-7.5, -4.5, -1.5, 1.5, 4.5, 7.5):
            ly = cy + 1.0 + (1 - (lx / 10.5) ** 2) * 3.6
            ex, ey = lx * 1.25, ly + 3.6
            dist, tt = _seg_dist(x, y, cx + lx, ly, cx + ex, ey)
            if dist < 0.55:
                c = lerp(rgb("#ffe7a8"), rgb("#b8862a"), tt)
        # The fold of the lid above, faint.
        fold = cy - 1.5 + (1 - (dx / 9.0) ** 2) * 2.4 if abs(dx) < 9.0 else None
        if fold is not None and abs(y - fold) < 0.5:
            c = lerp(c, rgb("#7a5ac8"), 0.6)
    c = lerp(c, rgb("#5a3a9a"), smooth(r - 2.0, r - 0.5, d) * 0.6)
    return c + (a,)


# --- pips -------------------------------------------------------------------------------------------------------------

def moon_lit(i, x, y, cx, cy, r):
    """How much of the moon at (x, y) is lit in phase i (0 new .. 4 full, 5 black: eclipsed)."""
    u = (x - cx) / r
    if i == 0:
        return 0.0
    if i == 4:
        return 1.0
    if i == 5:
        return 0.0
    # The terminator: an ellipse of half-width w; lit to its right.
    w = {1: 0.55, 2: 0.0, 3: -0.55}[i]
    v = (y - cy) / r
    half = math.sqrt(max(0.0, 1 - v * v))
    term = w * half
    return smooth(term - 0.12, term + 0.12, u)


def pip(i, s, x, y):
    cx = cy = 6.0
    r = 4.3
    dx, dy = x - cx, y - cy
    d = math.hypot(dx, dy)
    if s == 2 and d >= r:
        # The halo of the current one, soft, inside the cell.
        g = math.exp(-((d - r) / 1.3) ** 2) * 0.8
        if i == 5:
            g = max(g, math.exp(-((d - r - 0.4) / 0.6) ** 2))
        if g < 0.04 or d > 5.95:
            return None
        return (196.0, 160.0, 255.0, g)
    if d >= r + 0.5:
        return None
    a = 1 - smooth(r - 0.4, r + 0.5, d)
    lit = moon_lit(i, x, y, cx, cy, r)
    outline = smooth(r - 1.3, r - 0.3, d)
    if s == 0:
        body = lerp(rgb("#1a1a20"), rgb("#4a4a54"), lit * 0.8)
        c = lerp(body, rgb("#6e6e78"), outline * 0.9)
    elif s == 1:
        body = lerp(rgb("#20222c"), ramp([rgb("#ffffff"), rgb("#d8dde8"), rgb("#a8b0c0")], d / r), lit)
        c = lerp(body, rgb("#c8ccd8"), outline * 0.6)
    else:
        body = lerp(rgb("#2a1a48"), ramp([rgb("#ffffff"), rgb("#efe4ff"), rgb("#c4a8ff")], d / r), lit)
        c = lerp(body, rgb("#d8c4ff"), outline * 0.8)
    if i == 5:
        # The black moon: a dark disc ringed in fire.
        ring = smooth(r - 1.2, r - 0.4, d)
        dark = {0: rgb("#101014"), 1: rgb("#08080c"), 2: rgb("#0a0414")}[s]
        hot = {0: rgb("#6a6470"), 1: rgb("#fff0d0"), 2: rgb("#f0e0ff")}[s]
        c = lerp(dark, hot, ring)
    if i == 0:
        # The new moon: only the faint earthshine and the edge.
        c = lerp(lerp(rgb("#14141a"), rgb("#2a2a36") if s else rgb("#18181e"), 0.5), c, outline)
    return c + (a,)


# --- bars ---------------------------------------------------------------------------------------------------------------

def world_frame(t):
    ox, oy, w, h = WORLD_FRAME
    rng = random.Random(791)
    out = hexc("#1a1206")
    for y in range(h):
        for x in range(w):
            inner = 2 <= x < w - 2 and 2 <= y < h - 2
            if inner:
                continue
            if x == 0 or y == 0 or x == w - 1 or y == h - 1:
                # Corners clipped: the frame reads as a plate, not a box.
                if (x in (0, w - 1)) and (y in (0, h - 1)):
                    continue
                t.set(ox + x, oy + y, out)
                continue
            # The gold band: lit along the top, shadowed below, worn to brown in patches.
            v = 0.72 if y == 1 else 0.38 if y == h - 2 else 0.55
            wear = fbm(x * 2, y * 4, 792, 256, 32, 2, 12.0)
            v -= max(0.0, wear - 0.6) * 0.9
            if x % 18 == 9 and y == 1:
                v = 0.95          # little studs of bright gold along the top
            if rng.random() < 0.03:
                v -= 0.25         # nicks
            c = _gold(v)
            t.set(ox + x, oy + y, (int(c[0]), int(c[1]), int(c[2]), 255))


def world_fill(t):
    ox, oy, w, h = WORLD_FILL
    for y in range(h):
        for x in range(w):
            n = fbm(x, y, 795, 256, 8, 2, 16.0)
            v = 255 if y == 1 else int(206 + n * 30)
            t.set(ox + x, oy + y, (v, v, v, 255))


def chuck_frame(t):
    ox, oy, w, h = CHUCK_FRAME
    ink = hexc("#0d0b10")
    paper = [hexc("#f4ecd6"), hexc("#e6dbbf"), hexc("#d3c4a0")]
    for y in range(h):
        for x in range(w):
            if 2 <= x < w - 2 and 2 <= y < h - 2:
                continue
            outer = x == 0 or y == 0 or x == w - 1 or y == h - 1
            if outer:
                if (x in (0, w - 1)) and (y in (0, h - 1)):
                    continue
                # A pen line, its pressure wavering: here and there it skips and the paper shows.
                if fbm(x * 3, y * 7, 801, 512, 32, 2, 32.0) > 0.78 and 2 < x < w - 3:
                    t.set(ox + x, oy + y, paper[1])
                else:
                    t.set(ox + x, oy + y, ink)
                continue
            n = fbm(x * 2, y * 2, 802, 256, 32, 2, 12.0)
            c = paper[0] if n < 0.55 else paper[1]
            if y == h - 2:
                c = paper[2]
            t.set(ox + x, oy + y, c)
    # Two blots where the pen rested.
    for bx, by in ((17, 0), (w - 31, h - 1)):
        t.set(ox + bx, oy + by, ink)
        t.set(ox + bx + 1, oy + by, ink)
        t.set(ox + bx, oy + (1 if by == 0 else h - 2), ink)


def chuck_fill(t):
    ox, oy, w, h = CHUCK_FILL
    for y in range(h):
        for x in range(w):
            v = 255 if y == 1 else 232
            t.set(ox + x, oy + y, (v, v, v, 255))


# --- drips ------------------------------------------------------------------------------------------------------------

INK = rgb("#050308")
SHEEN = rgb("#8a6ad8")

# Per frame: the hanging bead (top, length, bulb radius), the falling drop (y, radius, stretch) and splash parts.
DRIPS = [
    dict(bead=(2.4, 2.0)),
    dict(bead=(4.2, 3.0)),
    dict(bead=(7.0, 3.4), neck=1.0),
    dict(bead=(9.8, 3.2), neck=0.5),
    dict(bead=(2.6, 2.0), drop=(11.4, 2.9, 1.4)),
    dict(bead=(1.8, 1.4), drop=(15.0, 2.7, 1.6)),
    dict(splash=(15.2, 1.0)),
    dict(bead=(1.2, 1.1), splash=(15.6, 0.5)),
]


def drip(f, x, y):
    spec = DRIPS[f]
    cx = 8.0
    best = None

    def shape(dist, hl):
        nonlocal best
        if dist < 0:
            c = lerp(INK, SHEEN, hl)
            if best is None or hl > 0:
                best = c + (1.0,)
    # The film along the top edge the bead hangs from.
    if y < 1.2 and abs(x - cx) < 5.5 - y * 2:
        shape(-1.0, 0.0)
    if "bead" in spec:
        length, rad = spec["bead"]
        neck = spec.get("neck", 1.0)
        by = length
        dist = math.hypot(x - cx, y - by) - rad
        # Neck: a tapering column from the edge down to the bulb.
        if y < by:
            wdt = (rad * 0.55 * neck) + (1 - y / max(0.1, by)) * rad * 0.3
            dist = min(dist, abs(x - cx) - wdt)
        hl = 1.0 if math.hypot(x - (cx - rad * 0.4), y - (by - rad * 0.35)) < 0.75 else 0.0
        shape(dist, hl)
    if "drop" in spec:
        dy_, rad, stretch = spec["drop"]
        u = (x - cx) / rad
        v = (y - dy_) / (rad * stretch) if y < dy_ else (y - dy_) / rad
        dist = (math.hypot(u, v) - 1) * rad
        hl = 1.0 if math.hypot(x - (cx - rad * 0.35), y - (dy_ - rad * 0.3)) < 0.7 else 0.0
        shape(dist, hl)
    if "splash" in spec:
        sy, k = spec["splash"]
        # A crown: a flat pool at the bottom edge and droplets flung up and out.
        pool = abs(y - sy) - 1.3 * k if abs(x - cx) < 6.0 * k + 1.5 else 1
        shape(pool, 0.0)
        for ddx, ddy, rr in ((-4.5, -2.5, 1.2), (4.0, -3.2, 1.1), (-2.0, -5.0, 0.9), (2.4, -6.0, 0.8), (6.2, -1.2, 0.9),
                             (-6.4, -1.4, 0.9)):
            px, py = cx + ddx * (1.4 - k * 0.4), sy + ddy * (0.75 + (1 - k) * 0.9)
            hl = 1.0 if math.hypot(x - px + 0.3, y - py + 0.3) < 0.4 else 0.0
            shape(math.hypot(x - px, y - py) - rr * (0.75 + k * 0.25), hl)
    return best


# --- assembly and the self-check ----------------------------------------------------------------------------------------

def build():
    t = Tex(W, H, 77)
    for i, fn in enumerate(SKINS):
        cell(t, skin_region(i), fn, ss=4)
    cell(t, CORONA, corona, ss=4)
    cell(t, DISC, disc, ss=4)
    for i in range(6):
        for s in range(3):
            cell(t, pip_region(i, s), lambda x, y, i=i, s=s: pip(i, s, x, y), ss=4)
    cell(t, SEAL, seal, ss=4)
    world_frame(t)
    world_fill(t)
    chuck_frame(t)
    chuck_fill(t)
    for f in range(8):
        cell(t, drip_region(f), lambda x, y, f=f: drip(f, x, y), ss=4)
    return t


def check(t):
    """Every region has content and nothing lies outside them; the frames' hollows stay clear."""
    regs = regions()
    for name, (x0, y0, w, h) in regs:
        assert any(t.rows[y][x][3] for y in range(y0, y0 + h) for x in range(x0, x0 + w)), name + " is empty"
    for y in range(H):
        for x in range(W):
            if t.rows[y][x][3] and not any(x0 <= x < x0 + w and y0 <= y < y0 + h for _, (x0, y0, w, h) in regs):
                raise AssertionError(f"pixel ({x}, {y}) lies outside every region")
    for x0, y0, w, h in (WORLD_FRAME, CHUCK_FRAME):
        assert not any(t.rows[y][x][3] for y in range(y0 + 2, y0 + h - 2) for x in range(x0 + 2, x0 + w - 2)), "frame hollow"
    x0, y0, w, h = CORONA
    assert t.rows[y0 + 32][x0 + 32][3] == 0, "corona hole"


def generate():
    t = build()
    check(t)
    save(t, "gui", "amara_hud")
    return t
