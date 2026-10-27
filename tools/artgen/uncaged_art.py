"""Lucifer Uncaged: the archangel out of his Cage. Geometry, six phase textures (+ glowmasks) and
every animation (his own, plus Lucifer's attacks re-used under his prefix).

  P1 Prisoner      -- burned skin split by red light, Enochian shackles and chains, no wings.
  P2 Hellfire      -- the first pair of wings, black with ember tips; the cracks blaze.
  P3 Cold          -- rime on skin and wings, the cracks run pale.
  P4 Legion        -- two pairs, charred black and blood red.
  P5 Morning Star  -- a broken halo, gold light in every crack.
  P6 Light-Bringer -- six white wings, the halo burning, the body almost all light.

He is an archangel in a man's body (the vessel), not a horned demon: a bare, burned torso under a
torn robe, iron left on him by the Cage. The rig is the shared humanoid at two texels per unit on
a 256x256 atlas. Contract with LuciferUncagedRenderer: the bones wing_r1..wing_l3 (and their
children), chain_r / chain_l / chain_neck (and their children) and halo keep their names and
parents; the renderer hides and shows them by phase. Everything added hangs below those bones:
chain links on their own swinging bones, the robe's front and back panels, the secondaries of each
wing as one bone and every primary feather on a bone of its own (so the clips can fan them).
"""

import math
import random

import lucifer_art
from animkit import AnimFile
from common import ASSETS, save
from geomodel import box
from humanoid import humanoid
from paint import SHADE
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
A = "animation.lucifer_uncaged."
D = 2  # texels per unit on everything but the smallest bits
CLEAR = (0, 0, 0, 0)

# --- palettes -------------------------------------------------------------------------------------------

CHAR = Ramp("#0b0605", "#170c0a", "#26130f", "#381c15", "#4b261c", "#5e3224")
RAW = Ramp("#3a0c08", "#5a140c", "#7a2112", "#97341e", "#b04a2e", "#c76a48")
HAIR = Ramp("#07050a", "#110c10", "#1c1418", "#2a1f22", "#3a2c2c", "#4c3a36")
ROBE = Ramp("#040305", "#09070b", "#110d14", "#1a141e", "#251c2a", "#322636")
LINEN = Ramp("#1a1410", "#2a221b", "#3b3127", "#4d4134", "#615343", "#776753")
IRON = Ramp("#060607", "#0e0e11", "#18181d", "#24242b", "#33333c", "#4a4a55")
RUST = Ramp("#1a0b06", "#2e140a", "#47200f", "#5f2d16", "#7a3d1e", "#94512a")
GOLD = Ramp("#4a2e08", "#7a5212", "#b07e22", "#dcac40", "#f6d678", "#fff4c8")
ICE = hexc("#dff4ff")
BLOOD = Ramp("#1e0204", "#3a0408", "#5c080e", "#7e1016", "#a01c20", "#c03030")

SKIN = {
    1: Ramp("#2e1a15", "#4a2c23", "#6a4033", "#895646", "#a36d59", "#bb8870"),
    2: Ramp("#2a1612", "#46271e", "#653a2c", "#835040", "#9e6852", "#b8846a"),
    3: Ramp("#24232c", "#3d3a47", "#5a5465", "#787182", "#958ea0", "#b8b2c2"),
    4: Ramp("#2a1412", "#45231e", "#62342b", "#7f483b", "#99604e", "#b47c66"),
    5: Ramp("#3a2216", "#5e3a26", "#82563a", "#a2744e", "#c09468", "#dcb88a"),
    6: Ramp("#9c8a6c", "#bba98a", "#d4c4a6", "#e8dcc4", "#f6efe0", "#ffffff"),
}
# Light through the cracks: (scorched rim, core, white-hot centre).
CRACK = {
    1: (hexc("#3a0604"), hexc("#a8140e"), hexc("#ff4a24")),
    2: (hexc("#4a0e04"), hexc("#ff5a14"), hexc("#ffd070")),
    3: (hexc("#14202c"), hexc("#7fd2f4"), hexc("#effcff")),
    4: (hexc("#2a0204"), hexc("#d01212"), hexc("#ff7050")),
    5: (hexc("#4a2a06"), hexc("#ffb838"), hexc("#fff3c4")),
    6: (hexc("#c8b48a"), hexc("#fff0c8"), hexc("#ffffff")),
}
EYE = {1: hexc("#ff2a12"), 2: hexc("#ff4a12"), 3: hexc("#bff0ff"), 4: hexc("#ff1010"), 5: hexc("#ffd060"), 6: hexc("#ffffff")}
GLYPH = {1: hexc("#ff3b1f"), 2: hexc("#ff7a24"), 3: hexc("#9fe6ff"), 4: hexc("#ff2a2a"), 5: hexc("#ffd36a"), 6: hexc("#ffffff")}
EMBER = [hexc("#c42a06"), hexc("#ff5a0a"), hexc("#ff9a24"), hexc("#ffd060")]

# Wings by phase and pair: (feather ramp, tip glow).
W_BLACK = Ramp("#050405", "#0c0a0c", "#151216", "#201b22", "#2e2731", "#3e3542")
W_RIME = Ramp("#1a2430", "#2e3e50", "#4a6078", "#7090aa", "#a0bccf", "#d8eaf6")
W_CHAR = Ramp("#040303", "#0a0707", "#130d0c", "#1f1513", "#2d1e1b", "#3b2924")
W_BLOOD = Ramp("#140103", "#2a0306", "#45070c", "#640d13", "#86171b", "#a82626")
W_BRONZE = Ramp("#120a04", "#24160a", "#3c2610", "#5a3c18", "#7e5824", "#a87a34")
W_WHITE = Ramp("#5e5038", "#7e7054", "#9e9072", "#bcb092", "#d8ceb4", "#f0ead8")


def wing_ramp(phase, pair):
    if phase == 3:
        return W_RIME
    if phase == 4:
        return W_CHAR if pair == 1 else W_BLOOD
    if phase == 5:
        return W_BRONZE
    if phase == 6:
        return W_WHITE
    return W_BLACK


ENOCHIAN = [
    ["x.x", "xxx", "..x"], ["xx.", ".x.", ".xx"], ["x..", "xxx", "x.x"], [".x.", "x.x", "xxx"],
    ["xxx", "x..", "xx."], ["x.x", ".x.", "x.x"], ["xx.", "x.x", ".xx"], [".xx", "xx.", "..x"],
]


def ramp_at(ramp, v):
    """A continuous sample of a ramp (0 = darkest), so the shading reads as a gradient, not bands."""
    v = max(0.0, min(len(ramp) - 1.0, v))
    i = int(v)
    if i >= len(ramp) - 1:
        return ramp[len(ramp) - 1]
    return mix(ramp[i], ramp[i + 1], v - i)


def dim(c, f):
    return shade(c, f)


# --- geometry -------------------------------------------------------------------------------------------

def _tag(c, tag_, **meta):
    c.tag = tag_
    c.meta = meta
    return c


def rig():
    r, p = humanoid("lucifer_uncaged", 256, 256)
    for k in ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg"):
        p[k].density = D
    head, body = r.get("head"), r.get("body")

    # Hair: a short, swept-back crop with real volume (cap, fringe, temples, the nape and a few loose locks).
    p["hair_cap"] = _tag(box(head, (-4, 29.6, -4), (8, 3, 8), inflate=0.35, density=D), "hair_cap")
    p["hair_back"] = _tag(box(head, (-4, 25.5, 3.3), (8, 5, 1), inflate=0.3, density=D), "hair_fall")
    p["hair_fringe"] = _tag(box(head, (-3.5, 28.4, -4.7), (6, 2, 1), inflate=0.05, density=D,
                                rotation=(-12, 0, 0), pivot=(0, 30, -4.2)), "hair_fall")
    for s in (-1, 1):
        x = -4.9 if s < 0 else 3.9
        p[f"hair_side_{s}"] = _tag(box(head, (x, 26.5, -1.5), (1, 4, 5), inflate=0.1, density=D), "hair_side")
    for i, (o, sz, rot) in enumerate((((-3, 32.4, -3.5), (3, 1, 4), (8, 0, 10)), ((0.3, 32.5, -2.5), (3, 1, 5), (-6, 0, -9)),
                                      ((-1.5, 32.6, 0.5), (4, 1, 3), (10, 0, 4)))):
        p[f"tuft_{i}"] = _tag(box(head, o, sz, inflate=0.15, density=D, rotation=rot,
                                  pivot=(o[0] + sz[0] / 2, o[1], o[2] + sz[2] / 2)), "hair_cap")

    # The torn robe: a sash at the waist, a drape over his left shoulder, long ragged panels front and back.
    p["belt"] = _tag(box(body, (-4, 11, -2), (8, 2, 4), inflate=0.55, density=D), "belt")
    p["belt_knot"] = _tag(box(body, (1.5, 9, -3.1), (2, 3, 1), inflate=0.1, density=D, rotation=(0, 0, -14),
                              pivot=(2.5, 12, -2.6)), "belt")
    p["drape"] = _tag(box(body, (-2.5, 11, -2), (5, 15, 4), inflate=0.35, density=D, rotation=(0, 0, 38), pivot=(0, 18, 0)), "drape")
    rf = r.bone("robe_front", (0, 12, -2.6), "body")
    p["robe_front"] = _tag(box(rf, (-3.5, 1, -3.3), (7, 11, 1), density=D), "panel", seed=1)
    rb = r.bone("robe_back", (0, 12, 2.6), "body")
    p["robe_back"] = _tag(box(rb, (-4.5, 0, 2.3), (9, 12, 1), density=D), "panel", seed=2)
    p["robe_r"] = _tag(box(r.get("right_leg"), (-4.1, 2, -2), (4, 10, 4), inflate=0.45, density=D), "robe", seed=3)
    p["robe_l"] = _tag(box(r.get("left_leg"), (0.1, 2, -2), (4, 10, 4), inflate=0.45, density=D), "robe", seed=4)

    # Pauldrons: three overlapping lames of black iron on each shoulder, with a crest.
    for side, s, arm in (("r", -1, r.get("right_arm")), ("l", 1, r.get("left_arm"))):
        x0 = -8.8 if s < 0 else 3.8
        piv = (-8.4 if s < 0 else 8.4, 24, 0)
        p[f"pauldron_{side}0"] = _tag(box(arm, (x0, 22.8, -2.6), (5, 2, 5), inflate=0.3, density=D), "iron", glyphs=True)
        p[f"pauldron_{side}1"] = _tag(box(arm, (x0 + 0.4 * s, 21.5, -2.5), (5, 1, 5), inflate=0.15, density=D,
                                          rotation=(0, 0, -14 * s), pivot=piv), "iron", glyphs=False)
        p[f"crest_{side}"] = _tag(box(arm, ((-7 if s < 0 else 5), 24.6, -2.2), (2, 1, 4), inflate=0.05, density=D), "iron", glyphs=False)

    # The broken halo: a ring of segments with a gap at the lower right, its shards floating loose, and short rays.
    halo = r.bone("halo", (0, 29.5, 5.5), "head")
    cx, cy, z = 0, 29.5, 5.0
    n = 18
    gap = {11, 12, 13}
    for i in range(n):
        if i in gap:
            continue
        a = i * 360 / n
        _tag(box(halo, (cx - 1.5, cy + 6 - 0.5, z), (3, 1, 1), inflate=0.12, density=D, rotation=(0, 0, a), pivot=(cx, cy, z + 0.5)),
             "halo", cracked=(i in (10, 14)))
    rng = random.Random(66)
    for i, a in enumerate((222, 236, 250, 262)):
        rr = 6.6 + i * 0.7
        ang = math.radians(a)
        px, py = cx + math.sin(ang) * rr, cy + math.cos(ang) * rr
        sz = (2, 1, 1) if i % 2 == 0 else (1, 1, 1)
        _tag(box(halo, (px - sz[0] / 2, py - 0.5, z + rng.uniform(-0.6, 0.8)), sz, inflate=0.05, density=D,
                 rotation=(rng.uniform(-30, 30), rng.uniform(-40, 40), a + rng.uniform(-50, 50)), pivot=(px, py, z + 0.5)), "shard")

    # Chains: an Enochian shackle on each wrist with a length of real links (alternating planes) dragging from it,
    # on two swinging bones; a collar with a chain down the chest.
    for side, s, arm in (("r", -1, "right_arm"), ("l", 1, "left_arm")):
        x0 = -8.6 if s < 0 else 3.6
        ch = r.bone(f"chain_{side}", (6 * s, 13, 0), arm)
        _tag(box(ch, (x0 + 0.6, 13, -2), (4, 2, 4), inflate=0.45, density=D), "iron", glyphs=True)
        xr = 9.0 * s
        hang = r.bone(f"chain_{side}_hang", (xr, 13.8, 0), f"chain_{side}")
        end = r.bone(f"chain_{side}_end", (xr, 13.8 - 2 * 2.4, 0), f"chain_{side}_hang")
        for i in range(5):
            yt = 13.8 - i * 2.4
            bone = hang if i < 2 else end
            if i % 2 == 0:
                _tag(box(bone, (xr - 1, yt - 3, -0.5), (2, 3, 1), density=D), "link", wide=True, broken=(i == 4))
            else:
                _tag(box(bone, (xr - 0.5, yt - 3, -1), (1, 3, 2), density=D), "link", wide=False, broken=False)
    neck = r.bone("chain_neck", (0, 24, 0), "body")
    _tag(box(neck, (-4.5, 22.4, -2.5), (9, 2, 5), inflate=0.22, density=D), "iron", glyphs=True)
    _tag(box(neck, (-1, 20.6, -3.4), (2, 2, 1), density=D), "link", wide=True, broken=False)
    nh = r.bone("chain_neck_hang", (0, 21.4, -3.4), "chain_neck")
    for i in range(4):
        yt = 21.4 - i * 2.4
        if i % 2 == 0:
            _tag(box(nh, (-0.5, yt - 3, -4.4), (1, 3, 2), density=D), "link", wide=False, broken=False)
        else:
            _tag(box(nh, (-1, yt - 3, -3.9), (2, 3, 1), density=D), "link", wide=True, broken=(i == 3))

    # Three pairs of great wings (Lucifer's bones and rest angles, so his wing clips play here too), layered:
    # the bar, lesser and greater coverts, the secondaries on one bone, and each primary on its own bone.
    r.bone("wings", (0, 21, 2), "body")
    for side, s in (("r", -1), ("l", 1)):
        wing(r, side, s)
    share_mirrored(r)
    pack(r)
    return r, p


def share_mirrored(r):
    """The left wings (and the left pauldron and shackle) reuse the right side's texels: the atlas holds one of each."""
    right = {}
    for bone, c in r.cubes():
        if bone.name.startswith(("wing_r", "chain_r")) or (bone.name == "right_arm" and c.tag == "iron"):
            right.setdefault(bone.name, []).append(c)
    for bone, c in r.cubes():
        name = bone.name
        twin = None
        if name.startswith("wing_l"):
            twin = "wing_r" + name[6:]
        elif name.startswith("chain_l"):
            twin = "chain_r" + name[7:]
        elif name == "left_arm" and c.tag == "iron":
            twin = "right_arm"
        if twin is None:
            continue
        mine = [k for k in bone.cubes if (k.tag == "iron") == (name == "left_arm")] if name == "left_arm" else bone.cubes
        theirs = right[twin]
        c.share = theirs[mine.index(c)]


def pack(r, gutter=0):
    """geomodel's shelf packing, skipping the cubes that share another's texels."""
    own = [(c.unwrap_size, i, c) for i, (_, c) in enumerate(r.cubes()) if not getattr(c, "share", None)]
    items = sorted(own, key=lambda t: (-t[0][1], -t[0][0], t[1]))
    x = y = shelf_h = 0
    for (w, h), _, c in items:
        if x + w > r.tex_w:
            x, y = 0, y + shelf_h + gutter
            shelf_h = 0
        if y + h > r.tex_h:
            raise ValueError(f"{r.identifier}: atlas {r.tex_w}x{r.tex_h} too small")
        c.box = (x, y)
        c.layout()
        x += w + gutter
        shelf_h = max(shelf_h, h)
    for _, c in r.cubes():
        if getattr(c, "share", None):
            assert c.share.unwrap_size == c.unwrap_size and c.share.size == c.size, (c.tag, c.size, c.share.size)
            c.box = c.share.box
            c.layout()


PRIMARY_FAN = 6.5  # degrees between neighbouring primaries at rest


def _feather(bone, xc, top, length, z, pair, kind, idx, s, rot=None, width=3):
    c = box(bone, (xc - width / 2, top - length, z), (width, length, 0), faces=("north", "south"), density=D,
            rotation=rot, pivot=(xc, top, z) if rot else None)
    return _tag(c, "feather", pair=pair, kind=kind, idx=idx, side=s)


def wing(r, side, s):
    """One side's three wings. s = -1 for the right (reaching to -X), +1 for the left."""
    def xs(a, b):
        """The x origin of a span from |a| to |b| out from the spine on this side."""
        return a if s > 0 else -b

    # Pair 1, the great wings.
    w1 = r.bone(f"wing_{side}1", (2 * s, 21, 2), "wings", rotation=(0, 14 * -s, 24 * -s))
    _tag(box(w1, (xs(2, 20), 20, 2), (18, 2, 1), inflate=0.18, density=D), "bar", pair=1)
    _tag(box(w1, (xs(2, 20), 17.6, 2), (18, 3, 1), inflate=0.32, density=D), "covert", pair=1, row=0)
    _tag(box(w1, (xs(2.5, 19.5), 14, 2), (17, 5, 1), inflate=0.12, density=D), "covert", pair=1, row=1)
    sec = r.bone(f"wing_{side}1_sec", (11 * s, 20, 2.5), f"wing_{side}1")
    for i in range(7):
        xc = s * (3.5 + i * 2.5)
        _feather(sec, xc, 19.5, (15, 14, 14, 13, 13, 14, 14)[i], 2.5 + (0.07 if i % 2 else -0.07), 1, "sec", i, s, rot=(0, 0, -s * (1 + i * 2.2)))
    tip = r.bone(f"wing_{side}1_tip", (20 * s, 21, 2), f"wing_{side}1", rotation=(0, 10 * -s, -12 * -s))
    _tag(box(tip, (xs(20, 34), 20, 2), (14, 2, 1), inflate=0.14, density=D), "bar", pair=1)
    _tag(box(tip, (xs(20, 34), 18.4, 2), (14, 2, 1), inflate=0.3, density=D), "covert", pair=1, row=0)
    _tag(box(tip, (xs(20.5, 33.5), 15.2, 2), (13, 4, 1), inflate=0.1, density=D), "covert", pair=1, row=1)
    for i in range(6):
        xc = s * (21.5 + i * 2.3)
        b = r.bone(f"wing_{side}1_p{i}", (xc, 20.5, 2.5), f"wing_{side}1_tip", rotation=(0, 0, -s * (3 + i * PRIMARY_FAN)))
        _feather(b, xc, 20.5, 16 + 2 * i, 2.5 + (0.06 if i % 2 else -0.06), 1, "pri", i, s, width=4)

    # Pair 2.
    w2 = r.bone(f"wing_{side}2", (2 * s, 18, 2.2), "wings", rotation=(0, 20 * -s, -4 * -s))
    _tag(box(w2, (xs(2, 18), 17, 2.5), (16, 1, 1), inflate=0.15, density=D), "bar", pair=2)
    _tag(box(w2, (xs(2, 18), 13.6, 2.5), (16, 4, 1), inflate=0.22, density=D), "covert", pair=2, row=1)
    sec2 = r.bone(f"wing_{side}2_sec", (10 * s, 17, 3.0), f"wing_{side}2")
    for i in range(6):
        xc = s * (3.5 + i * 2.5)
        _feather(sec2, xc, 16.5, (13, 12, 12, 11, 12, 12)[i], 3.0 + (0.07 if i % 2 else -0.07), 2, "sec", i, s, rot=(0, 0, -s * (1 + i * 2.0)))
    tip2 = r.bone(f"wing_{side}2_tip", (18 * s, 18, 2.5), f"wing_{side}2", rotation=(0, 8 * -s, -10 * -s))
    _tag(box(tip2, (xs(18, 30), 17, 2.5), (12, 1, 1), inflate=0.12, density=D), "bar", pair=2)
    _tag(box(tip2, (xs(18.5, 29.5), 14.2, 2.5), (11, 3, 1), inflate=0.2, density=D), "covert", pair=2, row=1)
    for i in range(5):
        xc = s * (19.5 + i * 2.3)
        b = r.bone(f"wing_{side}2_p{i}", (xc, 17.5, 3.0), f"wing_{side}2_tip", rotation=(0, 0, -s * (3 + i * PRIMARY_FAN)))
        _feather(b, xc, 17.5, 13 + round(i * 1.7), 3.0 + (0.06 if i % 2 else -0.06), 2, "pri", i, s, width=4)

    # Pair 3, the low wings.
    w3 = r.bone(f"wing_{side}3", (2 * s, 15, 2.4), "wings", rotation=(0, 26 * -s, -38 * -s))
    _tag(box(w3, (xs(2, 16), 14, 3), (14, 1, 1), inflate=0.15, density=D), "bar", pair=3)
    _tag(box(w3, (xs(2, 16), 10.6, 3), (14, 4, 1), inflate=0.2, density=D), "covert", pair=3, row=1)
    for i in range(5):
        xc = s * (4 + i * 2.6)
        b = r.bone(f"wing_{side}3_p{i}", (xc, 14.5, 3.5), f"wing_{side}3", rotation=(0, 0, -s * (2 + i * PRIMARY_FAN)))
        _feather(b, xc, 14.5, 11 + round(i * 1.5), 3.5 + (0.06 if i % 2 else -0.06), 3, "pri", i, s, width=4)


PRIMARIES = {1: 6, 2: 5, 3: 5}


def primary_bones(side, pair):
    return [f"wing_{side}{pair}_p{i}" for i in range(PRIMARIES[pair])]


# --- texture --------------------------------------------------------------------------------------------

def form(face, x, y, w, h):
    """Soft volume on a box face: darker edges and a light top, so flat faces read as round forms."""
    if face == "up":
        return 0.35
    if face == "down":
        return -0.6
    d = 0.0
    if w > 3:
        e = min(x, w - 1 - x)
        d -= 0.45 if e == 0 else (0.18 if e == 1 else 0.0)
    d += 0.25 * (1 - y / max(1, h - 1)) - 0.2
    return d


def crack_paths(seed, w, h, count, length=(10, 22)):
    """Branching light cracks as sets of texels (with momentum, so they read as fractures, not noise)."""
    rng = random.Random(seed)
    pts, hot = set(), set()
    for _ in range(count):
        x, y = rng.uniform(0, w), rng.uniform(0, h)
        ang = rng.uniform(0, math.tau)
        for step in range(rng.randint(*length)):
            ix, iy = int(x) % w, int(y) % h
            pts.add((ix, iy))
            if step % 5 == 2:
                hot.add((ix, iy))
            ang += rng.uniform(-0.45, 0.45)
            x += math.cos(ang)
            y += math.sin(ang)
            if rng.random() < 0.12:
                bx, by, ba = x, y, ang + rng.choice((-1.1, 1.1))
                for _ in range(rng.randint(2, 5)):
                    bx += math.cos(ba)
                    by += math.sin(ba)
                    pts.add((int(bx) % w, int(by) % h))
    return pts, hot


class Painter:
    """One phase of him: the albedo texture and its glowmask (additive, so dim colours glow softly)."""

    def __init__(self, rig_, parts, phase):
        self.r, self.p, self.phase = rig_, parts, phase
        self.t = Tex(rig_.tex_w, rig_.tex_h, 140 + phase)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 150 + phase)
        self.skin = SKIN[phase]

    # -- pixel helpers --------------------------------------------------------------------------------

    def fill(self, cube, material, shading=True):
        for face, (u, v, w, h) in cube.rects.items():
            for y in range(h):
                for x in range(w):
                    c = material(face, x, y, w, h)
                    if c is None:
                        continue
                    if shading and c[3]:
                        c = shade(c, SHADE[face])
                    self.t.set(u + x, v + y, c)

    def px(self, cube, face, x, y):
        u, v, w, h = cube.rects[face]
        return self.t.rows[v + y][u + x]

    def put(self, cube, face, x, y, c, glow=None):
        u, v, w, h = cube.rects[face]
        if 0 <= x < w and 0 <= y < h:
            self.t.set(u + x, v + y, c)
            if glow is not None:
                self.g.set(u + x, v + y, glow)

    def glow_at(self, cube, face, x, y, c, strength=1.0):
        self.put(cube, face, x, y, c, dim(c, strength))

    # -- the whole phase ------------------------------------------------------------------------------

    def paint(self):
        for bone, c in self.r.cubes():
            kind = c.tag
            if getattr(c, "share", None):
                continue
            if self.phase == 1 and kind in ("feather", "covert", "bar"):
                continue  # no wings yet (and the ink echo of P1 keeps them clear)
            if kind in ("head", "body", "right_arm", "left_arm"):
                self.flesh(c, kind)
            elif kind in ("right_leg", "left_leg"):
                self.legs(c)
            else:
                getattr(self, "p_" + kind)(c)
        self.face(self.p["head"])
        self.torso(self.p["body"])
        for k in ("right_arm", "left_arm"):
            self.arm(self.p[k], "east" if k == "right_arm" else "west")
        for k in ("head", "body", "right_arm", "left_arm"):
            self.cracks(self.p[k], k)
        if self.phase == 3:
            self.rime()
        if self.phase == 6:
            self.radiance()
        return self.t, self.g

    # -- flesh ----------------------------------------------------------------------------------------

    def flesh(self, cube, kind):
        ph, skin = self.phase, self.skin
        burn = {1: 0.58, 2: 0.54, 3: 0.6, 4: 0.5, 5: 0.62, 6: 0.9}[ph]
        seed = {"head": 7, "body": 11, "right_arm": 13, "left_arm": 17}[kind]

        def m(face, x, y, w, h):
            n = fbm(x * 2 + len(face) * 9, y * 2, seed, 64, 64, 3, 6.0)
            v = 3.05 + (n - 0.5) * 0.9 + form(face, x, y, w, h)
            ch = fbm(x * 2 + len(face) * 13, y * 2, seed + 40, 64, 64, 3, 7.0)
            if kind == "head":
                ch -= 0.08 if face != "north" else 0.2  # keep his face readable
            if ch > burn + 0.07:
                return ramp_at(CHAR, 2.2 + (n - 0.5) * 2.0 + form(face, x, y, w, h))
            if ch > burn:
                return ramp_at(RAW, 2.6 + (n - 0.5) * 1.6)
            return ramp_at(skin, v)
        self.fill(cube, m)

    def face(self, head):
        """The vessel's face on the 16x16 front of the head (rows 0-5 are under the hair)."""
        ph = self.phase
        sk = self.skin
        eye = EYE[ph]
        dark, mid = ramp_at(sk, 0.6), ramp_at(sk, 1.6)
        lite = ramp_at(sk, 4.4)
        rows = [
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "..BBBB....BBBB..",
            "..sddds..sddds..",
            "...sEe....eEs...",
            "...ssk....kss...",
            ".......nl.......",
            "......dnld......",
            ".c............c.",
            "..c..mmmmmm..c..",
            "...cc.llll.cc...",
        ]
        pal = {"B": ramp_at(HAIR, 1.2), "s": mid, "d": dark, "k": ramp_at(sk, 1.1), "n": lite, "l": ramp_at(sk, 3.6),
               "m": hexc("#1e0806"), "c": ramp_at(sk, 1.4)}
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in pal:
                    self.put(head, "north", x, y, pal[ch])
        # Eyes: a lit iris and a dimmer white around it.
        for x in (4, 11):
            self.glow_at(head, "north", x, 9, eye, 1.0)
        for x in (5, 10):
            self.glow_at(head, "north", x, 9, mix(eye, hexc("#000000"), 0.35), 0.7)
        # Stubble on the jaw.
        rng = random.Random(5)
        for y in range(12, 16):
            for x in range(1, 15):
                if rng.random() < 0.28 and not (5 <= x <= 10 and y == 14):
                    self.put(head, "north", x, y, mix(self.px(head, "north", x, y), HAIR[1], 0.45))
        if ph == 4:
            for x in (4, 11):  # bleeding eyes
                for y in (10, 11, 12):
                    self.put(head, "north", x, y, BLOOD[3 - (y - 10)])
        if ph >= 5:
            for x in range(6, 10):
                self.glow_at(head, "north", x, 14, CRACK[ph][1], 0.8 if ph == 6 else 0.5)
        # Ears on the sides.
        for face in ("east", "west"):
            u, v, w, h = head.rects[face]
            for y in range(8, 12):
                for x in range(7, 10):
                    c = ramp_at(sk, 2.4 if (x == 8 and 9 <= y <= 10) else 3.3)
                    self.put(head, face, x, y, c)

    def torso(self, body):
        """Relief on the bare chest and back: collarbones, chest, ribs and belly; the wing scars on the back."""
        sk = self.skin

        def rel(x, y):
            d = 0.0
            if y == 2 and 2 <= x <= 13:
                d += 0.35
            if y == 3 and 2 <= x <= 13 and x not in (7, 8):
                d -= 0.35
            for cx in (4, 11):
                if 4 <= y <= 9:
                    k = 1 - ((x - cx) / 4.0) ** 2 - ((y - 6.5) / 3.5) ** 2
                    if k > 0:
                        d += 0.5 * k
            if y == 10 and 1 <= x <= 14 and x not in (7, 8):
                d -= 0.55
            if 4 <= y <= 21 and x in (7, 8):
                d -= 0.3
            if 12 <= y <= 21 and 4 <= x <= 11 and y in (14, 17, 20):
                d -= 0.4
            if 11 <= y <= 21 and 4 <= x <= 11 and y in (12, 15, 18):
                d += 0.25
            if 12 <= y <= 22 and ((x - 1) * 4 == (y - 10) or (14 - x) * 4 == (y - 10)):
                d -= 0.3
            return d
        u, v, w, h = body.rects["north"]
        for y in range(h):
            for x in range(w):
                c = self.t.rows[v + y][u + x]
                d = rel(x, y)
                if d:
                    self.t.set(u + x, v + y, mix(c, ramp_at(sk, 5) if d > 0 else ramp_at(CHAR, 1), min(1, abs(d) * 0.55)))
        # The back: the spine, and two raw scars where the wings come through.
        u, v, w, h = body.rects["south"]
        for y in range(2, h - 2):
            c = self.t.rows[v + y][u + 7]
            self.t.set(u + 7, v + y, mix(c, CHAR[1], 0.4))
        rim, core, hot = CRACK[self.phase]
        for sx in (3, 12):
            for y in range(2, 12):
                off = 1 if 5 <= y <= 8 else 0
                self.put(body, "south", sx + off - 1, y, rim)
                self.put(body, "south", sx + off + 1, y, rim)
                self.glow_at(body, "south", sx + off, y, hot if 4 <= y <= 9 else core, 0.85)

    def arm(self, arm, inner):
        sk = self.skin
        # The side against the torso sits in shadow, so the arm reads apart from the chest.
        u, v, w, h = arm.rects[inner]
        for y in range(h):
            for x in range(w):
                self.t.set(u + x, v + y, mix(self.t.rows[v + y][u + x], CHAR[0], 0.35))
        for face in ("north", "south"):
            u, v, w, h = arm.rects[face]
            col = (w - 1) if (inner == "east") == (face == "north") else 0
            for y in range(h):
                self.t.set(u + col, v + y, mix(self.t.rows[v + y][u + col], CHAR[0], 0.45))
        for face in ("north", "south", "east", "west"):
            u, v, w, h = arm.rects[face]
            for y in range(h):
                for x in range(w):
                    c = self.t.rows[v + y][u + x]
                    if face == "north" and 5 <= y <= 10 and 2 <= x <= 5:
                        c = mix(c, ramp_at(sk, 4.6), 0.25)  # biceps
                    if y == 12:
                        c = mix(c, CHAR[1], 0.25)  # elbow crease
                    if y >= 20:
                        c = ramp_at(sk, 2.6 + (0.6 if y == 20 else 0) - (0.9 if (face == "north" and x in (2, 4, 6) and y >= 21) else 0))
                    self.t.set(u + x, v + y, c)
        # Where the shackles bit: a raw band round the wrist (under the iron in P1, a glowing scar after).
        rim, core, hot = CRACK[self.phase]
        for face in ("north", "south", "east", "west"):
            u, v, w, h = arm.rects[face]
            for x in range(w):
                for y in (17, 18, 21, 22):
                    self.put(arm, face, x, y, mix(self.px(arm, face, x, y), RAW[1], 0.6))
                if (x + len(face)) % 3:
                    self.glow_at(arm, face, x, 19 if x % 2 else 20, core, 0.55)

    def cracks(self, cube, kind):
        ph = self.phase
        rim, core, hot = CRACK[ph]
        count = {1: 2, 2: 4, 3: 3, 4: 4, 5: 6, 6: 9}[ph]
        strength = {1: 0.55, 2: 0.9, 3: 0.75, 4: 0.85, 5: 1.0, 6: 1.0}[ph]
        for face, (u, v, w, h) in cube.rects.items():
            if face == "down" or (kind == "head" and face in ("up", "south")):
                continue
            k = count if face not in ("up",) else max(1, count // 3)
            if kind == "head":
                k = max(1, k // 2)
            pts, hot_pts = crack_paths(sum(map(ord, kind + face)) * 31 + ph * 977, w, h, k)
            if kind == "head" and face == "north":
                pts = {q for q in pts if not (3 <= q[0] <= 12 and 7 <= q[1] <= 15)}  # never across the eyes and mouth
            for (x, y) in pts:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in pts:
                        self.t.set(u + nx, v + ny, mix(self.t.rows[v + ny][u + nx], rim, 0.3))
            for (x, y) in pts:
                c = hot if (x, y) in hot_pts else core
                self.glow_at(cube, face, x, y, c, strength)
            if ph == 4:  # Legion: blood runs from the cracks
                rng = random.Random(len(pts) + u)
                for (x, y) in list(pts)[::5]:
                    for dy in range(1, rng.randint(2, 5)):
                        if y + dy < h and (x, y + dy) not in pts:
                            self.put(cube, face, x, y + dy, BLOOD[max(1, 4 - dy)])

    def legs(self, cube):
        sk = self.skin

        def m(face, x, y, w, h):
            if y >= h - 4 and face != "up":  # bare, burned feet
                if face == "down":
                    return CHAR[1]
                v = 2.6 + form(face, x, y, w, h)
                if face == "north" and y >= h - 2 and x % 2 == 1:
                    v -= 1.0  # toes
                n = fbm(x, y, 31, 16, 16, 2, 4.0)
                return ramp_at(CHAR if n > 0.55 else sk, v if n <= 0.55 else v - 0.6)
            band = ((x + y) // 3) % 2
            v = (2.6 if band else 1.4) + form(face, x, y, w, h) + (fbm(x, y, 33, 16, 32, 2, 4.0) - 0.5) * 0.8
            return ramp_at(LINEN, v)
        self.fill(cube, m)

    # -- cloth ----------------------------------------------------------------------------------------

    def cloth(self, seed, ragged, trim=True, folds=0.9):
        ph = self.phase
        rng = random.Random(seed)
        cuts = [rng.choice((0, 0, 1, 2, 3, 4, 6)) for _ in range(64)]
        trim_c = {1: hexc("#3a0a0a"), 2: hexc("#5a1a08"), 3: hexc("#2a3a48"), 4: hexc("#4a0608"), 5: GOLD[3], 6: GOLD[4]}[ph]
        base = ROBE if ph < 6 else Ramp("#8a7c62", "#a39576", "#bcae8e", "#d2c6a8", "#e6dcc4", "#f6f0e2")

        def m(face, x, y, w, h):
            if face == "up":
                return None
            if face == "down":
                return None if ragged else ramp_at(base, 0.6)
            cut = cuts[(x // 2 + len(face) * 5) % 64] if ragged else 0
            if y >= h - cut:
                return None
            if ragged and y >= h - cut - 1 and (x % 2 == 0):
                return None
            n = fbm(x * 2 + len(face) * 7, y, seed, 64, 64, 2, 5.0)
            fold = math.sin(x * 1.3 + seed + y * 0.08) * folds
            v = 2.3 + fold * 0.6 + (n - 0.5) * 1.0 + form(face, x, y, w, h) * 0.6
            if trim and h - cut - 5 <= y <= h - cut - 4:
                return trim_c if (x + y) % 3 else ramp_at(base, 1)
            return ramp_at(base, v)
        return m

    def cloth_glow(self, cube, ragged_from=4):
        """Hems that smoulder (P2), freeze (P3) or shine (P5/P6)."""
        ph = self.phase
        for face, (u, v, w, h) in cube.rects.items():
            if face in ("up", "down"):
                continue
            for x in range(w):
                ys = [y for y in range(h) if self.t.rows[v + y][u + x][3]]
                if not ys:
                    continue
                y = max(ys)
                if ph == 2 and (x * 7 + u) % 3:
                    self.glow_at(cube, face, x, y, EMBER[(x + y) % 3 + 1], 0.9)
                    if (x * 5) % 4 == 0 and y > 0:
                        self.glow_at(cube, face, x, y - 1, EMBER[0], 0.6)
                elif ph == 3 and x % 2 == 0:
                    self.put(cube, face, x, y, mix(self.t.rows[v + y][u + x], ICE, 0.7))
                elif ph == 5:
                    for yy in range(h):
                        c = self.t.rows[v + yy][u + x]
                        if c == GOLD[3]:
                            self.glow_at(cube, face, x, yy, GOLD[4], 0.55)
                elif ph == 6:
                    self.glow_at(cube, face, x, y, hexc("#fff6dc"), 0.7)

    def p_robe(self, c):
        self.fill(c, self.cloth(40 + c.meta["seed"], True))
        self.cloth_glow(c)

    def p_panel(self, c):
        self.fill(c, self.cloth(60 + c.meta["seed"] * 3, True, folds=1.2))
        # Long slits up from the hem: tatters.
        rng = random.Random(70 + c.meta["seed"])
        for face in ("north", "south"):
            u, v, w, h = c.rects[face]
            for sx in range(2, w - 1, rng.choice((3, 4))):
                top = h - rng.randint(5, 11)
                for y in range(top, h):
                    self.t.set(u + sx, v + y, CLEAR)
        self.cloth_glow(c)

    def p_belt(self, c):
        def m(face, x, y, w, h):
            if face in ("up", "down"):
                return ramp_at(ROBE, 1.2)
            wrap = (y + x // 5) % 3
            return ramp_at(LINEN, 1.0 + wrap * 0.7 + form(face, x, y, w, h))
        self.fill(c, m)

    def p_drape(self, c):
        self.fill(c, self.cloth(90, False, trim=False, folds=1.0))
        ph = self.phase
        u, v, w, h = c.rects["north"]
        for y in range(0, h, 2):  # a rope of Enochian script down the drape, faintly lit
            for x in (2, 3):
                if (y * 3 + x) % 5 < 3:
                    self.glow_at(c, "north", x, y, GLYPH[ph], 0.25 if ph < 5 else 0.6)

    # -- hair -----------------------------------------------------------------------------------------

    def hair(self, x, y, w, h, seed):
        n = fbm(x * 3, y, seed, 64, 32, 2, 6.0)
        strand = 0.6 if (x * 5 + y // 3 + seed) % 4 == 0 else (-0.4 if (x * 3 + seed) % 5 == 0 else 0)
        sheen = 0.9 * math.exp(-((y - h * 0.3) ** 2) / 6.0)
        c = ramp_at(HAIR, 1.6 + (n - 0.5) * 1.2 + strand + sheen)
        if self.phase == 3 and y < 2 and (x * 3 + y) % 4 == 0:
            c = mix(c, ICE, 0.3)
        if self.phase == 6:
            c = mix(c, hexc("#d8c8a0"), 0.55)
        return c

    def p_hair_cap(self, c):
        def m(face, x, y, w, h):
            if face == "down":
                return None
            if face in ("north", "east", "west", "south") and y == h - 1 and (x * 7) % 3 == 0:
                return None
            return self.hair(x, y, w, h, 3 + len(face))
        self.fill(c, m)

    def p_hair_fall(self, c):
        rng = random.Random(9)
        cuts = [rng.randint(0, 3) for _ in range(32)]

        def m(face, x, y, w, h):
            if face in ("up", "down"):
                return self.hair(x, y, w, h, 5)
            if y >= h - cuts[x % 32]:
                return None
            return self.hair(x, y, w, h, 7 + len(face))
        self.fill(c, m)

    def p_hair_side(self, c):
        def m(face, x, y, w, h):
            if face in ("down",):
                return None
            if y >= h - 2 and (x % 3 == 0):
                return None
            return self.hair(x, y, w, h, 11)
        self.fill(c, m)

    # -- iron -----------------------------------------------------------------------------------------

    def p_iron(self, c):
        ph = self.phase
        seed = c.rects["north"][0] if "north" in c.rects else 1

        def m(face, x, y, w, h):
            n = fbm(x * 3, y, seed, 64, 32, 2, 7.0)
            v = 2.3 + (n - 0.5) * 0.9
            if face == "up":
                v += 0.6
                if x in (0, w - 1) or y in (0, h - 1):
                    v += 0.8
            elif face == "down":
                v -= 1.0
            else:
                if y == 0:
                    v += 1.5  # bevel catching the light
                elif y == 1:
                    v += 0.5
                elif y == h - 1:
                    v -= 1.0
                if x in (0, w - 1):
                    v -= 0.4
            col = ramp_at(IRON, v)
            r = fbm(x * 2, y * 2, seed + 9, 32, 32, 2, 5.0)
            if r > 0.72:
                col = mix(col, ramp_at(RUST, 2 + (r - 0.72) * 8), 0.6)
            if ph == 3 and face == "up":
                col = mix(col, ICE, 0.45)
            if ph == 6:
                col = mix(col, hexc("#c8c0b0"), 0.35)
            return col
        self.fill(c, m)
        # Rivets at the corners of the big faces, and glowing Enochian marks cut into the iron.
        for face, (u, v, w, h) in c.rects.items():
            if face in ("up", "down") or w < 6 or h < 3:
                continue
            for rx in (1, w - 2):
                self.put(c, face, rx, 1 if h > 3 else 0, IRON[5])
            if c.meta.get("glyphs") and h >= 4 and w >= 8:
                gi = (0, 1, 2, 3, 4, 6, 7)[(u * 3 + v + len(face)) % 7]
                gy0 = (h - 3) // 2
                for x in range(1, w - 1):
                    for yy in (gy0 - 1, gy0 + 3):
                        if 0 < yy < h - 1:
                            self.put(c, face, x, yy, mix(self.px(c, face, x, yy), IRON[0], 0.6))
                ox = (w - 3) // 2
                for gy, row in enumerate(ENOCHIAN[gi]):
                    for gx, ch in enumerate(row):
                        if ch == "x":
                            self.glow_at(c, face, ox + gx, gy0 + gy, GLYPH[ph], 0.8 if ph != 1 else 0.65)

    def p_link(self, c):
        wide, broken = c.meta["wide"], c.meta["broken"]
        hole_faces = ("north", "south") if wide else ("east", "west")

        def m(face, x, y, w, h):
            v = 2.6 + (0.9 if y == 0 else 0) - (0.8 if y == h - 1 else 0) + ((x + y) % 3 == 0) * 0.3
            if face in hole_faces and 1 <= x <= w - 2 and 1 <= y <= h - 2:
                return CLEAR
            if broken and face in hole_faces and x == w - 1 and y >= h // 2:
                return CLEAR
            col = ramp_at(IRON, v)
            if (x * 3 + y) % 7 == 0:
                col = mix(col, RUST[3], 0.5)
            return col
        self.fill(c, m)

    # -- halo -----------------------------------------------------------------------------------------

    def p_halo(self, c):
        ph = self.phase
        ramp = GOLD if ph < 6 else Ramp("#d8c890", "#efe0b0", "#fff0c8", "#fff6dc", "#fffaf0", "#ffffff")

        def m(face, x, y, w, h):
            v = 3.3 + (0.9 if face in ("north", "south") and y == 0 else 0) - (0.8 if y == h - 1 else 0)
            if c.meta.get("cracked") and x in (w // 2, w // 2 + 1) and face in ("north", "south"):
                return ramp_at(GOLD, 0.6)
            return ramp_at(ramp, v)
        self.fill(c, m, shading=False)
        for face, (u, v, w, h) in c.rects.items():
            for y in range(h):
                for x in range(w):
                    col = self.t.rows[v + y][u + x]
                    if col[3] and col != ramp_at(GOLD, 0.6):
                        self.g.set(u + x, v + y, dim(col, 0.85))

    def p_ray(self, c):
        self.fill(c, lambda f, x, y, w, h: ramp_at(GOLD if self.phase < 6 else W_WHITE, 5 - y * 0.4), shading=False)
        for face, (u, v, w, h) in c.rects.items():
            for y in range(h):
                for x in range(w):
                    self.g.set(u + x, v + y, dim(self.t.rows[v + y][u + x], 0.9))

    def p_shard(self, c):
        self.fill(c, lambda f, x, y, w, h: GOLD[5] if (x + y) % 2 else GOLD[4], shading=False)
        for face, (u, v, w, h) in c.rects.items():
            for y in range(h):
                for x in range(w):
                    self.g.set(u + x, v + y, self.t.rows[v + y][u + x])

    # -- wings ----------------------------------------------------------------------------------------

    def tip_glow(self, pair, frac, edge, reach=0.82):
        """Glow colour (and strength) for a feather texel; frac = 0 at the root .. 1 at the tip; reach = where the
        burning tip starts on this feather (varied per feather and per texel, so the edge is ragged)."""
        ph = self.phase
        if ph == 2:
            if frac > reach:
                return EMBER[min(3, int((frac - reach) / max(0.01, 1 - reach) * 4))], 0.95
            if edge and frac > reach - 0.2:
                return EMBER[0], 0.45
        elif ph == 3:
            if edge and frac > 0.5:
                return hexc("#cfefff"), 0.45
            if frac > 0.9:
                return hexc("#effcff"), 0.6
        elif ph == 4:
            if pair == 1 and frac > reach + 0.1 and edge:
                return EMBER[0], 0.45
            if pair == 2 and frac > reach + 0.06:
                return hexc("#c81a14"), 0.5
            if edge and frac > 0.7 and pair == 2:
                return hexc("#a01010"), 0.5
        elif ph == 5:
            if edge:
                return GOLD[4], 0.6 if frac > 0.5 else 0.3
            if frac > 0.88:
                return GOLD[5], 0.9
        elif ph == 6:
            if edge or frac > reach + 0.04:
                return hexc("#fff8e8"), 0.8
            return hexc("#fff4dc"), 0.3
        return None, 0

    def p_feather(self, c):
        ph, meta = self.phase, c.meta
        ramp = wing_ramp(ph, meta["pair"])
        primary = meta["kind"] == "pri"
        rng = random.Random(meta["pair"] * 100 + meta["idx"] * 7 + (meta["side"] > 0) * 3 + primary * 50 + ph)
        torn = ph in (2, 4) or (ph == 3 and rng.random() < 0.4)
        reach0 = rng.uniform(0.74, 0.9)
        for face, (u, v, w, h) in c.rects.items():
            cx = (w - 1) / 2.0
            nicks = {rng.randrange(h // 3, h - 3) for _ in range(rng.randint(1, 3))} if torn else set()
            for y in range(h):
                ty = h - 1 - y
                frac = y / max(1, h - 1)
                for x in range(w):
                    dx = abs(x - cx)
                    if ty == 0 and dx > 0.6 or ty == 1 and dx > 1.6 or ty == 2 and dx > 2.2:
                        self.t.set(u + x, v + y, CLEAR)
                        continue
                    if y in nicks and x == (0 if face == "north" else w - 1):
                        self.t.set(u + x, v + y, CLEAR)
                        continue
                    val = 1.6 + frac * 1.6 + (0.35 if primary else 0.0)
                    if x == w // 2:
                        val += 1.4  # the shaft
                    elif (y + int(dx * 2)) % 3 == 0:
                        val -= 0.45  # barbs
                    edge = x in (0, w - 1) or ty <= 2
                    if x in (0, w - 1):
                        val -= 0.6
                    val += rng.uniform(-0.15, 0.15)
                    col = ramp_at(ramp, val)
                    if ph == 4 and meta["pair"] == 1 and frac > 0.55 and x != w // 2:
                        col = mix(col, BLOOD[3], (frac - 0.55) * 0.9)
                    self.t.set(u + x, v + y, col)
                    reach = reach0 + fbm(x, y, meta["idx"] * 5 + meta["pair"], 8, 64, 2, 4.0) * 0.16 - 0.08
                    gc, gs = self.tip_glow(meta["pair"], frac, edge, reach)
                    if gc is not None and (ph != 2 or rng.random() < 0.8):
                        if ph == 6 and not edge and frac <= 0.8:
                            self.g.set(u + x, v + y, dim(col, gs))
                        else:
                            self.t.set(u + x, v + y, mix(col, gc, 0.75))
                            self.g.set(u + x, v + y, dim(gc, gs))

    def p_covert(self, c):
        ph = self.phase
        ramp = wing_ramp(ph, c.meta["pair"])
        small = c.meta["row"] == 0
        cw = 4 if small else 6

        def m(face, x, y, w, h):
            if face == "up":
                return ramp_at(ramp, 1.4 + ((x // 2) % 2) * 0.4)
            if face == "down":
                return ramp_at(ramp, 0.8)
            if face in ("east", "west"):
                return ramp_at(ramp, 1.6 + (y / h))
            rows = 2 if small else 1
            rh = h // rows
            row = min(rows - 1, y // rh)
            ly = y - row * rh
            off = (cw // 2) * row
            lx = (x + off) % cw
            cell = (x + off) // cw
            bottom = rh - 1 - ((cell * 7) % 2)
            if row == rows - 1 and ly > bottom:
                return None
            if row == rows - 1 and ly == bottom and lx in (0, cw - 1):
                return None
            v = 2.0 + ly / max(1, rh) * 1.4 + (0.9 if lx == cw // 2 else 0) - (0.7 if lx == 0 else 0)
            v += ((cell * 13) % 5 - 2) * 0.12
            return ramp_at(ramp, v)
        self.fill(c, m)
        for face in ("north", "south"):
            u, v, w, h = c.rects[face]
            for x in range(w):
                ys = [y for y in range(h) if self.t.rows[v + y][u + x][3]]
                if not ys:
                    continue
                y = max(ys)
                gc, gs = self.tip_glow(c.meta["pair"], 1.0 if ph in (5, 6) else 0.0, True)
                if gc is not None and ph in (3, 5, 6):
                    self.glow_at(c, face, x, y, gc, gs * 0.8)
                if ph == 6:
                    for yy in range(y):
                        self.g.set(u + x, v + yy, dim(self.t.rows[v + yy][u + x], 0.4))

    def p_bar(self, c):
        ph = self.phase
        ramp = wing_ramp(ph, c.meta["pair"])

        def m(face, x, y, w, h):
            v = 1.5 + (1.3 if (face in ("north", "south") and y == 0) or face == "up" else 0) + ((x // 2 + y) % 2) * 0.35
            return ramp_at(ramp, v)
        self.fill(c, m)
        if ph in (5, 6):
            for face, (u, v, w, h) in c.rects.items():
                if face in ("north", "south", "up"):
                    for x in range(w):
                        self.glow_at(c, face, x, 0, GOLD[4] if ph == 5 else hexc("#ffffff"), 0.5)

    # -- phase overlays -------------------------------------------------------------------------------

    def rime(self):
        """P3: frost on every upward face and the top edge of the shoulders, iron and wings."""
        rng = random.Random(33)
        for bone, c in self.r.cubes():
            if c.tag in ("feather", "halo", "shard", "hair_fall", "hair_side") or getattr(c, "share", None):
                continue
            for face, (u, v, w, h) in c.rects.items():
                rows = range(h) if face == "up" else (range(0, 2) if face in ("north", "south", "east", "west") else ())
                for y in rows:
                    for x in range(w):
                        col = self.t.rows[v + y][u + x]
                        if col[3] and rng.random() < (0.7 if face == "up" else 0.45):
                            self.t.set(u + x, v + y, mix(col, ICE, rng.uniform(0.35, 0.8)))
                            if rng.random() < 0.08:
                                self.g.set(u + x, v + y, dim(ICE, 0.35))

    def radiance(self):
        """P6: the vessel is mostly light -- every skin texel glows softly, more toward the cracks."""
        for k in ("head", "body", "right_arm", "left_arm"):
            c = self.p[k]
            for face, (u, v, w, h) in c.rects.items():
                for y in range(h):
                    for x in range(w):
                        if self.g.rows[v + y][u + x][3]:
                            continue
                        col = self.t.rows[v + y][u + x]
                        if col[3]:
                            self.g.set(u + x, v + y, dim(mix(col, hexc("#fff4dc"), 0.5), 0.55))


def paint_phase(phase):
    r, p = rig()
    return Painter(r, p, phase).paint()


# --- animations -----------------------------------------------------------------------------------------

E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_IN = "easeInQuad"
E_BACK = "easeOutBack"
E_SNAP = "easeInQuart"

# Lucifer's clips he uses as they are (under his own prefix).
SHARED = ["snap", "fling", "grasp", "summon", "wing_sweep", "rain", "fissure", "leap", "cage", "beam", "illusion",
          "collapse", "storm", "judgement", "smite_charge", "drain", "teleport"]
SIDES = (("r", -1), ("l", 1))


def osc(length, cycles, amp, phase=0.0, base=(0, 0, 0), steps_per_cycle=8):
    """Keyframes of a sine (amp is a 3-vector, phase in cycles) sampled densely enough to read smooth."""
    steps = max(4, int(round(steps_per_cycle * cycles)))
    out = []
    for i in range(steps + 1):
        t = length * i / steps
        a = math.sin(math.tau * (cycles * i / steps + phase))
        out.append((round(t, 4), [round(base[k] + amp[k] * a, 3) for k in range(3)]))
    return out


def sampled(length, fn, steps):
    """Keyframes of an arbitrary function of time."""
    return [(round(length * i / steps, 4), [round(v, 3) for v in fn(length * i / steps)]) for i in range(steps + 1)]


def chain_sway(clip, length, cycles, amp=1.0, phase=0.0):
    for side, s in SIDES:
        clip.rot(f"chain_{side}_hang", *osc(length, cycles, (5 * amp, 0, 3 * amp * s), phase))
        clip.rot(f"chain_{side}_end", *osc(length, cycles, (8 * amp, 0, 5 * amp * s), phase - 0.12))
    clip.rot("chain_neck_hang", *osc(length, cycles, (4 * amp, 0, 3 * amp), phase + 0.25))


def robe_sway(clip, length, cycles, amp=1.0, phase=0.0, front=0.0, back=0.0):
    clip.rot("robe_front", *osc(length, cycles, (3 * amp, 0, 1.5 * amp), phase, (front, 0, 0)))
    clip.rot("robe_back", *osc(length, cycles, (3 * amp, 0, -1.5 * amp), phase + 0.2, (back, 0, 0)))


def fan(clip, side, s, pair, keys):
    """Primaries of one wing: keys are (time, spread, easing); spread 1 = fanned wide, 0 = rest, -1 = folded shut."""
    for i, name in enumerate(primary_bones(side, pair)):
        a = (3 + i * PRIMARY_FAN)
        out = []
        for k in keys:
            t, sp = k[0], k[1]
            deg = -s * a * 0.9 * sp if sp >= 0 else -s * a * 0.85 * sp
            vec = [0, 0, round(deg, 2)]
            out.append((t, vec, k[2]) if len(k) > 2 else (t, vec))
        clip.rot(name, *out)


def fan_all(clip, keys, pairs=(1, 2, 3)):
    for side, s in SIDES:
        for pair in pairs:
            fan(clip, side, s, pair, keys)


def sec(clip, keys):
    """Secondaries: keys (time, degrees outward)."""
    for side, s in SIDES:
        for pair in (1, 2):
            clip.rot(f"wing_{side}{pair}_sec", *[(k[0], [0, 0, round(-s * k[1] * (1 if pair == 1 else 0.8), 2)]) + tuple(k[2:])
                                                 for k in keys])


def anims():
    base = lucifer_art.anims(A)
    f = AnimFile()
    for a in base.anims:
        if a.name[len(A):] in SHARED:
            f.add(a)

    # --- loops on the base controller ------------------------------------------------------------------
    idle = f.new(A + "idle", 4.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (2.0, [0, -0.3, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("body", (0, [0, 0, 0]), (2.0, [-1.5, 0, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [4, 0, 0]), (1.4, [6, -10, 3], E_IO), (2.8, [1, 8, -2], E_IO), (4.0, [4, 0, 0], E_IO))
    idle.rot("right_arm", (0, [0, 0, 4]), (2.0, [-5, 0, 6], E_IO), (4.0, [0, 0, 4], E_IO))
    idle.rot("left_arm", (0, [0, 0, -4]), (2.0, [-5, 0, -6], E_IO), (4.0, [0, 0, -4], E_IO))
    robe_sway(idle, 4.0, 1, 1.0)
    chain_sway(idle, 4.0, 1, 1.0)

    walk = f.new(A + "walk", 1.2, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [24 * s, 0, 0]), (0.6, [-24 * s, 0, 0], E_IO), (1.2, [24 * s, 0, 0], E_IO))
    for bone, s in (("right_arm", -1), ("left_arm", 1)):
        walk.rot(bone, (0, [16 * s, 0, 3 * -s]), (0.6, [-16 * s, 0, 3 * -s], E_IO), (1.2, [16 * s, 0, 3 * -s], E_IO))
    walk.pos("body", (0, [0, 0, 0]), (0.3, [0, 0.5, 0], E_OUT), (0.6, [0, 0, 0], E_IN), (0.9, [0, 0.5, 0], E_OUT), (1.2, [0, 0, 0], E_IN))
    walk.rot("body", (0, [4, 6, 0]), (0.6, [4, -6, 0], E_IO), (1.2, [4, 6, 0], E_IO))
    walk.rot("head", (0, [-4, -4, 0]), (0.6, [-4, 4, 0], E_IO), (1.2, [-4, -4, 0], E_IO))
    w = math.tau / 1.2
    walk.rot("robe_front", *sampled(1.2, lambda t: (-4 - 9 * abs(math.cos(w * (t - 0.08))), 0, 3 * math.sin(w * t)), 12))
    walk.rot("robe_back", *sampled(1.2, lambda t: (5 + 8 * abs(math.cos(w * (t - 0.12))), 0, -3 * math.sin(w * t)), 12))
    for side, s in SIDES:
        sign = -1 if side == "r" else 1
        walk.rot(f"chain_{side}_hang", *sampled(1.2, lambda t, g=sign: (14 * g * math.sin(w * t), 0, 2 * s), 12))
        walk.rot(f"chain_{side}_end", *sampled(1.2, lambda t, g=sign: (18 * g * math.sin(w * t - 0.7), 0, 3 * s), 12))
    walk.rot("chain_neck_hang", *sampled(1.2, lambda t: (6 * math.sin(2 * w * t - 0.5), 0, 4 * math.sin(w * t)), 12))

    fly = f.new(A + "idle_fly", 3.0, loop=True)
    fly.pos("root", (0, [0, 0, 0]), (1.5, [0, 1.5, 0], E_IO), (3.0, [0, 0, 0], E_IO))
    fly.rot("body", (0, [6, 0, 0]), (1.5, [9, 0, 0], E_IO), (3.0, [6, 0, 0], E_IO))
    fly.rot("right_leg", (0, [14, 0, 4]), (1.5, [20, 0, 6], E_IO), (3.0, [14, 0, 4], E_IO))
    fly.rot("left_leg", (0, [8, 0, -4]), (1.5, [12, 0, -6], E_IO), (3.0, [8, 0, -4], E_IO))
    fly.rot("right_arm", (0, [-10, 0, 25]), (1.5, [-14, 0, 30], E_IO), (3.0, [-10, 0, 25], E_IO))
    fly.rot("left_arm", (0, [-10, 0, -25]), (1.5, [-14, 0, -30], E_IO), (3.0, [-10, 0, -25], E_IO))
    fly.rot("head", (0, [-8, 0, 0]), (3.0, [-8, 0, 0]))
    robe_sway(fly, 3.0, 2, 2.2, front=14, back=24)
    chain_sway(fly, 3.0, 1, 1.5)

    # --- loops on the wings controller ---------------------------------------------------------------
    wi = f.new(A + "wings_idle", 12.0, loop=True)
    for side, s in SIDES:
        for n, amp in (("1", 5), ("2", 4), ("3", 3)):
            wi.rot(f"wing_{side}{n}", *osc(12.0, 3, (0, 3 * -s, amp * -s), 0.25 * int(n) / 3))
            if n != "3":
                wi.rot(f"wing_{side}{n}_tip", *osc(12.0, 3, (0, 0, 3 * -s), 0.1))
        for pair in (1, 2, 3):
            for i, name in enumerate(primary_bones(side, pair)):
                wi.rot(name, *osc(12.0, 3, (0, 0, -s * (0.6 + i * 0.5)), 0.15 + i * 0.04))
    sec(wi, [(0, 0), (6.0, 2.0, E_IO), (12.0, 0, E_IO)])
    wi.rot("halo", (0, [0, 0, 0]), (12.0, [0, 0, 360]))

    beat = f.new(A + "wings_fly", 6.0, loop=True)
    for side, s in SIDES:
        for n, amp, lag in (("1", 34, 0.0), ("2", 26, 0.06), ("3", 18, 0.12)):
            keys = [(0, [0, 0, 14 * s])]
            for b in range(6):
                keys += [(b + 0.45 + lag, [0, 6 * s, -amp * s], E_OUT), (b + 1.0, [0, 0, 14 * s], E_IN)]
            beat.rot(f"wing_{side}{n}", *keys)
        for n in ("1", "2"):
            keys = [(0, [0, 0, 10 * s])]
            for b in range(6):
                keys += [(b + 0.5, [0, 0, -18 * s], E_OUT), (b + 1.0, [0, 0, 10 * s], E_IN)]
            beat.rot(f"wing_{side}{n}_tip", *keys)
        for pair in (1, 2, 3):
            keys = [(0, -0.5)]
            for b in range(6):
                keys += [(b + 0.5, 1.0, E_OUT), (b + 1.0, -0.5, E_IN)]
            fan(beat, side, s, pair, keys)
    sec(beat, [(0, -2.0)] + [k for b in range(6) for k in ((b + 0.5, 4.0, E_OUT), (b + 1.0, -2.0, E_IN))])
    beat.rot("halo", (0, [0, 0, 0]), (6.0, [0, 0, 360]))

    # --- in chains ------------------------------------------------------------------------------------
    chained = f.new(A + "chained", 6.0, loop=True)
    chained.rot("right_arm", (0, [-150, 0, 28]), (3.0, [-155, 0, 32], E_IO), (6.0, [-150, 0, 28], E_IO))
    chained.rot("left_arm", (0, [-150, 0, -28]), (3.0, [-155, 0, -32], E_IO), (6.0, [-150, 0, -28], E_IO))
    chained.rot("head", (0, [38, 0, 0]), (2.5, [34, 6, 0], E_IO), (4.0, [10, -4, 0], E_IO), (4.6, [36, 0, 0], E_IO), (6.0, [38, 0, 0], E_IO))
    chained.rot("body", (0, [8, 0, 2]), (3.0, [10, 0, -2], E_IO), (4.0, [4, 0, 0], E_IO), (4.6, [10, 0, 1], E_IO), (6.0, [8, 0, 2], E_IO))
    chained.rot("right_leg", (0, [6, 0, 0]), (3.0, [8, 0, 1], E_IO), (6.0, [6, 0, 0], E_IO))
    chained.rot("left_leg", (0, [-2, 0, 0]), (3.0, [-4, 0, -1], E_IO), (6.0, [-2, 0, 0], E_IO))
    robe_sway(chained, 6.0, 1, 1.4)
    # The chains are taut above him; they shudder when he strains against them at 4 s.
    for side, s in SIDES:
        chained.rot(f"chain_{side}_hang", (0, [0, 0, 0]), (3.9, [0, 0, 0]), (4.05, [3, 0, 2 * s]), (4.2, [-2, 0, -1 * s]),
                    (4.4, [1, 0, 0]), (6.0, [0, 0, 0], E_IO))
        chained.rot(f"chain_{side}_end", (0, [0, 0, 0]), (4.0, [0, 0, 0]), (4.15, [5, 0, 3 * s]), (4.35, [-4, 0, -2 * s]),
                    (4.6, [2, 0, 0]), (6.0, [0, 0, 0], E_IO))
    chained.rot("chain_neck_hang", *osc(6.0, 1, (3, 0, 2)))

    # Let down from the Cage: hanging in his chains, then he tears his arms down and lifts his head.
    emerge = f.new(A + "emerge", 8.0, hold=True)
    emerge.rot("right_arm", (0, [-150, 0, 28]), (5.0, [-155, 0, 30]), (5.4, [-30, 0, 60], E_SNAP), (6.4, [-20, 0, 70], E_BACK),
               (8.0, [0, 0, 3], E_IO))
    emerge.rot("left_arm", (0, [-150, 0, -28]), (5.0, [-155, 0, -30]), (5.4, [-30, 0, -60], E_SNAP), (6.4, [-20, 0, -70], E_BACK),
               (8.0, [0, 0, -3], E_IO))
    emerge.rot("head", (0, [38, 0, 0]), (5.0, [36, 0, 0]), (5.6, [-35, 0, 0], E_BACK), (6.6, [-15, 0, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    emerge.rot("body", (0, [10, 0, 0]), (5.0, [10, 0, 0]), (5.5, [-12, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
    for side, s in SIDES:
        emerge.rot(f"chain_{side}", (0, [0, 0, 0]), (5.4, [0, 0, 0]), (5.6, [40, 0, 0], E_SNAP), (6.4, [-20, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
        emerge.rot(f"chain_{side}_hang", (0, [0, 0, 0]), (5.4, [0, 0, 0]), (5.7, [-50, 0, 20 * s], E_SNAP), (6.3, [30, 0, -10 * s], E_IO),
                   (7.0, [-12, 0, 4 * s], E_IO), (8.0, [0, 0, 0], E_IO))
        emerge.rot(f"chain_{side}_end", (0, [0, 0, 0]), (5.5, [0, 0, 0]), (5.8, [-60, 0, 25 * s], E_SNAP), (6.5, [40, 0, -15 * s], E_IO),
                   (7.2, [-15, 0, 5 * s], E_IO), (8.0, [0, 0, 0], E_IO))
    emerge.rot("chain_neck_hang", (0, [0, 0, 0]), (5.5, [0, 0, 0]), (5.8, [-35, 0, 0], E_SNAP), (6.6, [20, 0, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    emerge.rot("robe_front", (0, [0, 0, 0]), (5.4, [0, 0, 0]), (5.7, [-25, 0, 0], E_OUT), (6.6, [6, 0, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    emerge.rot("robe_back", (0, [0, 0, 0]), (5.4, [0, 0, 0]), (5.7, [28, 0, 0], E_OUT), (6.6, [-5, 0, 0], E_IO), (8.0, [0, 0, 0], E_IO))

    # --- the transformations --------------------------------------------------------------------------
    def roar(clip, start, end):
        clip.rot("body", (0, [0, 0, 0]), (start, [25, 0, 0], E_OUT), (start + 1.2, [25, 0, 0]), (start + 1.6, [-20, 0, 0], E_BACK),
                 (end, [0, 0, 0], E_IO))
        clip.rot("head", (0, [0, 0, 0]), (start, [30, 0, 0], E_OUT), (start + 1.2, [30, 0, 0]), (start + 1.6, [-45, 0, 0], E_BACK),
                 (end, [0, 0, 0], E_IO))
        clip.rot("right_arm", (0, [0, 0, 0]), (start, [-40, 0, -15], E_OUT), (start + 1.2, [-40, 0, -15]), (start + 1.6, [-25, 0, 95], E_BACK),
                 (end, [0, 0, 3], E_IO))
        clip.rot("left_arm", (0, [0, 0, 0]), (start, [-40, 0, 15], E_OUT), (start + 1.2, [-40, 0, 15]), (start + 1.6, [-25, 0, -95], E_BACK),
                 (end, [0, 0, -3], E_IO))
        clip.rot("robe_front", (0, [0, 0, 0]), (start, [6, 0, 0], E_OUT), (start + 1.6, [-22, 0, 0], E_BACK), (end, [0, 0, 0], E_IO))
        clip.rot("robe_back", (0, [0, 0, 0]), (start, [-4, 0, 0], E_OUT), (start + 1.6, [26, 0, 0], E_BACK), (end, [0, 0, 0], E_IO))

    def burst(clip, pair, at, settle):
        """A pair of wings tears out of his back: scaled up from nothing, the primaries flung wide, then settling."""
        for side, s in SIDES:
            clip.scale(f"wing_{side}{pair}", (0, [0.05, 0.05, 0.05]), (at, [0.05, 0.05, 0.05]), (at + 0.6, [1.2, 1.2, 1.2], E_BACK),
                       (settle, [1, 1, 1], E_IO))
            clip.rot(f"wing_{side}{pair}", (0, [0, 0, 0]), (at, [0, 30 * s, 40 * s]), (at + 0.6, [0, -10 * s, -20 * s], E_BACK),
                     (settle, [0, 0, 0], E_IO))
            fan(clip, side, s, pair, [(0, -1.0), (at, -1.0), (at + 0.7, 1.4, E_BACK), (settle, 0, E_IO)])

    t2 = f.new(A + "transform_2", 4.0)
    roar(t2, 1.0, 4.0)
    burst(t2, 1, 2.4, 3.8)
    for side, s in SIDES:  # the chains whip loose as they break
        t2.rot(f"chain_{side}_hang", (0, [0, 0, 0]), (2.4, [0, 0, 0]), (2.6, [-60, 0, 30 * s], E_SNAP), (4.0, [0, 0, 0], E_IO))

    t3 = f.new(A + "transform_3", 4.0)
    roar(t3, 1.0, 4.0)
    for side, s in SIDES:  # the wings close round him like a cloak against the cold, then crack open
        t3.rot(f"wing_{side}1", (0, [0, 0, 0]), (1.0, [0, 50 * s, 10 * s], E_OUT), (2.2, [0, 55 * s, 12 * s]),
               (2.7, [0, -20 * s, -30 * s], E_BACK), (4.0, [0, 0, 0], E_IO))
        fan(t3, side, s, 1, [(0, 0), (1.0, -1.0, E_OUT), (2.2, -1.0), (2.8, 1.3, E_BACK), (4.0, 0, E_IO)])

    t4 = f.new(A + "transform_4", 4.0)
    roar(t4, 1.0, 4.0)
    burst(t4, 2, 2.2, 3.6)
    for side, s in SIDES:
        t4.rot(f"wing_{side}1", (0, [0, 0, 0]), (2.2, [0, 10 * s, 15 * s], E_OUT), (2.8, [0, -12 * s, -25 * s], E_BACK), (4.0, [0, 0, 0], E_IO))

    t5 = f.new(A + "transform_5", 4.0)
    roar(t5, 1.0, 4.0)
    t5.scale("halo", (0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.9, [1.4, 1.4, 1.4], E_BACK), (3.6, [1, 1, 1], E_IO))
    t5.rot("halo", (0, [0, 0, 0]), (2.2, [0, 0, -90]), (3.6, [0, 0, 0], E_OUT))
    fan_all(t5, [(0, 0), (2.4, 1.3, E_OUT), (3.2, 1.3), (4.0, 0, E_IO)], pairs=(1, 2))
    sec(t5, [(0, 0), (2.4, 6, E_OUT), (4.0, 0, E_IO)])

    t6 = f.new(A + "transform_6", 8.0)
    t6.pos("root", (0, [0, 0, 0]), (1.5, [0, -2, 0], E_OUT), (5.5, [0, 18, 0], E_IO), (6.2, [0, 16, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    t6.rot("body", (0, [0, 0, 0]), (1.5, [35, 0, 0], E_OUT), (4.5, [30, 0, 0]), (5.6, [-25, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
    t6.rot("head", (0, [0, 0, 0]), (1.5, [40, 0, 0], E_OUT), (4.5, [35, 0, 0]), (5.6, [-55, 0, 0], E_BACK), (8.0, [-6, 0, 0], E_IO))
    t6.rot("right_arm", (0, [0, 0, 0]), (1.5, [-40, 0, -10], E_OUT), (4.5, [-45, 0, -10]), (5.6, [-30, 0, 125], E_BACK), (8.0, [-10, 0, 25], E_IO))
    t6.rot("left_arm", (0, [0, 0, 0]), (1.5, [-40, 0, 10], E_OUT), (4.5, [-45, 0, 10]), (5.6, [-30, 0, -125], E_BACK), (8.0, [-10, 0, -25], E_IO))
    t6.rot("robe_front", (0, [0, 0, 0]), (1.5, [8, 0, 0], E_OUT), (5.5, [20, 0, 0], E_IO), (5.8, [-10, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
    t6.rot("robe_back", (0, [0, 0, 0]), (1.5, [-6, 0, 0], E_OUT), (5.5, [30, 0, 0], E_IO), (5.8, [40, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
    for side, s in SIDES:  # the old wings wrap him as he rises, then all six open at once
        for pair in ("1", "2"):
            t6.rot(f"wing_{side}{pair}", (0, [0, 0, 0]), (1.5, [0, 45 * s, 20 * s], E_OUT), (4.5, [0, 50 * s, 20 * s]),
                   (5.4, [0, -15 * s, -30 * s], E_BACK), (8.0, [0, 0, 0], E_IO))
    burst(t6, 3, 4.5, 6.5)
    fan_all(t6, [(0, 0), (1.5, -1.0, E_OUT), (4.5, -1.0), (5.5, 1.5, E_BACK), (6.8, 1.0), (8.0, 0, E_IO)], pairs=(1, 2))
    t6.scale("halo", (0, [1, 1, 1]), (5.4, [1, 1, 1]), (5.9, [1.6, 1.6, 1.6], E_BACK), (7.0, [1, 1, 1], E_IO))
    t6.rot("halo", (0, [0, 0, 0]), (5.4, [0, 0, 40]), (8.0, [0, 0, 360], E_OUT))

    # Dragged back into the Cage: arms hauled up by the chains, wings folding away, the halo shattering.
    death = f.new(A + "death", 10.0, hold=True)
    death.rot("right_arm", (0, [0, 0, 0]), (1.5, [-60, 0, 40], E_OUT), (3.0, [-155, 0, 30], E_SNAP), (10.0, [-160, 0, 28]))
    death.rot("left_arm", (0, [0, 0, 0]), (1.5, [-60, 0, -40], E_OUT), (3.0, [-155, 0, -30], E_SNAP), (10.0, [-160, 0, -28]))
    death.rot("head", (0, [0, 0, 0]), (1.5, [-40, 0, 0], E_OUT), (3.0, [-30, 0, 0]), (6.0, [40, 0, 0], E_IO), (10.0, [45, 0, 0]))
    death.rot("body", (0, [0, 0, 0]), (1.5, [-15, 0, 0], E_OUT), (3.0, [12, 0, 0], E_SNAP), (10.0, [10, 0, 0]))
    death.rot("right_leg", (0, [0, 0, 0]), (3.0, [15, 0, 4], E_IO), (10.0, [8, 0, 2]))
    death.rot("left_leg", (0, [0, 0, 0]), (3.0, [-8, 0, -4], E_IO), (10.0, [-4, 0, -2]))
    death.rot("robe_front", (0, [0, 0, 0]), (1.5, [-14, 0, 0], E_OUT), (3.0, [10, 0, 0], E_SNAP), (5.0, [4, 0, 0], E_IO), (10.0, [5, 0, 0]))
    death.rot("robe_back", (0, [0, 0, 0]), (1.5, [16, 0, 0], E_OUT), (3.0, [-6, 0, 0], E_SNAP), (5.0, [3, 0, 0], E_IO), (10.0, [2, 0, 0]))
    for side, s in SIDES:
        for n in ("1", "2", "3"):
            death.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (3.0, [0, 30 * s, -50 * s], E_IO), (7.0, [0, -60 * s, 40 * s], E_IN))
            death.scale(f"wing_{side}{n}", (0, [1, 1, 1]), (6.0, [1, 1, 1]), (9.0, [0.05, 0.05, 0.05], E_IN), (10.0, [0.05, 0.05, 0.05]))
    fan_all(death, [(0, 0), (1.5, 1.2, E_OUT), (3.0, -1.0, E_SNAP), (10.0, -1.0)])
    sec(death, [(0, 0), (1.5, 4, E_OUT), (3.0, -3, E_SNAP), (10.0, -3)])
    death.rot("halo", (0, [0, 0, 0]), (6.0, [0, 0, 120], E_IN), (9.0, [0, 0, 400], E_IN), (10.0, [0, 0, 400]))
    death.scale("halo", (0, [1, 1, 1]), (6.0, [1.3, 1.3, 1.3]), (9.0, [0, 0, 0], E_IN), (10.0, [0, 0, 0]))

    # --- his own attacks --------------------------------------------------------------------------------
    def attack(name, length):
        return f.new(A + name, length)

    lash = attack("chain_lash", 2.7)
    lash.rot("right_arm", (0, [0, 0, 0]), (1.0, [-120, 0, -40], E_OUT), (1.25, [-20, 0, 60], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("left_arm", (0, [0, 0, 0]), (1.0, [-120, 0, 40], E_OUT), (1.25, [-20, 0, -60], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("chain_r", (0, [0, 0, 0]), (1.0, [60, 0, 0], E_OUT), (1.3, [-90, 0, 0], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("chain_l", (0, [0, 0, 0]), (1.0, [60, 0, 0], E_OUT), (1.3, [-90, 0, 0], E_SNAP), (2.7, [0, 0, 0], E_IO))
    for side, s in SIDES:
        lash.rot(f"chain_{side}_end", (0, [0, 0, 0]), (1.0, [40, 0, 0], E_OUT), (1.35, [-70, 0, 0], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("body", (0, [0, 0, 0]), (1.0, [-12, 0, 0], E_OUT), (1.25, [18, 0, 0], E_SNAP), (2.7, [0, 0, 0], E_IO))

    pull = attack("shackle_pull", 3.4)
    pull.rot("right_arm", (0, [0, 0, 0]), (0.8, [-90, 0, 40], E_OUT), (2.2, [-60, 0, 10], E_IO), (2.4, [-150, 0, 20], E_SNAP), (3.4, [0, 0, 0], E_IO))
    pull.rot("left_arm", (0, [0, 0, 0]), (0.8, [-90, 0, -40], E_OUT), (2.2, [-60, 0, -10], E_IO), (2.4, [-150, 0, -20], E_SNAP), (3.4, [0, 0, 0], E_IO))
    pull.rot("body", (0, [0, 0, 0]), (0.8, [10, 0, 0], E_OUT), (2.2, [-15, 0, 0], E_IO), (2.4, [25, 0, 0], E_SNAP), (3.4, [0, 0, 0], E_IO))

    pillars = attack("pillars", 3.7)
    pillars.rot("right_arm", (0, [0, 0, 0]), (1.4, [-170, 0, 15], E_OUT), (1.6, [-20, 0, 10], E_SNAP), (3.7, [0, 0, 0], E_IO))
    pillars.rot("left_arm", (0, [0, 0, 0]), (1.4, [-170, 0, -15], E_OUT), (1.6, [-20, 0, -10], E_SNAP), (3.7, [0, 0, 0], E_IO))
    pillars.rot("body", (0, [0, 0, 0]), (1.4, [-15, 0, 0], E_OUT), (1.6, [30, 0, 0], E_SNAP), (2.6, [25, 0, 0]), (3.7, [0, 0, 0], E_IO))
    pillars.pos("body", (0, [0, 0, 0]), (1.6, [0, -2.5, 0], E_SNAP), (2.6, [0, -2.5, 0]), (3.7, [0, 0, 0], E_IO))

    hounds = attack("hounds", 3.0)
    hounds.rot("right_arm", (0, [0, 0, 0]), (1.0, [-80, 0, 20], E_OUT), (1.5, [-30, 0, 70], E_BACK), (3.0, [0, 0, 0], E_IO))
    hounds.rot("head", (0, [0, 0, 0]), (1.0, [20, -20, 0], E_OUT), (1.5, [-25, 0, 0], E_BACK), (3.0, [0, 0, 0], E_IO))

    prison = attack("frozen_prison", 6.0)
    prison.rot("right_arm", (0, [0, 0, 0]), (1.0, [-95, -10, 0], E_OUT), (1.1, [-90, -10, -15], E_SNAP), (5.0, [-90, -10, -10]), (6.0, [0, 0, 0], E_IO))
    prison.rot("head", (0, [0, 0, 0]), (1.0, [5, -12, 0], E_OUT), (5.0, [5, -12, 0]), (6.0, [0, 0, 0], E_IO))

    abyss = attack("abyssal_pull", 6.0)
    abyss.rot("right_arm", (0, [0, 0, 0]), (1.5, [-40, 0, 100], E_OUT), (5.0, [-45, 0, 105]), (6.0, [0, 0, 0], E_IO))
    abyss.rot("left_arm", (0, [0, 0, 0]), (1.5, [-40, 0, -100], E_OUT), (5.0, [-45, 0, -105]), (6.0, [0, 0, 0], E_IO))
    abyss.rot("head", (0, [0, 0, 0]), (1.5, [30, 0, 0], E_OUT), (5.0, [30, 0, 0]), (6.0, [0, 0, 0], E_IO))

    stars = attack("falling_stars", 5.0)
    stars.rot("right_arm", (0, [0, 0, 0]), (1.8, [-178, 0, 10], E_OUT), (2.0, [-60, 0, 30], E_SNAP), (4.0, [-60, 0, 30]), (5.0, [0, 0, 0], E_IO))
    stars.rot("left_arm", (0, [0, 0, 0]), (1.8, [-178, 0, -10], E_OUT), (2.0, [-60, 0, -30], E_SNAP), (4.0, [-60, 0, -30]), (5.0, [0, 0, 0], E_IO))
    stars.rot("head", (0, [0, 0, 0]), (1.8, [-45, 0, 0], E_OUT), (2.0, [10, 0, 0], E_SNAP), (5.0, [0, 0, 0], E_IO))

    tempest = attack("tempest", 3.5)
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2", "3"):
            tempest.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (1.6, [0, 45 * s, 30 * s], E_OUT), (1.85, [0, -85 * s, -10 * s], E_SNAP),
                        (3.5, [0, 0, 0], E_IO))
    tempest.rot("body", (0, [0, 0, 0]), (1.6, [-14, 0, 0], E_OUT), (1.85, [18, 0, 0], E_SNAP), (3.5, [0, 0, 0], E_IO))
    fan_all(tempest, [(0, 0), (1.6, -1.0, E_OUT), (1.9, 1.4, E_SNAP), (3.5, 0, E_IO)])

    nova = attack("supernova", 7.5)
    nova.pos("root", (0, [0, 0, 0]), (4.5, [0, 8, 0], E_IO), (4.6, [0, 6, 0], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.rot("right_arm", (0, [0, 0, 0]), (3.0, [-60, 0, 120], E_OUT), (4.5, [-70, 0, 135]), (4.6, [-20, 0, 30], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.rot("left_arm", (0, [0, 0, 0]), (3.0, [-60, 0, -120], E_OUT), (4.5, [-70, 0, -135]), (4.6, [-20, 0, -30], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.rot("head", (0, [0, 0, 0]), (3.0, [-45, 0, 0], E_OUT), (4.5, [-50, 0, 0]), (4.6, [25, 0, 0], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.scale("halo", (0, [1, 1, 1]), (4.5, [1.8, 1.8, 1.8], E_IO), (4.6, [1, 1, 1], E_SNAP), (7.5, [1, 1, 1]))
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2", "3"):
            nova.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (4.5, [0, -20 * s, 50 * s], E_OUT), (4.6, [0, 25 * s, -15 * s], E_SNAP),
                     (7.5, [0, 0, 0], E_IO))
    fan_all(nova, [(0, 0), (4.5, -0.8, E_OUT), (4.6, 1.5, E_SNAP), (7.5, 0, E_IO)])
    return f


def generate():
    r, _ = rig()
    r.write(GEO + "lucifer_uncaged.geo.json")
    for phase in range(1, 7):
        t, g = paint_phase(phase)
        save(t, "entity", f"lucifer_uncaged_p{phase}")
        save(g, "entity", f"lucifer_uncaged_p{phase}_glowmask")
    a = anims()
    a.write(ANIM + "lucifer_uncaged.animation.json")
    with open(ASSETS + "/animations/entity/lucifer_uncaged.names.txt", "w") as fh:
        fh.write("\n".join(a.names()) + "\n")
