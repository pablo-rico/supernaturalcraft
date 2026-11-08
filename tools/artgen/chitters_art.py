"""A Chitter (v0.42, season 11): rig, texture + glowmask and clips (S11Assets.CHITTER: geo/entity/chitter.geo.json,
animations/entity/chitter.animation.json, textures/entity/chitter.png and chitter_glowmask.png).

A human hollowed out by the spirit parasites of "Thin Lizzie"/"Into the Mystic"'s Colorado hills (the Chitters): 31 px, wiry.
Pallid, blotchy skin with raw red patches, as if half-skinned; patchy, matted dark hair; EMPTY black eye sockets with a faint
sickly glint deep inside (glowmask); a mouth stretched far too wide, from cheekbone to cheekbone, packed with rows of tiny
teeth, on a jaw that drops wide (`jaw`). A torn, filthy flannel shirt hanging open over a stained undershirt, jeans ripped at
the knees, bare dirty feet with long toenails. Twitchy and hunched: the clips carry the posture.

Skeleton: apoc_vessel's (root, body, head, eyes, brows, arms with forearm / hand / finger bones, legs with shins) plus `jaw`
(head) and the shirt's flaps `shirt_r`, `shirt_l` (body). The glow is the eye sockets' glint and a faint wet sheen of the teeth.
"""

import math

import horsemen_art as H
import s1214_kit as K
import thule_art as T
from animkit import AnimFile
from apoc_vessel import Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, hair_m, legs, neck, skeleton, tone, torso
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, fbm, hexc, mix, shade

NAME = "chitter"
A = "animation.chitter."

B = Body(HIP=11.8, NECK=23.4, TW=3.6, TD=1.8, AW=3.0, SY=22.8, ELBOW=17.9, WRIST=13.3, PALM=11.5, LX=1.8, LW=3.3, KNEE=6.2,
         SHOE_H=0.9)
JAW_H = 2.5

SKIN = Ramp("#4a3433", "#6e5352", "#937776", "#b19896", "#c9b4b0", "#ddcdc8")
RAW = Ramp("#3a0f10", "#5a1b1a", "#7a2a26", "#954038", "#ab5a4e", "#c27a6c")
HAIR = Ramp("#0d0a08", "#17120e", "#211a14", "#2c231b", "#382d23", "#463a2e")
FLANNEL_A = Ramp("#1f0c0b", "#2f1512", "#431f19", "#572a21", "#6b372b", "#814839")
FLANNEL_B = Ramp("#141710", "#1d2117", "#282d1f", "#333a28", "#3f4733", "#4f5940")
UNDER = Ramp("#4a4538", "#635d4c", "#7c7561", "#938b74", "#a69e86", "#b9b29a")
DENIM = Ramp("#12161d", "#1b212b", "#252d3a", "#2f3a4a", "#3b485b", "#4c5b70")
DIRT = hexc("#3a2d1e")
BLOOD = hexc("#4a0d0c")
SOCKET = hexc("#030203")
THROAT = hexc("#0c0405")
TOOTH = Ramp("#5c5440", "#7c735a", "#9c9276", "#b6ad90", "#cbc3a6", "#ddd6bb")
GLINT_HOT, GLINT, GLINT_DIM = hexc("#e6ff9a"), hexc("#a8c84a"), hexc("#4a5a1e")

# The face: rows 0-10 on the skull, 11-15 on the jaw. O socket (black), o socket rim, G glint, t tooth, m gap between teeth,
# w the wide mouth's inner dark, r raw patch, R raw edge.
FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhshhhhhhhhh",
    "hhsshsssssshhhhh",
    "hssssssrrssssshh",
    "sssssssRrrssssss",
    "sbbbbsssssbbbbbs",
    "soOOOossssoOOOos",
    "sOGGOOssssOOGGOs",
    "soOOOossssoOOOos",
    "srsoosnnnssooscs",
    "RrssssNnnNsssRrs",
    "ctttttttttttttts",
    "cwmwmwmwmwmwmwms",
    "ctttttttttttttts",
    "sjsssrssssssssjs",
    "ssjjjjjjjjjjjjss",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def skin_mat(seed, base=3.0, raw=0.24):
    """Pallid, blotchy -- raw red where the skin looks peeled away, a few dark veins."""
    def m(f, x, y, w, h):
        n = fbm(x * 1.5 + len(f) * 5, y * 1.5, seed, 64, 64, 3, 6.0)
        c = tone(SKIN, base + (n - 0.5) * 1.2)
        r = fbm(x * 2.1 + len(f) * 13, y * 2.1, seed + 7, 64, 64, 2, 5.0)
        if r > 1 - raw:
            k = (r - (1 - raw)) / raw
            c = tone(RAW, 2.0 + 2.0 * (1 - k)) if k > 0.3 else mix(c, RAW[4], 0.55)
        if h01("vein", seed, f, x // 2, y) < 0.04:
            c = mix(c, hexc("#4c3a4a"), 0.4)
        return c
    return m


def filthy(mat, seed, holes=0.07, rag=0):
    def m(f, x, y, w, h):
        c = mat(f, x, y, w, h)
        if f in ("up", "down"):
            return c
        n = fbm(x * 1.4 + len(f) * 7, y * 1.4, seed, 64, 64, 3, 6.0)
        if n > 0.66:
            c = mix(c, DIRT if h01("dt", seed, f, x // 3, y // 3) < 0.7 else BLOOD, min(0.65, (n - 0.66) * 3.0))
        if fbm(x * 2.3 + len(f) * 3, y * 2.3, seed + 5, 64, 64, 2, 5.0) > 1 - holes:
            return None
        if rag and y >= h - rag and h01("rag", seed, f, x) < 0.2 + 0.6 * (y - (h - rag) + 1) / rag:
            return None
        return c
    return m


def plaid(seed, base=3.0):
    def m(f, x, y, w, h):
        a = (x // 2) % 3 == 0
        b = (y // 2) % 3 == 0
        r = FLANNEL_A if not (a and b) else FLANNEL_B
        v = base + (0.6 if a else 0) + (0.5 if b else 0) - 0.6 * y / max(1, h) + 0.3 * math.sin(x * 0.9 + y * 0.3 + seed)
        if f in ("east", "west"):
            v -= 0.3
        return tone(r if not a or not b else FLANNEL_B, v - (0.8 if (a and b) else 0))
    return m


def build_head(m, seed=64001):
    sk = skin_mat(seed, 3.1)
    hr0 = hair_m(HAIR, seed + 1, base=2.6)

    def hr(f, x, y, w, h):
        if fbm(x * 2.5 + len(f) * 3, y * 2.5, seed + 4, 64, 64, 2, 5.0) > 0.72:
            return sk(f, x, y, w, h)                              # bald, scabbed patches
        return hr0(f, x, y, w, h)
    sh = lambda k: (lambda f, x, y, w, h: shade(sk(f, x, y, w, h), k))
    lg = {"s": sk, "h": hr, "b": sh(0.66), "O": SOCKET, "o": hexc("#2a1416"), "G": hexc("#1a1c0c"), "n": SKIN[4], "N": SKIN[1],
          "c": sh(0.78), "j": sh(0.84), "r": RAW[2], "R": RAW[4],
          "t": lambda f, x, y, w, h: tone(TOOTH, 3.6 - (x % 2) * 1.2), "m": THROAT, "w": lambda f, x, y, w, h: tone(TOOTH, 2.4 + (x % 2))}
    sides = H.head_sides(sk, hr, top=3, back=11, temple=7, sideburn=5)

    def face(row0, xo=0):
        def f_(f, x, y, w, h):
            yy = y + row0
            if f == "north":
                ch = FACE[yy][x + xo] if yy < 16 and 0 <= x + xo < 16 else "s"
                v = lg.get(ch, sk)
                return v(f, x, yy, w, 16) if callable(v) else v
            if f in ("east", "west"):
                fx = x if f == "west" else w - 1 - x
                if row0 and fx < 2 and y < 3:
                    return tone(TOOTH, 2.8)                      # the mouth runs round the corners of the face
            return sides(f, x, yy if f != "up" else y, w, 16 if f not in ("up", "down") else h)
        return f_

    def glow(f, x, y, w, h, c, row0=0, xo=0):
        if f != "north":
            return None
        ch = FACE[y + row0][x + xo] if y + row0 < 16 and 0 <= x + xo < 16 else "s"
        if ch == "G":
            return GLINT
        if ch == "t" and (x + y) % 3 == 0:
            return hexc("#2a2a20")                               # a wet glint on the teeth
        return None
    hs = B.HEAD / 2
    skull_h = B.HEAD - JAW_H

    def skull(f, x, y, w, h):
        if f == "down":
            if y <= 1:
                return tone(TOOTH, 3.4 - (x % 2) * 1.4)          # the upper rows of tiny teeth
            return THROAT
        return face(0)(f, x, y, w, h)
    m.cube("head", (-hs, B.NECK + JAW_H, -hs), (B.HEAD, skull_h, B.HEAD), skull, glow=glow, density=2, tag="skull")
    m.cube("head", (-0.5, B.NECK + JAW_H + 0.6, -hs - 0.4), (1, 1.5, 0.4),
           lambda f, x, y, w, h: (THROAT if (f == "north" and y == h - 1) else SKIN[3] if f == "north" else SKIN[2]), density=2, tag="nose")
    jaw = m.bone("jaw", (0, B.NECK + JAW_H, 1.6), "head")

    def jawm(f, x, y, w, h):
        if f == "up":
            if y <= 1 and 1 <= x <= w - 2:
                return tone(TOOTH, 3.2 - (x % 2) * 1.4)          # the lower rows
            return THROAT
        if f == "down":
            return sk(f, x, y, w, h)
        return face(11, 0)(f, x, y, w, h)
    m.cube(jaw, (-hs, B.NECK, -hs), (B.HEAD, JAW_H, B.HEAD - 0.4), jawm,
           glow=lambda f, x, y, w, h, c: glow(f, x, y, w, h, c, 11) if f == "north" else None, density=2, tag="jaw")
    # matted locks hanging over the brow and down the nape
    hp = lambda f, x, y, w, h: hr0(f, x, y, w, h)
    m.cube("head", (-hs, B.crown - 1.6, -hs), (B.HEAD, 1.6, B.HEAD), none_on(("down",), lambda f, x, y, w, h: (None if f == "north" and (h01("fr", x) < 0.45 and y > 0) else hp(f, x, y, w, h))),
           inflate=0.25, density=2, tag="hair_cap")
    for i, (x, ln) in enumerate(((-3.4, 4.2), (-0.8, 1.0), (1.4, 1.3), (3.4, 3.6))):
        m.cube("head", (x - 0.5, B.crown - 1.2 - ln, -hs - 0.35), (1.0, ln, 0.3), hp,
               density=2, tag="lock")
    neck(m, B, sk, 2.8)


def build_body(m, seed=64200):
    sk = skin_mat(seed + 1, 3.0)
    under = filthy(fabric(UNDER, seed + 2, 3.0, 0.3), seed + 2, holes=0.08)

    def tee(f, x, y, w, h):
        if f == "north" and y < 3 and abs(x - (w - 1) / 2) < 3 - y:
            return sk(f, x, y, w, h)
        return under(f, x, y, w, h)
    torso(m, B, tee, tag="torso")
    flannel = filthy(plaid(seed + 3, 3.0), seed + 3, holes=0.06, rag=2)
    # the open flannel: back and sides as a shell, the fronts as two flaps that hang open (bones shirt_r / shirt_l)
    m.cube("body", (-B.TW, B.HIP - 1.0, -B.TD), (2 * B.TW, B.NECK - B.HIP + 0.6, 2 * B.TD),
           lambda f, x, y, w, h: None if f in ("north", "down") else flannel(f, x, y, w, h), inflate=0.3, density=2, tag="shirt")
    for side, s in (("right", -1), ("left", 1)):
        b = m.bone(f"shirt_{side[0]}", (s * B.TW, B.NECK - 0.4, -B.TD - 0.35), "body")
        xa, xb = (-B.TW - 0.3, -1.4) if s < 0 else (1.4, B.TW + 0.3)

        def flap(f, x, y, w, h, s=s):
            c = flannel(f, x, y, w, h)
            if c is None:
                return None
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return FLANNEL_B[0]                                 # the edge
            if f == "north" and (x == w - 2 if s < 0 else x == 1) and y % 5 == 2:
                return UNDER[4]                                     # a button left dangling
            return c
        m.cube(b, (xa, B.HIP - 1.2, -B.TD - 0.6), (xb - xa, B.NECK - B.HIP + 0.4, 0.4), flap, density=2, tag="shirt_flap")
    cuff = lambda f, x, y, w, h: None if f in ("up",) or h01("cuff", x, y, f) < 0.4 else tone(FLANNEL_A, 2.4)
    arms(m, B, filthy(plaid(seed + 4, 3.0), seed + 4, holes=0.06, rag=2), sk, cuff=cuff, cuff_depth=0.5, sleeve_inflate=0.3)
    # the sleeves end ragged above the wrist: bare, raw forearms below them
    denim = filthy(fabric(DENIM, seed + 5, 3.0, 0.3, crease=False), seed + 5, holes=0.04)

    def jeans(f, x, y, w, h):
        c = denim(f, x, y, w, h)
        if c is not None and f == "north" and h01("fade", seed, x, y) < 0.18:
            c = shade(c, 1.25)
        return c

    def ripped(f, x, y, w, h):
        if f == "north" and y < 4 and 1 <= x <= w - 2 and h01("rip", seed, x, y) < 0.7:
            return sk(f, x, y, w, h)                                # torn open at the knee
        return jeans(f, x, y, w, h)
    for side, s in (("right", -1), ("left", 1)):
        x0 = B.LX * s - B.LW / 2
        m.cube(f"{side}_leg", (x0, B.KNEE, -B.LW / 2), (B.LW, B.HIP - B.KNEE, B.LW), jeans, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, 2.2, -B.LW / 2), (B.LW, B.KNEE - 2.2, B.LW), filthy(ripped, seed + 6, 0.0, rag=2), density=2,
               tag="shin")
        m.cube(f"{side}_shin", (x0 + 0.2, 0, -B.LW / 2 + 0.2), (B.LW - 0.4, 2.4, B.LW - 0.4), lambda f, x, y, w, h: mix(sk(f, x, y, w, h), DIRT, 0.35 + 0.1 * y),
               density=2, tag="ankle")

        def foot(f, x, y, w, h):
            if f == "north" and y >= h - 1 and x % 2 == 0:
                return hexc("#2a2018")                              # toenails
            return mix(sk(f, x, y, w, h), DIRT, 0.5 if f in ("down", "north") else 0.3)
        m.cube(f"{side}_shin", (x0 + 0.1, 0, -B.LW / 2 - 1.4), (B.LW - 0.2, 0.9, B.LW + 1.2), foot, density=2, tag="foot")


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=False)
    build_head(m)
    build_body(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

HUNCH = P_(K.relax("right"), K.relax("left"),
           {"body": [34, 0, 0], "head": [-32, 0, 6], "right_arm": [-20, 0, 8], "left_arm": [-24, 0, -6], "right_forearm": [-24, 0, 0],
            "left_forearm": [-30, 0, 0], "right_leg": [-14, 0, 4], "left_leg": [-8, 0, -4], "right_shin": [22, 0, 0],
            "left_shin": [16, 0, 0], "shirt_r": [-10, 0, 0], "shirt_l": [-10, 0, 0], "@root": [0, -0.8, 0]})
CLAWS = P_(K.spread("right"), K.spread("left"), {"right_index": [-30, 0, 0], "left_index": [-30, 0, 0]})


def _limb(rig_, pose, side, leg, end_t, joint_t):
    """Two-point IK for one limb: its end (palm or sole) to `end_t`, its joint (elbow or knee) toward `joint_t`."""
    s = -1 if side == "right" else 1
    if leg:
        up, low, end_b, joint_b = f"{side}_leg", f"{side}_shin", f"{side}_shin", f"{side}_shin"
        end_p, joint_p = (B.LX * s, 0.4, -0.6), (B.LX * s, B.KNEE, 0)
    else:
        up, low, end_b, joint_b = f"{side}_arm", f"{side}_forearm", f"{side}_hand", f"{side}_forearm"
        end_p, joint_p = (B.AX * s, B.PALM + 0.4, 0), (B.AX * s, B.ELBOW, 0)

    def cost(p):
        e = T.world(rig_, p, end_b, end_p)
        j = T.world(rig_, p, joint_b, joint_p)
        return (math.dist(e, end_t) + 0.35 * math.dist(j, joint_t)
                + 0.00001 * sum(v * v for v in p[up] + p[low]))
    chans = [(up, 0), (up, 1), (up, 2), (low, 0), (low, 2)]
    best = None
    for st in ((0, 0, 60 * s), (-60, 0, 60 * s), (60, 0, 60 * s), (0, 0, 0)):
        q = T.solve(P_(pose, {up: list(st), low: [0, 0, -60 * s]}), chans, cost)
        cc = cost(q)
        if best is None or cc < best[0]:
            best = (cc, q)
    return best[1]


def spider(rig_, u):
    """The crawl at phase u (0..1): the torso flat and low, the limbs splayed out like a spider's -- elbows and knees high
    and wide, palms and soles on the ground; diagonal pairs (right hand + left foot, left hand + right foot) swing in turn."""
    sway = math.sin(u * 2 * math.pi)
    pose = P_(CLAWS, {"body": [80, 5 * sway, 4 * sway], "head": [-74, -8 * sway, 6 * sway], "@root": [0, -4.4, 3.0],
                      "shirt_r": [-40, 0, 0], "shirt_l": [-40, 0, 0], "right_hand": [30, 0, 0], "left_hand": [30, 0, 0]})
    for side, s in (("right", -1), ("left", 1)):
        for leg in (False, True):
            ph = u * 2 * math.pi + (0 if (side == "right") != leg else math.pi)
            dz = 2.4 * math.cos(ph)                       # forward (-z) at the start of the planted phase
            lift = 2.6 * max(0.0, math.sin(ph))           # off the ground while swinging forward
            if leg:
                end_t = (6.8 * s, 0.3 + lift, 8.0 - dz)
                joint_t = (7.6 * s, 9.0 + lift * 0.5, 4.0 - dz * 0.5)
            else:
                end_t = (7.2 * s, 0.3 + lift, -11.0 - dz)
                joint_t = (8.0 * s, 11.0 + lift * 0.5, -6.0 - dz * 0.5)
            pose = _limb(rig_, pose, side, leg, end_t, joint_t)
    return pose


def anims(rig_):
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]
    base = T.ground(rig_, HUNCH, 0.0)
    gy = base["@root"][1]

    # --- loops ---------------------------------------------------------------------------------------------------
    # Idle (2.4 s): hunched, breathing in shallow pants; the head snaps from one wrong angle to another and holds.
    c = clip("idle", 2.4, loop=True)
    jerks = [(0.0, [-32, 0, 6]), (0.5, [-32, 0, 6]), (0.56, [-40, 18, 34]), (0.95, [-38, 16, 30]), (1.0, [-24, -10, -12]),
             (1.5, [-26, -8, -14]), (1.56, [-36, 6, 22]), (1.62, [-30, -4, -6]), (2.0, [-32, 2, 4]), (2.4, [-32, 0, 6])]
    for i, (t, hd) in enumerate(jerks):
        b = math.sin(t / 2.4 * 2 * math.pi * 3)
        k = 0 if i == len(jerks) - 1 else i          # the last key repeats the first: a seamless loop
        c.key(t, P_(base, {"head": hd, "@body": [0, 0.15 * b, 0], "right_fingers": [18 + 20 * (k % 2), 0, 0],
                           "left_index": [10 + 30 * ((k + 1) % 2), 0, 0]}),
              (E_SNAP if t in (0.56, 1.0, 1.56, 1.62) else E_IO) if i else None)

    # Walk (1.1 s): jerky, uneven steps, the shoulders hitching, the head twitching on every footfall.
    c = clip("walk", 1.1, loop=True)
    K.walk(c, base, 1.1, stride=20, arm=16, bob=0.6, twist=7, knee=(10, 34),
           extra=lambda s: {"head": [-32 + 6 * s, 10 * s, 14 * s], "body": [36, 7 * s, 6 * s], "@root": [0, gy, 0],
                            "shirt_r": [-14 + 6 * s, 0, 0], "shirt_l": [-14 - 6 * s, 0, 0]})
    c.key(0.2, P_(c.frames[0][1], {"head": [-40, -12, -20], "@body": [0, 0.6, 0]}), E_SNAP)
    c.key(0.75, P_(c.frames[1][1], {"head": [-24, 14, 24], "@body": [0, 0.6, 0]}), E_SNAP)

    # Crawl (0.9 s): on all fours like a spider -- body pitched flat, the limbs splayed wide, elbows and knees up,
    # diagonal pairs stepping; the head craned up to look ahead.
    c = clip("crawl", 0.9, loop=True)
    for i in range(5):
        c.key(0.9 * i / 4, spider(rig_, (i % 4) / 4.0), E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    # Pounce (1.0 s; airborne at 0.35 s, the claws land at 0.5 s): a deep crouch, a spring forward, arms out, a landing crouch.
    c = clip("pounce", 1.0)
    crouch = T.ground(rig_, P_(base, CLAWS, {"body": [40, 0, 0], "head": [-40, 0, 0], "right_leg": [-80, 0, 8], "left_leg": [-74, 0, -8],
                                             "right_shin": [110, 0, 0], "left_shin": [110, 0, 0], "right_arm": [20, 0, 20],
                                             "left_arm": [20, 0, -20], "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0]}), 0.0)
    leap = P_(CLAWS, {"body": [50, 0, 0], "head": [-50, 0, 0], "right_arm": [-150, 0, -18], "left_arm": [-150, 0, 18],
                      "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "right_leg": [40, 0, 10], "left_leg": [30, 0, -10],
                      "right_shin": [50, 0, 0], "left_shin": [40, 0, 0], "jaw": [30, 0, 0], "@root": [0, 6.0, -6.0],
                      "shirt_r": [40, 0, 0], "shirt_l": [40, 0, 0]})
    land = P_(CLAWS, {"body": [56, 0, 0], "head": [-50, 0, 0], "right_arm": [-80, 0, -10], "left_arm": [-80, 0, 10],
                      "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0], "right_leg": [-70, 0, 8], "left_leg": [-50, 0, -8],
                      "right_shin": [100, 0, 0], "left_shin": [90, 0, 0], "jaw": [20, 0, 0], "@root": [0, 0, -8.0]})
    land = T.ground(rig_, land, 0.0)
    c.key(0, base)
    c.key(0.25, crouch, E_IO)
    c.key(0.35, P_(leap, {"@root": [0, 4.0, -3.0]}), E_OUT)
    c.key(0.45, leap, E_OUT)
    c.key(0.55, land, E_IN)
    c.key(0.75, P_(land, {"@root": [0, land["@root"][1], -8.0]}), E_OUT)
    c.key(1.0, base, E_IO)

    # Bite (0.8 s; the teeth close at 0.35 s): the head shoots forward, the jaw gapes, snaps shut.
    c = clip("bite", 0.8)
    gape = P_(base, CLAWS, {"body": [36, 0, 0], "head": [-40, 0, 0], "@head": [0, 0, -1.0], "jaw": [56, 0, 0], "@jaw": [0, -0.8, 0],
                            "right_arm": [-60, 0, 10], "left_arm": [-60, 0, -10], "@root": [0, gy, -1.6]})
    snap = P_(base, CLAWS, {"body": [42, 0, 0], "head": [-30, 0, 0], "@head": [0, -0.4, -2.0], "jaw": [0, 0, 0],
                            "right_arm": [-40, 0, 10], "left_arm": [-40, 0, -10], "@root": [0, gy, -3.0]})
    c.key(0, base)
    c.key(0.22, gape, E_IO)
    c.key(0.35, snap, E_SNAP)
    c.key(0.5, P_(snap, {"head": [-26, 10, 14]}), E_OUT)
    c.key(0.8, base, E_IO)

    # Screech (1.4 s; the cry peaks at 0.4 s and holds to 1.0 s): head thrown back, jaw wide, arms flung out, chest up.
    c = clip("screech", 1.4)
    rear = P_(K.spread("right"), K.spread("left"),
              {"body": [-10, 0, 0], "head": [-50, 0, 0], "jaw": [62, 0, 0], "@jaw": [0, -1.2, 0.2], "right_arm": [-30, 0, 70],
               "left_arm": [-30, 0, -70], "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0], "right_leg": [-6, 0, 6],
               "left_leg": [-6, 0, -6], "shirt_r": [20, 0, 10], "shirt_l": [20, 0, -10], "@root": [0, 0.2, 0.4]})
    c.key(0, base)
    c.key(0.25, P_(base, {"head": [-10, 0, 0], "jaw": [10, 0, 0], "body": [34, 0, 0]}), E_IO)
    c.key(0.4, rear, E_SNAP)
    for i, t in enumerate((0.5, 0.6, 0.7, 0.8, 0.9, 1.0)):
        j = K.jit("chitter-screech", i, 5)
        c.key(t, P_(rear, {"head": [-50 + j[0], j[1] * 2, j[2] * 2], "jaw": [62 + j[1], 0, 0]}))
    c.key(1.4, base, E_IO)

    # Death (2.0 s, held): it convulses, falls onto its back, the limbs jerk, then go still -- the jaw left hanging open.
    c = clip("death", 2.0, hold=True)
    spasm = P_(K.spread("right"), K.spread("left"), {"body": [-20, 0, 10], "head": [-40, 20, 20], "jaw": [40, 0, 0],
                                                     "right_arm": [-40, 0, 50], "left_arm": [-20, 0, -60], "@root": [0, 0.4, 1.0]})
    back = P_(K.relax("right"), K.relax("left"), {"root": [-88, 0, 0], "head": [10, -30, 0], "jaw": [36, 0, 0],
                                                  "right_arm": [-30, 0, 40], "left_arm": [-150, 0, -20], "right_forearm": [-40, 0, 0],
                                                  "left_forearm": [-30, 0, 0], "right_leg": [-30, 0, 6], "left_leg": [-4, 0, -8],
                                                  "right_shin": [50, 0, 0], "left_shin": [10, 0, 0], "shirt_r": [-20, 0, -10],
                                                  "shirt_l": [-20, 0, 10]})
    back = T.ground(rig_, back, 0.0)
    c.key(0, base)
    c.key(0.15, spasm, E_SNAP)
    c.key(0.3, P_(spasm, {"head": [-30, -20, -24], "body": [-10, 0, -12]}), E_SNAP)
    c.key(0.8, back, E_IN)
    for i, t in enumerate((1.0, 1.15, 1.3, 1.45)):
        j = K.jit("chitter-death", i, 8 - i * 1.6)
        c.key(t, P_(back, {"right_forearm": [-40 + j[0] * 2, 0, 0], "left_shin": [10 + abs(j[1]) * 3, 0, 0], "head": [10 + j[2], -30 + j[0], 0],
                           "right_fingers": [40 + j[1] * 4, 0, 0]}), E_SNAP)
    c.key(1.7, back, E_IO)
    c.key(2.0, back)
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================

def build():
    m = rig()
    t, g = m.build(gutter=1, seed=64000)
    f, clips = anims(m.rig)
    T.check(NAME, m, f)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t, g)


def preview(out_dir=T.SCRATCH, poses=(("idle", 0.0), ("crawl", 0.0), ("pounce", 0.45), ("bite", 0.22), ("screech", 0.6), ("death", 2.0)),
            scale=5):
    from chuck_art import composite
    m, t, g, f, clips = build()
    paths = K.render(m, t, clips, out_dir, NAME, poses, scale=scale)
    paths += K.render(m, composite(t, g, 0.35), clips, out_dir, NAME + "_lit", (("idle", 0.0),), scale=scale, views=("front",))
    return paths


if __name__ == "__main__":
    generate()
