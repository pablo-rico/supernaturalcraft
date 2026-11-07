"""The British Men of Letters' field agents (v0.41): one rig, three texture variants (`british_agent_0..2`, the base
`british_agent` = variant 0), clips (names and timings parsed from BritishAgentAssets).

A field agent (32 px to the crown): a black tactical vest (`vest`: plates, pouches, a radio) over a white shirt and a dark
tie, black trousers, black boots; a short black carbine in the hands (`rifle`: the barrel down along the right hand, level
when he raises it; the left hand under the foregrip). Variant 0 wears a black beret with the Order's silver badge, variant 1
is bare-headed (the beret's texels clear), variant 2 a woman with her hair tied back under the beret. Same geometry for all.

Skeleton: apoc_vessel's (no coat) + vest (body), rifle (right_hand), beret (head). No clip keys them.
"""

import math

import horsemen_art as H
import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import Body, E_IN, E_IO, E_OUT, E_SNAP, arms, glove_m, hair_m, head, legs, neck, skeleton, skin_m, tone, torso
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, hexc, mix, shade

NAME = "british_agent"
A = "animation.british_agent."
C = K.Contract("british/agent/BritishAgentAssets")

B = Body(HIP=12.4, NECK=24.0, TW=3.9, TD=2.0, AW=3.4, SY=23.3, ELBOW=18.5, WRIST=14.0, PALM=12.1, LX=1.95, LW=3.6, KNEE=6.5,
         SHOE_H=1.4)

SHIRT = Ramp("#8f9298", "#b3b6bc", "#cdd0d5", "#dfe2e6", "#eceef1", "#f9fafb")
TIE = Ramp("#07080a", "#0e1014", "#16181e", "#1f2229", "#2a2e37", "#383d48")
VEST = Ramp("#060607", "#0c0c0e", "#141417", "#1d1d21", "#28282e", "#3a3a42")
TROUSER = Ramp("#08080a", "#0f0f12", "#17171b", "#202025", "#2b2b31", "#3a3a42")
BOOT = Ramp("#030303", "#08080a", "#0f0f12", "#18181c", "#25252b", "#4a4a55")
GUN = Ramp("#0a0b0c", "#141518", "#1e2023", "#292c30", "#363a3f", "#50555c")
GLOVE = Ramp("#040405", "#09090b", "#111114", "#1a1a1e", "#26262c", "#3c3c45")
BERET = Ramp("#050506", "#0b0b0d", "#121215", "#1a1a1e", "#24242a", "#34343c")
SILVER = Ramp("#4a4f58", "#6e7580", "#959ca6", "#bcc2ca", "#dde1e6", "#ffffff")

VARIANTS = [
    dict(skin=Ramp("#55403a", "#86685e", "#ac8c80", "#c5a596", "#d6bbad", "#e8d2c6"),
         hair=Ramp("#1a1410", "#2a2018", "#3a2c22", "#4a3a2c", "#5c4838", "#705a46"), beret=True, woman=False, iris="#4a5a6a"),
    dict(skin=Ramp("#4a3528", "#76553f", "#9b7357", "#b58c6c", "#c9a283", "#dcb99c"),
         hair=Ramp("#3a2a14", "#5a421e", "#7a5a2a", "#957236", "#ad8a46", "#c4a25a"), beret=False, woman=False, iris="#3a2a1a"),
    dict(skin=Ramp("#2a1a12", "#43291d", "#5c3b2a", "#744b37", "#8a5c45", "#a06e55"),
         hair=Ramp("#050403", "#0c0907", "#15100c", "#1f1812", "#2a2119", "#372c22"), beret=True, woman=True, iris="#1e140c"),
]
LIPS_M = Ramp("#4a2e28", "#62403a", "#7a524a", "#8f6359", "#a27468", "#b48678")
LIPS_W = Ramp("#4a2228", "#6a3038", "#8a4048", "#a05058", "#b06068", "#c07078")

FACE_M = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhsssssssssssshh",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "ssUUssssssssUUss",
    "skwIIwsssswIIwks",
    "ssssssssssssssss",
    "ssssssnnssssssss",
    "ssssssnnssssssss",
    "sjsssNnnNssssjss",
    "sjssssssssssssjs",
    "ssjsslllllllsjss",
    "sssjssssssssjsss",
    "ssssjjjjjjjjssss",
]
FACE_W = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssssssssshhh",
    "hsssssssssssssss",
    "ssssssssssssssss",
    "ssbbbbssssbbbbss",
    "ssUUssssssssUUss",
    "skwIIwsssswIIwks",
    "ssssssssssssssss",
    "ssssssnnssssssss",
    "ssssssnnssssssss",
    "sssssNnnNsssssss",
    "ssssssssssssssss",
    "sssssLMMMLssssss",
    "ssssssllllssssss",
    "ssssssssssssssss",
]
assert all(len(r) == 16 for r in FACE_M + FACE_W)


def build(m, v, seed=45000):
    sk = skin_m(v["skin"], seed + 1, 3.0, 0.35)
    hr = hair_m(v["hair"], seed + 2, base=2.6)
    lips = LIPS_W if v["woman"] else LIPS_M
    lg = K.legend(sk, hr, hexc("#d4cec6"), hexc(v["iris"]), lips, v["skin"], brow=v["hair"][1])
    sides = H.head_sides(sk, hr, top=3, back=12 if v["woman"] else 10, temple=8, sideburn=5)
    head(m, B, FACE_W if v["woman"] else FACE_M, lg, sides, nose=lambda f, x, y, w, h: v["skin"][4] if f == "north" else v["skin"][2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" and y > 0 else hr(f, x, y, w, h), shell_inflate=0.15, shell_rows=1.5)
    K.brows(m, B, v["hair"][1], tilt=0, w=2.3, h=0.4)
    neck(m, B, sk, 3.2)
    # The beret, tipped to his right; clear on the bare-headed variant.
    bt = m.bone("beret", (0, B.crown, 0), "head")

    def beret(f, x, y, w, h):
        if not v["beret"]:
            return None
        if f == "north" and 1 <= y <= 3 and w - 7 <= x <= w - 5:
            return SILVER[4] if (x + y) % 2 else SILVER[2]     # the badge over his left eye
        if f == "down":
            return BERET[0]
        return tone(BERET, 2.6 + (1.0 if f == "up" else 0) + 0.4 * math.sin(x * 0.9 + y))
    m.cube(bt, (-4.4, B.crown - 0.8, -4.3), (8.8, 1.6, 8.8), beret, density=2, tag="beret", rotation=(0, 0, 8), pivot=(0, B.crown, 0))
    m.cube(bt, (-4.6, B.crown - 1.2, -4.5), (9.2, 0.6, 9.2), lambda f, x, y, w, h: None if not v["beret"] or f in ("up", "down")
           else tone(BERET, 2.0), density=2, tag="beret_band", rotation=(0, 0, 8), pivot=(0, B.crown, 0))
    # Shirt, tie, vest.
    shirt = fabric(SHIRT, seed + 3, 3.6, 0.2)

    def front(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and abs(x - mid) < 0.9:
            return tone(TIE, 3.0 + (0.5 if x % 2 else 0))
        return shirt(f, x, y, w, h) if f == "north" else SHIRT[2]
    torso(m, B, front, tag="torso")
    vs = m.bone("vest", (0, B.HIP, 0), "body")

    def vest(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and y < 5 and abs(x - mid) < 2.6 - y * 0.4:
            return None                               # the shirt and tie show at the throat
        c = tone(VEST, 2.6 + 0.4 * math.sin(x * 0.6) - 0.4 * y / max(1, h) + (0.9 if f == "up" else 0))
        if f in ("north", "south") and y % 6 == 0:
            c = VEST[4]                               # webbing rows
        if f in ("east", "west") and y % 3 == 0:
            c = VEST[1]
        return c
    m.cube(vs, (-B.TW, B.HIP + 0.6, -B.TD), (2 * B.TW, B.NECK - B.HIP - 1.4, 2 * B.TD), vest, inflate=0.45, density=2, tag="vest")
    pouch = lambda f, x, y, w, h: tone(VEST, 3.0 + (1.0 if f == "up" else 0) - 0.5 * y / max(1, h) + (0.6 if y == 1 else 0))
    for i, px in enumerate((-2.9, -1.0, 1.0)):
        m.cube(vs, (px, B.HIP + 1.0, -B.TD - 1.1), (1.8, 2.6, 0.8), pouch, density=2, tag=f"pouch_{i}")
    m.cube(vs, (2.6, B.NECK - 5.0, -B.TD - 0.9), (1.2, 2.8, 0.6), lambda f, x, y, w, h: GUN[2] if y else SILVER[1], density=4, tag="radio")
    m.cube(vs, (2.9, B.NECK - 2.4, -B.TD - 0.75), (0.3, 1.4, 0.3), solid(GUN[1]), density=4, tag="radio_aerial")
    sleeve = fabric(SHIRT, seed + 4, 3.4, 0.25)
    arms(m, B, sleeve, None, glove=glove_m(GLOVE, seed + 5), sleeve_inflate=0.25)
    legs(m, B, fabric(TROUSER, seed + 6, 2.8, 0.25), BOOT, boot_h=3.2, toe=0.9)
    # The carbine: barrel down along the right hand (level when raised), the stock up along the forearm.
    cx, y = -B.AX, B.PALM
    r = m.bone("rifle", (cx, y, 0), "right_hand")
    body = lambda f, xx, yy, w, h: tone(GUN, 2.6 + (1.0 if f == "north" else 0) + (0.5 if xx == 0 else 0) - 0.2 * yy / max(1, h))
    m.cube(r, (cx - 0.55, y - 5.0, -1.8), (1.1, 6.0, 1.6), body, density=2, tag="receiver")
    m.cube(r, (cx - 0.45, y - 1.6, -0.3), (0.9, 1.8, 1.4), body, density=2, tag="magazine", rotation=(-12, 0, 0), pivot=(cx, y - 1.6, 0))
    m.cube(r, (cx - 0.3, y - 10.0, -1.55), (0.6, 5.0, 0.8), body, density=4, tag="barrel")
    m.cube(r, (cx - 0.5, y - 7.6, -2.1), (1.0, 2.6, 1.2), lambda f, xx, yy, w, h: tone(GUN, 2.2 + (yy % 2) * 0.6), density=4, tag="foregrip")
    m.cube(r, (cx - 0.45, y + 1.0, -1.6), (0.9, 4.2, 1.2), body, density=2, tag="stock")
    m.cube(r, (cx - 0.3, y - 3.2, -2.3), (0.6, 1.2, 0.6), solid(GUN[4]), density=4, tag="sight")


def rig(v=0):
    m = Model(NAME, 256, 128)
    skeleton(m, B, coat=False)
    build(m, VARIANTS[v])
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

GRIP = P_(K.fist("right"), K.fist("left", 0.7))
# Low ready: the carbine pointed at the ground ahead, the left hand under its foregrip.
READY = P_(GRIP, {"right_arm": [-34, 6, 6], "right_forearm": [-30, 0, 0], "left_arm": [-56, 34, 0], "left_forearm": [-30, -10, 0],
                  "body": [3, -8, 0], "head": [-3, 8, 0]})
RAISED = P_(GRIP, {"right_arm": [-86, 8, 0], "right_forearm": [-4, 0, 0], "left_arm": [-84, 34, 0], "left_forearm": [-12, -10, 0],
                   "body": [2, -14, 0], "head": [-4, 12, 0], "right_leg": [-8, 0, 2], "left_leg": [10, 0, -2]})


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    c = clip("idle", 3.0, loop=True)
    K.breathe(c, READY, 3.0, head_turn=10)
    c = clip("walk", 1.1, loop=True)
    K.walk(c, READY, 1.1, stride=18, arm=0, bob=0.4, twist=3, knee=(6, 16), arms=False)
    c = clip("run", 0.6, loop=True)
    K.walk(c, P_(READY, {"body": [14, -8, 0], "head": [-14, 8, 0], "right_arm": [-50, 6, 6], "left_arm": [-66, 34, 0]}), 0.6,
           stride=40, arm=0, bob=0.8, twist=4, knee=(14, 70), arms=False)

    # Shoot (3): raised, a burst of three -- the muzzle climbs.
    c = clip("shoot")
    c.key(0, READY)
    c.key(0.1, RAISED, E_OUT)
    for i, t in enumerate((h("shoot"), 0.25, 0.35)):
        c.key(t, P_(RAISED, {"right_arm": [-94 - 2 * i, 8, 0], "left_arm": [-92 - 2 * i, 34, 0], "@root": [0, 0, 0.25]}), E_SNAP)
    c.key(L("shoot"), P_(RAISED, {"right_arm": [-88, 8, 0]}), E_OUT)

    # Melee (5): a rifle-butt strike -- the stock driven forward and up.
    c = clip("melee")
    back = P_(GRIP, {"right_arm": [10, 20, 10], "right_forearm": [-110, 0, 0], "left_arm": [-60, 40, 0], "left_forearm": [-60, 0, 0],
                     "body": [-6, 24, 0], "@root": [0, 0, 0.6]})
    smash = P_(GRIP, {"right_arm": [-60, -20, 10], "right_forearm": [-120, 0, 0], "right_hand": [0, 0, 0], "left_arm": [-90, 10, 0],
                      "left_forearm": [-30, 0, 0], "body": [10, -24, 0], "head": [-8, 14, 0], "@root": [0, -0.4, -1.4],
                      "right_leg": [-26, 0, 0], "left_leg": [14, 0, 0], "right_shin": [16, 0, 0]})
    c.key(0, READY)
    c.key(0.15, back, E_IO)
    c.key(h("melee"), smash, E_SNAP)
    c.key(0.4, smash, E_OUT)
    c.key(L("melee"), READY, E_IO)

    # Death (20): hit, a stagger back, falls on his back.
    c = clip("death", hold=True)
    c.key(0, READY)
    c.key(0.2, P_(READY, {"body": [-14, 0, 0], "head": [-24, 0, 0], "@root": [0, 0, 1.0], "right_arm": [-20, 0, 30], "left_arm": [-30, 0, -40]}),
          E_SNAP)
    c.key(0.5, P_(READY, {"@root": [0, -4.0, 2.0], "right_leg": [-60, 0, 0], "left_leg": [-30, 0, 0], "right_shin": [70, 0, 0],
                          "left_shin": [40, 0, 0], "body": [-20, 0, 0], "head": [-10, 0, 0]}), E_IN)
    lying = {"root": [88, 0, 0], "@root": [0, 2.0, 0], "head": [10, 20, 0], "right_arm": [-10, 0, 60], "left_arm": [-10, 0, -50],
             "right_forearm": [-20, 0, 0], "left_forearm": [-10, 0, 0], "right_leg": [0, 0, 6], "left_leg": [-20, 0, -4],
             "left_shin": [30, 0, 0]}
    c.key(h("death"), P_(GRIP, lying), E_IN)
    c.key(L("death"), P_(GRIP, lying))

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


def build_all():
    out = []
    for v in range(len(VARIANTS)):
        m = rig(v)
        t, _ = m.build(gutter=1, seed=45000)
        out.append((m, t))
    sig = [[(c.box, c.size) for _, c in m.rig.cubes()] for m, _ in out]
    assert all(s == sig[0] for s in sig), "variants must share one atlas layout"
    f, clips = anims()
    K.verify_contract(out[0][0], f, C)
    return out, f, clips


def generate():
    from common import save
    out, f, _ = build_all()
    K.write_entity(out[0][0], f, NAME, out[0][1])
    for i, (_, t) in enumerate(out):
        save(t, "entity", f"{NAME}_{i}")


def preview(out_dir, scale=5):
    out, f, clips = build_all()
    paths = []
    for i, (m, t) in enumerate(out):
        paths += K.render(m, t, clips, out_dir, f"{NAME}_{i}", [("idle", 0.0)] if i else [("idle", 0.0), ("shoot", 0.15), ("melee", 0.25), ("death", 1.5)],
                          scale=scale, views=("front", "three_quarter", "side"))
    return paths


if __name__ == "__main__":
    generate()
