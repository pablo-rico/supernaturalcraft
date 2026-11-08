"""Season 11 (v0.42): the five Hands of God and their reliquaries. Contract: S11Assets (Hand.item, HAND_ALTAR,
RELIQUARY_BLOCKS, NEST_BLOCKS).

Items (16x16, outlined, each with a gold glint or two, the touch of the Hand of God):
  delphines_hand       a small mummified hand held upright in a corroded bronze reliquary cuff, verdigris in its chasing
  nail_of_the_cross    a long forged crucifixion nail, square-shanked, dark rust, a thin divine gold shimmer down it
  apostles_chalice     a plain cup of olive wood, worn gold at its rim, a soft light in it
  rod_of_aaron         Aaron's gnarled almond-wood staff, sprouting pink-white blossoms and leaves at its head
  trumpet_of_jericho   a long ram's horn trumpet (a shofar), ridged, its bell flared, brass bands, the mouthpiece in gold

Blocks (16x16):
  hand_altar_top/_side/_bottom  white stone and gold; on top a hand-shaped recess lined with gold where a Hand rests
  rusted_hull           U-boat hull steel, plates and rivet seams, rust blooms and pitting (tileable)
  rusted_hull_plate     one heavy riveted panel, rust running from its bolts (tileable as panels)
  thule_concrete        grey wartime bunker concrete, the formwork boards' lines, pits and water stains (tileable)
  thule_sigil_plate     a steel plate set in concrete, the Thule sun wheel in black on it: a disc, a ring, straight spokes
                        (an invented sigil: never a swastika, never the historical "black sun" of sig runes)
  crypt_bricks          old dark crypt brick, crumbling mortar, moss in the joints and dust on the ledges (tileable)
  sun_baked_bricks      desert mud brick, straw in it, sun-cracked, sand in the joints (tileable)
  ark_gold_trim         hammered gold, a border rule, a pair of cherubim's wings engraved and meeting (tileable sideways)
  monastery_stone       pale weathered ashlar, frost in the joints and rime on the faces (tileable)
  chitter_nest          woven hair and amber resin, sticky, glistening (tileable)
  chitter_egg_side/_top a glistening sac: a veined membrane, something curled dark inside; the top puckered
"""

import math

import palette as P
from chuck_art import h01
from common import art, save
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade

O = P.OUTLINE
GOLD = P.GOLD
HOLY = hexc("#fff6c8")
MUMMY = Ramp("#1c120b", "#2e2014", "#46321f", "#5e472e", "#78603f", "#937a55")
BRONZE = Ramp("#2a1a0a", "#4a3014", "#6c4a1e", "#8e652c", "#ae8240", "#cca462")
VERDI = Ramp("#1a3a30", "#2a5a48", "#3e7a62", "#5a9a7e", "#82bca0", "#b0dcc4")
IRON = Ramp("#120e0c", "#1f1915", "#2d241e", "#3d3129", "#4f4036", "#625247")
RUST = Ramp("#2a1208", "#45200e", "#643016", "#86421e", "#a65a2a", "#c47a40")
OLIVE = Ramp("#2a1c10", "#43301c", "#5e4528", "#7a5c36", "#977546", "#b39158")
ALMOND = Ramp("#24160c", "#3a2616", "#523820", "#6c4c2c", "#86623a", "#a07c4c")
LEAF = Ramp("#14280e", "#204016", "#2e5a1e", "#3f7628", "#5a9436", "#7cb24a")
BLOSSOM = Ramp("#7a4a5a", "#a86c7e", "#d08ea0", "#eab4c2", "#f8d8e0", "#ffffff")
HORN = Ramp("#2e2214", "#4c3a22", "#6e5634", "#90744a", "#b09462", "#ccb27e")
BRASS = Ramp("#3b2a10", "#5e4519", "#836226", "#a98236", "#cba354", "#e9c880")

# --- blocks' ramps
MARBLE = Ramp("#7e7a72", "#9c978d", "#b8b3a8", "#cfcac0", "#e2ded6", "#f2efe9")
HULL = Ramp("#1a1c1e", "#272a2d", "#363a3e", "#464b50", "#575d63", "#6a7076")
CONCRETE = Ramp("#3a3a39", "#4f4f4d", "#646461", "#7a7a76", "#8f8f8a", "#a4a49e")
CRYPT = Ramp("#120e0e", "#1e1817", "#2b2321", "#382e2b", "#463a36", "#554742")
MORTAR_D = Ramp("#2a2722", "#3a3630", "#4c473f", "#5e584e", "#706a5e", "#827b6e")
MOSS = Ramp("#141e0e", "#1e2e14", "#2a3e1a", "#385020", "#486228", "#5a7432")
MUD = Ramp("#5a3e22", "#76542e", "#90693c", "#a8804c", "#bd975e", "#d2ae74")
SAND = Ramp("#8a7044", "#a68a58", "#bea26c", "#d2b880", "#e2cc96", "#f0dfb0")
PALE = Ramp("#6a6a70", "#82838a", "#9a9ba2", "#b2b3b9", "#c8c9ce", "#dcdde1")
FROST = P.FROST
HAIR = Ramp("#070605", "#100d0b", "#1a1512", "#251e19", "#322822", "#40342c")
STRANDS = (hexc("#3a2c22"), hexc("#4c3a2c"), hexc("#5e4836"), hexc("#6e5a46"), hexc("#2c221c"))
RESIN = Ramp("#3a1e04", "#5e3208", "#86500e", "#ae7018", "#d29a30", "#f2c860")
SAC = Ramp("#2a1418", "#462228", "#62343a", "#80484e", "#9e6268", "#c08890")


# =====================================================================================================
# Helpers
# =====================================================================================================

def tex(fn, seed, w=16, h=16):
    t = Tex(w, h, seed)
    for y in range(h):
        for x in range(w):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def n2(x, y, seed, scale=4.0, octaves=3):
    """Seamless noise on the 16 x 16 tile (integer scales wrap)."""
    return fbm(x % 16, y % 16, seed, 16, 16, octaves, scale)


def tone(ramp, v):
    return ramp[max(0, min(len(ramp) - 1, int(round(v))))]


def glint(t, x, y, strong=True):
    """The Hand of God's touch: a gold-white sparkle (a plus when strong)."""
    t.set(x, y, HOLY)
    if strong:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if t.get(x + dx, y + dy)[3]:
                t.set(x + dx, y + dy, mix(t.get(x + dx, y + dy), GOLD[5], 0.65))


def stroke(t, pts, radius, colour):
    """A thick polyline: pts [(x, y)], radius(u) and colour(u, side) with u along the whole length 0..1 and side = signed
    distance across (negative = the lit upper-left side)."""
    segs = list(zip(pts, pts[1:]))
    lens = [math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in segs]
    total = sum(lens)
    for y in range(t.h):
        for x in range(t.w):
            px, py = x + 0.5, y + 0.5
            best = None
            acc = 0.0
            for (a, b), L in zip(segs, lens):
                dx, dy = b[0] - a[0], b[1] - a[1]
                f = max(0.0, min(1.0, ((px - a[0]) * dx + (py - a[1]) * dy) / (L * L)))
                qx, qy = a[0] + dx * f, a[1] + dy * f
                d = math.hypot(px - qx, py - qy)
                # The sign: which side of the segment (normal pointing up-left is negative).
                cross = (px - a[0]) * dy - (py - a[1]) * dx
                u = (acc + f * L) / total
                if best is None or d < best[0]:
                    best = (d, u, -1 if cross > 0 else 1)
                acc += L
            d, u, s = best
            r = radius(u)
            if d <= r:
                c = colour(u, s * d / max(0.01, r))
                if c is not None:
                    t.set(x, y, c)


# =====================================================================================================
# Items
# =====================================================================================================

def delphines_hand():
    """A small withered hand upright in a bronze cuff, verdigris in the cuff's chasing."""
    pal = {"o": O, "1": MUMMY[1], "2": MUMMY[2], "3": MUMMY[3], "4": MUMMY[4], "5": MUMMY[5], "n": MUMMY[0],
           "b": BRONZE[1], "c": BRONZE[2], "C": BRONZE[3], "B": BRONZE[4], "H": BRONZE[5],
           "v": VERDI[2], "V": VERDI[4], "g": GOLD[3], "G": GOLD[4]}
    t = art([
        "................",
        "......o.o.......",
        "....o.o4o.o.....",
        "...o4oo3oo4o....",
        "...o3o43o43o....",
        "...o3o32o32o.oo.",
        "...o3o32o32oo43o",
        "...o32n2n322o32o",
        "...o3222222232o.",
        "...o432222222o..",
        "...ooooooooooo..",
        "...oHBBBBBBCCo..",
        "...oBcVcgcVcbo..",
        "...oCcvcGcvcbo..",
        "..oHBBBBBBBCCbo.",
        "..ooooooooooooo.",
    ], pal)
    # The withered knuckles: a darker crease across the fingers.
    for x, y in ((5, 5), (8, 5), (5, 6), (8, 6)):
        t.set(x, y, MUMMY[1])
    glint(t, 8, 12)
    t.set(13, 11, GOLD[5])
    return t


def nail_of_the_cross():
    """A thick forged nail on the diagonal: a broad hammered head up and right, the square shank tapering to the point down
    and left; dark iron under rust, a thin line of gold light down its lit edge."""
    nail = Ramp(*[mix(IRON[i], RUST[i], 0.55) for i in range(6)])
    pal = {"H": mix(IRON[5], RUST[5], 0.3), "L": nail[5], "l": nail[4], "m": nail[3], "d": nail[1], "k": nail[0],
           "h": nail[4], "s": nail[2]}
    t = art([
        "................",
        "...........H....",
        "..........HLl...",
        ".........HLllm..",
        "........HLlmmmd.",
        ".........kmmmd..",
        "........hlkdd...",
        ".......hlsd.....",
        "......hlsd......",
        ".....hlsd.......",
        "....hlsd........",
        "...hlsd.........",
        "..hlsd..........",
        "..lsd...........",
        ".ls.............",
        "................",
    ], pal)
    for y in range(16):
        for x in range(16):
            c = t.get(x, y)
            if c[3] and h01("flake", x, y) < 0.16:
                t.set(x, y, mix(c, RUST[3], 0.5))           # rust blooming through
    item_outline(t, O)
    # The shimmer: gold down the lit edge, fading toward the point.
    for i, (x, y) in enumerate(((8, 6), (7, 7), (6, 8), (5, 9), (4, 10), (3, 11), (2, 12))):
        t.set(x, y, mix(nail[4], GOLD[5] if i < 3 else GOLD[4], 0.8 - i * 0.07))
    glint(t, 11, 2)
    return t


def apostles_chalice():
    """A humble cup of olive wood: a deep bowl, a short stem, a round foot; worn gold at the lip and light inside."""
    pal = {"o": O, "1": OLIVE[1], "2": OLIVE[2], "3": OLIVE[3], "4": OLIVE[4], "5": OLIVE[5], "g": GOLD[2], "G": GOLD[3],
           "Y": GOLD[4], "h": HOLY, "l": mix(GOLD[5], HOLY, 0.5), "w": GOLD[5]}
    t = art([
        "................",
        "................",
        "..oooooooooooo..",
        ".oGYYYwYYYGGgGo.",
        ".ohlwlhlwllwlGo.",
        ".o3Ywlhlwlll12o.",
        ".o453YY4YY3212o.",
        "..o4532322211o..",
        "..o4532322211o..",
        "...o45322211o...",
        "....oo4321oo....",
        "......o32o......",
        "......o42o......",
        "....oo4321oo....",
        "...o54333221o...",
        "...oooooooooo...",
    ], pal)
    # The grain: a couple of darker rings around the bowl.
    for x in range(4, 12):
        if h01("grain", x) < 0.5:
            t.set(x, 8, shade(t.get(x, 8), 0.86))
    glint(t, 6, 3)
    t.set(12, 4, GOLD[5])
    return t


def rod_of_aaron():
    """A gnarled almond-wood staff from bottom left to top right, budding: blossoms and leaves at its head."""
    t = Tex(16, 16, 42003)
    path = [(2.2, 14.2), (4.6, 11.0), (5.8, 10.2), (7.6, 7.6), (9.4, 6.4), (11.0, 3.8), (12.6, 2.4)]

    def col(u, s):
        if s < -0.3:
            c = ALMOND[4]
        elif s > 0.4:
            c = ALMOND[1]
        else:
            c = ALMOND[2] if int(u * 30) % 4 == 0 else ALMOND[3]
        return c
    stroke(t, path, lambda u: 1.25 - 0.45 * u, col)
    # Knots where it twists.
    for x, y in ((5, 10), (8, 7)):
        t.set(x, y, ALMOND[5])
        t.set(x + 1, y + 1, ALMOND[0])
    item_outline(t, O)
    # Leaves (green) sprouting off the staff, and the blossoms (pink-white, a gold heart) at its head.
    leaves = [((4, 9), (3, 8)), ((10, 7), (11, 7)), ((13, 4), (14, 5)), ((9, 3), (8, 2))]
    for (a, b) in leaves:
        t.set(*a, LEAF[3])
        t.set(*b, LEAF[4])
    for (cx, cy) in ((12, 1), (14, 2), (10, 2), (7, 5)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                t.set(x, y, BLOSSOM[4] if (dx, dy) != (0, 0) else GOLD[4])
        if 0 <= cx + 1 < 16 and 0 <= cy + 1 < 16:
            t.set(cx + 1, cy + 1, BLOSSOM[2])
    glint(t, 12, 1, strong=False)
    t.set(3, 12, GOLD[5])
    return t


def trumpet_of_jericho():
    """A long ram's horn trumpet: the gold mouthpiece bottom left, the ridged horn sweeping up and round, the flared bell
    top right turned toward the viewer, dark inside, brass bands at the bell and halfway."""
    t = Tex(16, 16, 42004)
    path = [(1.2, 14.4), (3.6, 13.8), (6.4, 13.2), (8.6, 11.8), (10.0, 9.6), (10.8, 7.2), (11.2, 4.6)]

    def col(u, s):
        if u < 0.07:
            return GOLD[4] if s < 0 else GOLD[2]        # the mouthpiece
        if abs(u - 0.45) < 0.035 or abs(u - 0.86) < 0.035:
            return BRASS[4] if s < 0 else BRASS[2]      # the brass bands
        ridge = int(u * 22) % 2 == 0
        v = 3.3 - s * 1.5 - (0.7 if ridge else 0)
        return tone(HORN, v)
    stroke(t, path, lambda u: 0.7 + 2.0 * u ** 2.6, col)
    # The bell's mouth at the top: a flared rim, lit on its far lip, the throat dark inside.
    cx, cy = 11.3, 3.4
    for y in range(7):
        for x in range(6, 16):
            ex, ey = (x + 0.5 - cx) / 3.6, (y + 0.5 - cy) / 1.9
            d = ex * ex + ey * ey
            if d <= 1.0:
                ix, iy = (x + 0.5 - cx) / 2.4, (y + 0.5 - cy - 0.15) / 0.95
                if ix * ix + iy * iy <= 1.0:
                    t.set(x, y, HORN[0] if iy > -0.2 else HORN[1])
                elif y + 0.5 < cy:
                    t.set(x, y, HORN[5] if d > 0.7 else HORN[4])
                else:
                    t.set(x, y, HORN[3] if d > 0.7 else HORN[2])
    item_outline(t, O)
    glint(t, 9, 1)
    t.set(2, 14, GOLD[5])
    return t


# =====================================================================================================
# The Hand altar
# =====================================================================================================

# The hand's silhouette pressed into the altar's top (fingers up), 8 wide x 11 tall; 'x' recess, 'r' its gold rim.
HAND_RECESS = [
    "..r.r.r.",
    ".rxrxrxr",
    ".rxrxrxr",
    ".rxrxrxr",
    "rrxxxxxr",
    "rxrxxxxr",
    "rxxxxxxr",
    ".rxxxxxr",
    ".rxxxxr.",
    "..rxxxr.",
    "..rrrr..",
]


def _marble(x, y, seed, base=2.9):
    n = n2(x, y, seed, 4.0)
    c = tone(MARBLE, base + (n - 0.5) * 1.6)
    # A faint grey vein through the stone.
    v = n2(x * 1, y * 1, seed + 5, 2.0, 2)
    if abs(v - 0.52) < 0.025:
        c = mix(c, MARBLE[1], 0.5)
    return c


def hand_altar_top():
    def f(x, y):
        if x in (0, 15) or y in (0, 15):
            return GOLD[2] if (x + y) % 2 else GOLD[3]
        if x in (1, 14) or y in (1, 14):
            return GOLD[4] if x == 1 or y == 1 else GOLD[1]
        return _marble(x, y, 43001, 3.2)
    t = tex(f, 43001)
    # The recess, centred: sunk into the stone, so its upper-left inside walls are in shadow and the floor glows faintly.
    ox, oy = 4, 3
    cells = {(ox + xx, oy + yy): ch for yy, row in enumerate(HAND_RECESS) for xx, ch in enumerate(row) if ch != "."}
    for (x, y), ch in cells.items():
        if ch == "x":
            shadow = cells.get((x - 1, y)) == "r" or cells.get((x, y - 1)) == "r"
            lit = cells.get((x + 1, y)) == "r" or cells.get((x, y + 1)) == "r"
            col = mix(MARBLE[2], GOLD[3], 0.3)
            if shadow:
                col = mix(MARBLE[0], GOLD[1], 0.3)
            elif lit:
                col = mix(MARBLE[3], GOLD[4], 0.4)
            t.set(x, y, col)
        else:
            t.set(x, y, GOLD[4] if (cells.get((x + 1, y)) == "x" or cells.get((x, y + 1)) == "x") else GOLD[2])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.set(x, y, GOLD[5])
    t.set(8, 9, HOLY)
    t.set(7, 10, GOLD[5])
    return t


def hand_altar_side():
    def f(x, y):
        if y in (0, 1):
            return GOLD[4] if y == 0 else GOLD[2]
        if y in (14, 15):
            return GOLD[3] if y == 14 else GOLD[1]
        if y == 2:
            return MARBLE[1]                               # the shadow under the top's rim
        c = _marble(x, y, 43002, 2.9)
        # A recessed gold-lined panel in the middle.
        if 3 <= x <= 12 and 4 <= y <= 12:
            if x in (3, 12) or y in (4, 12):
                return GOLD[3] if (x == 3 or y == 4) else GOLD[1]
            c = shade(_marble(x, y, 43003, 2.6), 0.92)
        return c
    t = tex(f, 43002)
    # An open right hand engraved in gold in the panel, palm out, the thumb to the left.
    pal = {"g": GOLD[3], "G": GOLD[4], "d": GOLD[1]}
    t.stamp([
        "..G.G.",
        ".GgGgG",
        ".GgGgg",
        "Gggggg",
        ".Ggggd",
        "..gggd",
        "..ggd.",
    ], pal, 5, 5)
    return t


def hand_altar_bottom():
    return tex(lambda x, y: shade(_marble(x, y, 43004, 2.4), 0.95), 43004)


# =====================================================================================================
# The reliquaries' blocks
# =====================================================================================================

def _rust(x, y, seed, base):
    """Steel under rust: blooms of orange, pitting, runs down from the seams."""
    n = n2(x, y, seed, 4.0)
    c = tone(HULL, base + (n - 0.5) * 1.4)
    r = n2(x, y, seed + 1, 2.0, 3)
    if r > 0.5:
        c = mix(c, tone(RUST, 1.5 + (r - 0.5) * 6), min(1.0, (r - 0.5) * 3.2))
    if h01("pit", seed, x, y) < 0.03:
        c = RUST[1]
    return c


def rusted_hull():
    """Two courses of hull plate (seams at rows 7 and 15), the vertical joints offset, rivets along each seam."""
    def f(x, y):
        c = _rust(x, y, 44001, 2.4)
        if y in (7, 15):
            return HULL[0]
        if y in (0, 8):
            return mix(c, HULL[4], 0.35)                    # the plate's top edge catching the light
        joint = 4 if y < 7 else 12
        if x == joint:
            return HULL[0]
        if x == (joint + 1) % 16:
            return mix(c, HULL[4], 0.3)
        # Rust run-off streaking down from the rivet line.
        if y in (1, 2, 3, 9, 10, 11) and h01("run", x) < 0.25:
            c = mix(c, RUST[3], 0.45 - 0.1 * ((y % 8) - 1))
        return c
    t = tex(f, 44001)
    for y in (5, 13):
        for x in range(1, 16, 3):
            t.set(x, y, HULL[5])
            t.set(x, y + 1, RUST[1])
    return t


def rusted_hull_plate():
    """One heavy panel: a riveted frame, the plate inside slightly sunk, rust bleeding from every bolt."""
    def f(x, y):
        c = _rust(x, y, 44011, 2.6)
        if x == 0 or y == 0:
            return mix(c, HULL[5], 0.4)
        if x == 15 or y == 15:
            return HULL[0]
        if x in (1, 14) or y in (1, 14):
            return mix(c, HULL[1], 0.3)
        if x == 2 or y == 2:
            return HULL[1]                                   # the inner plate's shadowed edge
        if x == 13 or y == 13:
            return mix(c, HULL[4], 0.4)
        return c
    t = tex(f, 44011)
    for k in (1, 5, 10, 14):
        for (x, y) in ((k, 1), (1, k), (k, 14), (14, k)):
            t.set(x, y, HULL[5])
            if y + 1 < 16 and y + 1 not in (15,):
                t.set(x, y + 1, mix(t.get(x, y + 1), RUST[3], 0.6))
            if y + 2 < 15 and h01("bleed", x, y) < 0.6:
                t.set(x, y + 2, mix(t.get(x, y + 2), RUST[2], 0.4))
    return t


def _concrete(x, y, seed, base=2.6):
    n = n2(x, y, seed, 4.0)
    c = tone(CONCRETE, base + (n - 0.5) * 1.3)
    r = h01("agg", seed, x, y)
    if r < 0.06:
        c = CONCRETE[1]                                      # a pore
    elif r > 0.95:
        c = CONCRETE[4]                                      # a light aggregate grain
    return c


def thule_concrete():
    """Wartime bunker concrete: the board marks of the formwork every four rows, water stains running down."""
    def f(x, y):
        c = _concrete(x, y, 45001)
        if y % 4 == 3:
            c = shade(c, 0.78)                               # the gap between two boards
        elif y % 4 == 0:
            c = shade(c, 1.08)
        # Wood grain printed in the concrete: faint streaks along each board.
        if h01("grain", x // 3, y) < 0.18 and y % 4 in (1, 2):
            c = shade(c, 0.94)
        # Damp stains running down from the joints.
        s = n2(x * 2, y, 45002, 2.0, 2)
        if s > 0.62:
            c = mix(c, hexc("#3e3c36"), (s - 0.62) * 1.6)
        return c
    return tex(f, 45001)


def thule_sigil_plate():
    """A steel plate in concrete, the Thule sun wheel on it in black: a disc, a ring, eight straight spokes, a hub."""
    def f(x, y):
        if x in (0, 15) or y in (0, 15):
            return _concrete(x, y, 45011, 2.3)
        if x in (1, 14) or y in (1, 14):
            return HULL[4] if (x == 1 or y == 1) else HULL[0]
        n = n2(x, y, 45012, 4.0)
        return tone(HULL, 2.5 + (n - 0.5) * 1.0)
    t = tex(f, 45011)
    cx = cy = 8.0
    blk, edge = hexc("#060606"), hexc("#1a1a1c")
    for y in range(2, 14):
        for x in range(2, 14):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            a = math.degrees(math.atan2(dy, dx)) % 45
            ring = 4.6 <= d <= 5.6
            spoke = d <= 4.6 and (a < 7 or a > 38)
            hub = d <= 1.6
            if ring or spoke or hub:
                t.set(x, y, blk)
            elif 5.6 < d <= 6.1:
                t.set(x, y, mix(t.get(x, y), edge, 0.5))
    t.set(8, 8, HULL[2])                                     # the hub's bolt
    # Bolts at the plate's corners.
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.set(x, y, HULL[5])
    return t


def _bricks(x, y, h, w, shift):
    """(brick column, row, local x, local y) of a running bond with rows h tall and bricks w wide."""
    row = y // h
    off = (row % 2) * shift
    col = ((x + off) % 16) // w
    return col, row, (x + off) % w, y % h


def crypt_bricks():
    """Old dark brick in a running bond (4 rows), crumbling mortar, moss in the low joints and dust on the ledges."""
    def f(x, y):
        col, row, lx, ly = _bricks(x, y, 4, 8, 4)
        if ly == 3 or lx == 7:
            m = tone(MORTAR_D, 1.6 + n2(x, y, 46002, 4.0) * 1.4)
            if n2(x, y, 46003, 2.0, 2) > 0.56:
                return tone(MOSS, 2 + h01("moss", x, y) * 2)      # moss creeping in the joints
            return m
        k = h01("brick", col, row)
        c = tone(CRYPT, 2.2 + k * 1.6 + (n2(x, y, 46001, 4.0) - 0.5) * 1.2)
        if ly == 0:
            c = mix(c, hexc("#7a7468"), 0.35)                # dust settled on the ledge
        if lx == 0:
            c = shade(c, 1.1)
        if h01("chip", x, y) < 0.05:
            c = CRYPT[0]
        return c
    return tex(f, 46001)


def sun_baked_bricks():
    """Desert mud brick in a running bond (4 rows), straw flecks, sun cracks, sand in the joints."""
    def f(x, y):
        col, row, lx, ly = _bricks(x, y, 4, 8, 4)
        if ly == 3 or lx == 7:
            return tone(SAND, 2.2 + n2(x, y, 47002, 4.0) * 1.5)
        k = h01("mud", col, row)
        c = tone(MUD, 2.4 + k * 1.4 + (n2(x, y, 47001, 4.0) - 0.5) * 1.2)
        if ly == 0 or lx == 0:
            c = mix(c, MUD[5], 0.3)
        if ly == 2:
            c = shade(c, 0.92)
        r = h01("straw", x, y)
        if r < 0.07:
            c = hexc("#d8c27a")                              # a straw fleck
        # A hairline sun crack in some bricks.
        if h01("crack", col, row) < 0.4 and lx == 2 + int(h01("cx", col, row) * 4) and ly in (1, 2) and h01("c2", x, y) < 0.8:
            c = MUD[0]
        return c
    return tex(f, 47001)


def ark_gold_trim():
    """Hammered gold, a rule top and bottom, and the cherubim's two wings engraved, arching up to meet in the middle over a
    small mercy seat (tileable sideways)."""
    def f(x, y):
        n = n2(x, y, 48001, 4.0)
        dent = h01("hammer", x // 2, y // 2)
        c = tone(GOLD, 3.0 + (n - 0.5) * 1.2 + (0.5 if dent > 0.8 else -0.4 if dent < 0.15 else 0))
        if y in (0, 15):
            return GOLD[1]
        if y in (1, 14):
            return GOLD[5] if y == 1 else GOLD[2]
        if y in (2, 13):
            return GOLD[2] if y == 2 else GOLD[4]
        return c
    t = tex(f, 48001)
    # The left wing (mirrored for the right): a lit leading edge arching toward the centre, the feathers hanging under it.
    half = [
        "........",
        "....hhhh",
        "..hhdddd",
        ".hdddFdF",
        "hddFdFd.",
        "dFdFd.d.",
        "d.d.d...",
        "........",
        "......ee",
        ".....eSS",
    ]
    pal = {"h": GOLD[5], "d": GOLD[1], "F": GOLD[2], "e": GOLD[1], "S": GOLD[4]}
    for yy, row in enumerate(half):
        for xx, ch in enumerate(row):
            if ch in pal:
                t.set(xx, 3 + yy, pal[ch])
                t.set(15 - xx, 3 + yy, pal[ch])
    return t


def monastery_stone():
    """Pale weathered ashlar (two courses of long blocks), frost in the joints and rime on the faces, heaviest on top."""
    def f(x, y):
        col, row, lx, ly = _bricks(x, y, 8, 16, 8)
        if ly == 7 or lx == 15:
            # Frost packed into the joints.
            return FROST[4] if h01("joint", x, y) < 0.55 else PALE[1]
        k = h01("ashlar", col, row)
        c = tone(PALE, 2.5 + k * 1.2 + (n2(x, y, 49001, 4.0) - 0.5) * 1.4)
        if ly == 0:
            c = mix(c, PALE[5], 0.4)
        if ly == 6:
            c = shade(c, 0.93)
        # Rime: a white-blue crust, thicker toward the top of each block.
        r = n2(x, y, 49002, 4.0, 2)
        if r > 0.5 + ly * 0.035:
            c = mix(c, FROST[5], 0.55)
        elif r > 0.44 + ly * 0.035 and (x + y) % 2 == 0:
            c = mix(c, FROST[4], 0.35)
        if h01("weather", x, y) < 0.04:
            c = PALE[0]
        return c
    return tex(f, 49001)


def chitter_nest():
    """Woven hair: a dark felted mat, long strands laid across it every which way (wrapping, so the tile is seamless), glued
    here and there with amber resin that glistens."""
    def f(x, y):
        n = n2(x, y, 50001, 4.0)
        c = tone(HAIR, 1.0 + (n - 0.5) * 1.2)
        r = n2(x, y, 50003, 4.0, 3)
        if r > 0.64:
            c = tone(RESIN, 1.2 + (r - 0.64) * 30)
        elif r > 0.6:
            c = mix(c, RESIN[1], 0.55)
        return c
    t = tex(f, 50001)
    rng = __import__("random").Random(50002)
    # Strands: 26 long hairs, each a gently curving walk across the tile, lighter where they lie on top.
    for i in range(26):
        x, y = rng.uniform(0, 16), rng.uniform(0, 16)
        ang = rng.uniform(0, math.pi)
        length = rng.randint(7, 15)
        col = rng.choice(STRANDS)
        for k in range(length):
            px, py = int(x) % 16, int(y) % 16
            under = t.get(px, py)
            if under[0] > 120:                       # over resin: the hair shows dark through it
                t.set(px, py, mix(under, HAIR[1], 0.5))
            else:
                t.set(px, py, col)
            ang += rng.uniform(-0.35, 0.35)
            x += math.cos(ang)
            y += math.sin(ang)
    # Wet glints on the resin.
    for y in range(16):
        for x in range(16):
            c = t.get(x, y)
            if c[0] > 200 and h01("wet", x, y) < 0.35:
                t.set(x, y, hexc("#fff0b0"))
    return t


def chitter_egg_side():
    """The sac's skin: a smooth dark-pink membrane lit from the upper left, a few veins, the curled shape of what grows
    inside showing through it, a long wet highlight."""
    def f(x, y):
        n = n2(x, y, 51001, 2.0, 2)
        lx, ly = (x + 0.5 - 6.0) / 9.0, (y + 0.5 - 5.0) / 10.0
        c = tone(SAC, 3.3 - math.hypot(lx, ly) * 1.6 + (n - 0.5) * 1.0)
        # The thing inside: a curled body (an arc open to the right) and its head at the top end.
        dx, dy = x + 0.5 - 8.0, y + 0.5 - 9.0
        r = math.hypot(dx, dy)
        a = math.degrees(math.atan2(dy, dx)) % 360
        body = abs(r - 2.9) < 1.25 and not (a < 50 or a > 330)
        head = math.hypot(x + 0.5 - 9.6, y + 0.5 - 5.2) < 1.8
        if body or head:
            c = mix(c, SAC[0], 0.55 if head else 0.45)
        elif r < 4.4:
            c = mix(c, SAC[1], 0.2)
        return c
    t = tex(f, 51001)
    # Veins: a few branching up from the bottom, thin and dark.
    veins = [[(3, 15), (3, 14), (4, 13), (4, 12), (3, 11), (3, 10), (2, 9), (2, 8)],
             [(12, 15), (12, 14), (13, 13), (13, 12), (14, 11), (14, 10)], [(4, 12), (5, 11)],
             [(13, 13), (12, 12)], [(8, 15), (8, 14)], [(1, 4), (2, 5), (2, 6)], [(14, 3), (13, 4)]]
    for v in veins:
        for x, y in v:
            t.set(x, y, mix(SAC[0], t.get(x, y), 0.35))
    # The wet highlight: a long glint over the top left, a couple of droplets.
    for x, y in ((4, 2), (5, 2), (6, 2), (3, 3), (4, 3), (3, 4)):
        t.set(x, y, SAC[5] if (x, y) not in ((3, 4), (6, 2)) else SAC[4])
    for x, y in ((12, 6), (10, 13)):
        t.set(x, y, SAC[5])
    return t


def chitter_egg_top():
    """The sac from above: the membrane drawn into a puckered knot in the middle, folds radiating, a little resin, wet."""
    def f(x, y):
        dx, dy = x + 0.5 - 8, y + 0.5 - 8
        d = math.hypot(dx, dy)
        a = math.atan2(dy, dx)
        n = n2(x, y, 51011, 2.0, 2)
        c = tone(SAC, 2.9 + (n - 0.5) * 1.2)
        if 1.6 < d < 6.5 and math.cos(a * 7 + d * 0.3) > 0.7:
            c = shade(c, 0.8)                            # the folds
        if d < 1.4:
            c = SAC[0]
        elif d < 2.4:
            c = SAC[1]
        if 1.2 < d < 2.4 and (x + y) % 3 == 0:
            c = mix(RESIN[2], SAC[1], 0.4)               # resin crusted round the knot
        return c
    t = tex(f, 51011)
    for x, y in ((4, 4), (5, 4), (4, 5), (11, 10), (10, 3)):
        t.set(x, y, SAC[5])
    return t


ITEMS = {
    "delphines_hand": delphines_hand,
    "nail_of_the_cross": nail_of_the_cross,
    "apostles_chalice": apostles_chalice,
    "rod_of_aaron": rod_of_aaron,
    "trumpet_of_jericho": trumpet_of_jericho,
}

BLOCKS = {
    "hand_altar_top": hand_altar_top,
    "hand_altar_side": hand_altar_side,
    "hand_altar_bottom": hand_altar_bottom,
    "rusted_hull": rusted_hull,
    "rusted_hull_plate": rusted_hull_plate,
    "thule_concrete": thule_concrete,
    "thule_sigil_plate": thule_sigil_plate,
    "crypt_bricks": crypt_bricks,
    "sun_baked_bricks": sun_baked_bricks,
    "ark_gold_trim": ark_gold_trim,
    "monastery_stone": monastery_stone,
    "chitter_nest": chitter_nest,
    "chitter_egg_side": chitter_egg_side,
    "chitter_egg_top": chitter_egg_top,
}


def generate():
    for name, fn in ITEMS.items():
        save(fn(), "item", name)
    for name, fn in BLOCKS.items():
        save(fn(), "block", name)


if __name__ == "__main__":
    generate()
