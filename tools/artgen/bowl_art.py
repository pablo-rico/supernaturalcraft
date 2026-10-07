"""The Spell Bowl: a wide, low octagonal bowl of old bronze, green with verdigris, an Enochian band
engraved round its waist. Writes its block model (also drawn in hand by the item renderer), its
textures, the greyscale liquid the renderer tints with the mix colour, and the bowl's smoke puffs.

The bowl is built from boxes: an octagonal foot and floor (four crossed strips each), three octagonal
wall bands (4 straight walls + 4 walls turned 45 degrees) and a lip that overhangs inward. Every wall
is 5 px or more from the centre above y = 1.1, because the code draws the liquid there as a disc of
radius 5 between y = 1.2 and 4.4 (the lip starts at 4.5). Where boxes overlap, the turned ones are
nudged a hair up/down so coplanar faces never z-fight.

Textures are 32x32 (two texels per model pixel): the bowl is seen up close in the hand.
"""

import math
import random

from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix
from spire_art import write_model

BRONZE = Ramp("#2b1a0e", "#553619", "#7f5427", "#a87538", "#cb9a52", "#e9c47f")
BRIGHT = Ramp("#5a3a1a", "#8a5f2c", "#b3843f", "#d4a65a", "#ecc97f", "#fbe7b4")
VERDIGRIS = Ramp("#14352e", "#215446", "#317562", "#4b967d", "#73b99d", "#a6d9c1")
SOOT = hexc("#160f0b")

T = 2  # texels per model pixel
TAN = math.tan(math.radians(22.5))
EPS = 0.05  # nudge between overlapping coplanar faces (0.003 blocks: no z-fight within ~50 blocks)
NS = "supernaturalcraft:block/"


# --- materials ------------------------------------------------------------------------------

def _clamp(v, lo=0, hi=5):
    return max(lo, min(hi, v))


def bronze_px(x, y, seed, base=2.6, patina=0.56, w=32, h=32, ramp=BRONZE):
    """Old bronze: mottled metal, verdigris blooming where the second noise runs high."""
    n = fbm(x, y, seed, w, h, 3, 4.0)
    p = fbm(x, y, seed + 17, w, h, 3, 4.0) * 0.75 + fbm(x, y, seed + 31, w, h, 2, 16.0) * 0.25
    if p > patina + 0.05:
        return VERDIGRIS[_clamp(int(round(2.2 + (p - patina) * 9 + (n - 0.5) * 1.5)), 1, 5)]
    if p > patina:
        return mix(ramp[1], VERDIGRIS[1], 0.55)  # the crusty edge of a patina bloom
    return ramp[_clamp(int(round(base + (n - 0.5) * 2.2)))]


def hammer(t, seed, count, ramp=BRONZE, x0=0, y0=0, x1=31, y1=31):
    """Hammer marks: a lit top-left texel over a shadowed bottom-right one."""
    rng = random.Random(seed)
    for _ in range(count):
        x, y = rng.randint(x0, x1 - 1), rng.randint(y0, y1 - 1)
        c = t.get(x, y)
        if c[1] > c[0]:  # leave the patina alone
            continue
        t.set(x, y, ramp[4])
        t.set(x + 1, y + 1, ramp[1])


def bronze_tex():
    t = Tex(32, 32, 1101)
    for y in range(32):
        for x in range(32):
            # Rows 26+ dress the belly and foot (default uv = height): verdigris gathers low down.
            t.set(x, y, bronze_px(x, y, 1101, 2.5, 0.55 if y >= 26 else 0.63))
    hammer(t, 1102, 22)
    return t


def rim_tex():
    """The lip: rubbed bright by hands, patina only clinging in the odd pit."""
    t = Tex(32, 32, 1111)
    for y in range(32):
        for x in range(32):
            t.set(x, y, bronze_px(x, y, 1111, 3.2, 0.70, ramp=BRIGHT))
    hammer(t, 1112, 18, BRIGHT)
    # The lip's top is mapped onto two bevel strips (see RIM_TOP): outer edge lit, inner edge in shade.
    # Rows 16-18 run along u (north/south-facing tops), columns 16-18 along v (east/west ones).
    bevel = (BRIGHT[5], None, BRIGHT[1])
    for k, c in enumerate(bevel):
        for s in range(20):
            for (x, y) in ((s, 16 + k), (16 + k, s)):
                base = t.get(x, y)
                if c is None:
                    t.set(x, y, mix(base, BRIGHT[3], 0.4))
                else:
                    t.set(x, y, mix(base, c, 0.7 if k == 0 else 0.6))
    return t


def inner_tex():
    """Inside walls: rows map to height (row 22 = y 5, row 31 = y 0.5). Darker toward the floor,
    patina crusted low down, and faint tide marks left by old brews."""
    t = Tex(32, 32, 1121)
    for y in range(32):
        hy = 16 - y / T  # model height of this row on the walls
        low = max(0.0, min(1.0, (3.6 - hy) / 2.6))
        for x in range(32):
            c = bronze_px(x, y, 1121, 2.1 - low * 0.9, 0.64 - low * 0.08)
            if hy < 2.0 and fbm(x, y, 1122, 32, 32, 2, 8.0) > 0.55:
                c = mix(c, SOOT, 0.5)
            t.set(x, y, c)
    for row, k in ((24, 0.35), (26, 0.25), (28, 0.3)):  # tide marks at y 4, 3, 2
        for x in range(32):
            if fbm(x, row, 1123 + row, 32, 32, 2, 6.0) > 0.35:
                t.set(x, row, mix(t.get(x, row), hexc("#d8c9a0"), k))
    return t


def floor_tex():
    """The floor, seen from above (uv = world x/z): soot burnt into the centre."""
    t = Tex(32, 32, 1131)
    for y in range(32):
        for x in range(32):
            c = bronze_px(x, y, 1131, 1.6, 0.64)
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            k = max(0.0, 1 - d / 9) * 0.75 * (0.6 + fbm(x, y, 1132, 32, 32, 2, 8.0) * 0.8)
            t.set(x, y, mix(c, SOOT, min(0.85, k)))
    return t


def sigil_tex():
    """A decal on the floor: a double ring with seven ticks round a small cross, engraved and greened."""
    t = Tex(32, 32, 1141)
    cx = cy = 16.0
    groove = lambda x, y: VERDIGRIS[3] if fbm(x, y, 1142, 32, 32, 2, 8.0) > 0.45 else VERDIGRIS[1]
    for y in range(32):
        for x in range(32):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if 6.1 <= d <= 7.0 or 3.4 <= d <= 4.2:
                t.set(x, y, groove(x, y))
            elif 7.0 < d <= 7.6 or 4.2 < d <= 4.7:
                t.set(x, y, BRONZE[4])  # the lit far edge of each groove
    for i in range(7):
        a = 2 * math.pi * i / 7
        for r in (4.8, 5.4, 6.0):
            x, y = int(cx + r * math.sin(a)), int(cy - r * math.cos(a))
            t.set(x, y, groove(x, y))
    for k in (-1, 0, 1):
        t.set(15 + k, 15, groove(15 + k, 15))
        t.set(15, 15 + k, groove(15, 15 + k))
    t.set(16, 16, BRONZE[4])
    return t


# Enochian-flavoured glyphs, 3 wide by 4 tall.
GLYPHS = [
    ["#.#", "#.#", ".#.", ".#."],
    ["##.", "#.#", "##.", "#.."],
    ["###", "..#", ".#.", "#.."],
    ["#..", "###", "#.#", "#.#"],
    [".#.", "#.#", "###", "#.#"],
    ["###", "#..", "#.#", "###"],
    ["#.#", "###", "..#", "..#"],
    [".##", "#..", ".#.", "##."],
    ["#.#", ".#.", "#.#", "#.#"],
    ["###", ".#.", ".#.", "##."],
    ["..#", ".##", "#.#", "..#"],
    ["#..", "#.#", "###", "..#"],
    [".#.", "###", ".#.", "#.#"],
    ["##.", ".##", "#..", "###"],
    ["#.#", "#.#", "###", ".#."],
    [".##", ".#.", "##.", ".#."],
]

BAND_W, BAND_H = 10, 6  # texels per wall face of the rune band (5 x 3 model pixels)


def band_slot(i):
    """uv rectangle of the i-th wall face's rune strip."""
    col, row = i % 3, i // 3
    return [col * 5, row * 3, col * 5 + 5, row * 3 + 3]


def runes_tex():
    t = Tex(32, 32, 1151)
    for y in range(32):
        for x in range(32):
            t.set(x, y, bronze_px(x, y, 1151, 3.0, 0.68))
    rng = random.Random(1152)
    order = list(range(len(GLYPHS)))
    rng.shuffle(order)
    for i in range(8):
        ox, oy = (i % 3) * BAND_W, (i // 3) * BAND_H
        # Grooves framing the band, catching light on their lower lip.
        for x in range(BAND_W):
            t.set(ox + x, oy, BRONZE[1])
            t.set(ox + x, oy + BAND_H - 1, BRONZE[1] if (x + i) % 4 else VERDIGRIS[2])
        for g, gx in ((order[(2 * i) % 16], 1), (order[(2 * i + 1) % 16], 5)):
            glyph = GLYPHS[g]
            for r, row in enumerate(glyph):
                for c, ch in enumerate(row):
                    if ch != "#":
                        continue
                    x, y = ox + gx + c, oy + 1 + r
                    green = fbm(x, y, 1153, 32, 32, 2, 8.0) > 0.52
                    t.set(x, y, VERDIGRIS[2] if green else BRONZE[0])
            # Lit lip on the right of each stroke.
            for r, row in enumerate(glyph):
                for c, ch in enumerate(row):
                    if ch == "#" and (c == 2 or row[c + 1] != "#"):
                        x, y = ox + gx + c + 1, oy + 1 + r
                        if x < ox + BAND_W and t.get(x, y) not in (BRONZE[0], VERDIGRIS[2]):
                            t.set(x, y, BRONZE[4])
        # A diamond separator between this face's glyphs and the next face's.
        t.set(ox + 9, oy + 2, BRONZE[4])
        t.set(ox + 9, oy + 3, BRONZE[1])
    return t


# --- model ----------------------------------------------------------------------------------

def _face(tex, uv=None):
    f = {"texture": "#" + tex}
    if uv is not None:
        f["uv"] = [round(v, 4) for v in uv]
    return f


def _el(name, frm, to, faces, rot=None):
    e = {"name": name, "from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
    if rot is not None:
        e["rotation"] = {"origin": [round(v, 4) for v in rot[0]], "axis": "y", "angle": rot[1]}
    return e


def rim_top_uv(i, outer):
    """The lip's top, mapped so the rim texture's bevel strip runs lit on the outer edge."""
    o = (i % 3) * 3
    if outer == "east":
        return [9.5, o, 8, o + 5]
    if outer == "west":
        return [8, o, 9.5, o + 5]
    if outer == "south":
        return [o, 9.5, o + 5, 8]
    return [o, 8, o + 5, 9.5]


def ring(name, y0, y1, r_out, r_in, out_tex, in_tex, top=None, bottom=None, out_uv=None, in_uv=None, lift=(0, 0),
         top_uv=None):
    """An octagonal band of wall. `out_uv(i)` gives the outer face's uv for face i (N, NE, E, SE, S, SW, W, NW)."""
    s = 2 * r_out * TAN
    th = r_out - r_in
    els = []
    c = 8.0
    straight = {  # face index -> (from, to, outer face, inner face)
        0: ((c - s / 2, y0, c - r_out), (c + s / 2, y1, c - r_in), "north", "south"),
        2: ((c + r_in, y0, c - s / 2), (c + r_out, y1, c + s / 2), "east", "west"),
        4: ((c - s / 2, y0, c + r_in), (c + s / 2, y1, c + r_out), "south", "north"),
        6: ((c - r_out, y0, c - s / 2), (c - r_in, y1, c + s / 2), "west", "east"),
    }
    rc = (r_out + r_in) / 2 / math.sqrt(2)
    diagonal = {  # face index -> (centre dx, dz, angle, outer face)
        1: (rc, -rc, -45, "north"),
        3: (rc, rc, 45, "south"),
        5: (-rc, rc, -45, "south"),
        7: (-rc, -rc, 45, "north"),
    }
    for i in range(8):
        faces = {}
        if i in straight:
            frm, to, outer, inner = straight[i]
            rot = None
            lo, hi = y0, y1
        else:
            dx, dz, angle, outer = diagonal[i]
            inner = "south" if outer == "north" else "north"
            lo, hi = y0 - lift[0], y1 + lift[1]
            frm = (c + dx - s / 2, lo, c + dz - th / 2)
            to = (c + dx + s / 2, hi, c + dz + th / 2)
            rot = ((c + dx, (y0 + y1) / 2, c + dz), angle)
        faces[outer] = _face(out_tex, out_uv(i, lo, hi) if out_uv else None)
        faces[inner] = _face(in_tex, in_uv(i, lo, hi) if in_uv else None)
        if top:
            faces["up"] = _face(top, top_uv(i, outer) if top_uv else None)
        if bottom:
            faces["down"] = _face(bottom)
        els.append(_el(f"{name}_{i}", frm, to, faces, rot))
    return els


def strips(name, y0, y1, r, faces_for, step=EPS):
    """An octagonal slab as four crossed strips (0, 90, +45, -45 degrees); each one's ends are two
    of the octagon's sides. Strip k is raised/lowered by k*step so overlaps never z-fight."""
    w = r * TAN
    c = 8.0
    out = []
    for k, (angle, along_x) in enumerate(((0, True), (0, False), (45, True), (-45, True))):
        lo, hi = y0 - k * step, y1 + k * step
        if along_x:
            frm, to = (c - r, lo, c - w), (c + r, hi, c + w)
            ends = ("west", "east")
        else:
            frm, to = (c - w, lo, c - r), (c + w, hi, c + r)
            ends = ("north", "south")
        rot = ((c, (y0 + y1) / 2, c), angle) if angle else None
        out.append(_el(f"{name}_{k}", frm, to, faces_for(ends), rot))
    return out


def model():
    els = []
    # Foot: an octagon of radius 4, 0.6 tall; only its rim faces and underside show.
    els += strips("foot", 0.0, 0.6, 4.0, lambda ends: {ends[0]: _face("bronze"), ends[1]: _face("bronze"), "down": _face("bronze")})
    # Floor: radius 5, top at y 1.1 (below the lowest liquid at 1.2); its rim hides inside the lower band.
    els += strips("floor", 0.6, 1.1 - 3 * EPS, 5.0, lambda ends: {"up": _face("floor"), "down": _face("bronze")})
    # The engraved sigil on the floor (a decal plane just above it).
    els.append(_el("sigil", (4.5, 1.12, 4.5), (11.5, 1.12, 11.5), {"up": _face("sigil", [4.5, 4.5, 11.5, 11.5])}))

    def in_uv(i, lo, hi):
        return [(i % 3) * 5, 16 - hi, (i % 3) * 5 + 5, 16 - lo]  # rows map to height (see inner_tex)

    # Lower band: plain bronze, flaring out from the foot.
    els += ring("belly", 0.5, 1.5, 5.5, 5.0, "bronze", "inner", bottom="bronze", in_uv=in_uv, lift=(EPS, 0))
    # Rune band: the engraved octagon, each face its own strip of glyphs.
    els += ring("runes", 1.5, 4.5, 6.0, 5.0, "runes", "inner", bottom="bronze",
                out_uv=lambda i, lo, hi: band_slot(i), in_uv=in_uv, lift=(EPS, 0))
    # Lip: polished, overhanging inward to radius 4.5.
    els += ring("lip", 4.5, 5.0, 6.0, 4.5, "rim", "rim", top="rim", bottom="inner",
                out_uv=lambda i, lo, hi: [(i % 3) * 5, 1, (i % 3) * 5 + 5, 1.5],
                in_uv=lambda i, lo, hi: [(i % 3) * 5, 3, (i % 3) * 5 + 4, 3.5],
                lift=(0, EPS), top_uv=rim_top_uv)
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {
            "particle": NS + "spell_bowl_bronze",
            "bronze": NS + "spell_bowl_bronze",
            "rim": NS + "spell_bowl_rim",
            "runes": NS + "spell_bowl_runes",
            "inner": NS + "spell_bowl_inner",
            "floor": NS + "spell_bowl_floor",
            "sigil": NS + "spell_bowl_sigil",
        },
        "elements": els,
    }


# --- liquid and smoke -------------------------------------------------------------------------

def liquid_tex():
    """16x16 greyscale, tileable: soft ripples with thin bright caustic lines. Tinted at runtime."""
    t = Tex(16, 16, 1161)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1161, 16, 16, 3, 4.0)
            r = fbm(x + 3, y + 5, 1162, 16, 16, 2, 4.0)
            ridge = 1 - abs(2 * r - 1)
            v = 0.80 + (n - 0.5) * 0.22 + max(0.0, ridge - 0.78) * 0.9
            g = int(max(170, min(255, v * 255)))
            t.set(x, y, (g, g, g, 255))
    return t


def smoke_frame(i):
    """Soft white puffs: frame 0 a dense cauliflower, frame 3 thin wisps. Tinted at runtime."""
    t = Tex(16, 16, 1170 + i)
    rng = random.Random(1170 + i)
    lobes = []
    spread = 2.2 + i * 0.9
    for k in range(5 + i):
        a = rng.uniform(0, 2 * math.pi)
        d = rng.uniform(0, spread)
        lobes.append((8 + math.cos(a) * d, 8.3 + math.sin(a) * d * 0.85, rng.uniform(2.6, 3.8) - i * 0.35))
    density = (1.0, 0.82, 0.6, 0.38)[i]
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            f = 0.0
            lit = 0.0
            for (cx, cy, r) in lobes:
                d = math.hypot(px - cx, py - cy) / r
                if d < 1.4:
                    w = max(0.0, 1 - d * d / 1.96)
                    f += w
                    lit += w * (-(px - cx) - (py - cy)) / r  # light from the top-left
            if f <= 0.02:
                continue
            noise = fbm(x, y, 1180 + i, 16, 16, 2, 4.0)
            a = min(1.0, f * 0.9) * density * (0.7 + noise * 0.5)
            if i >= 2:
                a *= 0.55 + 0.9 * max(0.0, noise - 0.35)  # breaking up into wisps
            if a < 0.06:
                continue
            g = int(max(196, min(255, 228 + lit * 10 + (noise - 0.5) * 30)))
            t.set(x, y, (g, g, g, int(min(235, a * 255))))
    return t


def generate():
    save(bronze_tex(), "block", "spell_bowl_bronze")
    save(rim_tex(), "block", "spell_bowl_rim")
    save(runes_tex(), "block", "spell_bowl_runes")
    save(inner_tex(), "block", "spell_bowl_inner")
    save(floor_tex(), "block", "spell_bowl_floor")
    save(sigil_tex(), "block", "spell_bowl_sigil")
    write_model("spell_bowl", model())
    save(liquid_tex(), "block", "bowl_liquid")
    for i in range(4):
        save(smoke_frame(i), "particle", f"bowl_smoke_{i}")


if __name__ == "__main__":
    generate()
