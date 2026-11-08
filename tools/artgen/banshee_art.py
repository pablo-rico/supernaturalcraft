"""The banshee (v0.42, season 11): rig, texture + glowmask and clips (S11Assets.BANSHEE: geo/entity/banshee.geo.json,
animations/entity/banshee.animation.json, textures/entity/banshee.png and banshee_glowmask.png).

A gaunt grey woman, tall and thin (35 px to the crown), FLOATING: she has no legs -- below the hips her torn white grave-clothes
hang in ragged strips that trail off into tatters three pixels above the ground. Very long white hair, parted, down her back to
the waist and over her shoulders in strands. Hollow black eyes with a pale point of light deep in each (glowmask), sunken
cheeks, thin grey lips, a jaw that unhinges wide to scream (`jaw`: its gap shows a black throat with a faint cold light and two
rows of teeth). Long bare grey forearms, the shroud's sleeves torn off at the elbow (a rag hangs from each), long bony fingers
with dark nails.

Skeleton (Bedrock px; +X is her LEFT; she faces north):
  root -- body -- head -- jaw, hair -- hair_back -- hair_tip, hair_r, hair_l, strand_1..4
               -- right_arm -- right_rag, right_forearm -- right_hand -- right_index, right_middle, right_fingers, right_thumb
               -- left_arm  -- (the same)
               -- skirt -- tatter_0..9 -- tatter_0_tip..tatter_9_tip
Every clip may key any bone; `jaw` (scream, claw, death) and `root` scale (vanish, appear, death) do the tricks.
"""

import math

import s1214_kit as K
import thule_art as T
from animkit import AnimFile
from apoc_vessel import E_BACK, E_IN, E_IO, E_OUT, E_SNAP, tone
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_
from pixelkit import Ramp, fbm, hexc, mix, shade

NAME = "banshee"
A = "animation.banshee."

HIP, NECK, HEAD = 16.4, 27.2, 8.0
CROWN = NECK + HEAD
TW, TD = 3.1, 1.6
AW = 2.3
SY, ELBOW, WRIST, PALM = 26.6, 21.0, 15.6, 14.0
AX = TW + AW / 2 + 0.05
SX = AX - 0.5
JAW_H = 2.5                                   # the jaw's height (the skull above it is HEAD - JAW_H)

SKIN = Ramp("#2c2f34", "#464a50", "#62666c", "#7b8086", "#94989d", "#b0b3b6")
LINEN = Ramp("#5c5951", "#7c786e", "#9a968b", "#b4b0a4", "#cbc7bb", "#e2dfd5")
HAIR = Ramp("#6d6b66", "#8f8c86", "#adaaa3", "#c6c3bb", "#dbd9d1", "#f1f0ea")
EARTH = hexc("#3b342b")
SOCKET = hexc("#040405")
NAIL = hexc("#1e1a1c")
THROAT = hexc("#0a0607")
TEETH = Ramp("#5e5848", "#7d7662", "#a19a82", "#bdb69c", "#d0caaf", "#e3dec6")
GLOW_HOT, GLOW, GLOW_DIM = hexc("#f2f8ff"), hexc("#a9c6e6"), hexc("#4a6584")

# The face: rows 0-10 on the skull, 11-15 on the jaw (2 texels a px). Deep hollow sockets that run down into the cheeks.
FACE = [
    "HHHHHHHHHHHHHHHH",
    "HHHHHHHsHHHHHHHH",
    "HHHHHssssssHHHHH",
    "HHHHssssssssHHHH",
    "HHHssssssssssHHH",
    "HHkkOOOssOOOkkHH",
    "HHkOOGOssOGOOkHH",
    "HHkOOOOssOOOOkHH",
    "HHckOOcnncOOkcHH",
    "HHcckOsnnsOkccHH",
    "HHccsssNNsssccHH",
    "HHjcsllllllscjHH",
    "HHjcsssssssscjHH",
    "HHHjcsssssscjHHH",
    "HHHHjjssssjjHHHH",
    "HHHHHjjjjjjHHHHH",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def skin_mat(seed, base=3.0):
    def m(f, x, y, w, h):
        n = fbm(x * 1.6 + len(f) * 5, y * 1.6, seed, 64, 64, 3, 6.0)
        c = tone(SKIN, base + (n - 0.5) * 1.1 - 0.3 * y / max(1, h))
        if h01("vein", seed, f, x // 2, y) < 0.05:
            c = mix(c, hexc("#3c4a5e"), 0.4)                 # dark veins under the grey
        return c
    return m


def linen(seed, base=3.2, holes=0.08, rag_rows=0):
    """Grave linen: dirty white, earth-stained toward the bottom, torn through; the last `rag_rows` rows ragged."""
    def m(f, x, y, w, h):
        if f == "down":
            return None
        n = fbm(x * 1.2 + len(f) * 9, y * 1.2, seed, 64, 64, 3, 6.0)
        v = base + 0.6 * math.sin(x * 0.9 + y * 0.2 + seed) * 0.6 - 1.0 * y / max(1, h) + (n - 0.5) * 1.0
        c = tone(LINEN, v + (0.6 if f == "up" else 0))
        if n > 0.66 + 0.25 * (1 - y / max(1, h)):
            c = mix(c, EARTH, 0.45)
        if f in ("north", "south", "east", "west"):
            if fbm(x * 2.2 + len(f) * 3, y * 2.2, seed + 5, 64, 64, 2, 5.0) > 1 - holes:
                return None                                    # a hole right through
            if rag_rows and y >= h - rag_rows and h01("rag", seed, f, x) < 0.25 + 0.6 * (y - (h - rag_rows)) / rag_rows:
                return None
            if (x + y * 2) % 7 == 0 and h01("wv", seed, f, x, y) < 0.3:
                c = shade(c, 0.9)                              # the weave
        return c
    return m


def strand(seed, base=3.4):
    def m(f, x, y, w, h):
        v = base + 0.9 * math.sin(x * 1.9 + seed) - 0.6 * y / max(1, h) + (0.8 if (x * 5 + y) % 9 == 0 else 0)
        if f in ("east", "west"):
            v -= 0.5
        return tone(HAIR, v)
    return m


def ragged(mat, seed, rows=4):
    """Ends of hair or cloth that run out unevenly."""
    def m(f, x, y, w, h):
        if f in ("north", "south", "east", "west") and y >= h - rows and h01("end", seed, f, x) < 0.2 + 0.7 * (y - (h - rows) + 1) / rows:
            return None
        return mat(f, x, y, w, h)
    return m


def build_head(m, seed=63001):
    sk = skin_mat(seed, 3.1)
    hr = strand(seed + 1, 3.3)
    sh = lambda k: (lambda f, x, y, w, h: shade(sk(f, x, y, w, h), k))
    lg = {"s": sk, "H": hr, "b": sh(0.62), "O": SOCKET, "G": GLOW_HOT, "k": sh(0.6), "c": sh(0.74), "n": SKIN[4], "N": SKIN[1],
          "j": sh(0.82), "l": hexc("#2c2a30")}

    def face(row0):
        def f_(f, x, y, w, h):
            yy = y + row0
            if f == "north":
                ch = FACE[yy][x] if yy < 16 and x < 16 else "s"
                v = lg.get(ch, sk)
                return v(f, x, yy, w, 16) if callable(v) else v
            if f == "up":
                return hr(f, x, y, w, h)
            if f == "south":
                return hr(f, x, yy, w, 16)
            fx = x if f == "west" else w - 1 - x                # 0 at the front
            if fx > 3 or yy < 6:
                return hr(f, x, yy, w, 16)                       # the hair hangs over the sides of her face
            return sk(f, x, yy, w, 16)
        return f_

    def glow(f, x, y, w, h, c):
        if f != "north":
            return None
        if FACE[y][x] == "G":
            return GLOW_HOT
        near = any(FACE[yy][xx] == "G" for yy in (y - 1, y, y + 1) for xx in (x - 1, x, x + 1) if 0 <= yy < 16 and 0 <= xx < 16)
        return GLOW_DIM if near and FACE[y][x] == "O" else None
    hs = HEAD / 2
    skull_h = HEAD - JAW_H

    def skull(f, x, y, w, h):
        if f == "down":
            # the roof of the mouth: the upper teeth at the front, then the dark throat
            if y == 0 and 3 <= x <= 12:
                return tone(TEETH, 3.6 - (x % 2))
            return THROAT
        return face(0)(f, x, y, w, h)

    def skull_glow(f, x, y, w, h, c):
        if f == "down" and y > 2 and 4 <= x <= 11:
            return GLOW_DIM                                      # the cold light down her throat
        return glow(f, x, y, w, h, c)
    m.cube("head", (-hs, NECK + JAW_H, -hs), (HEAD, skull_h, HEAD), skull, glow=skull_glow, density=2, tag="skull")
    m.cube("head", (-0.5, NECK + JAW_H + 0.4, -hs - 0.4), (1, 1.5, 0.4), lambda f, x, y, w, h: SKIN[3] if f == "north" else SKIN[2],
           density=2, tag="nose")
    jaw = m.bone("jaw", (0, NECK + JAW_H, 1.2), "head")

    def jawm(f, x, y, w, h):
        if f == "up":
            if y == 0 and 2 <= x <= 11:
                return tone(TEETH, 3.4 - (x % 2))
            return THROAT
        if f == "down":
            return sk(f, x, y, w, h)
        if f == "north":
            return face(11)(f, x + 1, y, w, h)
        return face(11)(f, x, y, w, h)
    m.cube(jaw, (-hs + 0.5, NECK, -hs + 0.1), (HEAD - 1, JAW_H, HEAD - 1.0), jawm,
           glow=lambda f, x, y, w, h, c: GLOW_DIM if f == "up" and y > 2 and 3 <= x <= 10 else None, density=2, tag="jaw")
    # the neck, long and corded
    m.cube("body", (-1.1, NECK - 1.4, -1.1), (2.2, 2.4, 2.2), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.86 if x % 2 else 1.0),
           density=2, tag="neck")
    # the hair: a shell over the crown, the long fall down her back, locks over her shoulders and loose strands
    m.cube("head", (-hs, CROWN - 2.0, -hs), (HEAD, 2.0, HEAD), none_on(("down", "north"), hr), inflate=0.3, density=2, tag="hair_cap")
    m.cube("head", (-hs, NECK + 1.0, hs - 0.2), (HEAD, HEAD - 2.0, 0.6), hr, inflate=0.3, density=2, tag="hair_nape")
    K.long_hair(m, _B, ragged(strand(seed + 2), seed + 2, 5), seed + 3, length=17.0, width=8.6, lock_len=13.0)
    for i, (x, z, ln) in enumerate(((-3.6, 0.6, 12.0), (3.5, 1.4, 14.0), (-1.6, 3.9, 19.0), (2.2, 3.9, 16.0))):
        b = m.bone(f"strand_{i + 1}", (x, NECK + 5.0, z), "head")
        m.cube(b, (x - 0.4, NECK + 5.0 - ln, z - 0.4), (0.8, ln, 0.8), ragged(strand(seed + 10 + i, 3.6), seed + 10 + i, 3),
               density=2, tag="strand")


class _Body:
    HIP, NECK, HEAD, TW, TD, AW, SY = HIP, NECK, HEAD, TW, TD, AW, SY
    crown = CROWN


_B = _Body()


def build_body(m, seed=63200):
    sk = skin_mat(seed + 1, 3.0)
    # The shroud: a torso, a closed band round the hips, a cowl-neck fold.
    m.cube("body", (-TW, HIP, -TD), (2 * TW, NECK - HIP, 2 * TD), linen(seed + 2, 3.3, holes=0.05), density=2, tag="shroud")
    m.cube("body", (-1.3, NECK - 3.0, -TD - 0.05), (2.6, 2.6, 0),
           lambda f, x, y, w, h: (shade(sk(f, x, y, w, h), 0.8 if y == 1 else 0.95) if y <= 4 - abs(x - 2) * 2 else None),
           faces=("north",), density=2, tag="neckline")                # the shroud's torn neckline, the breastbone under it
    m.cube("body", (-TW - 0.2, HIP - 2.6, -TD - 0.2), (2 * TW + 0.4, 3.2, 2 * TD + 0.4), linen(seed + 3, 3.0, holes=0.02),
           inflate=0.15, density=2, tag="hips")
    m.cube("body", (-TW, HIP + 3.0, -TD), (2 * TW, 0.6, 2 * TD), lambda f, x, y, w, h: None if f in ("up", "down") else tone(LINEN, 1.6 + (x % 3 == 0)),
           inflate=0.3, density=2, tag="cord")                   # a grave-cord round the waist
    # The skirt of strips: a ring of ten, each in two segments, ragged at the ends.
    sk_b = m.bone("skirt", (0, HIP - 1.0, 0), "body")
    ring = [(-2.3, -TD - 0.3, 0, 13.0), (0.0, -TD - 0.45, 0, 14.5), (2.3, -TD - 0.3, 0, 12.0),
            (-2.3, TD + 0.3, 0, 13.5), (0.0, TD + 0.45, 0, 15.0), (2.3, TD + 0.3, 0, 12.5),
            (-TW - 0.3, -0.9, 90, 11.5), (-TW - 0.3, 0.9, 90, 13.0), (TW + 0.3, -0.9, 90, 12.0), (TW + 0.3, 0.9, 90, 13.5)]
    y0 = HIP - 1.0
    for i, (x, z, rot, ln) in enumerate(ring):
        a = m.bone(f"tatter_{i}", (x, y0, z), sk_b.name)
        upper = ln * 0.45
        lower = ln - upper
        b = m.bone(f"tatter_{i}_tip", (x, y0 - upper, z), a.name)
        w = 2.6 if rot == 0 else 2.2
        size = (w, upper, 0.4) if rot == 0 else (0.4, upper, w)
        org = (x - w / 2, y0 - upper, z - 0.2) if rot == 0 else (x - 0.2, y0 - upper, z - w / 2)
        m.cube(a, org, size, linen(seed + 20 + i, 3.0, holes=0.04), density=2, tag="tatter")
        size2 = (w - 0.2, lower + 0.2, 0.4) if rot == 0 else (0.4, lower + 0.2, w - 0.2)
        org2 = (x - (w - 0.2) / 2, y0 - ln, z - 0.2) if rot == 0 else (x - 0.2, y0 - ln, z - (w - 0.2) / 2)
        m.cube(b, org2, size2, linen(seed + 40 + i, 2.4, holes=0.1, rag_rows=6), density=2, tag="tatter_tip")
    # Arms: the shroud's sleeve torn off at the elbow, a rag hanging; long bare forearms; bony hands with long fingers.
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW + 0.6, -AW / 2), (AW, SY + 0.6 - ELBOW - 0.6, AW), linen(seed + 60 + s, 3.2, 0.06, rag_rows=2),
               inflate=0.3, density=2, tag="sleeve")
        rag = m.bone(f"{side}_rag", (cx + 0.6 * s, ELBOW + 1.2, 0.6), f"{side}_arm")
        m.cube(rag, (cx + 0.6 * s - 0.9, ELBOW - 4.6, 0.6), (1.8, 5.8, 0.3), linen(seed + 70 + s, 2.8, 0.05, rag_rows=4), density=2,
               tag="rag", rotation=(0, 0, -6 * s), pivot=(cx, ELBOW + 1.2, 0.6))
        fw = AW - 0.7
        m.cube(f"{side}_forearm", (cx - fw / 2, WRIST, -fw / 2), (fw, ELBOW + 0.8 - WRIST, fw),
               lambda f, x, y, w, h, sk=sk: shade(sk(f, x, y, w, h), 1.06 if y < 2 else 1.0), density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - fw / 2 - 0.15, WRIST - 0.1, -fw / 2 - 0.15), (fw + 0.3, 0.8, fw + 0.3), sk, density=2, tag="wrist")
        # the hand: a narrow palm facing her side, four long fingers in a row front to back, the thumb in front
        m.cube(f"{side}_hand", (cx - 0.5, PALM, -1.2), (1.0, WRIST - PALM + 0.1, 2.4), sk, density=2, tag="palm")

        def finger(f, x, y, w, h, sk=sk):
            if y >= h - 1:
                return NAIL                                     # the long dark nail
            c = sk(f, x, y, w, h)
            return shade(c, 0.82) if y in (h // 3, 2 * h // 3) else c   # the knuckles
        for nm, z, ln in (("index", -0.95, 3.6), ("middle", -0.3, 4.0), ("fingers", 0.55, 3.4)):
            wz = 0.55 if nm != "fingers" else 1.1
            m.cube(f"{side}_{nm}", (cx - 0.35, PALM - ln, z - wz / 2), (0.7, ln, wz), finger, density=4, tag="finger")
        m.cube(f"{side}_thumb", (cx - 0.3 - 0.35 * s, PALM - 1.6, -1.7), (0.6, 2.2, 0.6), finger, density=4, tag="thumb")


def rig():
    m = Model(NAME, 256, 256)
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, HIP, 0), "root")
    m.bone("head", (0, NECK, 0), "body")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (SX * s, SY, 0), "body")
        m.bone(f"{side}_forearm", (AX * s, ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (AX * s, WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_index", (AX * s, PALM, -0.95), f"{side}_hand")
        m.bone(f"{side}_middle", (AX * s, PALM, -0.3), f"{side}_hand")
        m.bone(f"{side}_fingers", (AX * s, PALM, 0.55), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 0.35 * s, PALM + 0.4, -1.4), f"{side}_hand")
    build_head(m)
    build_body(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

def claw(side, k=1.0):
    """The long fingers hooked toward the palm (her palms face her sides: the curl is about Z)."""
    s = -1 if side == "right" else 1
    return {f"{side}_index": [0, 0, 50 * k * s], f"{side}_middle": [0, 0, 56 * k * s], f"{side}_fingers": [0, 0, 62 * k * s],
            f"{side}_thumb": [0, 0, -30 * k * s]}


def splay(side):
    s = -1 if side == "right" else 1
    return {f"{side}_index": [-14, 0, -14 * s], f"{side}_middle": [0, 0, -18 * s], f"{side}_fingers": [14, 0, -22 * s],
            f"{side}_thumb": [-30, 0, -24 * s]}


HOVER = 0.0          # the float height is built into the rig (the tatters end ~2.5 px up)
REST = P_(claw("right", 0.35), claw("left", 0.35), {"right_arm": [-6, 0, 6], "left_arm": [-6, 0, -6], "right_forearm": [-14, 0, 0],
                                                   "left_forearm": [-14, 0, 0], "head": [8, 0, 4], "body": [4, 0, 0]})
N_TAT = 10


def tatters(t, amp=1.0, stream=0.0, period=3.0):
    """The strips stir (each its own phase); `stream` swings them all back (moving forward: +X swings a hanging strip back)."""
    p = {}
    for i in range(N_TAT):
        ph = 2 * math.pi * (t / period) + i * 1.7
        p[f"tatter_{i}"] = [stream + 5 * amp * math.sin(ph), 0, 3 * amp * math.sin(ph * 0.5 + i)]
        p[f"tatter_{i}_tip"] = [stream * 0.6 + 8 * amp * math.sin(ph - 0.9), 0, 4 * amp * math.sin(ph * 0.5 + i - 0.8)]
    return p


def hair(t, amp=1.0, stream=0.0, period=3.0):
    ph = 2 * math.pi * t / period
    p = {"hair_back": [stream + 3 * amp * math.sin(ph + 0.3), 0, 2 * amp * math.sin(ph * 0.5)],
         "hair_tip": [stream * 0.8 + 5 * amp * math.sin(ph + 1.0), 0, 0],
         "hair_r": [stream * 0.5 + 3 * amp * math.sin(ph + 0.6), 0, 2 * amp * math.sin(ph + 2)],
         "hair_l": [stream * 0.5 + 3 * amp * math.sin(ph + 1.6), 0, -2 * amp * math.sin(ph + 1)]}
    for i in range(4):
        p[f"strand_{i + 1}"] = [stream + 6 * amp * math.sin(ph + i * 1.3), 0, 4 * amp * math.sin(ph * 0.5 + i)]
    for side, s in (("right", -1), ("left", 1)):
        p[f"{side}_rag"] = [stream * 0.6 + 8 * amp * math.sin(ph + (2 if s > 0 else 0)), 0, 3 * amp * math.sin(ph) * s]
    return p


def drift(t, amp=1.0, stream=0.0, period=3.0):
    return P_(tatters(t, amp, stream, period), hair(t, amp, stream, period))


def loop_keys(c, length, pose_at, n=8):
    """A seamless loop sampled at n+1 keys (the last equals the first)."""
    for i in range(n + 1):
        t = length * i / n
        c.key(t, pose_at(t if i < n else 0.0), E_IO if i else None)


def hug(rig_, pose):
    """Her arms wrapped round herself: each hand on the opposite shoulder."""
    for side, s in (("right", -1), ("left", 1)):
        target = T.world(rig_, pose, "body", (-s * 2.6, SY - 1.0, -TD - 1.6))
        pose = T.reach(rig_, pose, side, target, (AX * s, PALM + 0.6, 0.0),
                       starts=((-60, -40 * s, 0), (-90, -60 * s, 0), (-40, 0, -30 * s), (-120, -30 * s, 0)))
    return pose


def anims(rig_):
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    # --- loops ---------------------------------------------------------------------------------------------------
    # Idle (3 s): she hangs in the air, rising and sinking a pixel, her rags and hair stirring, the fingers flexing.
    c = clip("idle", 3.0, loop=True)

    def idle_at(t):
        u = t / 3.0
        b = math.sin(u * 2 * math.pi)
        return P_(REST, drift(t, 1.0, 0.0, 3.0), {"@root": [0, HOVER + 0.8 * b, 0], "head": [8 + 3 * math.sin(u * 2 * math.pi + 1), 6 * math.sin(u * 2 * math.pi), 4],
                                                  "right_arm": [-6 + 3 * b, 0, 6], "left_arm": [-6 - 3 * b, 0, -6],
                                                  "right_index": [0, 0, -0.35 * 50 - 12 * max(0, b)], "left_index": [0, 0, 0.35 * 50 + 12 * max(0, -b)]})
    loop_keys(c, 3.0, idle_at, 6)

    # Float (2 s): gliding forward -- leaning in, arms trailing, the strips and the hair streaming back.
    c = clip("float", 2.0, loop=True)

    def float_at(t):
        u = t / 2.0
        b = math.sin(u * 2 * math.pi)
        return P_(REST, drift(t, 1.2, 16.0, 1.0), {"@root": [0, HOVER + 1.0 + 0.6 * b, 0], "body": [16, 0, 2 * b], "head": [-12, 4 * b, 0],
                                                   "right_arm": [30 + 4 * b, 0, 12], "left_arm": [30 - 4 * b, 0, -12],
                                                   "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "skirt": [12, 0, 0]})
    loop_keys(c, 2.0, float_at, 8)

    # --- once ----------------------------------------------------------------------------------------------------
    # Scream (1.6 s; the wail peaks at 0.45 s and holds to 1.2 s): the head thrust forward, the jaw unhinged, arms flung back.
    c = clip("scream", 1.6)
    gather = P_(REST, drift(0.4, 1.0), {"body": [-8, 0, 0], "head": [-16, 0, 0], "jaw": [10, 0, 0], "right_arm": [-20, 0, 10],
                                        "left_arm": [-20, 0, -10], "@root": [0, 0.6, 0.6]})
    wail = P_(splay("right"), splay("left"), drift(0.1, 1.6, 16.0, 0.6),
              {"body": [16, 0, 0], "head": [-12, 0, 0], "@head": [0, -0.4, -2.2], "jaw": [52, 0, 0], "@jaw": [0, -1.6, -0.4],
               "right_arm": [50, 0, 34], "left_arm": [50, 0, -34], "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0],
               "skirt": [14, 0, 0], "@root": [0, 0.4, 0.8]})
    c.key(0, REST)
    c.key(0.3, gather, E_IO)
    c.key(0.45, wail, E_SNAP)
    for i, t in enumerate((0.6, 0.75, 0.9, 1.05, 1.2)):
        j = K.jit("banshee-scream", i, 3)
        c.key(t, P_(wail, drift(t, 1.6, 16.0, 0.6), {"head": [-12 + j[0], j[1], j[2]], "jaw": [52 + j[1], 0, j[2]]}))
    c.key(1.6, REST, E_IO)

    # Claw (1.0 s; the rake lands at 0.45 s): both hands up and back, then raked down and across in front of her.
    c = clip("claw", 1.0)
    rear = P_(splay("right"), splay("left"), drift(0.2, 1.2), {"right_arm": [-150, 0, -30], "left_arm": [-150, 0, 30],
                                                               "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0],
                                                               "body": [-12, 0, 0], "head": [-14, 0, 0], "jaw": [16, 0, 0],
                                                               "@root": [0, 1.0, 1.0]})
    rake = P_(claw("right", 1.0), claw("left", 1.0), drift(0.5, 1.4, 16.0), {"right_arm": [-78, 0, -22], "left_arm": [-78, 0, 22],
                                                                             "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0],
                                                                             "body": [26, 0, 0], "head": [-10, 0, 0], "jaw": [24, 0, 0],
                                                                             "@root": [0, -0.6, -2.4], "skirt": [10, 0, 0]})
    c.key(0, REST)
    c.key(0.3, rear, E_IO)
    c.key(0.45, rake, E_SNAP)
    c.key(0.6, P_(rake, {"right_arm": [-56, 0, -18], "left_arm": [-56, 0, 18]}), E_OUT)
    c.key(1.0, REST, E_IO)

    # Vanish (1.0 s): she folds into herself -- arms wrapped round, head bowed, curling up -- and shrinks to nothing.
    c = clip("vanish", 1.0)
    fold = hug(rig_, P_(claw("right"), claw("left"), drift(0.2, 0.6, -12.0), {"body": [30, 0, 0], "head": [40, 0, 0], "skirt": [-24, 0, 0]}))
    c.key(0, REST)
    c.key(0.35, P_(fold, {"%root": [0.9, 1.05, 0.9]}), E_IO)
    c.key(0.75, P_(fold, {"%root": [0.25, 0.4, 0.25], "@root": [0, 8.0, 0], "root": [0, 140, 0]}), E_IN)
    c.key(1.0, P_(fold, {"%root": [0.0, 0.0, 0.0], "@root": [0, 12.0, 0], "root": [0, 220, 0]}), E_IN)

    # Appear (1.0 s): the reverse -- out of nothing, spinning open, unfolding into her hanging pose.
    c = clip("appear", 1.0)
    c.key(0, P_(fold, {"%root": [0.0, 0.0, 0.0], "@root": [0, 12.0, 0], "root": [0, -220, 0]}))
    c.key(0.3, P_(fold, {"%root": [0.3, 0.45, 0.3], "@root": [0, 8.0, 0], "root": [0, -120, 0]}), E_OUT)
    c.key(0.65, P_(splay("right"), splay("left"), drift(0.3, 1.5, 10.0),
                   {"right_arm": [-20, 0, 40], "left_arm": [-20, 0, -40], "body": [-10, 0, 0], "head": [-16, 0, 0], "jaw": [20, 0, 0],
                    "%root": [1.06, 1.04, 1.06]}), E_OUT)
    c.key(1.0, REST, E_IO)

    # Death (2.0 s, held): she arches back with a last silent wail, then crumples in on herself and comes apart into nothing.
    c = clip("death", 2.0, hold=True)
    arch = P_(splay("right"), splay("left"), drift(0.1, 2.0, -20.0, 0.5),
              {"body": [-24, 0, 0], "head": [-36, 0, 0], "jaw": [46, 0, 0], "@jaw": [0, -1.2, 0], "right_arm": [-40, 0, 60],
               "left_arm": [-40, 0, -60], "@root": [0, 2.0, 0.6], "skirt": [-16, 0, 0]})
    crumple = hug(rig_, P_(claw("right"), claw("left"), drift(0.4, 0.8, 10.0),
                           {"body": [40, 0, 10], "head": [44, 0, 14], "jaw": [30, 0, 0], "@root": [0, -4.0, 0], "skirt": [-10, 0, 0]}))
    c.key(0, REST)
    c.key(0.25, arch, E_SNAP)
    for i, t in enumerate((0.45, 0.65, 0.85)):
        j = K.jit("banshee-death", i, 4)
        c.key(t, P_(arch, {"head": [-36 + j[0], j[1], j[2]], "body": [-24 + j[1], j[2], 0]}))
    c.key(1.3, P_(crumple, {"%root": [0.85, 0.7, 0.85]}), E_IN)
    c.key(1.7, P_(crumple, {"%root": [0.3, 0.15, 0.3], "@root": [0, -2.0, 0]}), E_IN)
    c.key(2.0, P_(crumple, {"%root": [0.0, 0.0, 0.0], "@root": [0, -1.0, 0]}), E_IN)
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================

def build():
    m = rig()
    t, g = m.build(gutter=1, seed=63000)
    f, clips = anims(m.rig)
    T.check(NAME, m, f)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t, g)


def preview(out_dir=T.SCRATCH, poses=(("idle", 0.0), ("float", 0.5), ("scream", 0.6), ("claw", 0.45), ("vanish", 0.5), ("death", 1.0)),
            scale=5):
    from chuck_art import composite
    m, t, g, f, clips = build()
    paths = K.render(m, t, clips, out_dir, NAME, poses, scale=scale)
    paths += K.render(m, composite(t, g, 0.35), clips, out_dir, NAME + "_lit", (("scream", 0.6),), scale=scale, views=("front",))
    return paths


if __name__ == "__main__":
    generate()
