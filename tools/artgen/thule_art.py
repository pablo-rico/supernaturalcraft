"""The Thule Society's dead (v0.42, season 11): the raised soldier and the necromancer officer who raises him. Rigs, textures +
glowmasks and clips; the names are S11Assets.THULE_SOLDIER / THULE_OFFICER (geo/entity/<rig>.geo.json,
animations/entity/<rig>.animation.json, textures/entity/<rig>.png and <rig>_glowmask.png).

thule_soldier -- a soldier dug up from 1945 (32 px to the crown): a 1940s grey field uniform rotted through (grave stains,
holes onto dead flesh and ribs, frayed hems), a grey peaked field cap, grey-blue dead skin pulled tight over the skull, sunken
sockets with a pale lit pupil (glowmask), black jackboots. On his left arm a bone-white band with the society's BLACK SUN (a
wheel of twelve bent spokes; never any other insignia). A bolt rifle with a fixed bayonet in his right fist (`rifle`: the
barrel along the hanging hand's -Y, the stock along +Y; every clip orients it), the left hand on the forestock.

thule_officer -- the necromancer (33 px): a long black leather greatcoat, double-breasted, to the shins (`coat_r`, `coat_l`,
`coat_tail` swing), a Sam Browne strap, a black peaked officer's cap with a silver cord, a pale narrow face, cold violet-white
eyes (glowmask). In his right hand a rod of black iron (`staff`) topped with the sun wheel (`staff_sun`, its wheel lit
violet-white in the glowmask).

Skeleton: apoc_vessel's (root, body, head, eyes, brows, arms with forearm / hand / finger bones, legs with shins) plus `cap`
(head) and, by rig, `rifle` (right_hand) or `staff` -- `staff_sun` (right_hand). The officer adds the coat bones.

Also home to small rig helpers banshee_art and chitters_art share: forward kinematics on a pose (`world`), the lowest point of a
posed rig (`lowest`, to plant feet and bodies on the ground) and a coordinate-descent solver (`solve`) used to put the off hand
on the rifle and to orient the rifle and the staff.
"""

import math
import os

import horsemen_art as H
import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, coat, hair_m, head, leather_m, legs, neck, skeleton,
                         skin_m, tone, torso)
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, fbm, hexc, mix, shade

# The S11Assets contract (kept in sync by hand; check() verifies every rig against it).
CONTRACT = {
    "thule_soldier": (("idle", "walk", "aim_idle"), ("rise", "stab", "shoot", "death")),
    "thule_officer": (("idle", "walk"), ("raise", "cast", "strike", "death")),
    "banshee": (("idle", "float"), ("scream", "claw", "vanish", "appear", "death")),
    "chitter": (("idle", "walk", "crawl"), ("pounce", "bite", "screech", "death")),
}


# =====================================================================================================
# Rig helpers (shared with banshee_art and chitters_art)
# =====================================================================================================

def _rx(a, p):
    c, s = math.cos(a), math.sin(a)
    return (p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c)


def _ry(a, p):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c)


def _rz(a, p):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2])


def _split(pose):
    out = {}
    for k, v in pose.items():
        if k[0] == "@":
            out.setdefault(k[1:], {})["p"] = v
        elif k[0] == "%":
            out.setdefault(k[1:], {})["s"] = v
        else:
            out.setdefault(k, {})["r"] = v
    return out


def _bones(rig):
    return {b.name: b for b in rig.bones}


def _apply(by, sp, bone, q):
    """q in java space, through the bone chain (preview3d's / GeckoLib's math)."""
    b = by[bone]
    while b is not None:
        px, py, pz = -b.pivot[0], b.pivot[1], b.pivot[2]
        rest = b.rotation or (0, 0, 0)
        a = sp.get(b.name, {})
        rot, pos, scl = a.get("r") or (0, 0, 0), a.get("p") or (0, 0, 0), a.get("s") or (1, 1, 1)
        bx, by_, bz = [math.radians(rest[i] + rot[i]) for i in range(3)]
        d = ((q[0] - px) * scl[0], (q[1] - py) * scl[1], (q[2] - pz) * scl[2])
        d = _rz(bz, _ry(-by_, _rx(-bx, d)))
        q = (d[0] + px - pos[0], d[1] + py + pos[1], d[2] + pz + pos[2])
        b = by.get(b.parent) if b.parent else None
    return q


def world(rig, pose, bone, pt):
    """Where a point (Bedrock px, in the rest pose, attached to `bone`) ends up under a Clip pose; Bedrock coordinates
    (+x = the entity's left, -z = in front)."""
    q = _apply(_bones(rig), _split(pose), bone, (-pt[0], pt[1], pt[2]))
    return (-q[0], q[1], q[2])


def lowest(rig, pose, skip=()):
    """The lowest Y of every cube corner of the posed rig (bones scaled to ~0 and `skip` ignored)."""
    by, sp = _bones(rig), _split(pose)

    def gone(b):
        while b is not None:
            if b.name in skip:
                return True
            s = sp.get(b.name, {}).get("s")
            if s and min(abs(v) for v in s) < 0.05:
                return True
            b = by.get(b.parent) if b.parent else None
        return False
    low = 1e9
    for b in rig.bones:
        if gone(b):
            continue
        for c in b.cubes:
            x0, y0, z0 = c.origin
            w, h, d = c.size
            i = c.inflate
            for cx in (x0 - i, x0 + w + i):
                for cy in (y0 - i, y0 + h + i):
                    for cz in (z0 - i, z0 + d + i):
                        q = (-cx, cy, cz)
                        if c.rotation:
                            cp = c.pivot or (0, 0, 0)
                            pj = (-cp[0], cp[1], cp[2])
                            dq = (q[0] - pj[0], q[1] - pj[1], q[2] - pj[2])
                            dq = _rz(math.radians(c.rotation[2]), _ry(-math.radians(c.rotation[1]),
                                                                      _rx(-math.radians(c.rotation[0]), dq)))
                            q = (dq[0] + pj[0], dq[1] + pj[1], dq[2] + pj[2])
                        low = min(low, _apply(by, sp, b.name, q)[1])
    return low


def ground(rig, pose, at=0.0, skip=()):
    """The pose with @root lifted / lowered so its lowest point sits at `at`."""
    cur = pose.get("@root", [0, 0, 0])
    lo = lowest(rig, pose, skip)
    return P_(pose, {"@root": [cur[0], cur[1] + at - lo, cur[2]]})


def solve(pose, chans, cost, span=80.0, floor=0.25):
    """Coordinate descent over `chans` ((key, axis) pairs) from `pose`; returns the improved pose."""
    p = {k: list(v) for k, v in pose.items()}
    for k, _ in chans:
        p.setdefault(k, [0.0, 0.0, 0.0])
    best = cost(p)
    step = span / 2
    while step >= floor:
        moved = True
        while moved:
            moved = False
            for k, i in chans:
                for d in (step, -step):
                    p[k][i] += d
                    c = cost(p)
                    if c < best - 1e-7:
                        best = c
                        moved = True
                        break
                    p[k][i] -= d
        step /= 2
    return {k: [round(v, 1) for v in vs] if k[0] not in "@%" else vs for k, vs in p.items()}


def _norm(v):
    n = math.sqrt(sum(c * c for c in v)) or 1.0
    return [c / n for c in v]


def aim_cost(rig, bone, a, b, target, reg=None):
    """Cost of the segment a -> b (local points on `bone`) pointing away from `target` (a direction)."""
    t = _norm(target)

    def c(p):
        pa, pb = world(rig, p, bone, a), world(rig, p, bone, b)
        d = _norm([pb[i] - pa[i] for i in range(3)])
        return 1 - sum(d[i] * t[i] for i in range(3)) + (reg(p) if reg else 0)
    return c


def reach_cost(rig, bone, pt, target, reg=None):
    def c(p):
        w = world(rig, p, bone, pt)
        return math.sqrt(sum((w[i] - target[i]) ** 2 for i in range(3))) + (reg(p) if reg else 0)
    return c


def small(keys, k=0.0004):
    """A regulariser keeping the named angles small."""
    return lambda p: k * sum(sum(v * v for v in p.get(n, (0, 0, 0))) for n in keys)


def check(name, model, f):
    """Every S11Assets clip present (loop flags right), every keyed bone existing."""
    loops, once = CONTRACT[name]
    have = {a.name: a for a in f.anims}
    bones = {b.name for b in model.rig.bones}
    assert len(bones) == len(model.rig.bones), name + ": duplicate bones"
    for c in loops + once:
        a = have.get(f"animation.{name}.{c}")
        assert a is not None, f"{name}: missing clip {c}"
        assert (a.loop is True) == (c in loops), f"{name}.{c}: loop flag"
    for a in f.anims:
        unknown = set(a.bones) - bones
        assert not unknown, f"{a.name} keys missing bones {sorted(unknown)}"
    return True


SCRATCH = os.environ.get("SN_ART_PREVIEW", K.SCRATCH)


def sun_wheel(cx, cy, r_out, r_in=None, spokes=12, twist=0.55, width=0.42):
    """The Thule black sun as a texel test: a solid disc and `spokes` thin rays, each bent sideways as it runs out (the
    "jagged" spokes) -- no other figure. Returns fn(x, y) -> 0 (clear), 1 (wheel)."""
    r_in = r_out * 0.36 if r_in is None else r_in

    def f(x, y):
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        r = math.hypot(dx, dy)
        if r <= r_in:
            return 1
        if r > r_out:
            return 0
        a = math.atan2(dy, dx) - twist * (r - r_in) / max(0.5, r_out - r_in)
        k = (a / (2 * math.pi) * spokes) % 1.0
        return 1 if min(k, 1 - k) * 2 * math.pi * r / spokes < width else 0
    return f


# =====================================================================================================
# The soldier
# =====================================================================================================

S = Body(HIP=12.2, NECK=24.0, TW=3.8, TD=2.0, AW=3.3, SY=23.4, ELBOW=18.4, WRIST=13.9, PALM=12.0, LX=1.9, LW=3.6, KNEE=6.4,
         SHOE_H=1.4)

DEAD = Ramp("#27323a", "#3c4b53", "#54656b", "#6b7d80", "#829495", "#9eb0ae")
FIELD = Ramp("#1a1d18", "#262b23", "#343a2f", "#43493b", "#535a49", "#686f5b")
COLLAR = Ramp("#121512", "#1a1f1b", "#232a25", "#2e3630", "#3a443c", "#4a564c")
STAIN = hexc("#3a3022")
MOULD = hexc("#4f5a3a")
FLESH = Ramp("#120a0b", "#21131a", "#2f1d22", "#3f2a2c", "#523b38", "#6b5148")
BONE = Ramp("#5b5444", "#7f7660", "#a39a80", "#bdb497", "#d2caae", "#e6dfc6")
BOOT = Ramp("#040405", "#09090b", "#111114", "#1b1b20", "#29292f", "#4c4c56")
CAPR = Ramp("#181b16", "#22261f", "#2f3429", "#3d4336", "#4c5343", "#5f6754")
WOOD = Ramp("#1f1209", "#2f1b0e", "#432713", "#58341a", "#6e4423", "#8a5a31")
STEEL = Ramp("#1a1c20", "#2d3036", "#43474e", "#5d636b", "#7d838b", "#a9aeb5")
BAND = Ramp("#5d5848", "#7e7862", "#9d967c", "#b6ae93", "#c9c2a7", "#dcd6bd")
SUN = hexc("#0b0b0d")
SOCKET = Ramp("#06080a", "#0b1014", "#121a20", "#1a242c")
EYE_HOT, EYE, EYE_DIM = hexc("#eefaff"), hexc("#a6dcf0"), hexc("#4e8aa6")

S_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhsssssssssrrshh",
    "sssssssssssrssss",
    "sbbbbbssssbbbbbs",
    "skOOOOssssOOOOks",
    "sOgGGOssssOGGgOs",
    "skOggOcsscOggOks",
    "sccuuusnnsuuuccs",
    "scsssssnnssssscs",
    "scssssNnnNsssscs",
    "sjcsssssssssscjs",
    "sjsmtmtmtmtmmsjs",
    "ssjmmmmmmmmmmjss",
    "sssjjsrsssssjsss",
    "ssssjjjjjjjjssss",
]
assert all(len(r) == 16 for r in S_FACE), [len(r) for r in S_FACE]


def rotted(mat, seed, holes=0.06, stains=0.7, ribs=False, flesh=FLESH):
    """A cloth material gone to the grave: mould and earth stains, threadbare patches, holes onto dead flesh (on the torso,
    pale rib lines across it)."""
    def m(f, x, y, w, h):
        c = mat(f, x, y, w, h)
        if f in ("up", "down"):
            return c
        n = fbm(x * 1.3 + len(f) * 7, y * 1.3, seed, 64, 64, 3, 6.0)
        if n > stains:
            c = mix(c, STAIN if h01("st", seed, f, x // 3, y // 3) < 0.6 else MOULD, min(0.6, (n - stains) * 3.5))
        elif n < 0.28:
            c = shade(c, 0.86)
        hole = fbm(x * 2.0 + len(f) * 11, y * 2.0, seed + 3, 64, 64, 2, 5.0)
        if hole > 1 - holes:
            if hole > 1 - holes * 0.7:
                if ribs and y % 3 == 0:
                    return tone(BONE, 2.4 + (0.6 if x % 2 else 0))
                return tone(flesh, 1.6 + 1.2 * h01("fl", seed, f, x, y))
            return shade(c, 0.55)                       # the frayed edge of the hole
        if h01("lint", seed, f, x, y) < 0.035:
            c = shade(c, 1.14)
        return c
    return m


def dead_skin(seed, base=3.0):
    sk = skin_m(DEAD, seed, base, 0.35)

    def m(f, x, y, w, h):
        c = sk(f, x, y, w, h)
        n = fbm(x * 2.2 + len(f) * 5, y * 2.2, seed + 9, 64, 64, 2, 6.0)
        if n > 0.74:
            c = mix(c, FLESH[3], 0.45)                  # livid, rotting patches
        elif n < 0.2:
            c = mix(c, hexc("#90a8b0"), 0.15)           # waxy bloat
        return c
    return m


def s_head(m, seed=61001):
    sk = dead_skin(seed, 3.1)
    hr = hair_m(Ramp("#0d0d0c", "#151513", "#1e1d1a", "#282622", "#33302b", "#403c35"), seed + 1, base=2.4)
    sh = lambda k: (lambda f, x, y, w, h: shade(sk(f, x, y, w, h), k))
    lg = {"s": sk, "h": hr, "b": sh(0.55), "k": sh(0.72), "O": lambda f, x, y, w, h: SOCKET[1 + (x + y) % 2],
          "G": EYE_HOT, "g": EYE_DIM, "c": sh(0.78), "u": sh(0.68), "n": DEAD[4], "N": DEAD[1], "j": sh(0.86),
          "m": FLESH[0], "t": tone(BONE, 3.4), "r": FLESH[3]}
    glow = lambda f, x, y, w, h, c: ({"G": EYE_HOT, "g": EYE} .get(S_FACE[y][x]) if f == "north" else None)
    sides = H.head_sides(sk, hr, top=3, back=12, temple=8, sideburn=5)
    head(m, S, S_FACE, lg, sides, glow=glow, nose=lambda f, x, y, w, h: DEAD[3] if f == "north" else DEAD[2])
    neck(m, S, sk, 3.0)
    # The ear-lobe of one ear is gone; a dark cavity at the left cheek.
    m.cube("head", (S.HEAD / 2 - 0.1, S.NECK + 2.2, -1.2), (0.2, 1.0, 1.0), solid(FLESH[1]), density=2, tag="rot")


def peaked_cap(m, b, parent, ramp, band, visor, cord=None, emblem=None, crown_h=1.6, lift=1.6, seed=1, bone="cap"):
    """A peaked cap: the band round the head's top, the crown (wider, its front raised), the black visor and an emblem."""
    hs = b.HEAD / 2
    y0 = b.crown - 1.6
    cp = m.bone(bone, (0, b.crown - 1.0, 0), parent)
    cl = fabric(ramp, seed, 3.0, 0.3)
    m.cube(cp, (-hs, y0, -hs), (b.HEAD, 1.6, b.HEAD), lambda f, x, y, w, h: None if f in ("up", "down") else tone(band, 2.2 + (0.6 if y == 0 else 0)),
           inflate=0.32, density=2, tag="cap_band")

    def crown(f, x, y, w, h):
        if f == "down":
            return ramp[0]
        if f == "up":
            return tone(ramp, 3.6 + 0.4 * math.sin(x * 0.7 + y * 0.5))
        return cl(f, x, y, w, h)
    m.cube(cp, (-hs - 0.6, y0 + 1.6, -hs - 0.9), (b.HEAD + 1.2, crown_h, b.HEAD + 1.4), crown, inflate=0.1, density=2,
           tag="cap_crown", rotation=(-8, 0, 0), pivot=(0, y0 + 1.6, hs))
    # the raised front of the crown (the saddle)
    m.cube(cp, (-hs + 0.4, y0 + 1.6, -hs - 1.1), (b.HEAD - 0.8, lift, 0.6), crown, density=2, tag="cap_saddle",
           rotation=(-14, 0, 0), pivot=(0, y0 + 1.6, -hs - 0.8))

    def vis(f, x, y, w, h):
        c = tone(visor, 1.6 + (2.2 if f == "up" and y == h - 1 else 0) + (1.0 if f == "up" and (x + y) % 7 == 0 else 0))
        return c
    m.cube(cp, (-hs + 0.3, y0 + 0.1, -hs - 2.4), (b.HEAD - 0.6, 0.4, 2.2), vis, density=2, tag="cap_visor",
           rotation=(12, 0, 0), pivot=(0, y0 + 0.4, -hs - 0.3))
    if cord:
        m.cube(cp, (-hs + 0.6, y0 + 0.2, -hs - 0.5), (b.HEAD - 1.2, 0.3, 0.2), lambda f, x, y, w, h: cord[3 + (x % 2)],
               density=4, tag="cap_cord")
    if emblem:
        wheel = sun_wheel(3.0, 3.0, 3.0, 1.2, spokes=8, twist=0.4, width=0.6)
        m.cube(cp, (-0.75, y0 + 0.5, -hs - 0.45), (1.5, 1.5, 0), lambda f, x, y, w, h: emblem if wheel(x, y) else None,
               faces=("north",), density=4, tag="cap_emblem")
    return cp


def armband(m, b, side="left", base=BAND, wheel_col=SUN, y=None, tag="armband"):
    """A band round the upper arm with the black sun on its outer side and its front."""
    s = 1 if side == "left" else -1
    cx = b.AX * s
    yy = b.SY - 4.6 if y is None else y
    W = b.AW
    hh = 2.25
    wheel = sun_wheel(W * 4 / 2, hh * 4 / 2, hh * 4 / 2 - 0.1, spokes=12, twist=0.7, width=0.55)

    def mat(f, x, yv, w, h):
        if f in ("up", "down"):
            return None
        c = tone(base, 3.0 - 0.8 * yv / max(1, h) + (0.5 if yv == 0 else 0))
        if h01("band", f, x, yv) < 0.12:
            c = mix(c, STAIN, 0.5)
        outer = "east" if s > 0 else "west"
        if f == outer or f == "north":
            if wheel(x, yv):
                return wheel_col
        return c
    m.cube(f"{side}_arm", (cx - W / 2, yy, -W / 2), (W, hh, W), mat, inflate=0.42, density=4, tag=tag)


def s_uniform(m, seed=61200):
    cloth = rotted(fabric(FIELD, seed + 1, 3.0, 0.35), seed + 1, holes=0.11, stains=0.64, ribs=True)
    TW, TD = S.TW, S.TD

    def tunic(f, x, y, w, h):
        c = cloth(f, x, y, w, h)
        if f != "north":
            if f == "south" and y < 2:
                return tone(COLLAR, 3.0)
            return c
        mid = (w - 1) / 2
        if y < 2:
            return tone(COLLAR, 3.2 - y) if abs(x - mid) > 1.2 else FLESH[2]       # the collar, open at the throat
        if y == 2 and abs(x - mid) < 1.5:
            return FLESH[3]
        if abs(x - mid) < 0.6:
            return hexc("#6d7068") if y % 4 == 3 else shade(c, 0.8)             # the placket's dull buttons
        for px in (2, w - 6):                                                    # breast pockets, pleated, their flaps
            if px <= x < px + 4:
                if y == 5:
                    return shade(c, 0.6)
                if y == 4:
                    return shade(c, 1.18)
                if 6 <= y <= 9 and (x - px) in (0, 3):
                    return shade(c, 0.8)
                if y == 5 and x - px in (1, 2):
                    return hexc("#6d7068")
        return c
    torso(m, S, tunic, tag="tunic")
    # The skirt of the tunic below the belt, and its lower pockets.
    def skirt(f, x, y, w, h):
        c = cloth(f, x, y + 20, w, h + 20)
        if f == "north":
            for px in (1, w - 6):
                if px <= x < px + 5 and 0 <= y < h - 1:
                    if y == 0:
                        return shade(c, 1.15)
                    if y == 1:
                        return shade(c, 0.6)
                    if x - px in (0, 4):
                        return shade(c, 0.82)
        if y == h - 1 and h01("fray", seed, f, x) < 0.5:
            return None                                                          # a frayed hem
        return c
    m.cube("body", (-TW, S.HIP - 2.2, -TD), (2 * TW, 3.0, 2 * TD), none_on(("up", "down"), skirt), inflate=0.28, density=2,
           tag="tunic_skirt")
    # The belt, the plain buckle, two ammunition pouches.
    leather = leather_m(BOOT, seed + 2, 2.4, 0.8)

    def belt(f, x, y, w, h):
        if f == "north" and abs(x - (w - 1) / 2) < 1.6:
            return STEEL[4] if (y in (0, h - 1) or abs(x - (w - 1) / 2) > 1.0) else STEEL[2]
        return leather(f, x, y, w, h)
    m.cube("body", (-TW, S.HIP + 0.6, -TD), (2 * TW, 1.2, 2 * TD), belt, inflate=0.42, density=2, tag="belt")
    for s in (-1, 1):
        def pouch(f, x, y, w, h):
            c = leather(f, x, y, w, h)
            if f == "north" and y == 0:
                return BOOT[4]
            if f == "north" and x in (2, 5) and y == 1:
                return STEEL[3]
            return c
        m.cube("body", (s * 2.3 - 1.6, S.HIP - 0.2, -TD - 1.1), (3.2, 2.0, 0.8), pouch, density=2, tag="pouch")
    sleeve = rotted(fabric(FIELD, seed + 3, 2.9, 0.3), seed + 3, holes=0.09, stains=0.64)
    cuff = lambda f, x, y, w, h: None if f in ("up",) else (tone(FIELD, 2.0 + (1.0 if y == 0 else 0)) if h01("cf", f, x) > 0.3 else None)
    arms(m, S, sleeve, dead_skin(seed + 4, 2.9), cuff=cuff, cuff_depth=0.8)
    armband(m, S, "left")
    trousers = rotted(fabric(FIELD, seed + 5, 2.7, 0.3, crease=True), seed + 5, holes=0.05)
    legs(m, S, trousers, BOOT, boot_h=5.4, toe=1.0)
    # The jackboots: a leather shaft with a scuffed sheen and a turned top.
    for side, s in (("right", -1), ("left", 1)):
        x0 = S.LX * s - S.LW / 2
        m.cube(f"{side}_shin", (x0 - 0.15, 5.0, -S.LW / 2 - 0.15), (S.LW + 0.3, 0.6, S.LW + 0.3),
               lambda f, x, y, w, h: None if f in ("up", "down") else BOOT[3], density=2, tag="boot_top")


def rifle(m, seed=61400):
    """A bolt rifle with a fixed bayonet: the wrist of the stock in the right fist, the barrel along -Y, the butt along +Y.
    Its axis runs along the inside of the fist, so the stock lies along the forearm when he shoulders it."""
    cx = -S.AX + 1.0
    y = S.PALM - 0.2
    z = -0.4
    r = m.bone("rifle", (cx, y, z), "right_hand")

    def wood(f, x, yy, w, h):
        v = 2.6 + 0.9 * math.sin(yy * 0.35 + x * 0.6 + seed) + (0.8 if f in ("north",) else 0) + (0.6 if f == "up" else 0)
        c = tone(WOOD, v)
        if h01("wd", seed, f, x, yy) < 0.06:
            c = mix(c, STAIN, 0.5)
        if h01("scr", seed, f, x, yy) < 0.04:
            c = shade(c, 1.25)
        return c
    met = lambda f, x, yy, w, h: tone(STEEL, 2.2 + (1.4 if f in ("north", "up") else 0) + (0.6 if x == 0 else 0)
                                     - (0.8 if h01("rust", seed, f, x, yy) < 0.18 else 0))
    # the butt and the stock (behind the grip, up the forearm)
    m.cube(r, (cx - 0.5, y + 1.2, z - 0.4), (1.0, 4.6, 1.8), wood, density=4, tag="stock")
    m.cube(r, (cx - 0.55, y + 5.6, z - 0.6), (1.1, 0.4, 2.2), solid(STEEL[2]), density=4, tag="butt_plate")
    m.cube(r, (cx - 0.4, y - 0.6, z - 0.1), (0.8, 1.9, 1.0), wood, density=4, tag="wrist")
    # the fore-end and the barrel band, the barrel, the bolt and its handle (on his right), the trigger guard
    m.cube(r, (cx - 0.5, y - 9.0, z - 0.5), (1.0, 8.6, 1.2), wood, density=4, tag="fore_end")
    m.cube(r, (cx - 0.3, y - 10.8, z - 0.95), (0.6, 10.8, 0.6), met, density=4, tag="barrel")
    m.cube(r, (cx - 0.55, y - 7.4, z - 1.05), (1.1, 0.5, 1.6), solid(STEEL[3]), density=4, tag="band")
    m.cube(r, (cx - 0.4, y - 1.4, z - 1.15), (0.8, 2.0, 0.7), met, density=4, tag="receiver")
    m.cube(r, (cx - 1.35, y - 0.4, z - 0.95), (1.0, 0.35, 0.35), solid(STEEL[4]), density=4, tag="bolt_handle")
    m.cube(r, (cx - 0.2, y - 1.0, z + 0.55), (0.4, 1.2, 0.5), solid(STEEL[1]), density=4, tag="trigger_guard")
    m.cube(r, (cx - 0.15, y - 10.6, z - 1.25), (0.3, 0.3, 0.3), solid(STEEL[4]), density=4, tag="front_sight")

    # the bayonet under the muzzle: its ring, its blade
    def blade(f, x, yy, w, h):
        if f in ("east", "west"):
            return tone(STEEL, 4.6 - 1.5 * yy / max(1, h) if x == 0 else 3.4 - yy / max(1, h)) if h01("bl", seed, f, x, yy) > 0.12 else STEEL[2]
        return STEEL[2]
    m.cube(r, (cx - 0.35, y - 11.4, z - 0.85), (0.7, 1.4, 0.8), solid(STEEL[2]), density=4, tag="bayonet_ring")
    m.cube(r, (cx - 0.12, y - 15.8, z - 0.55), (0.25, 4.4, 0.9), blade, density=4, tag="bayonet")
    m.cube(r, (cx - 0.12, y - 16.4, z - 0.4), (0.25, 0.6, 0.5), solid(STEEL[3]), density=4, tag="bayonet_tip")
    # the sling, slack under the rifle
    m.cube(r, (cx - 0.1, y - 7.2, z + 0.75), (0.2, 11.6, 0.2), lambda f, x, yy, w, h: tone(BOOT, 3.0 + (1 if yy % 4 == 0 else 0)),
           density=4, tag="sling")
    return r


def soldier_rig():
    m = Model("thule_soldier", 256, 256)
    skeleton(m, S, coat=False)
    s_head(m)
    peaked_cap(m, S, "head", CAPR, COLLAR, BOOT, emblem=SUN, seed=61300)
    s_uniform(m)
    rifle(m)
    return m


# =====================================================================================================
# The officer
# =====================================================================================================

O = Body(HIP=12.8, NECK=24.8, TW=3.7, TD=1.95, AW=3.2, SY=24.1, ELBOW=19.0, WRIST=14.4, PALM=12.5, LX=1.85, LW=3.5, KNEE=6.8,
         SHOE_H=1.4, HEM=3.2)

PALE = Ramp("#4d4648", "#76696b", "#9b8c8b", "#b6a7a4", "#cbbebb", "#ddd3d0")
LEATHER = Ramp("#030304", "#08080a", "#0f0f12", "#18181c", "#24242a", "#3d3e46")
UNDER = Ramp("#0d0e0f", "#15171a", "#1e2125", "#282c31", "#33383e", "#40464d")
SILVER = Ramp("#2e3036", "#4c5058", "#6f747d", "#959ba4", "#bfc4cb", "#e8ebf0")
BROWN = Ramp("#120a06", "#1d110a", "#2a190f", "#382215", "#472d1c", "#5b3b26")
IRON = Ramp("#08080a", "#121216", "#1c1d22", "#28292f", "#383a42", "#53565f")
VIOLET_HOT, VIOLET, VIOLET_DIM = hexc("#f4ecff"), hexc("#c3a6ff"), hexc("#6a4cb0")

O_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhssssssssssshhh",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "sUkwwkssssxwwkUs",
    "skwGgwssssxgGwks",
    "sskUUsssssxUUkss",
    "ssssssnnssxsssss",
    "scsssNnnNsssssds",
    "scsssssssssssscs",
    "sjssssssssssssjs",
    "sjssllllllllssjs",
    "ssjssssssssssjss",
    "sssjjsssssssjsss",
    "ssssjjjjjjjjssss",
]
assert all(len(r) == 16 for r in O_FACE), [len(r) for r in O_FACE]


def o_head(m, seed=62001):
    sk = skin_m(PALE, seed, 3.2, 0.25)
    hr = hair_m(Ramp("#141210", "#1f1b18", "#2b2621", "#38312b", "#463e36", "#564c42"), seed + 1, base=2.8)
    sh = lambda k: (lambda f, x, y, w, h: shade(sk(f, x, y, w, h), k))
    lg = {"s": sk, "h": hr, "b": hexc("#2a2420"), "U": sh(0.78), "k": sh(0.8), "w": hexc("#3a3046"), "G": VIOLET_HOT,
          "g": VIOLET, "n": PALE[4], "N": PALE[1], "c": sh(0.86), "j": sh(0.88), "l": hexc("#4a2e30"), "x": hexc("#c9a8a4"),
          "d": sh(0.7)}
    glow = lambda f, x, y, w, h, c: ({"G": VIOLET_HOT, "g": VIOLET, "w": VIOLET_DIM}.get(O_FACE[y][x]) if f == "north" else None)
    sides = H.head_sides(sk, hr, top=3, back=11, temple=8, sideburn=5)
    head(m, O, O_FACE, lg, sides, glow=glow, nose=lambda f, x, y, w, h: PALE[4] if f == "north" else PALE[2])
    K.brows(m, O, hexc("#241f1b"), tilt=10, w=2.4, h=0.45)
    neck(m, O, sk, 3.0)


def o_coat(m, seed=62200):
    TW, TD = O.TW, O.TD
    hide = leather_m(LEATHER, seed + 1, 3.0, 1.0)

    def front(f, x, y, w, h, mat=hide):
        c = mat(f, x, y, w, h)
        if f != "north" or w < 14 or h > 26:          # only the shell's front (the skirt panels stay plain)
            return c
        mid = (w - 1) / 2
        if abs(x - mid) in (2.5, 3.0, 3.5) and y % 5 == 2 and y > 4:
            return SILVER[4] if abs(x - mid) == 2.5 or abs(x - mid) == 3.5 else SILVER[2]
        # the Sam Browne strap from his right shoulder down to the left hip
        if abs(x - (2 + y * 0.55)) < 0.9 and y < h:
            return tone(BROWN, 3.0 + (0.8 if f == "north" and int(x) % 2 else 0))
        return c
    torso(m, O, lambda f, x, y, w, h: tone(UNDER, 2.6 - y / max(1, h)), tag="torso")
    coat(m, O, LEATHER, front, seed + 3, lapel=True, collar_up=False, pockets=True, open_front=False)

    def belt(f, x, y, w, h):
        if f == "north" and abs(x - (w - 1) / 2) < 1.6:
            return SILVER[4] if (y in (0, h - 1) or abs(x - (w - 1) / 2) > 1.0) else SILVER[2]
        return tone(LEATHER, 2.0 + (1.6 if y == 0 else 0))
    m.cube("coat", (-TW, O.HIP + 0.4, -TD), (2 * TW, 1.4, 2 * TD), belt, inflate=0.78, density=2, tag="coat_belt")
    sleeve = leather_m(LEATHER, seed + 4, 2.9, 0.9)
    cuff = lambda f, x, y, w, h: None if f == "up" else tone(LEATHER, 3.6 - y * 0.6)
    glove = lambda f, x, y, w, h: tone(LEATHER, 2.0 + 0.8 * math.sin(x * 0.9 + y * 0.4) + (0.8 if f == "up" else 0))
    arms(m, O, sleeve, None, cuff=cuff, cuff_depth=1.4, glove=glove, sleeve_inflate=0.35)
    armband(m, O, "left", base=Ramp("#4e4a3e", "#6c6756", "#89836d", "#a19a82", "#b5ae96", "#c9c3ac"), y=O.SY - 4.8)
    legs(m, O, fabric(UNDER, seed + 5, 2.8, 0.25, crease=True), BOOT, boot_h=5.8, toe=1.0)


def staff(m, seed=62400):
    """A rod of black iron, its grip in the right fist, the shaft along +Y (up when the hand hangs), a spike at its foot and
    the sun wheel at its head (`staff_sun`), lit violet-white."""
    cx, y, z = -O.AX + 0.4, O.PALM - 0.2, -0.5
    st = m.bone("staff", (cx, y, z), "right_hand")
    iron = lambda f, x, yy, w, h: tone(IRON, 2.6 + (1.2 if f in ("north", "up") else 0) + (0.7 if x == 0 else 0)
                                      + (0.9 if (yy % 9) == 0 else 0))
    m.cube(st, (cx - 0.4, y - 8.0, z - 0.4), (0.8, 22.0, 0.8), iron, density=4, tag="shaft")
    m.cube(st, (cx - 0.6, y - 1.0, z - 0.6), (1.2, 3.2, 1.2), lambda f, x, yy, w, h: tone(LEATHER, 2.4 + (yy % 2)), density=4,
           tag="grip_wrap")
    m.cube(st, (cx - 0.25, y - 10.0, z - 0.25), (0.5, 2.0, 0.5), solid(IRON[4]), density=4, tag="ferrule")
    for k, yy in enumerate((y + 9.5, y + 13.0)):
        m.cube(st, (cx - 0.65, yy, z - 0.65), (1.3, 0.6, 1.3), solid(SILVER[2 + k]), density=4, tag="collar_ring")
    sun = m.bone("staff_sun", (cx, y + 17.0, z), "staff")
    R = 3.0
    wheel = sun_wheel(R * 4, R * 4, R * 4 - 0.2, R * 4 * 0.34, spokes=12, twist=0.65, width=0.75)
    ring = lambda x, yy: abs(math.hypot(x + 0.5 - R * 4, yy + 0.5 - R * 4) - (R * 4 - 0.6)) < 0.9

    def face(f, x, yy, w, h):
        if wheel(x, yy):
            return tone(IRON, 1.4 + (0.8 if (x + yy) % 5 == 0 else 0))
        if ring(x, yy):
            return SILVER[2]
        return None

    def lit(f, x, yy, w, h, c):
        d = math.hypot(x + 0.5 - R * 4, yy + 0.5 - R * 4) / (R * 4)
        if ring(x, yy):
            return VIOLET_DIM
        if c and wheel(x, yy):
            return VIOLET_HOT if d < 0.3 else (VIOLET if d < 0.62 else VIOLET_DIM)
        return None
    for zz, fc in ((z - 0.35, "north"), (z + 0.35, "south")):
        m.cube(sun, (cx - R, y + 14.0, zz), (2 * R, 2 * R, 0), face, glow=lit, faces=(fc,), density=4, tag="sun_" + fc)
    m.cube(sun, (cx - 0.5, y + 14.4, z - 0.3), (1.0, 5.2, 0.6), solid(IRON[3]), density=4, tag="sun_spine")
    # the pale core between the faces, for the glow to bloom from
    m.cube(sun, (cx - 0.6, y + 16.4, z - 0.32), (1.2, 1.2, 0.64), solid(hexc("#d8ccf0")),
           glow=lambda f, x, yy, w, h, c: VIOLET_HOT, density=4, tag="sun_core")
    return st


def officer_rig():
    m = Model("thule_officer", 256, 256)
    skeleton(m, O, coat=True)
    o_head(m)
    peaked_cap(m, O, "head", Ramp("#060607", "#0c0c0e", "#141417", "#1d1d21", "#28282e", "#3a3a42"), LEATHER, LEATHER,
               cord=SILVER, emblem=SILVER[4], crown_h=1.8, lift=2.2, seed=62300)
    o_coat(m)
    staff(m)
    return m


# =====================================================================================================
# Clips: the soldier
# =====================================================================================================

PALM_L = (S.AX, S.PALM + 0.7, -0.2)


def reach(rig, pose, side, target, palm, reg=0.00001, starts=((-40, 20, 20), (-80, 30, 0), (-20, 0, 30), (-120, 20, 0))):
    """The `side` arm moved so its palm point lands on `target` (a few starting guesses, the best kept)."""
    chans = [(f"{side}_arm", 0), (f"{side}_arm", 1), (f"{side}_arm", 2), (f"{side}_forearm", 0), (f"{side}_forearm", 1)]
    cost = reach_cost(rig, f"{side}_hand", palm, target, small([f"{side}_arm", f"{side}_forearm"], reg))
    best = None
    for st in starts:
        s = 1 if side == "left" else -1
        p0 = P_(pose, {f"{side}_arm": [st[0], st[1] * s, st[2] * s], f"{side}_forearm": [-20, 0, 0]})
        q = solve(p0, chans, cost)
        c = cost(q)
        if best is None or c < best[0]:
            best = (c, q)
    return best[1]


def grip(rig, pose, along=-6.0):
    """The left hand moved onto the rifle's fore-end (`along` px down the rifle from the grip)."""
    target = world(rig, pose, "rifle", (-S.AX + 1.0, S.PALM - 0.2 + along, -0.6))
    return reach(rig, pose, "left", target, PALM_L)


def orient(rig, pose, target, chans=(("rifle", 0), ("rifle", 1), ("rifle", 2))):
    """The rifle turned so its muzzle points along `target`."""
    a, b = (-S.AX + 1.0, S.PALM, -0.4), (-S.AX + 1.0, S.PALM - 10, -0.4)
    return solve(pose, list(chans), aim_cost(rig, "rifle", a, b, target, small(["rifle"], 0.00002)))


GRIP_R = (-S.AX + 1.0, S.PALM - 0.2, -0.4)       # the rifle's pivot: the wrist of the stock in the right fist


def body_pt(rig, pose, pt):
    return world(rig, pose, "body", pt)


def body_dir(rig, pose, d):
    a, b = world(rig, pose, "body", (0, 18, 0)), world(rig, pose, "body", (d[0], 18 + d[1], d[2]))
    return [b[i] - a[i] for i in range(3)]


def shoulder(rig, pose, at, muzzle, along, world_dir=False):
    """The rifle's grip put at `at` (a point on the torso), the muzzle along `muzzle` (in the torso's frame, or the world's),
    the left hand on the fore-end `along` px from the grip."""
    p = reach(rig, pose, "right", body_pt(rig, pose, at), GRIP_R,
              starts=((-30, -10, 10), (-60, -20, 0), (-90, 0, 0), (-10, 0, 20)))
    p = orient(rig, p, muzzle if world_dir else body_dir(rig, p, muzzle))
    return grip(rig, p, along)


def soldier_poses(rig):
    fist = P_(K.fist("right"), K.fist("left", 0.75))
    # Port arms, slack: the rifle across the body, the butt at his right hip, the bayonet up past his left shoulder.
    port = shoulder(rig, P_(fist, {"body": [6, 0, 0], "head": [10, 0, -8]}), (-2.6, 14.0, -3.4), (0.62, 0.72, -0.32), -5.5)
    # Shouldered: the grip at his right collarbone, the rifle level, the left shoulder forward, the cheek down to the stock.
    aim = shoulder(rig, P_(fist, {"body": [2, 18, 0], "head": [8, -16, 0], "right_leg": [-6, 0, 2], "left_leg": [10, 0, -2]}),
                   (-2.2, 20.6, -4.2), (0.0, 0.02, -1.0), -4.5, world_dir=True)
    # Bayonet low and forward, ready to thrust (the coil and the lunge).
    low = shoulder(rig, P_(fist, {"body": [10, 14, 0], "head": [-4, -10, 0], "@root": [0, -0.4, 0.8], "right_leg": [8, 0, 2],
                                  "left_leg": [-12, 0, -2], "left_shin": [10, 0, 0]}),
                   (-2.6, 15.2, -3.4), (0.14, -0.1, -1.0), -3.0, world_dir=True)
    lunge = shoulder(rig, P_(fist, {"body": [22, 6, 0], "head": [-18, -4, 0], "@root": [0, -1.6, -4.0], "left_leg": [-48, 0, -2],
                                    "left_shin": [44, 0, 0], "right_leg": [30, 0, 2], "right_shin": [12, 0, 0]}),
                     (-1.6, 17.6, -6.6), (0.1, -0.2, -1.0), -2.5, world_dir=True)
    return port, aim, low, lunge


def soldier_anims(rig):
    f = AnimFile()
    clips = []
    A = "animation.thule_soldier."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]
    PORT, AIM, LOW, LUNGE = soldier_poses(rig)

    # --- loops ---------------------------------------------------------------------------------------------------
    # Idle (3 s): slack on his feet, a slow rattle of breath, the head lolls and twitches once.
    c = clip("idle", 3.0, loop=True)
    for i, t in enumerate((0, 0.75, 1.5, 1.62, 1.9, 2.25, 3.0)):
        u = t / 3.0
        b = math.sin(u * 2 * math.pi)
        jerk = {1.62: [-8, 14, 8], 1.9: [4, -6, -4]}.get(t, [0, 0, 0])
        hd = PORT["head"]
        c.key(t, P_(PORT, {"@body": [0, 0.15 * (1 - math.cos(u * 2 * math.pi)) * 0.5, 0], "body": [6 + 1.5 * b, 0, 0],
                           "head": [hd[0] + 2 * b + jerk[0], hd[1] + 6 * math.sin(u * 2 * math.pi + 0.6) + jerk[1], hd[2] + jerk[2]]}),
              (E_SNAP if t == 1.62 else E_IO) if i else None)

    # Walk (1.4 s): a dead man's march -- stiff, the right foot drags, the head rolls with each step.
    c = clip("walk", 1.4, loop=True)
    K.walk(c, PORT, 1.4, stride=17, arm=0, bob=0.5, twist=5, knee=(4, 26), arms=False,
           extra=lambda s: {"head": [PORT["head"][0] + 4, -4 * s, PORT["head"][2] - 5 * s], "body": [8, 5 * s, -2 * s],
                            "right_shin": [8 if s > 0 else 30, 0, 0]})

    # Aim idle (2 s): shouldered, a slow sway along the sights.
    c = clip("aim_idle", 2.0, loop=True)
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        b = math.sin(t / 2.0 * 2 * math.pi)
        cc = math.cos(t / 2.0 * 2 * math.pi)
        c.key(t, P_(AIM, {"body": [AIM["body"][0] + 0.8 * cc, AIM["body"][1] + 1.6 * b, 0], "@body": [0, -0.08 * abs(b), 0]}),
              E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    # Rise (3 s): out of the grave -- a hand claws up through the earth, then the other with the rifle, he hauls himself
    # up onto his elbows and a knee, and stands. Below y=0 the ground hides him.
    c = clip("rise", 3.0)
    claw = P_(K.spread("left"), {"left_fingers": [-30, 0, 0], "left_index": [-30, 0, 0], "left_middle": [-30, 0, 0]})
    c.key(0, P_(PORT, claw, {"@root": [0, -40, 0], "left_arm": [-176, 0, 10], "left_forearm": [-10, 0, 0], "right_arm": [-170, 0, -10],
                             "right_forearm": [-20, 0, 0], "body": [6, 0, 0], "head": [-30, 0, 0]}))
    c.key(0.5, P_(PORT, claw, {"@root": [0, -31, 0], "left_arm": [-178, 0, 14], "left_forearm": [-30, 0, 0], "right_arm": [-150, 0, -14],
                               "right_forearm": [-20, 0, 0], "body": [6, 0, 0], "head": [-34, 0, 0]}), E_OUT)
    c.key(0.9, P_(PORT, claw, {"@root": [0, -24, -1], "left_arm": [-112, 0, -20], "left_forearm": [-40, 0, 0], "right_arm": [-160, 0, -16],
                               "right_forearm": [-20, 0, 0], "body": [18, 0, 0], "head": [-30, 8, 0]}), E_SNAP)
    c.key(1.4, P_(PORT, claw, {"@root": [0, -15, -2], "left_arm": [-60, 0, -20], "left_forearm": [-50, 0, 0], "right_arm": [-70, 0, 20],
                               "right_forearm": [-50, 0, 0], "body": [44, 0, 0], "head": [-40, -6, 0]}), E_IO)
    kneel = P_(PORT, {"@root": [0, -5.4, -1], "left_leg": [-86, 0, -4], "left_shin": [88, 0, 0], "right_leg": [-20, 0, 6],
                      "right_shin": [100, 0, 0], "body": [34, 0, 0], "head": [-24, 0, -6], "left_arm": [-40, 0, -20],
                      "left_forearm": [-40, 0, 0]})
    kneel = ground(rig, kneel, 0.0)
    c.key(2.0, kneel, E_IO)
    c.key(2.5, P_(PORT, {"@root": [0, -1.2, -0.5], "left_leg": [-30, 0, -2], "left_shin": [40, 0, 0], "right_leg": [6, 0, 2],
                         "right_shin": [20, 0, 0], "body": [18, 0, 0], "head": [8, 0, -12]}), E_IO)
    c.key(3.0, PORT, E_IO)

    # Stab (1.0 s; the bayonet lands at 0.45 s): the rifle drops to the hip, then a long lunge.
    c = clip("stab", 1.0)
    c.key(0, PORT)
    c.key(0.25, LOW, E_IO)
    c.key(0.45, LUNGE, E_SNAP)
    c.key(0.62, P_(LUNGE, {"@root": [0, -1.6, -4.2]}), E_OUT)
    c.key(1.0, PORT, E_IO)

    # Shoot (0.7 s; the shot at 0.15 s): up to the shoulder if he is not there already, the kick, the bolt worked.
    c = clip("shoot", 0.7)
    kick = P_(AIM, {"right_arm": [AIM["right_arm"][0] - 14, AIM["right_arm"][1], AIM["right_arm"][2]],
                    "left_arm": [AIM["left_arm"][0] - 12, AIM["left_arm"][1], AIM["left_arm"][2]],
                    "body": [AIM["body"][0] - 8, AIM["body"][1], 0], "head": [AIM["head"][0] - 6, AIM["head"][1], 0],
                    "@root": [0, 0, 0.8]})
    c.key(0, AIM)
    c.key(0.12, AIM)
    c.key(0.15, kick, E_SNAP)
    c.key(0.32, P_(AIM, {"right_forearm": [-30, 0, 0], "left_arm": [AIM["left_arm"][0] + 6, AIM["left_arm"][1], AIM["left_arm"][2]]}), E_OUT)
    c.key(0.5, P_(AIM, {"right_hand": [0, 0, -20]}), E_IO)                     # the bolt
    c.key(0.7, AIM, E_IO)

    # Death (2.0 s, held): shot through, the knees go, he pitches forward onto his face; the rifle falls flat beside him.
    c = clip("death", 2.0, hold=True)
    hit = P_(PORT, {"body": [-14, 0, 6], "head": [-24, 0, 10], "@root": [0, 0, 1.0], "right_arm": [-40, 0, 30], "left_arm": [-30, 0, -40],
                    "left_forearm": [-20, 0, 0]})
    knees = P_(PORT, {"@root": [0, -5.6, 0.4], "right_leg": [-88, 0, 4], "left_leg": [-90, 0, -4], "right_shin": [90, 0, 0],
                      "left_shin": [90, 0, 0], "body": [16, 0, 0], "head": [26, 0, -10], "right_arm": [-10, 0, 14],
                      "left_arm": [-10, 0, -14], "left_forearm": [-10, 0, 0], "right_forearm": [-20, 0, 0]})
    knees = ground(rig, knees, 0.0)
    down = P_(K.relax("right"), K.relax("left"), {"root": [87, 0, 0], "head": [-36, 34, 6], "body": [-4, 0, 4],
                                                   "right_arm": [-150, 0, 16], "right_forearm": [-20, 0, 0], "left_arm": [-14, 0, -18],
                                                   "left_forearm": [-30, 0, 0], "right_leg": [2, 0, 7], "left_leg": [-2, 0, -5],
                                                   "right_shin": [14, 0, 0], "left_shin": [6, 0, 0]})
    down = orient(rig, down, (0.9, 0.0, -0.4))
    down = ground(rig, down, 0.0)
    c.key(0, PORT)
    c.key(0.18, hit, E_SNAP)
    c.key(0.75, knees, E_IN)
    c.key(1.0, P_(knees, {"body": [30, 0, 0], "head": [30, 0, -14]}), E_IO)
    c.key(1.45, down, E_IN)
    c.key(1.6, P_(down, {"@root": [0, down["@root"][1] + 0.6, 0]}), E_OUT)
    c.key(2.0, down, E_IN)
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# Clips: the officer
# =====================================================================================================

SA, SB = (-O.AX + 0.4, O.PALM - 8.0, -0.5), (-O.AX + 0.4, O.PALM + 14.0, -0.5)   # foot -> head of the staff


def point_staff(rig, pose, target, extra=()):
    chans = [("staff", 0), ("staff", 1), ("staff", 2)] + list(extra)
    return solve(pose, chans, aim_cost(rig, "staff", SA, SB, target, small(["staff"], 0.00002)))


def officer_poses(rig):
    # Idle: the rod upright at his right side, the left hand behind his back.
    behind = {"left_arm": [14, 0, -10], "left_forearm": [-62, 34, 0], "left_hand": [0, 0, -20]}
    idle = P_(K.fist("right"), K.relax("left"), behind, {"right_arm": [-12, 0, 6], "right_forearm": [-34, 0, 0], "head": [-3, 0, 0],
                                                         "body": [-2, 0, 0]})
    idle = point_staff(rig, idle, (0, 1, 0))
    # Raise: both arms up, the rod straight up and high, the head thrown back.
    up = P_(K.fist("right"), K.spread("left"), {"right_arm": [-166, 0, -34], "right_forearm": [-10, 0, 0], "left_arm": [-160, 0, 40],
                                                "left_forearm": [-16, 0, 0], "head": [-26, 0, 0], "body": [-8, 0, 0],
                                                "@root": [0, 0.3, 0]})
    up = point_staff(rig, up, (-0.25, 1, -0.1))
    # Cast: the rod thrust out, its head at the target.
    cast = P_(K.fist("right"), K.spread("left"), {"right_arm": [-88, 6, 0], "right_forearm": [-4, 0, 0], "left_arm": [-30, 0, -24],
                                                  "left_forearm": [-30, 0, 0], "body": [8, -12, 0], "head": [-8, 12, 0],
                                                  "@root": [0, -0.4, -1.2], "right_leg": [-16, 0, 2], "left_leg": [12, 0, -2]})
    cast = point_staff(rig, cast, (0.0, 0.08, -1))
    # Strike: wound up over the right shoulder, swung down across to the left.
    wind = P_(K.fist("right"), K.relax("left"), {"right_arm": [-150, -20, -26], "right_forearm": [-40, 0, 0], "body": [-6, 24, 0],
                                                 "head": [-6, -14, 0], "left_arm": [-30, 0, -20], "@root": [0, 0, 0.6],
                                                 "right_leg": [8, 0, 0], "left_leg": [-6, 0, 0]})
    wind = point_staff(rig, wind, (-0.5, 0.6, 0.6))
    swing = P_(K.fist("right"), K.relax("left"), {"right_arm": [-60, 30, -30], "right_forearm": [-10, 0, 0], "body": [12, -26, 0],
                                                  "head": [-10, 16, 0], "left_arm": [10, 0, -24], "@root": [0, -0.6, -1.2],
                                                  "right_leg": [-22, 0, 0], "left_leg": [14, 0, 0]})
    swing = point_staff(rig, swing, (0.7, -0.35, -0.6))
    return idle, up, cast, wind, swing


def officer_anims(rig):
    f = AnimFile()
    clips = []
    A = "animation.thule_officer."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]
    IDLE, UP, CAST, WIND, SWING = officer_poses(rig)
    coat_still = {"coat_r": [-1, 0, 0], "coat_l": [-1, 0, 0], "coat_tail": [2, 0, 0]}

    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, P_(IDLE, coat_still), 4.0, head_turn=10,
              extra=lambda u, b: {"coat_tail": [2 + 1.5 * b, 0, 0], "staff": [IDLE["staff"][0], IDLE["staff"][1], IDLE["staff"][2] + 1.5 * b]})

    # Walk (1.3 s): measured strides, the rod swung like a cane, the coat skirts kicking.
    c = clip("walk", 1.3, loop=True)
    K.walk(c, IDLE, 1.3, stride=20, arm=0, bob=0.35, twist=4, knee=(6, 20), arms=False,
           extra=lambda s: {"right_arm": [-12 - 14 * s, 0, 6], "staff": [IDLE["staff"][0] + 14 * s, IDLE["staff"][1], IDLE["staff"][2]],
                            "coat_r": [-10 * s if s > 0 else 4, 0, 0], "coat_l": [10 * s if s < 0 else 4, 0, 0],
                            "coat_tail": [10, 0, 2 * s], "head": [-3, -3 * s, 0]})

    # --- once ----------------------------------------------------------------------------------------------------
    # Raise (2.0 s; the dead answer at 1.0 s): the rod lifted high in both hands' gesture, held, brought down.
    c = clip("raise", 2.0)
    c.key(0, IDLE)
    c.key(0.55, P_(UP, {"head": [-10, 0, 0], "@root": [0, 0, 0]}), E_IO)
    c.key(1.0, P_(UP, {"coat_tail": [12, 0, 0], "coat_r": [-6, 0, 4], "coat_l": [-6, 0, -4]}), E_BACK)
    for i, t in enumerate((1.15, 1.3, 1.45)):
        j = K.jit("thule-raise", i, 2)
        c.key(t, P_(UP, {"head": [-26 + j[0], j[1], j[2]], "coat_tail": [10, 0, 0]}))
    c.key(2.0, IDLE, E_IO)

    # Cast (1.2 s; the bolt leaves at 0.5 s): the rod drawn back, then thrust out.
    c = clip("cast", 1.2)
    draw = P_(CAST, {"right_arm": [-60, 0, 10], "right_forearm": [-60, 0, 0], "body": [-4, 16, 0], "@root": [0, 0, 0.4],
                     "head": [-6, -8, 0]})
    draw = point_staff(rig, draw, (0.1, 0.4, -1))
    c.key(0, IDLE)
    c.key(0.32, draw, E_IO)
    c.key(0.5, CAST, E_SNAP)
    c.key(0.8, P_(CAST, {"right_arm": [-84, 6, 0]}), E_OUT)
    c.key(1.2, IDLE, E_IO)

    # Strike (1.0 s; the head of the rod lands at 0.45 s).
    c = clip("strike", 1.0)
    c.key(0, IDLE)
    c.key(0.3, WIND, E_IO)
    c.key(0.45, SWING, E_SNAP)
    c.key(0.6, P_(SWING, {"right_arm": [-54, 32, -34]}), E_OUT)
    c.key(1.0, IDLE, E_IO)

    # Death (2.2 s, held): he staggers, drops to his knees, topples onto his side; the rod falls from his hand.
    c = clip("death", 2.2, hold=True)
    hit = P_(IDLE, {"body": [-12, 10, 0], "head": [-20, 10, 0], "@root": [0, 0, 1.0], "right_arm": [-30, 0, 30],
                    "left_arm": [-40, 0, -30], "left_forearm": [-30, 0, 0], "left_hand": [0, 0, 0]})
    knees = P_(IDLE, {"@root": [0, -5.8, 0.4], "right_leg": [-90, 0, 4], "left_leg": [-90, 0, -4], "right_shin": [90, 0, 0],
                      "left_shin": [90, 0, 0], "body": [10, 0, 0], "head": [18, 0, 0], "right_arm": [-10, 0, 20],
                      "left_arm": [-20, 0, -16], "left_forearm": [-30, 0, 0], "coat_tail": [30, 0, 0], "coat_r": [-60, 0, 0],
                      "coat_l": [-60, 0, 0]})
    side = K.lie(side=-1, lift=0.0, extra={"coat_tail": [10, 0, 0], "coat_r": [-20, 0, 0], "coat_l": [-10, 0, 0]})
    side = P_(side, K.relax("left"), K.relax("right"))
    side = point_staff(rig, side, (0.3, 0.0, -1.0))
    side = ground(rig, side, 0.0)
    c.key(0, IDLE)
    c.key(0.2, hit, E_SNAP)
    c.key(0.8, knees, E_IN)
    c.key(1.1, P_(knees, {"body": [20, 0, -10], "head": [24, 0, -10]}), E_IO)
    c.key(1.6, side, E_IN)
    c.key(1.75, P_(side, {"@root": [0, side["@root"][1] + 0.5, 0]}), E_OUT)
    c.key(2.2, side, E_IN)
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# Build, write, preview
# =====================================================================================================

def build():
    out = {}
    for name, rigf, animf, seed in (("thule_soldier", soldier_rig, soldier_anims, 61000), ("thule_officer", officer_rig, officer_anims, 62000)):
        m = rigf()
        t, g = m.build(gutter=1, seed=seed)
        f, clips = animf(m.rig)
        check(name, m, f)
        out[name] = (m, t, g, f, clips)
    return out


def generate():
    for name, (m, t, g, f, _) in build().items():
        K.write_entity(m, f, name, t, g)


def preview(out_dir=SCRATCH, scale=5, which=None, poses=None):
    from chuck_art import composite
    paths = []
    default = {"thule_soldier": (("idle", 0.0), ("aim_idle", 0.0), ("stab", 0.45), ("rise", 1.4), ("death", 2.0)),
               "thule_officer": (("idle", 0.0), ("raise", 1.0), ("cast", 0.5), ("strike", 0.45), ("death", 2.2))}
    for name, (m, t, g, f, clips) in build().items():
        if which and name not in which:
            continue
        paths += K.render(m, t, clips, out_dir, name, (poses or {}).get(name, default[name]), scale=scale)
        paths += K.render(m, composite(t, g, 0.35), clips, out_dir, name + "_lit", (("idle", 0.0),), scale=scale,
                          views=("front",))
    return paths


if __name__ == "__main__":
    generate()
