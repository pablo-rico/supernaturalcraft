"""The Horsemen's steeds (v0.11): one GeckoLib horse rig, painted four ways.

`build_steed(model, scheme)` adds the horse under a `steed` group to any `chuck_art.Model`: the standalone
`horseman_steed` (one geometry, four textures + glowmasks, a `saddle` group the code toggles) and the steed every
Horseman carries in his own model (hidden until he mounts). Every steed bone is prefixed `steed_` so the same
motion functions key both, and a rider in the same model can follow the saddle (`seat_follow`).

Rig (Bedrock px, the horse faces north; +X is its LEFT):
  steed -- steed_body (pivot at the hind hips, so rearing turns about them)
             -- steed_neck -- steed_head -- steed_jaw, steed_ear_r, steed_ear_l
             -- steed_leg_fr/fl -- steed_shin_fr/fl,   steed_leg_hr/hl -- steed_shin_hr/hl
             -- steed_tail -- steed_tail_tip
             -- saddle
Seat: the top of the saddle is SEAT_Y px above the ground (rider's hips go RIDER_HIP above it).
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"

LIFT = 2.0             # the body rides this much above a 13-px-leg base layout
BODY_PIVOT = (0.0, 14.0 + LIFT, 8.0)
SEAT_Y = 24.5 + LIFT   # top of the saddle seat
RIDER_HIP = 1.5        # a seated rider's hip pivot sits this far above the seat (half a thigh)
LEGS = ("fr", "fl", "hr", "hl")


# --- schemes -----------------------------------------------------------------------------------------

def scheme(name, coat, mane, hoof, eye, tack, metal, blanket, marks, eye_glow=1.0, body_glow=0.0, pupil=None):
    return {"name": name, "coat": coat, "mane": mane, "hoof": hoof, "eye": hexc(eye) if isinstance(eye, str) else eye,
            "tack": tack, "metal": metal, "blanket": blanket, "marks": marks, "eye_glow": eye_glow,
            "body_glow": body_glow, "pupil": pupil}


DARK_LEATHER = Ramp("#0d0807", "#1a110d", "#281a13", "#37251b", "#473024", "#5a3d2e")
BLACK_LEATHER = Ramp("#050506", "#0c0c0e", "#141418", "#1d1d22", "#28282e", "#34343b")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
IRON = Ramp("#1c1d21", "#2f3137", "#464951", "#60646d", "#7d828c", "#a0a6b0")
SILVER = Ramp("#3a3f48", "#626a76", "#8c95a1", "#b5bdc7", "#d9dfe6", "#ffffff")
TARNISH = Ramp("#2a2b1e", "#3e3f2b", "#555638", "#6c6d47", "#868659", "#a3a26f")

SCHEMES = {
    # War's red horse: deep blood-red coat, black mane and stockings, ember eyes, black and gold tack.
    "war": scheme("war", Ramp("#2a0405", "#4a0a0b", "#6e1210", "#8e1c15", "#ad2a1d", "#c94a2e"),
                  Ramp("#060404", "#0f0909", "#1a0f0e", "#261614", "#331d1a", "#432622"),
                  Ramp("#0c0908", "#191311", "#261d19", "#342823", "#43342d", "#54423a"),
                  "#ff3b12", BLACK_LEATHER, GOLD, Ramp("#1e0405", "#3a0709", "#560b0e", "#741216", "#8f1b1e", "#ad2a2a"),
                  "war", eye_glow=1.0, body_glow=0.10),
    # Famine's black horse: a starved black coat, ribs and hips showing, a thin ragged mane, dull grey eyes.
    "famine": scheme("famine", Ramp("#060607", "#0f0f12", "#18181d", "#24242b", "#33333c", "#454550"),
                     Ramp("#09090a", "#141416", "#202024", "#2d2d33", "#3c3c44", "#4e4e58"),
                     Ramp("#0a0908", "#151311", "#211e1a", "#2e2a24", "#3c372f", "#4c463c"),
                     "#b8b2a0", DARK_LEATHER, IRON, Ramp("#120f0c", "#211c16", "#302820", "#40362a", "#524536", "#655644"),
                     "famine", eye_glow=0.8, body_glow=0.10),
    # Pestilence's pale horse: a sickly grey-green coat with sores and flies' bites, a dirty yellowed mane.
    "pestilence": scheme("pestilence", Ramp("#3b4232", "#566049", "#717d5f", "#8d9978", "#a9b394", "#c6ccb1"),
                         Ramp("#4a4430", "#655d41", "#807553", "#9a8f67", "#b3a77d", "#cbc095"),
                         Ramp("#1d1a12", "#2d281c", "#3e3826", "#504932", "#625a3e", "#756c4b"),
                         "#c8e04a", DARK_LEATHER, TARNISH, Ramp("#1f2a14", "#2f3d1e", "#405229", "#526634", "#657a40", "#7a8f50"),
                         "sores", eye_glow=1.0, body_glow=0.16),
    # Death's pale horse: bone-white with grey shadows, a long white mane, dead milky eyes, black tack.
    "death": scheme("death", Ramp("#5d5e63", "#808187", "#a3a4aa", "#c2c3c8", "#dadbe0", "#eeeff2"),
                    Ramp("#8a8b90", "#a7a8ad", "#c2c3c7", "#d8d9dd", "#eaebee", "#f8f8fa"),
                    Ramp("#141416", "#202024", "#2d2d32", "#3b3b41", "#4a4a51", "#5a5a62"),
                    "#e8f0ff", BLACK_LEATHER, SILVER, Ramp("#050506", "#0b0b0d", "#121215", "#1a1a1e", "#232328", "#2e2e34"),
                    "death", eye_glow=1.0, body_glow=0.16),
}


# --- materials --------------------------------------------------------------------------------------

def coat_mat(sc, seed, base=3.0, part="barrel"):
    coat = sc["coat"]
    marks = sc["marks"]

    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 7, y * 1.3, seed, 64, 64, 3, 9.0)
        v = base + (n - 0.5) * 1.3
        # Sheen along the top, shadow underneath.
        if face == "up":
            v += 0.5
        elif face == "down":
            v -= 1.0
        elif h > 6:
            v += 0.5 * (1 - y / h) - 0.4 * (y / h) ** 2
        # Hair direction: faint diagonal streaks.
        if (x * 2 + y) % 9 == 0:
            v -= 0.25
        c = coat[v]
        r = h01("coat", seed, face, x, y)
        if marks == "war" and part in ("barrel", "rump", "chest") and face in ("east", "west") and r < 0.04:
            c = mix(c, hexc("#1a0203"), 0.5)  # old dark scars
        if marks == "famine" and part == "barrel" and face in ("east", "west"):
            # Ribs: light ridges with dark hollows, over the front two thirds.
            fx = x if face == "west" else w - 1 - x
            if 3 <= fx <= w * 0.66 and h * 0.25 < y < h * 0.85:
                k = (fx - 3) % 4
                c = coat[v + 1.6] if k == 0 else (coat[v - 1.2] if k == 2 else c)
        if marks == "famine" and part == "rump" and face in ("east", "west") and 2 <= y <= 4:
            fx = x if face == "west" else w - 1 - x
            if 3 <= fx <= 6:
                c = coat[v + 1.4]  # a hip bone
        if marks == "sores" and face != "down":
            s = fbm(x * 3 + 11, y * 3 + len(face), seed + 5, 64, 64, 2, 7.0)
            if s > 0.72:
                c = mix(c, hexc("#5c3a26"), 0.55)
                if s > 0.8:
                    c = mix(hexc("#a8b03a"), hexc("#6a2a1a"), 0.4 if r < 0.5 else 0.15)  # weeping sores
            elif s < 0.22:
                c = mix(c, hexc("#4a5238"), 0.35)  # bruised mottling
        if marks == "death" and face in ("east", "west", "up") and part in ("barrel", "neck"):
            # A ghostly grey dapple.
            if fbm(x * 2, y * 2 + len(face), seed + 9, 64, 64, 2, 6.0) > 0.66:
                c = mix(c, coat[1], 0.35)
        return c
    return m


def mane_mat(sc, seed, base=2.6):
    mane = sc["mane"]

    def m(face, x, y, w, h):
        strand = math.sin(x * 2.3 + fbm(x, y, seed, 64, 64, 1, 4.0) * 5)
        v = base + strand * 0.9 + (fbm(x * 3, y, seed + 1, 64, 64, 2, 12.0) - 0.5) * 1.2
        if face in ("north", "south", "east", "west") and y >= h - 1 and h01("mane", seed, face, x) < 0.45:
            return None  # ragged ends
        if sc["name"] == "famine" and h01("thin", seed, face, x, y) < 0.18:
            return None  # thin, moth-eaten
        return mane[v]
    return m


def hoof_mat(sc, seed):
    hoof = sc["hoof"]
    return lambda face, x, y, w, h: hoof[2.2 + (fbm(x, y, seed, 32, 32, 2, 6.0) - 0.5) * 1.2 + (0.8 if face == "up" else 0)
                                         - (0.8 if y >= h - 1 else 0)]


def leather_mat(ramp, seed, base=2.6):
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 32, 32, 2, 8.0)
        c = ramp[base + (n - 0.5) * 1.2 + (0.6 if face == "up" else 0)]
        if face in ("north", "south", "east", "west") and (y == 0 or y == h - 1):
            c = shade(c, 1.25)  # stitched edge
        return c
    return m


# --- geometry ---------------------------------------------------------------------------------------

def build_steed(m, sc, parent="root", glow=True, seed=900):
    """Adds the horse to Model `m` under a `steed` group parented to `parent`."""
    U = LIFT  # everything above the legs is raised by this much over a 13-px-legged base layout
    m.bone("steed", (0, 0, 0), parent)
    m.bone("steed_body", BODY_PIVOT, "steed")
    m.bone("steed_neck", (0, 20 + U, -9), "steed_body", rotation=(30, 0, 0))
    m.bone("steed_head", (0, 31 + U, -10.5), "steed_neck", rotation=(22, 0, 0))
    m.bone("steed_jaw", (0, 28.5 + U, -13), "steed_head")
    m.bone("steed_ear_r", (-1.75, 32.5 + U, -10), "steed_head", rotation=(-10, 0, 12))
    m.bone("steed_ear_l", (1.75, 32.5 + U, -10), "steed_head", rotation=(-10, 0, -12))
    m.bone("steed_tail", (0, 22.5 + U, 11.5), "steed_body", rotation=(22, 0, 0))
    m.bone("steed_tail_tip", (0, 16 + U, 11.5), "steed_tail", rotation=(-14, 0, 0))
    for leg in LEGS:
        s = -1 if leg[1] == "r" else 1
        front = leg[0] == "f"
        z = -8.5 if front else 8.0
        m.bone(f"steed_leg_{leg}", (3 * s, (14 if front else 15) + U, z), "steed_body")
        m.bone(f"steed_shin_{leg}", (3 * s, 8.5 if front else 8, z + (0 if front else 1.0)), f"steed_leg_{leg}")
    m.bone("saddle", (0, 23 + U, 0), "steed_body")

    coat = lambda part, base=3.0, sd=0: coat_mat(sc, seed + sd, base, part)
    eglow = sc["body_glow"] if glow else None

    def cube(bone, origin, size, mat, g=eglow, **kw):
        return m.cube(bone, origin, size, mat, glow=g, density=2, **kw)

    # Body: barrel, a deep chest in front, round haunches behind, the withers' rise.
    cube("steed_body", (-5, 13.5 + U, -9), (10, 9.5, 18), coat("barrel", 3.0, 1), tag="barrel")
    cube("steed_body", (-5.5, 12.5 + U, -12.5), (11, 10.5, 6), coat("chest", 3.1, 2), tag="chest")
    cube("steed_body", (-5.5, 13 + U, 5), (11, 10.5, 7.5), coat("rump", 3.1, 3), tag="rump")
    cube("steed_body", (-3.5, 22.5 + U, -11), (7, 1.5, 5), coat("withers", 3.3, 4), tag="withers")
    # Neck and mane (mane falls on the horse's right side, a crest along the top).
    cube("steed_neck", (-2.5, 18 + U, -13), (5, 13.5, 7), coat("neck", 3.0, 5), tag="neck")
    cube("steed_neck", (-1.25, 19 + U, -6.6), (2.5, 13, 2), mane_mat(sc, seed + 6), tag="mane_crest")
    cube("steed_neck", (-3.4, 19.5 + U, -9.5), (1, 11.5, 4), none_on(("east",), mane_mat(sc, seed + 7)), tag="mane_fall")

    # Head: skull, the long face, nostrils; the jaw opens for a neigh.
    def eye_col(ex, ey):
        e = sc["eye"]
        if sc["name"] == "death":
            return e if (ex + ey) % 2 == 0 else mix(e, hexc("#9aa4b8"), 0.4)
        if ex == 0 and ey == 0:
            return mix(e, hexc("#ffffff"), 0.5)
        return e

    def skull(face, x, y, w, h):
        c = coat("head", 3.1, 8)(face, x, y, w, h)
        if face in ("east", "west"):
            fx = x if face == "west" else w - 1 - x  # 0 at the front
            if fx in (2, 3) and y in (2, 3):
                return eye_col(fx - 2, y - 2)
            if fx in (1, 4) and y in (2, 3) or fx in (2, 3) and y in (1, 4):
                return shade(c, 0.55)  # the socket
        return c
    head_glow = (lambda face, x, y, w, h, col: eye_glow_px(face, x, y, w, col, sc)) if glow else None
    cube("steed_head", (-3, 27.5 + U, -14.5), (6, 5.5, 6), skull, g=head_glow, tag="skull")
    cube("steed_head", (-2.25, 28.5 + U, -21.5), (4.5, 3.5, 7.5), coat("muzzle", 2.8, 9), tag="muzzle")
    cube("steed_head", (-2, 28.2 + U, -22.3), (4, 3, 1), lambda f, x, y, w, h: sc["coat"][1] if (f == "north" and y == 1 and x in (0, 6))
         else sc["coat"][2], tag="nose")
    cube("steed_jaw", (-1.75, 27 + U, -21), (3.5, 1.5, 7.5), coat("jaw", 2.4, 10), tag="jaw")
    cube("steed_jaw", (-2, 27.2 + U, -13.5), (4, 1.5, 2.5), coat("jaw", 2.6, 11), tag="cheek")
    cube("steed_head", (-1.5, 33 + U, -14.5), (3, 1, 3), mane_mat(sc, seed + 12), tag="forelock")
    cube("steed_head", (-1.5, 29 + U, -15), (3, 3.5, 1), mane_mat(sc, seed + 13), faces=("north",), tag="forelock_fall")
    for side in ("r", "l"):
        s = -1 if side == "r" else 1
        cube(f"steed_ear_{side}", (1.75 * s - 0.5, 32.5 + U, -10.5), (1, 2.5, 1),
             lambda f, x, y, w, h: sc["coat"][1] if f == "north" else sc["coat"][3], tag="ear")
    # Bridle: browband, noseband, cheek straps, the bit's rings.
    tack = leather_mat(sc["tack"], seed + 14, 2.4)
    metal = lambda f, x, y, w, h: sc["metal"][3.5 + (0.8 if f == "up" else 0) - (0.6 if y else 0)]
    cube("steed_head", (-3, 31.5 + U, -14.5), (6, 0.5, 6), none_on(("up", "down"), tack), inflate=0.12, tag="browband")
    cube("steed_head", (-2.25, 29.5 + U, -20), (4.5, 0.5, 2), none_on(("up", "down"), tack), inflate=0.15, tag="noseband")
    for s in (-1, 1):
        cube("steed_head", (2.3 * s - 0.25, 28.5 + U, -17.5), (0.5, 3, 0.5), tack, tag="cheek_strap")
        cube("steed_head", (2.4 * s - 0.5, 28.2 + U, -19.5), (1, 1, 1), metal, tag="bit")
    # Legs: forearm / gaskin, cannon, fetlock feathering, hoof.
    stock = sc["name"] == "war"
    for leg in LEGS:
        s = -1 if leg[1] == "r" else 1
        front = leg[0] == "f"
        z = -8.5 if front else 8.0
        up, low = f"steed_leg_{leg}", f"steed_shin_{leg}"
        if front:
            cube(up, (3 * s - 1.25, 8, z - 1.25), (2.5, 8, 2.5), coat("leg", 2.8, 20), tag="forearm")
            cube(up, (3 * s - 1.75, 12, z - 2), (3.5, 4.5, 3.5), coat("leg", 3.0, 21), tag="elbow")
        else:
            cube(up, (3 * s - 1.75, 9, z - 2), (3.5, 8.5, 4.5), coat("leg", 2.9, 22), tag="gaskin")
            cube(up, (3 * s - 1.25, 7.5, z), (2.5, 2, 2.5), coat("leg", 2.7, 26), tag="hock")
        zz = z + (0 if front else 1.0)

        def cannon(face, x, y, w, h, stock=stock):
            c = coat("leg", 2.5, 23)(face, x, y, w, h)
            return mix(c, sc["mane"][2], 0.85) if stock and y > h * 0.3 else c
        cube(low, (3 * s - 1, 1.5, zz - 1), (2, 7.5 if front else 7, 2), cannon, tag="cannon")
        cube(low, (3 * s - 1.25, 1.5, zz - 1.25), (2.5, 1.5, 2.5), none_on(("up", "down"), mane_mat(sc, seed + 24, 2.2)),
             tag="feather")
        cube(low, (3 * s - 1.5, 0, zz - 1.5), (3, 1.5, 3), hoof_mat(sc, seed + 25), tag="hoof")
    # Tail: a dock and the long fall of hair.
    cube("steed_tail", (-1.25, 16 + U, 10.5), (2.5, 7, 2), mane_mat(sc, seed + 30, 2.8), tag="tail_dock")
    cube("steed_tail_tip", (-1.75, 5 + U, 10.25), (3.5, 11.5, 2.5), mane_mat(sc, seed + 31, 2.5), tag="tail_hair")
    # Saddle: the cloth over the barrel, the seat, pommel and cantle, girth and stirrups.
    blanket = sc["blanket"]

    def cloth(face, x, y, w, h):
        if face == "down":
            return None
        n = fbm(x * 2, y * 2 + len(face), seed + 40, 32, 32, 2, 6.0)
        c = blanket[2.6 + (n - 0.5) * 1.2]
        if face in ("east", "west") and (y >= h - 2 or x in (0, w - 1)):
            c = sc["metal"][3] if (x + y) % 2 == 0 else sc["metal"][2]  # trim
        return c
    leather = leather_mat(sc["tack"], seed + 41, 2.8)
    cube("saddle", (-5.4, 17 + U, -5), (10.8, 6.1, 10), cloth, inflate=0.2, tag="saddle_cloth")
    cube("saddle", (-3.5, 23 + U, -4), (7, 1.5, 8), leather, tag="seat")
    cube("saddle", (-2, 24.5 + U, -4.5), (4, 1.5, 1.5), leather, tag="pommel")
    cube("saddle", (-3, 24.5 + U, 3), (6, 1.5, 1.5), leather, tag="cantle")
    cube("saddle", (-5.3, 13.5 + U, -1), (10.6, 1, 2), none_on(("up",), leather), inflate=0.15, tag="girth")
    for s in (-1, 1):
        cube("saddle", (5.6 * s - 0.25, 13.5 + U, -0.5), (0.5, 9, 1), leather, tag="stirrup_strap")
        cube("saddle", (5.6 * s - 0.75, 12.5 + U, -1), (1.5, 1, 2), metal, tag="stirrup")
    return m


def eye_glow_px(face, x, y, w, col, sc):
    if face not in ("east", "west"):
        return shade(col, sc["body_glow"]) if sc["body_glow"] else None
    fx = x if face == "west" else w - 1 - x
    if fx in (2, 3) and y in (2, 3):
        return shade(col, sc["eye_glow"])
    return shade(col, sc["body_glow"]) if sc["body_glow"] else None


# --- motion -----------------------------------------------------------------------------------------
# Pose functions return {bone: [rx, ry, rz]} plus "@bone" for positions. Bedrock degrees: -X lifts the
# front of a horizontal part (and swings a hanging leg forward).

def _s(t, period, phase=0.0):
    return math.sin(2 * math.pi * (t / period + phase))


def idle_pose(t, L=4.0):
    k = 2 * math.pi / L
    p = {
        "steed_neck": [2 * math.sin(k * t), 3 * math.sin(k * t * 0.5), 0],
        "steed_head": [3 * math.sin(k * t + 1.0), 0, 0],
        "steed_tail": [4 * math.sin(k * t * 2), 0, 10 * math.sin(k * t)],
        "steed_tail_tip": [3 * math.sin(k * t * 2 + 0.6), 0, 8 * math.sin(k * t + 0.8)],
        "steed_ear_r": [0, 0, 10 * max(0.0, math.sin(k * t * 2 + 2.0)) ** 8],
        "steed_ear_l": [0, 0, -10 * max(0.0, math.sin(k * t + 0.4)) ** 8],
        "steed_body": [0.4 * math.sin(k * t * 2), 0, 0],
        "@steed_body": [0, 0.15 * math.sin(k * t * 2), 0],
        # Resting a hind hoof: tipped up for part of the loop.
        "steed_leg_hl": [-4 * max(0.0, math.sin(k * t)), 0, 0],
        "steed_shin_hl": [10 * max(0.0, math.sin(k * t)), 0, 0],
    }
    return p


def walk_pose(t, L=1.2, amp=1.0):
    """A four-beat walk: hind left, front left, hind right, front right."""
    phases = {"hl": 0.0, "fl": 0.25, "hr": 0.5, "fr": 0.75}
    p = {}
    for leg, ph in phases.items():
        a = _s(t, L, ph)
        lift = max(0.0, _s(t, L, ph + 0.25))
        front = leg[0] == "f"
        p[f"steed_leg_{leg}"] = [-22 * amp * a, 0, 0]
        p[f"steed_shin_{leg}"] = [30 * amp * lift if front else -18 * amp * lift, 0, 0]
    p["steed_body"] = [0.8 * _s(t, L / 2), 0, 1.2 * _s(t, L)]
    p["@steed_body"] = [0, 0.4 * _s(t, L / 2, 0.25), 0]
    p["steed_neck"] = [4 * _s(t, L / 2, 0.1), 2 * _s(t, L), 0]
    p["steed_head"] = [-2 * _s(t, L / 2, 0.1), 0, 0]
    p["steed_tail"] = [6, 0, 8 * _s(t, L)]
    p["steed_tail_tip"] = [4 * _s(t, L / 2), 0, 6 * _s(t, L, 0.2)]
    return p


def gallop_pose(t, L=0.6, amp=1.0, stretch=1.0):
    """A rotary gallop: hind right, hind left, front right, front left, then the moment of suspension."""
    phases = {"hr": 0.0, "hl": 0.1, "fr": 0.45, "fl": 0.55}
    p = {}
    for leg, ph in phases.items():
        a = _s(t, L, ph)
        b = _s(t, L, ph + 0.25)
        front = leg[0] == "f"
        if front:
            p[f"steed_leg_{leg}"] = [-42 * amp * stretch * a - 6, 0, 0]
            p[f"steed_shin_{leg}"] = [55 * amp * max(0.0, b), 0, 0]
        else:
            p[f"steed_leg_{leg}"] = [-38 * amp * stretch * a + 4, 0, 0]
            p[f"steed_shin_{leg}"] = [-38 * amp * max(0.0, -b), 0, 0]
    pitch = 5 * amp * _s(t, L, 0.3)
    p["steed_body"] = [pitch, 0, 0]
    p["@steed_body"] = [0, 1.0 * amp * _s(t, L, 0.05), 0]
    p["steed_neck"] = [-14 + 10 * amp * _s(t, L, 0.6), 0, 0]
    p["steed_head"] = [-6 - 6 * amp * _s(t, L, 0.7), 0, 0]
    p["steed_jaw"] = [6 * max(0.0, _s(t, L, 0.1)), 0, 0]
    p["steed_ear_r"] = [-25, 0, -8]
    p["steed_ear_l"] = [-25, 0, 8]
    p["steed_tail"] = [55 + 8 * _s(t, L, 0.2), 0, 4 * _s(t, L * 2)]
    p["steed_tail_tip"] = [10 + 10 * _s(t, L, 0.4), 0, 6 * _s(t, L * 2, 0.3)]
    return p


def rear_pose(k, flail=0.0):
    """Reared up on the hind legs; k in 0..1 is how far up, flail (radians) kicks the forelegs."""
    up = -48 * k
    p = {
        "steed_body": [up, 0, 0],
        "@steed_body": [0, 0, 0],
        "steed_leg_hr": [-up - 8 * k, 0, 0], "steed_leg_hl": [-up - 12 * k, 0, 0],
        "steed_shin_hr": [10 * k, 0, 0], "steed_shin_hl": [14 * k, 0, 0],
        "steed_leg_fr": [-30 * k - 30 * k * math.sin(flail), 0, 0],
        "steed_leg_fl": [-30 * k - 30 * k * math.sin(flail + 2.0), 0, 0],
        "steed_shin_fr": [80 * k, 0, 0], "steed_shin_fl": [70 * k, 0, 0],
        "steed_neck": [-20 * k, 0, 0],
        "steed_head": [-10 * k, 0, 0],
        "steed_jaw": [22 * k, 0, 0],
        "steed_ear_r": [-30 * k, 0, 0], "steed_ear_l": [-30 * k, 0, 0],
        "steed_tail": [40 * k, 0, 0],
        "steed_tail_tip": [10 * k, 0, 0],
    }
    return p


def seat_follow(pose, lean=0.0):
    """Where a rider (bone `rider`, pivot at the origin, hips at y=12) must go to stay in the saddle of a steed
    posed with `pose`: returns (position, rotation) keys for `rider`. Same math as GeckoLib for X rotations."""
    rx = (pose.get("steed_body") or [0, 0, 0])[0]
    off = pose.get("@steed_body") or [0, 0, 0]
    hip = (0.0, SEAT_Y + RIDER_HIP, 0.0)
    py, pz = BODY_PIVOT[1], BODY_PIVOT[2]
    y, z = hip[1] - py, hip[2] - pz
    a = math.radians(rx)
    y2 = y * math.cos(a) + z * math.sin(a)
    z2 = -y * math.sin(a) + z * math.cos(a)
    ny, nz = py + y2 + off[1], pz + z2 + off[2]
    # The rider's own pivot is at his feet (origin): rotating him about it by rx moves his hips too.
    ry_, rz_ = 12 * math.cos(a), -12 * math.sin(a)
    return [0, round(ny - ry_, 3), round(nz - rz_, 3)], [round(rx + lean, 3), 0, 0]


def sample(anim, fn, t0, t1, step, shift=0.0, names=None):
    """Keys every bone of pose function fn(t) from t0 to t1 (inclusive) at `step` intervals (clip time + shift)."""
    n = max(1, int(round((t1 - t0) / step)))
    for i in range(n + 1):
        t = t0 + (t1 - t0) * i / n
        pose = fn(t)
        for bone, v in pose.items():
            if names is not None and bone.lstrip("@") not in names:
                continue
            vec = [round(float(c), 3) for c in v]
            if bone.startswith("@"):
                anim.pos(bone[1:], (round(t + shift, 4), vec))
            else:
                anim.rot(bone, (round(t + shift, 4), vec))


def lerp_pose(a, b, k):
    out = {}
    for key in set(a) | set(b):
        va, vb = a.get(key, [0, 0, 0]), b.get(key, [0, 0, 0])
        out[key] = [va[i] + (vb[i] - va[i]) * k for i in range(3)]
    return out


def add_pose(*poses):
    out = {}
    for p in poses:
        for key, v in p.items():
            o = out.get(key, [0, 0, 0])
            out[key] = [o[i] + v[i] for i in range(3)]
    return out


def smooth(k):
    k = max(0.0, min(1.0, k))
    return k * k * (3 - 2 * k)


# --- the standalone steed ---------------------------------------------------------------------------

A = "animation.horseman_steed."


def anims():
    f = AnimFile()
    idle = f.new(A + "idle", 4.0, loop=True)
    sample(idle, idle_pose, 0, 4.0, 0.25)
    walk = f.new(A + "walk", 1.2, loop=True)
    sample(walk, walk_pose, 0, 1.2, 0.1)
    gallop = f.new(A + "gallop", 0.6, loop=True)
    sample(gallop, gallop_pose, 0, 0.6, 0.05)

    rear = f.new(A + "rear", 2.0)

    def rear_t(t):
        k = smooth(t / 0.45) if t < 1.3 else smooth((2.0 - t) / 0.7)
        return rear_pose(k, t * 9.0)
    sample(rear, rear_t, 0, 2.0, 0.05)
    return f


def standalone():
    """One geometry for all four steeds (identical UV layout), so the code can swap textures."""
    out = {}
    for name, sc in SCHEMES.items():
        m = Model("horseman_steed", 256, 256)
        m.bone("root", (0, 0, 0))
        build_steed(m, sc)
        t, g = m.build(gutter=1, seed=950)
        out[name] = (m, t, g)
    return out


def generate():
    steeds = standalone()
    steeds["war"][0].rig.write(GEO + "horseman_steed.geo.json")
    for name, (m, t, g) in steeds.items():
        save(t, "entity", f"horseman_steed_{name}")
        save(g, "entity", f"horseman_steed_{name}_glowmask")
    anims().write(ANIM + "horseman_steed.animation.json")
