"""The witches' shared kit (v0.21): a woman's proportions and skeleton, slim hands, a face drawn from legend rows, gowns
(a bell of stacked tiers that hang from the hips and sway), wavy hair, velvet, lace, and the clip helpers every witch rig
uses (witch_art, rowena_art, grand_witch_art). Not a module of its own: nothing here writes a file.

Skeleton (Bedrock px; +X is her LEFT; the model faces north), legacy_art's man plus a left index and the gown:
  root -- body -- head
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_thumb
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_index, left_thumb
       -- right_leg -- right_shin,  left_leg -- left_shin
       -- skirt -- skirt_lower                (the gown hangs from the hips, not the torso: she can bow over it)
"""

import math

import horsemen_art as H
from chuck_art import Model, h01, none_on, solid
from legacy_art import Build, SIDES, fix, head_cube, skeleton
from michael_art import Clip, P_, tone
from pixelkit import Ramp, fbm, hexc, mix, shade

CLEAR = (0, 0, 0, 0)

# A woman: slimmer shoulders and arms, a narrow waist; the head the vanilla 8 px so she stands with the player.
WB = Build(hip=12.0, neck=24.0, tw=3.3, depth=3.8, aw=2.9, ax=4.85, sx=4.45, sy=23.0, elbow=18.2, wrist=14.0, palm=12.2,
           lx=1.6, lw=3.0, knee=6.2, shoe_h=1.0)
WAIST = 17.0          # where the bodice nips in
TWW = 2.8             # the waist's half-width

EYE_WHITE = hexc("#e9e2da")


def wskeleton(m, b=WB, skirt=True):
    m.rig.bone("root", (0, 0, 0))
    skeleton(m, b)
    m.bone("left_index", (b.ax, b.palm, -1.0), "left_hand")
    if skirt:
        m.bone("skirt", (0, b.hip, 0), "root")
        m.bone("skirt_lower", (0, b.knee + 0.5, 0), "skirt")


# =====================================================================================================
# Materials
# =====================================================================================================

def velvet(ramp, seed, base=2.8, sheen=0.9, folds=0.5):
    """Velvet: deep and soft, light pooling along vertical folds, a sheen on the up-facing pile."""
    def m(f, x, y, w, h):
        if f == "up":
            return tone(ramp, base + sheen)
        if f == "down":
            return tone(ramp, base - 1.3)
        n = fbm(x * 2 + len(f) * 5, y, seed, 64, 64, 2, 8.0)
        fold = math.sin(x * 1.25 + seed * 0.37 + 0.15 * y)
        v = base + folds * fold + (n - 0.5) * 0.5 - 0.35 * y / max(1, h)
        if fold > 0.82:
            v += sheen * 0.7
        if f in ("east", "west"):
            v -= 0.25
        elif f == "south":
            v -= 0.15
        return tone(ramp, v)
    return m


def lace(ramp, seed, open_=0.45, cell=6):
    """Black lace: a floral repeat (a ring with a dot, scallops, a mesh between) with holes where the mesh is open.
    Returns None for the holes (cut out, the layer under it shows through)."""
    def m(f, x, y, w, h):
        cx, cy = x % cell, y % cell
        row = (y // cell) % 2
        if row:
            cx = (x + cell // 2) % cell
        dx, dy = cx - (cell - 1) / 2, cy - (cell - 1) / 2
        r = math.hypot(dx, dy)
        if 1.6 < r < 2.6 or r < 0.7:
            return tone(ramp, 2.6 + 1.2 * h01("lc", seed, x, y))
        if (x + y) % 2 == 0 and h01("lm", seed, x, y) > open_:
            return tone(ramp, 1.6)
        return None
    return m


def wavy_hair(ramp, seed, base=2.7, wave=1.0, shine=0.7):
    """Long wavy hair: strands running down (wavering side to side), a light band where each wave turns to the light."""
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 10.0)
        if f in ("up", "down"):
            strand = math.sin(x * 2.1 + n * 3)
            return tone(ramp, base + 0.35 * strand + (0.5 if f == "up" else -1.0))
        ph = y * 0.9 + math.sin(x * 0.7 + seed) * 1.2
        strand = math.sin(x * 1.9 + wave * 1.8 * math.sin(y * 0.55 + seed) + n * 3)
        v = base + 0.45 * strand + (n - 0.5) * 0.5
        if math.sin(ph) > 0.75:
            v += shine
        if f in ("east", "west"):
            v -= 0.2
        return tone(ramp, v)
    return m


def satin(ramp, seed, base=3.0):
    """Satin ribbon / lining: smooth, a diagonal sheen."""
    def m(f, x, y, w, h):
        return tone(ramp, base + 0.8 * math.sin(x * 0.7 - y * 0.5 + seed) - (0.5 if f in ("east", "west", "south") else 0))
    return m


def metal(ramp, seed=0):
    def m(f, x, y, w, h):
        if f == "up":
            return ramp[5]
        if f == "down":
            return ramp[1]
        return ramp[4] if (x + y + seed) % 4 == 0 else ramp[3] if y < h / 2 else ramp[2]
    return m


def gem(ramp):
    def m(f, x, y, w, h):
        if f == "up" or (f == "north" and x == 0 and y == 0):
            return ramp[5]
        return ramp[3] if f == "north" else ramp[2]
    return m


def glow_keep(f, x, y, w, h, c):
    return c


def glow_k(k):
    return lambda f, x, y, w, h, c: shade(c, k)


# =====================================================================================================
# Building blocks
# =====================================================================================================

def bell(m, bone, y_top, y_bot, top, bot, mat, glow=None, n=3, inflate=0.0, z0=0.0, density=2, tag="skirt", cap=True,
         base=True):
    """A bell of `n` stacked boxes from y_top down to y_bot, half-sizes (hw, hd) easing from `top` to `bot`. The material
    is sampled as one face over the whole height (folds and hems run on), y measured from the top."""
    H_ = y_top - y_bot
    hw_avg = max(top[0], bot[0])
    W_ = int(round(2 * hw_avg * density))
    Htx = int(round(H_ * density))
    out = []
    for i in range(n):
        a, c = i / n, (i + 1) / n
        ya, yc = y_top - H_ * a, y_top - H_ * c
        k = (a + c) / 2
        k = k * k * (3 - 2 * k) if n > 2 else k
        hw = top[0] + (bot[0] - top[0]) * k
        hd = top[1] + (bot[1] - top[1]) * k
        off = int(round((ya - y_top) * -density))
        skip = set()
        if i > 0 or not cap:
            skip.add("up")
        if i < n - 1 or not base:
            skip.add("down")

        def tier(f, x, y, w, h, off=off, skip=skip):
            if f in skip:
                return None
            # Sample on the bell's full face: x re-centred to the widest tier, y running on from the top.
            xx = x + (W_ - w) // 2 if f in ("north", "south") else x
            return mat(f, xx, y + off if f not in ("up", "down") else y, w if f in ("up", "down") else W_, h if f in ("up", "down") else Htx)
        out.append(m.cube(bone, (-hw, yc - (0.15 if i < n - 1 else 0), z0 - hd), (2 * hw, ya - yc + (0.15 if i < n - 1 else 0), 2 * hd),
                          tier, glow, inflate=inflate, density=density, tag=tag))
    return out


def slim_hands(m, b, skin, glow=None, nails=None, p=""):
    """A slender hand: the palm, the index, the other three together, the thumb (the index its own bone on both sides)."""
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{p}{side}_hand", (cx - 1.05, b.palm, -1.25), (2.1, b.wrist - b.palm, 2.5), skin, glow, density=2, tag="palm")
        def fing(f, x, y, w, h):
            c = skin(f, x, y, w, h)
            if nails is not None and ((f == "down" and x % 2 == 0 and y == 0) or (f == "north" and y == h - 1 and x % 2 == 0)):
                return mix(c, nails, 0.7)
            return shade(c, 0.93 if x % 2 else 1.0)
        m.cube(f"{p}{side}_index", (cx - 1.0, b.palm - 1.5, -1.25), (2.0, 1.5, 0.65), fing, glow, density=2, tag="index")
        m.cube(f"{p}{side}_fingers", (cx - 1.0, b.palm - 1.35, -0.55), (2.0, 1.35, 1.75), fing, glow, density=2, tag="fingers")
        m.cube(f"{p}{side}_thumb", (cx - 0.9 * s - 0.4, b.palm + 0.1, -1.8), (0.8, 1.4, 0.75), skin, glow, density=2, tag="thumb")


def face_head(m, b, rows, legend, sides, glow=None, bone="head"):
    return head_cube(m, bone, b.neck, fix(rows), legend, sides, glow)


def nose_ears(m, b, skin, glow=None, bone="head", nose_h=1.4, ears=True):
    y0 = b.neck
    m.cube(bone, (-0.5, y0 + 2.7, -4.45), (1.0, nose_h, 0.5), lambda f, x, y, w, h: skin[4] if f == "north" and y < 2 else
           (skin[1] if f == "down" else skin[3]), glow, density=2, tag="nose")
    if ears:
        for s in (-1, 1):
            m.cube(bone, (4 * s - 0.2 + (0 if s > 0 else -0.25), y0 + 2.6, -0.5), (0.45, 2.0, 1.4),
                   lambda f, x, y, w, h: skin[2] if f not in ("east", "west") else skin[3], glow, density=2, tag="ear")


def womans_legs(m, b, stocking, shoe, glow=None, thigh=None, shoe_len=3.8, shoe_h=1.0, heel=0.0):
    """Legs under a gown (mostly hidden; the shins and shoes show when she steps or kneels)."""
    for side, s in SIDES:
        x0 = b.lx * s - b.lw / 2
        m.cube(f"{side}_leg", (x0, b.knee, -b.lw / 2), (b.lw, b.hip - b.knee, b.lw), thigh or stocking, glow, density=2, tag="thigh")
        m.cube(f"{side}_shin", (b.lx * s - 1.2, shoe_h, -1.2), (2.4, b.knee - shoe_h, 2.4), stocking, glow, density=2, tag="shin")
        m.cube(f"{side}_shin", (b.lx * s - 1.25, 0, -1.25 - (shoe_len - 2.5)), (2.5, shoe_h + 0.2, shoe_len), shoe, glow, density=2,
               tag="shoe")
        if heel:
            m.cube(f"{side}_shin", (b.lx * s - 0.45, 0, 0.3), (0.9, heel, 0.8), shoe, glow, density=4, tag="heel")


def boot_shoe(ramp):
    """A buttoned Victorian boot / plain shoe: a toe shine, the welt."""
    def m(f, x, y, w, h):
        if f == "down" or (f != "up" and y >= h - 1):
            return ramp[0]
        if f == "up":
            return ramp[4] if y < 2 and abs(x - (w - 1) / 2) < 1.2 else ramp[2]
        if f == "north":
            return ramp[4] if y == 0 else ramp[2]
        return ramp[3] if y == 0 else ramp[2]
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

def gown_follow(pose, k=1.0, extra=None):
    """The gown sways with her stride (pushed forward by the leading knee, a side-to-side roll) unless a key sets it.
    `extra(pose, out)` adds a character's own followers (hair, panels)."""
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    if "skirt" not in pose:
        out["skirt"] = [min(rl, ll) * 0.22 * k, 0, (ll - rl) * 0.05 * k]
    if "skirt_lower" not in pose:
        out["skirt_lower"] = [min(rl, ll) * 0.18 * k, 0, (ll - rl) * 0.04 * k]
    if extra:
        extra(pose, out)
    return out


class WClip(Clip):
    """A Clip whose every key runs through a follower (the gown, the hair)."""

    def __init__(self, f, name, length, loop=False, hold=False, forbid=(), follow=gown_follow, base=None):
        super().__init__(f, name, length, loop, hold, forbid)
        self.follow = follow
        self.base = base or {}

    def key(self, t, pose, ease=None):
        return super().key(t, self.follow(P_(self.base, pose)), ease)


def gown_walk(n, i, stride=16, arm=11, knee=22, bob=0.3, sway=2.5, lean=1.0):
    """A gliding step: short strides, a soft knee, the arms swinging close."""
    ph = 2 * math.pi * i / n
    sn = math.sin(ph)
    return {"right_leg": [-stride * sn, 0, 0], "left_leg": [stride * sn, 0, 0],
            "right_shin": [knee * max(0, math.sin(ph + 1.2)), 0, 0], "left_shin": [knee * max(0, -math.sin(ph + 1.2)), 0, 0],
            "right_arm": [arm * sn, 0, 4], "left_arm": [-arm * sn, 0, -4], "right_forearm": [-12, 0, 0],
            "left_forearm": [-12, 0, 0], "body": [lean, sway * sn, 0], "head": [0, -sway * sn, 0],
            "@root": [0, bob * abs(math.cos(ph)), 0]}


# Hand shapes (both sides carry an index).
FIST_R = {"right_fingers": [-85, 0, 0], "right_index": [-80, 0, 0], "right_thumb": [-20, 0, 20]}
FIST_L = {"left_fingers": [-85, 0, 0], "left_index": [-80, 0, 0], "left_thumb": [-20, 0, -20]}
OPEN_R = {"right_fingers": [0, 0, 0], "right_index": [0, 0, 0], "right_thumb": [0, 0, -16]}
OPEN_L = {"left_fingers": [0, 0, 0], "left_index": [0, 0, 0], "left_thumb": [0, 0, 16]}
CURL_R = {"right_fingers": [-40, 0, 0], "right_index": [-25, 0, 0], "right_thumb": [-10, 0, 10]}
CURL_L = {"left_fingers": [-40, 0, 0], "left_index": [-25, 0, 0], "left_thumb": [-10, 0, -10]}
CLAW_R = {"right_fingers": [-55, 0, 0], "right_index": [-50, 0, 0], "right_thumb": [-10, 0, -24]}
CLAW_L = {"left_fingers": [-55, 0, 0], "left_index": [-50, 0, 0], "left_thumb": [-10, 0, 24]}
POINT_R = {"right_fingers": [-85, 0, 0], "right_index": [0, 0, 0], "right_thumb": [-30, 0, 20]}
POINT_L = {"left_fingers": [-85, 0, 0], "left_index": [0, 0, 0], "left_thumb": [-30, 0, -20]}
GRACE_R = {"right_fingers": [-30, 0, 0], "right_index": [-8, 0, 0], "right_thumb": [0, 0, -10]}
GRACE_L = {"left_fingers": [-30, 0, 0], "left_index": [-8, 0, 0], "left_thumb": [0, 0, 10]}
REST = P_({"right_fingers": [-20, 0, 0], "right_index": [-14, 0, 0], "left_fingers": [-20, 0, 0], "left_index": [-14, 0, 0]},
          {"right_arm": [0, 0, 4], "left_arm": [0, 0, -4], "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0]})


def kneel_pose(head=14, body=10, skirt=(1.18, 0.5, 1.18)):
    """Down on both knees, the gown pooling round her (squashed and spread from the hips)."""
    return {"@root": [0, -5.6, 0], "right_leg": [-6, 0, 4], "left_leg": [-6, 0, -4], "right_shin": [94, 0, 0], "left_shin": [94, 0, 0],
            "body": [body, 0, 0], "head": [head, 0, 0], "skirt": [0, 0, 0], "skirt_lower": [0, 0, 0], "%skirt": list(skirt)}


def preview(m, tex, path, pose=None, hidden=(), scale=5, views=("front", "three_quarter", "side", "back")):
    import preview3d
    preview3d.render(m.rig, tex, path, pose=pose or {}, hidden=hidden, scale=scale, views=views)
