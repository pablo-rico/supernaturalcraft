"""The rival hunter (v0.13): a human hunter who comes for players who have sworn themselves to Heaven or Hell.

A new, detailed rig (not Dean or Sam): the working look of the show's hunters -- layered clothes, jeans, boots, a sawn-off
double-barrel loaded with rock salt, a machete, a flask of holy water and a belt full of pouches.

One geometry, three textures (same UV layout; the packing only depends on the cubes). Each look paints its own pieces and
leaves the others' clear, so the three silhouettes differ without the renderer hiding anything:
  rival_hunter_0  the jacket: a worn brown canvas work jacket over a grey hoodie (the hood out over the collar), a red
                  trucker cap, blue jeans, tan work boots; clean-shaven, short brown hair;
  rival_hunter_1  the flannel: a red-and-black flannel open over a dark tee, sleeves rolled to the elbow, its tails out over
                  the belt, an olive quilted vest over it; a greying beard, hair to the collar; dark jeans, brown boots;
  rival_hunter_2  the trench coat: a long charcoal coat to the shins (popped collar, the belt hanging loose from its loops)
                  over a dark henley; slicked black hair, stubble; black jeans, black boots.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head -- hair, cap
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_thumb,
                                                            shotgun -- shotgun_barrel -- shotgun_shell_r/l, muzzle
                                                                    -- shotgun_hammer_r/l
                                                            machete
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_thumb, flask -- flask_cap, reload_shells
               -- collar, lapel_r, lapel_l, hood, dog_tags, coat_tail_r, coat_tail_l
               -- belt -- belt_tail, pouch_0..3, flashlight, machete_sheathed, flask_belt
               -- shotgun_slung
       -- right_leg -- right_shin -- right_foot, knife_boot
       -- left_leg  -- left_shin  -- left_foot

Held things (AllegianceAssets.RIVAL_HUNTER_BONES): the renderer shows `shotgun` by default, swaps it for `machete` up close
and shows `flask` while throwing. Their stowed twins -- `shotgun_slung` across the back, `machete_sheathed` on the left hip,
`flask_belt` on the right hip -- are meant to show exactly when the held one is hidden. `reload_shells` sits inside the left
fist (invisible) until the reload clip brings it out; `muzzle` is an empty locator at the barrels' mouth for the flash.
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from michael_art import ANIM, E_IN, E_IO, E_OUT, E_SNAP, E_BACK, GEO, Clip, P_, tone
from pixelkit import Ramp, fbm, hexc, mix, shade

A = "animation.rival_hunter."
VARIANTS = (0, 1, 2)
CLEAR = (0, 0, 0, 0)

# --- palette ------------------------------------------------------------------------------------------------
SKIN = {
    0: Ramp("#5e3d2c", "#8c604a", "#b07e63", "#c69579", "#d8ac90", "#e7c3a8"),
    1: Ramp("#563626", "#80563f", "#a3735a", "#b98a6f", "#cca086", "#dbb79e"),
    2: Ramp("#4f3427", "#795341", "#9c6f58", "#b2866e", "#c59c84", "#d5b29a"),
}
HAIR = {
    0: Ramp("#1f140b", "#302012", "#43301c", "#584026", "#6c5131", "#80623d"),
    1: Ramp("#231a12", "#35281c", "#4a3a2a", "#5f4c39", "#76644f", "#8f806b"),
    2: Ramp("#060505", "#0c0a09", "#141110", "#1d1917", "#282220", "#352d2a"),
}
EYES = {0: hexc("#5a7a96"), 1: hexc("#5b4a2e"), 2: hexc("#3d5a3a")}
EYE_WHITE = hexc("#e6ddd2")
LIPS = hexc("#8e5a4c")

CANVAS = Ramp("#2e1f10", "#46311a", "#5d4423", "#74572e", "#8a6a3a", "#a07f4b")
HOODIE = Ramp("#2c2d30", "#3c3e42", "#4f5156", "#63666b", "#787b80", "#8e9196")
CAP_RED = Ramp("#2a0b0a", "#430f0d", "#5e1612", "#7a1f18", "#952b21", "#ad3b2e")
MESH = Ramp("#0f0f10", "#1a1a1c", "#252528", "#313135", "#3e3e43", "#4c4c52")
FLANNEL_RED = Ramp("#2e0b0a", "#4b1210", "#661915", "#80231b", "#972f24", "#ad3e30")
FLANNEL_BLK = Ramp("#0b0909", "#131010", "#1b1716", "#241f1d", "#2e2825", "#3a3330")
TEE = Ramp("#141416", "#1d1d20", "#27272b", "#323237", "#3e3e44", "#4b4b52")
VEST = Ramp("#1d2114", "#2b301d", "#3a4127", "#4a5232", "#5b643e", "#6e784c")
TRENCH = Ramp("#1c1d1f", "#28292c", "#343639", "#414447", "#4f5256", "#5f6267")
HENLEY = Ramp("#15181a", "#1e2326", "#283034", "#333d42", "#3f4b51", "#4d5b62")
DENIM = {0: Ramp("#162036", "#20304d", "#2b3f63", "#384f78", "#48618c", "#5c759f"),
         1: Ramp("#10172a", "#18223b", "#212e4c", "#2c3b5e", "#394a70", "#495b82"),
         2: Ramp("#0c0c0f", "#141418", "#1c1c22", "#25252c", "#303038", "#3c3c46")}
BOOT = {0: Ramp("#2c1d10", "#45301b", "#5f4426", "#785832", "#8f6c40", "#a5804f"),
        1: Ramp("#1e130b", "#2e1e12", "#40291a", "#533624", "#67452f", "#7c553b"),
        2: Ramp("#050505", "#0c0c0d", "#141416", "#1d1d20", "#28282c", "#35353a")}
SOLE = hexc("#1a1512")
LEATHER = Ramp("#1a0f08", "#2a1a0e", "#3c2615", "#4f331d", "#634226", "#785231")
POUCH = Ramp("#1f2216", "#2d3120", "#3c412b", "#4c5236", "#5d6443", "#6f7651")
STEEL = Ramp("#22262c", "#363c45", "#4e5560", "#6a727e", "#8d95a0", "#b9c0c9")
GUNMETAL = Ramp("#0f1013", "#18191d", "#222429", "#2e3036", "#3c3f46", "#4e525a")
WALNUT = Ramp("#24130a", "#3a1f10", "#512c17", "#673a1f", "#7d4a29", "#935b34")
BRASS = Ramp("#4a3510", "#6e5118", "#93702a", "#b38f3d", "#cfae58", "#e8cd80")
SHELL_RED = Ramp("#3a0806", "#5c0d0a", "#7e1610", "#9c2117", "#b6301f", "#cc4632")
GRIP_TAPE = Ramp("#0a0a0a", "#141414", "#1e1e1e", "#292929", "#353535", "#424242")
FLASK = Ramp("#2f3540", "#4b5462", "#6a7584", "#8e99a8", "#b3bdca", "#dfe6ee")
HOLY = hexc("#9fd0ff")

# Proportions: a torso a little narrower than the player's (7 px) and the arms set out from it, so the silhouette keeps a
# waist and a gap under the arms; legs 3.5 px.
TW = 3.5      # torso half-width
AX = 6.75     # arm centre (|x|)
SX = 5.5      # shoulder pivot (|x|)
LX = 1.8      # leg centre (|x|)
LW = 3.5      # leg width


def only(variant, looks, mat):
    """`mat` in the given looks, clear in the others (a piece of one outfit)."""
    if variant in looks:
        return mat
    return lambda f, x, y, w, h: CLEAR


# --- materials ----------------------------------------------------------------------------------------------

def worn(ramp, seed, base=3.0, amp=1.0, scuff=0.0, hem=True):
    """Hard-wearing cloth: soft top light, low-frequency wear, a darker hem, scuffed paler patches."""
    def m(face, x, y, w, h):
        if face == "up":
            return tone(ramp, base + 0.5)
        if face == "down":
            return tone(ramp, base - 1.2)
        n = fbm(x + len(face) * 7, y, seed, 64, 64, 3, 7.0)
        v = base + (n - 0.5) * amp * 1.6 - 0.35 * y / max(1, h)
        if scuff and fbm(x * 2 + 3, y * 2 + len(face), seed + 5, 64, 64, 2, 9.0) > 1 - scuff:
            v += 0.9
        if hem and y == h - 1:
            v -= 0.9
        return tone(ramp, v)
    return m


def canvas(seed):
    """Brown duck canvas: a fine diagonal twill, rubbed pale at the edges and elbows."""
    base = worn(CANVAS, seed, 3.0, 0.9, scuff=0.12)

    def m(face, x, y, w, h):
        c = base(face, x, y, w, h)
        if face not in ("up", "down") and (x + y) % 3 == 0:
            c = shade(c, 1.08)
        return c
    return m


def buffalo(seed, period=6):
    """Red-and-black buffalo check: black bands crossing on red, overlaps darkest, a soft woollen nap."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 64, 64, 2, 10.0)
        bx, by = (x // (period // 2)) % 2, (y // (period // 2)) % 2
        if bx and by:
            c = FLANNEL_BLK[2.2 + n]
        elif bx or by:
            c = mix(FLANNEL_RED[2.4 + n], FLANNEL_BLK[2], 0.55)
        else:
            c = FLANNEL_RED[3.0 + (n - 0.5) * 1.2]
        if face not in ("up", "down") and y == h - 1:
            c = shade(c, 0.75)
        return c
    return m


def quilted(seed):
    """The vest: olive nylon puffed into horizontal baffles (light at their tops, dark stitch lines between)."""
    def m(face, x, y, w, h):
        if face in ("up", "down"):
            return VEST[2.5]
        k = y % 5
        n = fbm(x + len(face), y, seed, 64, 64, 2, 8.0)
        v = 3.3 - 0.45 * k + (n - 0.5) * 0.6
        if k == 4:
            v = 1.4
        return VEST[v]
    return m


def denim(variant, seed):
    ramp = DENIM[variant]

    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 3, y, seed, 64, 64, 3, 8.0)
        v = 2.6 + (n - 0.5) * 1.5 + (0.45 if (x - y) % 3 == 0 else 0)
        c = ramp[v]
        if face == "north" and variant != 2 and 0.15 < y / max(1, h) < 0.55 and 2 <= x <= w - 3:
            c = mix(c, ramp[5], 0.22)  # faded thighs
        if face in ("east", "west") and x == w // 2:
            c = shade(c, 1.15) if y % 2 else shade(c, 0.85)  # the outer seam
        return c
    return m


def knit(ramp, seed, base=3.0):
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 64, 64, 2, 12.0)
        v = base + (n - 0.5) * 0.8 + (0.35 if x % 2 == 0 else -0.1)
        if face not in ("up", "down") and y >= h - 2:
            v -= 0.5  # ribbed cuff / hem
        return ramp[v]
    return m


def leather(seed, ramp=LEATHER, base=3.0):
    def m(face, x, y, w, h):
        n = fbm(x * 3 + len(face), y * 3, seed, 64, 64, 2, 9.0)
        v = base + (n - 0.5) * 1.2
        if face not in ("up", "down") and (y == 0 or y == h - 1):
            v -= 0.8
        return ramp[v]
    return m


def metal(ramp, base=3.2):
    def m(face, x, y, w, h):
        if face == "up":
            return ramp[base + 1]
        if face == "down":
            return ramp[base - 1.5]
        return ramp[base + 0.6 - 1.2 * y / max(1, h)]
    return m


# --- faces (16 x 16 texels, density 2) -----------------------------------------------------------------------------

FACES = {
    0: [  # clean-shaven, a square jaw, a tired squint
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhhshhhhhhhhshhh",
        "ssssssssssssssss",
        "ssssssssssssssss",
        "sbbbbssssssbbbbs",
        "sswwEssssssEwwss",
        "ssuuussssssuuuss",
        "sssssssnnsssssss",
        "ssssssnNNnssssss",
        "ssssssssssssssss",
        "sssssmmmmmmsssss",
        "ssssssssssssssss",
        "jsssssssssssssssj"[:16],
        "jjssssssssssssjj",
    ],
    1: [  # a greying beard and moustache, heavy brows
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hsssssssssssssshh"[:16],
        "ssssssssssssssss",
        "sBBBBssssssBBBBs",
        "sswwEssssssEwwss",
        "ssuuussssssuuuss",
        "sssssssnnsssssss",
        "dssssdnNNndssssd",
        "ddddddddddddddd d"[:16],
        "dddddmmmmmmddddd",
        "dddddddddddddddd",
        "dddddddddddddddd",
        "dddddddddddddddd",
    ],
    2: [  # stubble, a scar across the left brow, a hard stare
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hsssssssssssssss",
        "ssssssssssssssss",
        "ssssssssssssssss",
        "sbbbbsssssbbcbbs",
        "sswwEsssssEwwsss",
        "ssuuusssssuuusss",
        "sssssssnnsssssss",
        "ttsssnnNNnnssstt",
        "tttsssssssssttts",
        "ttttsmmmmmmstttt",
        "tttttttttttttttt",
        "jtttttttttttttj ",
        "jjttttttttttttjj",
    ],
}


def fix_rows(rows):
    return [(r + "s" * 16)[:16].replace(" ", "s") for r in rows]


# --- the rig ------------------------------------------------------------------------------------------------

def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 12, 0), "root")
    m.bone("head", (0, 24, 0), "body")
    m.bone("hair", (0, 24, 0), "head")
    m.bone("cap", (0, 31.5, 0), "head")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (SX * s, 22, 0), "body")
        m.bone(f"{side}_forearm", (AX * s, 18, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (AX * s, 13.5, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (AX * s, 11.5, -1.5), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 1.0 * s, 13.0, -1.5), f"{side}_hand")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_leg", (LX * s, 12, 0), "root")
        m.bone(f"{side}_shin", (LX * s, 6, 0), f"{side}_leg")
        m.bone(f"{side}_foot", (LX * s, 1.5, 0), f"{side}_shin")


def rig(variant=0):
    v = variant
    m = Model("rival_hunter", 512, 512)
    skeleton(m)
    build_person(m, v)
    build_clothes(m, v)
    build_belt(m, v)
    build_shotgun(m, "right_hand")
    build_machete(m)
    build_flask(m)
    build_stowed(m, v)
    return m


def build_person(m, v):
    skin = H.skin_mat(SKIN[v], 7001 + v, 3.1, 0.45)
    hair_ramp = HAIR[v]

    def hair(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, 7010 + v, 64, 64, 2, 12.0)
        return hair_ramp[2.4 + (n - 0.5) * 1.3 + 0.5 * math.sin(x * 1.6 + y * 0.5)]

    beard_m = lambda f, x, y, w, h: mix(hair(f, x, y, w, h), HAIR[1][4], 0.35 if h01("grey", f, x, y) < 0.35 else 0.0)
    stub = lambda f, x, y, w, h: mix(skin(f, x, y, w, h), hair_ramp[1], 0.42 if h01("stub", f, x, y) < 0.6 else 0.2)
    legend = {"h": hair, "s": skin, "b": hair_ramp[1], "B": hair_ramp[2], "w": EYE_WHITE, "E": EYES[v], "u": SKIN[v][2],
              "n": SKIN[v][4], "N": SKIN[v][1], "m": LIPS, "j": SKIN[v][2], "d": beard_m, "t": stub,
              "c": mix(SKIN[v][4], hexc("#c9a38e"), 0.5)}
    beard = beard_m if v == 1 else (stub if v == 2 else None)
    sides = H.head_sides(skin, hair, top=4 if v != 2 else 3, back=12 if v != 1 else 16, temple=6, sideburn=8 if v else 6,
                         beard=beard, beard_from=11 if v == 1 else 12)
    H.head_cube(m, fix_rows(FACES[v]), legend, sides)
    m.cube("head", (-0.5, 26.5, -4.5), (1, 2, 0.5), lambda f, x, y, w, h: SKIN[v][4] if f == "north" else SKIN[v][2],
           density=2, tag="nose")
    for s in (-1, 1):
        m.cube("head", (4 * s - 0.25 + (0 if s > 0 else -0.25), 26, -0.5), (0.5, 2, 1.5), solid(SKIN[v][2]), density=2, tag="ear")
    m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), skin, density=2, tag="neck")
    # Hair: a shell over the crown (all three), hair to the collar (the flannel), a beard (the flannel).
    shell = lambda f, x, y, w, h: None if f == "down" or (f == "north" and y > 1 + (x * 7 % 3 == 0)) else hair(f, x, y, w, h)
    m.cube("hair", (-4, 29.5, -4), (8, 2.5, 8), shell, inflate=0.3, density=2, tag="hair")
    if True:
        def long_hair(f, x, y, w, h):
            if v != 1 or f in ("up",):
                return CLEAR
            if f != "down" and y == h - 1 and (x * 5) % 3 == 0:
                return CLEAR  # ragged ends
            return hair(f, x, y, w, h)
        m.cube("hair", (-4, 23.5, 2.2), (8, 6.5, 2), long_hair, inflate=0.3, density=2, tag="hair_long")
        for s in (-1, 1):
            m.cube("hair", (4 * s - 0.5 + (0.3 * s), 24.5, -1.5), (1, 5, 4), long_hair, inflate=0.15, density=2, tag="hair_side")

        def beard_cube(f, x, y, w, h):
            if v != 1:
                return CLEAR
            if f == "up":
                return CLEAR
            if f == "north" and y < 2 and 4 <= x <= 11:
                return CLEAR  # the mouth shows above the chin
            return beard_m(f, x, y, w, h)
        m.cube("head", (-4, 23.4, -4.6), (8, 3.0, 1.2), beard_cube, density=2, tag="beard")
    # The trucker cap: a crown with a mesh back and a curved brim.
    cap_m = lambda f, x, y, w, h: CLEAR if v != 0 else (MESH[2 + (x + y) % 2] if f in ("south", "east", "west") and y > 1 and
                                                       (f == "south" or x > 7) else CAP_RED[3.0 + 0.6 * (f == "up") - 0.3 * y / max(1, h)])
    m.cube("cap", (-4.2, 30.5, -4.2), (8.4, 2.5, 8.4), none_on(("down",), cap_m), inflate=0.15, density=2, tag="cap")
    m.cube("cap", (-3.6, 32.6, -3.6), (7.2, 0.8, 7.2), only(v, (0,), solid(CAP_RED[3])), density=2, tag="cap_top")
    brim = lambda f, x, y, w, h: CLEAR if v != 0 else (CAP_RED[2] if f != "down" else hexc("#2c4b2a"))
    m.cube("cap", (-3.5, 30.4, -7.4), (7, 0.5, 3.4), brim, density=2, tag="brim", rotation=(-8, 0, 0), pivot=(0, 30.6, -4.2))
    m.cube("cap", (-0.5, 33.3, -0.5), (1, 0.3, 1), only(v, (0,), solid(CAP_RED[4])), density=2, tag="cap_button")
    # Hands and fingers.
    hand = H.skin_mat(SKIN[v], 7020 + v, 3.2, 0.4)
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.cube(f"{side}_hand", (cx - 1.5, 11.5, -1.5), (3, 2, 3), hand, density=2, tag="palm")
        m.cube(f"{side}_fingers", (cx - 1.5, 10.6, -1.7), (3, 1.4, 2.2), lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.92 if x % 2 else 1.0),
               density=2, tag="fingers")
        m.cube(f"{side}_thumb", (cx - 1.0 * s - 0.5, 11.6, -2.1), (1, 1.6, 1), hand, density=2, tag="thumb")
        # Fingerless leather gloves on the trench coat; nothing on the others.
        glove = only(v, (2,), lambda f, x, y, w, h: LEATHER[2.0 + 0.6 * (f == "up")])
        m.cube(f"{side}_hand", (cx - 1.5, 11.6, -1.5), (3, 1.8, 3), none_on(("down",), glove), inflate=0.12, density=2, tag="glove")


def build_clothes(m, v):
    # -- torso layers: the shirt underneath, then the jacket / vest / coat (each clear in the other looks) --
    if v == 0:
        under = knit(HOODIE, 7101, 3.0)
    elif v == 1:
        under = worn(TEE, 7102, 2.6, 0.6)
    else:
        under = worn(HENLEY, 7103, 2.8, 0.6)
    m.cube("body", (-TW, 12, -2), (2 * TW, 12, 4), under, density=2, tag="torso")
    if v == 2:
        def placket(f, x, y, w, h):
            if f == "north" and x in (w // 2 - 1, w // 2) and y < 7:
                return HENLEY[1] if x == w // 2 - 1 else (HENLEY[5] if y in (2, 5) else HENLEY[3])
            return under(f, x, y, w, h)
        m.cube("body", (-TW, 12, -2), (2 * TW, 12, 4), only(v, (2,), none_on(("up", "down", "east", "west", "south"), placket)),
               inflate=0.05, density=2, tag="placket")
    else:
        m.cube("body", (-TW, 12, -2), (2 * TW, 12, 4), only(v, (), under), inflate=0.05, density=2, tag="placket")
    # The flannel shirt open over the tee (the flannel look): open down the front, its collar up by the neck.
    fl = buffalo(7110)

    def flannel_front(f, x, y, w, h):
        if f in ("down",):
            return None
        if f in ("north", "up") and abs(x - (w - 1) / 2) <= 1.5 and (f == "north" or y < 3):
            return None
        return fl(f, x, y, w, h)
    m.cube("body", (-TW, 12, -2), (2 * TW, 12, 4), only(v, (1,), flannel_front), inflate=0.2, density=2, tag="flannel")
    # The work jacket (jacket look) and the trench coat (trench look): open fronts with lapels.
    cv = canvas(7120)
    tr = worn(TRENCH, 7121, 3.0, 0.7, scuff=0.04)

    def open_front(mat, gap, lapel):
        def m_(f, x, y, w, h):
            if f == "down":
                return None
            mid = (w - 1) / 2
            if f == "north" and abs(x - mid) <= gap:
                return None
            if f == "up" and abs(x - mid) <= gap and y < 3:
                return None
            if f == "north" and abs(x - mid) <= gap + 1.0:
                return lapel
            return mat(f, x, y, w, h)
        return m_
    m.cube("body", (-TW, 12, -2), (2 * TW, 12, 4), only(v, (0,), open_front(cv, 2.5, CANVAS[1])), inflate=0.45, density=2, tag="jacket")
    m.cube("body", (-TW, 12, -2), (2 * TW, 12, 4), only(v, (2,), open_front(tr, 2.5, TRENCH[4])), inflate=0.5, density=2, tag="coat")
    vest = m.bone("vest", (0, 23, 0), "body")
    m.cube(vest, (-TW, 12.5, -2), (2 * TW, 11.0, 4), only(v, (1,), open_front(quilted(7122), 2.0, VEST[1])), inflate=0.6, density=2,
           tag="vest")
    # Jacket and coat pockets, the hoodie's drawstrings.
    for s in (-1, 1):
        pocket = lambda f, x, y, w, h: CANVAS[1] if y == 0 else CANVAS[2]
        m.cube("body", (2.0 * s - 1.0, 13.0, -2.75), (2, 2.5, 0.3), only(v, (0,), pocket), density=2, tag="pocket")
        m.cube("body", (2.2 * s - 1.0, 12.6, -2.85), (2, 3.0, 0.3), only(v, (2,), lambda f, x, y, w, h: TRENCH[1] if y == 0 else TRENCH[2]),
               density=2, tag="pocket")
        m.cube("body", (0.9 * s - 0.25, 19.5, -2.35), (0.5, 3.5, 0.3), only(v, (0,), solid(HOODIE[5])), density=2, tag="drawstring")
    # Collar: the jacket's corduroy collar, the flannel's collar, the coat's tall popped collar.
    collar = m.bone("collar", (0, 23.5, 0), "body")

    def collar_m(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f in ("north", "up") and abs(x - mid) <= 3.0 and (f == "north" or y < 3):
            return None
        if v == 0:
            return hexc("#3b2a19") if (x % 2) else hexc("#4a3621")
        if v == 1:
            return fl(f, x, y, w, h)
        return tone(TRENCH, 3.2 + 0.5 * (y == 0) - 0.2 * (f == "south"))
    m.cube(collar, (-TW, 22.5, -2), (2 * TW, 2.0, 4), collar_m, inflate=0.75, density=2, tag="collar")
    # The coat's collar stands up round the neck (clear in the other looks).
    m.cube(collar, (-TW - 0.25, 24.4, -1.2), (2 * TW + 0.5, 2.6, 3.6), only(v, (2,), none_on(("down", "up"), lambda f, x, y, w, h:
           None if f == "north" and 3 <= x <= w - 4 else tone(TRENCH, 3.6 - 0.8 * y / max(1, h)))), inflate=0.55, density=2,
           tag="popped_collar")
    # Lapels: flaps either side of the opening that swing as he moves.
    for side, s in (("r", -1), ("l", 1)):
        lp = m.bone(f"lapel_{side}", (1.6 * s, 22.5, -2.6), "body")
        lmat = {0: CANVAS, 1: VEST, 2: TRENCH}[v]

        def lap(f, x, y, w, h, lmat=lmat, s=s):
            if f in ("up", "down"):
                return None
            if f == "north" and y > h - 3 and (x == (w - 1 if s < 0 else 0)):
                return None
            return tone(lmat, 3.4 - 0.9 * y / max(1, h))
        L = 6.0 if v != 1 else 4.0
        m.cube(lp, (1.6 * s - 0.9, 22.5 - L, -2.95), (1.8, L, 0.4), lap, density=2, tag="lapel",
               rotation=(4, 0, -10 * s), pivot=(1.6 * s, 22.5, -2.7))
    # The hood of the hoodie, lying out over the jacket's collar: a soft bag with a rim round the neck and a point below.
    hood = m.bone("hood", (0, 23.5, 2.2), "body")

    def fleece(f, x, y, w, h, top=3.4, opening=False):
        if v != 0:
            return CLEAR
        if f == "down":
            return None
        n = fbm(x * 2 + len(f), y * 2, 7130, 64, 64, 2, 7.0)
        val = top - 1.2 * y / max(1, h) + (n - 0.5) * 0.5 + (0.3 if f == "up" else 0)
        if opening and f == "up":
            return HOODIE[0] if 1 <= y <= h - 2 and 1 <= x <= w - 2 else HOODIE[4]   # the dark inside of the hood
        if f == "south":
            # Soft folds converging on the point, a lit middle.
            cx = (w - 1) / 2
            fold = abs(abs(x - cx) - (cx * (1 - 0.8 * y / max(1, h)))) < 0.8
            val += -0.8 if fold else 0.35 * (1 - abs(x - cx) / (cx + 1))
        return HOODIE[val]
    m.cube(hood, (-2.8, 18.8, 2.15), (5.6, 4.2, 1.7), fleece, density=2, tag="hood")
    m.cube(hood, (-1.6, 17.9, 2.4), (3.2, 1.2, 1.2), lambda f, x, y, w, h: fleece(f, x, y, w, h, 2.4), density=2, tag="hood_point")
    m.cube(hood, (-3.6, 22.2, 1.4), (7.2, 1.8, 2.8), lambda f, x, y, w, h: fleece(f, x, y, w, h, 3.8, opening=True), density=2,
           tag="hood_rim")
    # Dog tags on a ball chain (painted down the chest), the tags on their own bone.
    tags = m.bone("dog_tags", (0, 23, -2.3), "body")
    m.cube(tags, (-0.6, 18.2, -2.55), (1.2, 1.6, 0.2), lambda f, x, y, w, h: STEEL[4] if y < 2 else STEEL[3], density=4, tag="tag")
    # The trench coat's skirt (look 2): two front-and-side panels and a back panel with a vent, down to mid-calf, flared so
    # they stand off the legs and swing on their own.
    trench_m = lambda f, x, y, w, h: tr(f, x, y, w, h)

    def panel(f, x, y, w, h, s, folds=True):
        if v != 2 or f in ("up", "down"):
            return CLEAR if v != 2 else None
        if y >= h - 1:
            return TRENCH[1]   # the hem
        c = tr(f, x, y, w, h)
        if folds and (x + (s > 0)) % 4 == 0:
            c = shade(c, 0.84)
        return c
    for side, s in (("r", -1), ("l", 1)):
        b = m.bone(f"coat_tail_{side}", (2.0 * s, 12, -0.6), "body", rotation=(-4, 0, -6 * s))

        def tail(f, x, y, w, h, s=s):
            # Open at the front: the inner column of the north face is the coat's opening; no inner face, no back face.
            if f == ("west" if s < 0 else "east"):
                return CLEAR
            if f == "south":
                return CLEAR
            if f == "north" and ((s < 0 and x >= w - 1) or (s > 0 and x <= 0)):
                return CLEAR
            return panel(f, x, y, w, h, s)
        m.cube(b, ((0.1 if s > 0 else -4.1), 2.5, -2.9), (4.0, 9.5, 5.0), tail, density=2, tag="coat_tail")
    cb = m.bone("coat_back", (0, 12, 2.3), "body", rotation=(10, 0, 0))

    def back_panel(f, x, y, w, h):
        if v != 2:
            return CLEAR
        if f not in ("north", "south"):
            return TRENCH[1]
        if abs(x - (w - 1) / 2) < 0.6 and y > h * 0.45:
            return CLEAR   # the vent
        return panel(f, x, y, w, h, 1)
    m.cube(cb, (-4.0, 2.5, 2.2), (8.0, 9.5, 0.5), back_panel, density=2, tag="coat_back")
    # The flannel's shirt tails, out over the belt (look 1).
    st = m.bone("shirt_tails", (0, 12, 0), "body")

    def tails(f, x, y, w, h):
        if v != 1 or f in ("up", "down"):
            return CLEAR if v != 1 else None
        if f == "north" and abs(x - (w - 1) / 2) < 1.6:
            return CLEAR
        if y == h - 1 and x % 3 == 0:
            return CLEAR
        return fl(f, x, y, w, h)
    m.cube(st, (-TW, 9.0, -2), (2 * TW, 3.0, 4), tails, inflate=0.75, density=2, tag="shirt_tails")
    # -- sleeves --
    sleeve = {0: cv, 1: fl, 2: tr}[v]
    hand_skin = H.skin_mat(SKIN[v], 7140 + v, 3.1, 0.45)
    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.cube(f"{side}_arm", (cx - 2, 17.5, -2), (4, 6.5, 4), sleeve, density=2, inflate=0.25 if v != 1 else 0.15, tag="upper_arm")
        fore = hand_skin if v == 1 else sleeve  # rolled sleeves bare the forearm
        m.cube(f"{side}_forearm", (cx - 2, 13.5, -2), (4, 4.5, 4), fore, density=2, inflate=0.22 if v != 1 else 0.0, tag="forearm")
        # The roll of the sleeve at the elbow (flannel), a cuff at the wrist (jacket, coat).
        m.cube(f"{side}_forearm", (cx - 2, 16.5, -2), (4, 1.5, 4), only(v, (1,), none_on(("up", "down"), lambda f, x, y, w, h:
               shade(fl(f, x, y, w, h), 1.1 if y == 0 else 0.9))), inflate=0.45, density=2, tag="roll")
        cuff = lambda f, x, y, w, h: {0: CANVAS[1], 1: CLEAR, 2: TRENCH[1]}[v]
        m.cube(f"{side}_forearm", (cx - 2, 13.5, -2), (4, 1.0, 4), none_on(("up", "down"), cuff), inflate=0.35, density=2, tag="cuff")
        if v == 0:
            # Elbow patches.
            m.cube(f"{side}_arm", (cx - 1.0, 18.0, 2.3), (2, 2.0, 0.2), solid(CANVAS[1]), density=2, tag="patch")
        else:
            m.cube(f"{side}_arm", (cx - 1.0, 18.0, 2.3), (2, 2.0, 0.2), lambda f, x, y, w, h: CLEAR, density=2, tag="patch")
    # -- legs and boots --
    jeans = denim(v, 7150 + v)
    boot = BOOT[v]
    for side, s in (("right", -1), ("left", 1)):
        cx = LX * s
        hw = LW / 2
        m.cube(f"{side}_leg", (cx - hw, 6, -1.75), (LW, 6, 3.5), jeans, density=2, tag="thigh")
        m.cube(f"{side}_shin", (cx - hw, 2.5, -1.75), (LW, 3.5, 3.5), jeans, density=2, tag="shin")
        # Work boots: an ankle shaft with laces, a toe cap, a thick sole.

        def shaft(f, x, y, w, h):
            if f == "up":
                return None
            if f == "north" and 2 <= x <= 5 and y % 2 == 0 and y < h - 1:
                return hexc("#c7b38a") if v == 0 else boot[1]  # laces
            if y == 0:
                return boot[4]
            return boot[2.6 + 0.4 * math.sin(x + y)]
        m.cube(f"{side}_shin", (cx - hw - 0.1, 1.5, -1.85), (LW + 0.2, 3.0, 3.7), shaft, density=2, tag="boot_shaft")

        def foot(f, x, y, w, h):
            if f == "down":
                return SOLE
            if f != "up" and y == h - 1:
                return SOLE
            if f == "up":
                return boot[3.5 if y < 3 else 2.8]
            return boot[3.0 - 0.6 * y / max(1, h)]
        m.cube(f"{side}_foot", (cx - hw - 0.15, 0, -2.9), (LW + 0.3, 1.5, 4.9), foot, density=2, tag="boot")
        # The jeans' hem bunched over the boot (the trench and flannel: jeans over; the jacket: jeans tucked out too).
        m.cube(f"{side}_shin", (cx - hw, 3.5, -1.75), (LW, 1.0, 3.5), none_on(("up", "down"), lambda f, x, y, w, h: shade(jeans(f, x, y, w, h), 0.85)),
               inflate=0.3, density=2, tag="hem")
    kb = m.bone("knife_boot", (-3.6, 4.5, 0), "right_shin")
    m.cube(kb, (-3.9, 3.0, -0.5), (0.6, 2.8, 1.0), leather(7160), density=4, tag="knife_sheath")
    m.cube(kb, (-3.85, 5.8, -0.3), (0.5, 1.6, 0.6), lambda f, x, y, w, h: GRIP_TAPE[2 + (y % 2)], density=4, tag="knife_grip")
    m.cube(kb, (-3.9, 5.6, -0.45), (0.6, 0.25, 0.9), solid(STEEL[3]), density=4, tag="knife_guard")


def build_belt(m, v):
    belt = m.bone("belt", (0, 12, 0), "body")
    bl = leather(7201)

    def belt_m(f, x, y, w, h):
        if f in ("up", "down"):
            return LEATHER[1]
        return bl(f, x, y, w, h)
    m.cube(belt, (-TW, 11.0, -2), (2 * TW, 1.5, 4), belt_m, inflate=0.6, density=2, tag="belt")
    m.cube(belt, (-1.0, 10.8, -2.85), (2.0, 1.9, 0.4), lambda f, x, y, w, h: BRASS[3] if f == "north" and 0 < x < w - 1 and 0 < y < h - 1
           and not (1 < x < w - 2 and 1 < y < h - 2) else BRASS[2], density=4, tag="buckle")
    # A row of shotgun shells in loops across the left front of the belt.
    for i in range(5):
        x = 0.9 + i * 0.5
        m.cube(belt, (x, 10.6, -2.95), (0.4, 1.4, 0.4), lambda f, xx, y, w, h: BRASS[4] if y >= h - 1 else SHELL_RED[3 + (f == "north")],
               density=4, tag="belt_shell")
    # The trench coat's belt hanging loose from one loop (clear in the other looks).
    bt = m.bone("belt_tail", (-3.2, 13.5, -2.9), "belt")
    m.cube(bt, (-3.7, 6.5, -3.25), (1.0, 7.0, 0.35), only(v, (2,), lambda f, x, y, w, h: TRENCH[3] if y < h - 1 else TRENCH[1]),
           density=2, tag="belt_tail", rotation=(0, 0, -6), pivot=(-3.2, 13.5, -2.9))
    m.cube(bt, (-3.8, 12.7, -3.3), (1.2, 0.7, 0.45), only(v, (2,), solid(STEEL[3])), density=4, tag="belt_tail_buckle")
    # Pouches: two at the right hip, two behind.
    specs = [  # name, origin, size, pivot, (ramp, flap)
        ("pouch_0", (-3.9, 9.0, -3.1), (2.4, 2.8, 1.3), (-2.7, 11.5, -2.6), POUCH),
        ("pouch_1", (-4.85, 8.8, -1.4), (1.3, 3.0, 2.6), (-4.2, 11.5, 0), LEATHER),
        ("pouch_2", (-3.5, 9.2, 2.3), (2.5, 2.6, 1.3), (-2.2, 11.5, 2.6), POUCH),
        ("pouch_3", (0.6, 9.2, 2.3), (2.5, 2.6, 1.3), (1.8, 11.5, 2.6), LEATHER),
    ]
    for i, (name, o, sz, piv, ramp) in enumerate(specs):
        b = m.bone(name, piv, "belt")
        body_m = leather(7210 + i, ramp, 2.9)

        def pm(f, x, y, w, h, ramp=ramp, body_m=body_m):
            if f == "up":
                return ramp[3.6]
            if y < 3 and f != "down":
                c = ramp[3.4] if y < 2 else ramp[1]  # the flap and its shadow
                if y == 1 and abs(x - (w - 1) / 2) < 1 and f in ("north", "south", "east", "west"):
                    return BRASS[3]  # a snap
                return c
            return body_m(f, x, y, w, h)
        m.cube(b, o, sz, pm, density=2, tag="pouch")
    # A flashlight in a ring holder at the left front.
    fl = m.bone("flashlight", (3.6, 11.0, 1.2), "belt")
    m.cube(fl, (3.65, 7.4, 0.7), (1.0, 3.6, 1.0), metal(GUNMETAL, 3.0), density=4, tag="flashlight_body")
    m.cube(fl, (3.5, 6.6, 0.55), (1.3, 1.0, 1.3), lambda f, x, y, w, h: hexc("#e8e2c8") if f == "down" else GUNMETAL[3], density=4,
           tag="flashlight_head")
    m.cube(fl, (3.45, 10.0, 0.6), (1.4, 0.5, 1.2), solid(LEATHER[2]), density=4, tag="flashlight_ring")


def build_shotgun(m, hand):
    """The sawn-off double-barrel: walnut pistol grip through the fist, a case-hardened receiver with two hammers, the
    barrels (break open on a hinge at the receiver's front) and a stubby fore-end. Points along -Z from the hand."""
    x0 = -AX
    g = m.bone("shotgun", (x0, 12.5, 0), hand, rotation=(50, 0, 0))
    wood = leather(7301, WALNUT, 3.0)

    def receiver(f, x, y, w, h):
        n = fbm(x * 3 + len(f), y * 3, 7302, 64, 64, 2, 6.0)
        c = STEEL[2.4 + n * 1.6]  # colour case-hardening: mottled steel with a warm bloom
        if n > 0.62:
            c = mix(c, hexc("#6a4a7a"), 0.25)
        if f == "up":
            c = shade(c, 1.15)
        return c
    m.cube(g, (x0 - 0.8, 12.1, -4.6), (1.6, 2.0, 3.4), receiver, density=4, tag="receiver")
    m.cube(g, (x0 - 0.5, 10.0, -1.6), (1.0, 3.0, 1.6), wood, density=4, tag="grip", rotation=(-18, 0, 0), pivot=(x0, 12.5, -1.2))
    m.cube(g, (x0 - 0.55, 9.6, -0.9), (1.1, 0.8, 1.8), solid(WALNUT[1]), density=4, tag="grip_cap", rotation=(-18, 0, 0),
           pivot=(x0, 12.5, -1.2))
    m.cube(g, (x0 - 0.15, 11.2, -3.2), (0.3, 0.9, 1.6), solid(GUNMETAL[2]), density=4, tag="trigger_guard")
    for side, dx in (("r", -0.4), ("l", 0.4)):
        hb = m.bone(f"shotgun_hammer_{side}", (x0 + dx, 13.9, -1.6), g.name)
        m.cube(hb, (x0 + dx - 0.2, 13.9, -1.9), (0.4, 0.9, 0.6), solid(GUNMETAL[3]), density=4, tag="hammer",
               rotation=(-20, 0, 0), pivot=(x0 + dx, 13.9, -1.6))
    b = m.bone("shotgun_barrel", (x0, 12.2, -4.6), g.name)
    blued = metal(GUNMETAL, 3.0)
    for dx in (-0.45, 0.45):
        def bore(f, x, y, w, h):
            if f == "north":
                return VOID_C if 0 < x < w - 1 and 0 < y < h - 1 else GUNMETAL[3]
            return blued(f, x, y, w, h)
        m.cube(b, (x0 + dx - 0.45, 12.6, -10.8), (0.9, 0.9, 7.4), bore, density=4, tag="barrel")
    m.cube(b, (x0 - 0.15, 13.45, -10.6), (0.3, 0.25, 7.0), solid(GUNMETAL[4]), density=4, tag="rib")
    m.cube(b, (x0 - 0.1, 13.6, -10.6), (0.2, 0.25, 0.4), solid(BRASS[4]), density=4, tag="bead")
    m.cube(b, (x0 - 0.65, 12.0, -8.4), (1.3, 0.8, 3.8), wood, density=4, tag="fore_end")
    for side, dx in (("r", -0.45), ("l", 0.45)):
        sb = m.bone(f"shotgun_shell_{side}", (x0 + dx, 13.05, -3.4), b.name)
        m.cube(sb, (x0 + dx - 0.35, 12.7, -4.2), (0.7, 0.7, 2.2), lambda f, x, y, w, h: BRASS[4] if f == "south" else SHELL_RED[3],
               density=4, tag="chamber_shell")
    m.bone("muzzle", (x0, 13.05, -10.8), b.name)


VOID_C = hexc("#060606")


def build_machete(m):
    """A working machete: black grip through the right fist, a wide blade swelling to a clipped point (-Z)."""
    x0 = -AX
    b = m.bone("machete", (x0, 12.25, 0), "right_hand", rotation=(40, 0, 0))
    m.cube(b, (x0 - 0.5, 11.75, -1.6), (1.0, 1.0, 4.4), lambda f, x, y, w, h: GRIP_TAPE[2 + ((x + y) % 3 == 0)], density=4, tag="handle")
    m.cube(b, (x0 - 0.6, 11.65, 2.6), (1.2, 1.2, 0.5), solid(STEEL[3]), density=4, tag="pommel")
    m.cube(b, (x0 - 0.6, 11.5, -2.1), (1.2, 1.5, 0.5), solid(STEEL[2]), density=4, tag="bolster")

    def blade(f, x, y, w, h):
        # Side faces (east/west) carry the shape: w = length (z), h = height (y). Spine at the top, edge below.
        if f in ("east", "west"):
            z = x if f == "west" else w - 1 - x   # 0 at the bolster .. w-1 at the point
            t = z / max(1, w - 1)
            height = h * (0.62 + 0.38 * min(1.0, t * 1.6))
            if t > 0.86:
                height *= max(0.0, 1 - (t - 0.86) / 0.14 * 0.85)  # the clipped point
            top = 0
            if y < top or y >= top + height:
                return CLEAR
            if y >= top + height - 1.5:
                return STEEL[5]   # the honed edge
            if y < 1:
                return STEEL[2]   # the spine
            n = fbm(x, y, 7401, 64, 64, 2, 8.0)
            return STEEL[3.0 + (n - 0.5) * 1.4] if n < 0.7 else mix(STEEL[3], hexc("#6b4a2e"), 0.35)  # a little rust
        return STEEL[3]
    m.cube(b, (x0 - 0.15, 10.4, -12.1), (0.3, 2.6, 10.0), blade, density=4, tag="blade")


def build_flask(m):
    """A steel hip flask of holy water in the left fist; a cross is scratched into its face."""
    x0 = AX
    b = m.bone("flask", (x0, 12.25, 0), "left_hand")

    def body(f, x, y, w, h):
        c = FLASK[3.4 - 1.0 * y / max(1, h) + (0.8 if f == "north" and x == 1 else 0)]
        if f in ("east", "west") and (abs(x - (w - 1) / 2) < 0.6 and 2 <= y <= h - 3 or (y == 4 and 1 <= x <= w - 2)):
            return FLASK[1]
        return c
    m.cube(b, (x0 - 0.5, 12.0, -3.6), (1.0, 3.6, 2.4), body, density=4, tag="flask")
    cap = m.bone("flask_cap", (x0, 15.6, -2.4), b.name)
    m.cube(cap, (x0 - 0.35, 15.6, -2.75), (0.7, 0.7, 0.7), solid(FLASK[4]), density=4, tag="flask_cap")
    # Two shells hidden inside the left fist; the reload clip brings them out.
    sh = m.bone("reload_shells", (x0, 12.5, 0), "left_hand")
    for dx in (-0.4, 0.4):
        m.cube(sh, (x0 + dx - 0.3, 12.2, -0.9), (0.6, 0.6, 1.8), lambda f, x, y, w, h: BRASS[4] if f == "south" else SHELL_RED[3],
               density=4, tag="spare_shell")


def build_stowed(m, v):
    """The stowed twins: the shotgun slung across the back, the machete in its sheath, the flask on the hip."""
    sl = m.bone("shotgun_slung", (0, 18, 2.4), "body")
    wood = leather(7501, WALNUT, 3.0)
    blued = metal(GUNMETAL, 3.0)
    # Diagonal from the left shoulder down to the right hip, barrels down.
    rot = (0, 0, 38)
    piv = (0, 18, 2.4)
    m.cube(sl, (-0.9, 11.0, 2.6), (1.8, 7.5, 0.9), blued, density=4, tag="slung_barrels", rotation=rot, pivot=piv)
    m.cube(sl, (-0.8, 18.5, 2.6), (1.6, 3.2, 1.6), lambda f, x, y, w, h: mix(STEEL[3], hexc("#6a4a7a"), 0.15), density=4,
           tag="slung_receiver", rotation=rot, pivot=piv)
    m.cube(sl, (-0.5, 21.7, 2.7), (1.0, 2.4, 1.3), wood, density=4, tag="slung_grip", rotation=rot, pivot=piv)
    # The sling across the chest, from the right hip to the left shoulder.
    m.cube(sl, (-0.4, 9.5, -2.62), (0.8, 15.0, 0.2), lambda f, x, y, w, h: LEATHER[2 + (y % 3 == 0)], density=2, tag="sling",
           rotation=(0, 0, -36), pivot=(0, 17, -2.6))
    ms = m.bone("machete_sheathed", (3.4, 11.5, 2.4), "belt")
    m.cube(ms, (3.0, 3.5, 2.0), (0.9, 8.0, 2.6), lambda f, x, y, w, h: LEATHER[1.8 + 0.8 * (f in ("east", "west")) - 0.6 * y / max(1, h)],
           density=4, tag="sheath", rotation=(16, 0, -4), pivot=(3.4, 11.5, 2.4))
    m.cube(ms, (3.0, 11.3, 2.5), (0.9, 4.0, 1.0), lambda f, x, y, w, h: GRIP_TAPE[2 + ((x + y) % 3 == 0)], density=4,
           tag="sheathed_handle", rotation=(16, 0, -4), pivot=(3.4, 11.5, 2.4))
    fb = m.bone("flask_belt", (-3.9, 11, 1.6), "belt")
    m.cube(fb, (-4.45, 8.0, 0.5), (0.9, 3.4, 2.2), lambda f, x, y, w, h: FLASK[3.2 - 0.8 * y / max(1, h)], density=4, tag="flask_belt")
    m.cube(fb, (-4.35, 11.4, 1.2), (0.7, 0.6, 0.7), solid(FLASK[4]), density=4, tag="flask_belt_cap")


# --- animations -----------------------------------------------------------------------------------------------

# When each one-shot does its thing (seconds): the shot leaves the barrels, the machete lands, the flask leaves the hand.
HIT_TIMES = {"fire": 0.05, "slash": 0.32, "throw_water": 0.45, "reload": 1.35}
GUN_LOCAL = None


def gun_local():
    """The shotgun's barrel direction in the right hand's frame (its bone has a rest tilt)."""
    import michael_art as MA
    q = MA._rot((50, 0, 0), (0, 0, -1))
    return (-q[0], q[1], q[2])


def reach(r, pose, side, target, bends=(0, -15, -30, -45, -60, -75, -90)):
    """Arm and forearm rotations that put `side`'s hand (its fingers) on world point `target`, the rest of `pose` given."""
    import michael_art as MA
    best = None
    for bend in bends:
        p = dict(pose)
        p[f"{side}_forearm"] = [bend, 0, 0]
        P = MA.to_preview(p)
        sh = MA.world_point(r, P, f"{side}_arm", r.get(f"{side}_arm").pivot)
        d = tuple(target[i] - sh[i] for i in range(3))
        p[f"{side}_arm"] = MA.aim(r, p, f"{side}_arm", (0, -1, 0), d)
        # The forearm bend tilts the hand off the arm's axis: aim the arm at the hand, then measure.
        P = MA.to_preview(p)
        hand = MA.world_point(r, P, f"{side}_fingers", r.get(f"{side}_fingers").pivot)
        err = math.dist(hand, target)
        if best is None or err < best[0]:
            best = (err, p[f"{side}_arm"], bend)
    return {f"{side}_arm": best[1], f"{side}_forearm": [best[2], 0, 0]}


def anims():
    import michael_art as MA
    m = rig(0)
    r = m.rig
    f = AnimFile()
    clips = []
    loc = gun_local()

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def wrist(pose, target):
        """The right wrist rotation pointing the shotgun along world `target`."""
        return MA.aim(r, pose, "right_hand", loc, target)

    # -- stances --
    # The low two-handed carry: the gun across the body, muzzle down and to the left of his feet, the left hand under the
    # fore-end. (`carry1` is the one-handed version the run and the machete/flask clips start from.)
    carry1 = {"right_arm": [-12, 0, 6], "right_forearm": [-28, 0, 0], "left_arm": [-4, 0, -6], "left_forearm": [-14, 0, 0]}
    carry1["right_hand"] = wrist(carry1, (0.08, -0.62, -0.78))
    low = {"right_arm": [-22, -8, 4], "right_forearm": [-48, 0, 0], "body": [2, 0, 0]}
    low["right_hand"] = wrist(low, (0.55, -0.5, -0.67))
    Pl = MA.to_preview(low)
    carry = P_(low, reach(r, low, "left", MA.world_point(r, Pl, "shotgun_barrel", (-AX, 11.9, -6.4))))
    carry["left_hand"] = [0, 0, 25]

    aim_base = {"body": [2, 16, 0], "head": [2, -16, 0], "right_arm": [-86, 14, 0], "right_forearm": [-6, 0, 0],
                "left_leg": [-14, 0, -3], "left_shin": [10, 0, 0], "right_leg": [10, 0, 4], "@root": [0, -0.4, 0],
                "coat_tail_l": [-8, 0, 0], "coat_tail_r": [6, 0, 0]}
    aim_base["right_hand"] = wrist(aim_base, (0, 0.03, -1))
    P = MA.to_preview(aim_base)
    fore_end = MA.world_point(r, P, "shotgun_barrel", (-AX, 11.8, -6.6))
    aim_pose = P_(aim_base, reach(r, aim_base, "left", fore_end))
    aim_pose["left_hand"] = [10, 0, 20]

    # idle (loop): weight on one leg, gun low in the right hand, the left thumb hooked by the belt; a slow breath, a
    # look round, the coat stirring.
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        k = math.sin(math.pi * t / 2)
        look = [0, 14, -10, 0, 0][i]
        c.key(t, P_(carry, {"body": [0.8 * abs(k), 0, 0], "@body": [0, -0.15 * abs(k), 0], "head": [2 - 2 * abs(k), look, 0],
                             "right_leg": [0, 0, 3], "left_leg": [-3, 0, -2], "left_shin": [6, 0, 0], "@root": [0.3, 0, 0],
                             "coat_tail_r": [1.5 * abs(k), 0, 0], "coat_tail_l": [-1.5 * abs(k), 0, 0],
                             "lapel_r": [-2 * abs(k), 0, 0], "lapel_l": [-2 * abs(k), 0, 0]}), E_IO if i else None)

    # walk (loop): a steady hunter's walk, the gun carried low, the left arm swinging, the coat following the legs.
    c = clip("walk", 1.0, loop=True)
    for i in range(9):
        t = i / 8
        ph = 2 * math.pi * t
        sn, cs = math.sin(ph), math.cos(ph)
        c.key(t, P_(carry, {
            "right_leg": [-28 * sn, 0, 0], "left_leg": [28 * sn, 0, 0],
            "right_shin": [36 * max(0.0, math.sin(ph + 1.3)), 0, 0], "left_shin": [36 * max(0.0, -math.sin(ph + 1.3)), 0, 0],
            "right_foot": [-10 * max(0.0, -sn), 0, 0], "left_foot": [-10 * max(0.0, sn), 0, 0],
            "body": [3 + 1.5 * abs(cs), 3 * sn, 0], "head": [-2, -3 * sn, 0],
            "@root": [0, 0.6 * abs(cs), 0],
            "coat_tail_r": [-18 * sn * 0.7 + 4, 0, 0], "coat_tail_l": [18 * sn * 0.7 + 4, 0, 0], "coat_back": [6 + 4 * abs(cs), 0, 3 * sn],
            "lapel_r": [-4 - 3 * abs(cs), 0, 0], "lapel_l": [-4 - 3 * abs(cs), 0, 0],
            "pouch_1": [0, 0, 4 * sn], "flask_belt": [0, 0, 3 * sn]}), E_IO if i else None)

    # run (loop): leaning in, long strides, the gun pumping across the body, the coat flying out behind.
    c = clip("run", 0.6, loop=True)
    for i in range(9):
        t = 0.6 * i / 8
        ph = 2 * math.pi * i / 8
        sn, cs = math.sin(ph), math.cos(ph)
        c.key(t, {
            "right_leg": [-52 * sn, 0, 0], "left_leg": [52 * sn, 0, 0],
            "right_shin": [80 * max(0.0, math.sin(ph + 1.1)), 0, 0], "left_shin": [80 * max(0.0, -math.sin(ph + 1.1)), 0, 0],
            "right_foot": [-14 * max(0.0, -sn), 0, 0], "left_foot": [-14 * max(0.0, sn), 0, 0],
            "right_arm": [-30 + 26 * sn, 0, 8], "right_forearm": [-55, 0, 0], "right_hand": [55, 0, -10],
            "left_arm": [-30 * sn - 6, 0, -8], "left_forearm": [-70, 0, 0],
            "body": [14, 5 * sn, 0], "head": [-12, -5 * sn, 0], "@root": [0, 1.1 * abs(cs) - 0.3, 0],
            "coat_tail_r": [-30 * sn * 0.6 + 26, 0, 0], "coat_tail_l": [30 * sn * 0.6 + 26, 0, 0], "coat_back": [38 + 8 * cs, 0, 6 * sn],
            "lapel_r": [-16 - 6 * abs(cs), 0, 4], "lapel_l": [-16 - 6 * abs(cs), 0, -4], "hood": [-8, 0, 0],
            "belt_tail": [-30 - 10 * cs, 0, 10 * sn], "pouch_1": [0, 0, 8 * sn], "flask_belt": [0, 0, 6 * sn]},
            E_IO if i else None)

    # aim (hold): the shotgun comes up two-handed to chest height, the left hand on the fore-end, left foot forward.
    c = clip("aim", 0.35, hold=True)
    c.key(0, carry)
    c.key(0.2, P_(aim_pose, {"right_arm": [aim_pose["right_arm"][0] - 6, aim_pose["right_arm"][1], 0]}), E_OUT)
    c.key(0.35, aim_pose, E_IO)

    # fire (once): from the aim, the double barrel kicks -- the gun climbs, the shoulders rock back, then settle.
    c = clip("fire", 0.6)
    kick = P_(aim_pose, {"right_arm": [aim_pose["right_arm"][0] - 26, aim_pose["right_arm"][1], 0],
                         "left_arm": [aim_pose["left_arm"][0] - 20, aim_pose["left_arm"][1], aim_pose["left_arm"][2]],
                         "body": [-7, 16, 0], "head": [-6, -16, 0], "@body": [0, 0, 0.9], "@root": [0, -0.4, 0.6],
                         "shotgun_hammer_r": [24, 0, 0], "shotgun_hammer_l": [24, 0, 0]})
    c.key(0, aim_pose)
    c.key(0.05, kick, E_SNAP)
    c.key(0.22, P_(aim_pose, {"right_arm": [aim_pose["right_arm"][0] - 8, aim_pose["right_arm"][1], 0], "body": [0, 16, 0],
                              "shotgun_hammer_r": [24, 0, 0], "shotgun_hammer_l": [24, 0, 0]}), E_OUT)
    c.key(0.6, P_(aim_pose, {"shotgun_hammer_r": [24, 0, 0], "shotgun_hammer_l": [24, 0, 0]}), E_IO)

    # reload (once): breaks the gun open over the left forearm, the spent shells fly, two fresh ones from the left fist,
    # snaps it shut and thumbs the hammers back.
    c = clip("reload", 1.8)
    chest = {"right_arm": [-48, 30, 18], "right_forearm": [-50, 0, 0], "body": [6, 8, 0], "head": [18, -6, 0]}
    chest["right_hand"] = wrist(chest, (0.75, -0.15, -0.65))
    Pc = MA.to_preview(chest)
    breech = MA.world_point(r, Pc, "shotgun", (-AX, 14.0, -3.0))
    belt = (2.6, 11.2, -3.6)
    hold_l = P_(chest, reach(r, chest, "left", MA.world_point(r, Pc, "shotgun_barrel", (-AX, 12.4, -6.6))))
    to_belt = P_(chest, reach(r, chest, "left", belt))
    at_breech = P_(chest, reach(r, chest, "left", (breech[0] + 0.5, breech[1] + 1.5, breech[2] + 0.5)))
    opened = {"shotgun_barrel": [42, 0, 0]}
    zero = [0.01, 0.01, 0.01]
    c.key(0, carry)
    c.key(0.25, P_(hold_l, {"%shotgun_shell_r": [1, 1, 1], "%shotgun_shell_l": [1, 1, 1]}), E_OUT)
    c.key(0.42, P_(hold_l, opened, {"%shotgun_shell_r": [1, 1, 1], "%shotgun_shell_l": [1, 1, 1]}), E_SNAP)
    c.key(0.62, P_(hold_l, opened, {"right_hand": [chest["right_hand"][0] - 25, 0, chest["right_hand"][2]],
                                    "@shotgun_shell_r": [0.6, 3.5, 4.0], "@shotgun_shell_l": [-0.6, 3.0, 4.5],
                                    "shotgun_shell_r": [-160, 30, 0], "shotgun_shell_l": [-140, -30, 0],
                                    "%shotgun_shell_r": [1, 1, 1], "%shotgun_shell_l": [1, 1, 1]}), E_OUT)
    c.key(0.66, P_(hold_l, opened, {"%shotgun_shell_r": zero, "%shotgun_shell_l": zero}))
    c.key(0.95, P_(to_belt, opened, {"%shotgun_shell_r": zero, "%shotgun_shell_l": zero, "left_hand": [20, 0, 0]}), E_IO)
    c.key(1.1, P_(to_belt, opened, {"%shotgun_shell_r": zero, "%shotgun_shell_l": zero, "left_hand": [10, 0, 0],
                                    "@reload_shells": [0, 1.4, -1.0]}), E_OUT)
    c.key(1.3, P_(at_breech, opened, {"%shotgun_shell_r": zero, "%shotgun_shell_l": zero, "left_hand": [-30, 0, 30],
                                      "@reload_shells": [0, 1.6, -1.4]}), E_IO)
    c.key(1.35, P_(at_breech, opened, {"%reload_shells": zero, "@reload_shells": [0, 1.6, -1.4], "left_hand": [-30, 0, 30]}), E_SNAP)
    c.key(1.5, P_(hold_l, {"%reload_shells": zero, "right_hand": [chest["right_hand"][0] + 12, 0, chest["right_hand"][2]],
                           "shotgun_hammer_r": [24, 0, 0], "shotgun_hammer_l": [24, 0, 0]}), E_SNAP)
    c.key(1.62, P_(hold_l, {"%reload_shells": zero}), E_OUT)
    c.key(1.8, carry, E_IO)

    # slash (once): the machete raised over the right shoulder, a diagonal cut down across (0.32), the follow-through.
    c = clip("slash", 0.7)
    c.key(0, carry1)
    c.key(0.18, {"right_arm": [-150, 24, 28], "right_forearm": [-44, 0, 0], "right_hand": [-10, 0, 0], "body": [-5, 24, 0],
                 "head": [0, -14, 0], "left_arm": [-34, 0, -16], "left_forearm": [-30, 0, 0], "right_leg": [6, 0, 0],
                 "left_leg": [-10, 0, 0], "lapel_r": [-6, 0, 0]}, E_OUT)
    c.key(0.32, {"right_arm": [-48, -42, -12], "right_forearm": [-6, 0, 0], "right_hand": [24, 0, 0], "body": [14, -26, 0],
                 "head": [-2, 14, 0], "left_arm": [-20, 0, -26], "@root": [0, -0.8, -1.6], "left_leg": [-24, 0, 0],
                 "left_shin": [20, 0, 0], "right_leg": [14, 0, 0], "coat_tail_r": [10, 0, 6], "coat_tail_l": [-14, 0, -6],
                 "lapel_r": [-10, 0, 0], "lapel_l": [-12, 0, 0]}, E_SNAP)
    c.key(0.48, {"right_arm": [-18, -52, -14], "right_forearm": [-10, 0, 0], "right_hand": [30, 0, 0], "body": [11, -30, 0],
                 "head": [0, 16, 0], "left_arm": [-14, 0, -20], "@root": [0, -0.6, -1.6], "left_leg": [-18, 0, 0],
                 "left_shin": [14, 0, 0], "right_leg": [10, 0, 0], "coat_tail_r": [6, 0, 4], "coat_tail_l": [-8, 0, -4]}, E_OUT)
    c.key(0.7, carry1, E_IO)

    # throw_water (once): the flask drawn back over the left shoulder and thrown overhand (leaves the hand at 0.45); the
    # flask bone is scaled away from the release to the end.
    c = clip("throw_water", 0.9)
    gone = {"%flask": zero}
    c.key(0, carry1)
    c.key(0.3, P_(carry1, {"left_arm": [-168, -10, -22], "left_forearm": [-62, 0, 0], "left_hand": [-20, 0, 0], "body": [-6, -24, 0],
                          "head": [0, 20, 0], "right_leg": [-12, 0, 0], "left_leg": [10, 0, 0], "@root": [0, -0.3, 0.6]}), E_OUT)
    c.key(0.45, P_(carry1, {"left_arm": [-104, 14, -8], "left_forearm": [-8, 0, 0], "left_hand": [20, 0, 0], "body": [12, 18, 0],
                           "head": [-4, -14, 0], "right_leg": [12, 0, 0], "left_leg": [-20, 0, 0], "left_shin": [14, 0, 0],
                           "@root": [0, -0.6, -1.2]}), E_SNAP)
    c.key(0.47, P_(carry1, gone, {"left_arm": [-96, 16, -8], "left_forearm": [-6, 0, 0], "left_hand": [24, 0, 0],
                                 "body": [13, 19, 0], "head": [-4, -14, 0], "right_leg": [12, 0, 0], "left_leg": [-20, 0, 0],
                                 "left_shin": [14, 0, 0], "@root": [0, -0.6, -1.2]}))
    c.key(0.62, P_(carry1, gone, {"left_arm": [-40, 20, -10], "left_forearm": [-10, 0, 0], "body": [10, 14, 0], "head": [-2, -10, 0],
                                 "left_leg": [-10, 0, 0], "@root": [0, -0.4, -1.0]}), E_OUT)
    c.key(0.9, P_(carry1, gone), E_IO)

    # die (hold): the blast hits him, he staggers back, drops to his knees and falls forward; the cap tumbles off.
    c = clip("die", 2.0, hold=True)
    c.key(0, carry)
    c.key(0.22, P_(carry, {"body": [-16, 0, 0], "head": [-24, 0, 0], "right_arm": [-34, 0, 34], "left_arm": [-30, 0, -34],
                           "@root": [0, 0, 2.0], "right_leg": [-12, 0, 0], "left_leg": [8, 0, 0], "@cap": [0, 1.5, 2.5],
                           "cap": [-30, 0, 15], "coat_tail_r": [-14, 0, 0], "coat_tail_l": [-14, 0, 0]}), E_SNAP)
    c.key(0.9, {"@root": [0, -5.5, 1.5], "right_leg": [-10, 0, 0], "left_leg": [-10, 0, 0], "right_shin": [90, 0, 0],
                "left_shin": [90, 0, 0], "body": [20, 0, 0], "head": [26, 0, 0], "right_arm": [10, 0, 12], "left_arm": [10, 0, -12],
                "right_forearm": [-20, 0, 0], "@cap": [2, -9, 9], "cap": [-80, 30, 70], "coat_tail_r": [70, 0, 0],
                "coat_tail_l": [70, 0, 0]}, E_IN)
    lying = {"@root": [0, -4.5, -4], "root": [72, 0, 0], "right_leg": [-60, 0, 0], "left_leg": [-60, 0, 0], "right_shin": [100, 0, 0],
             "left_shin": [100, 0, 0], "body": [6, 0, 0], "head": [-20, 0, 12], "right_arm": [-150, 0, 22], "left_arm": [-150, 0, -10],
             "right_forearm": [-10, 0, 0], "@cap": [2, -9, 9], "cap": [-80, 30, 70], "coat_tail_r": [-40, 0, 6], "coat_tail_l": [-40, 0, -6]}
    c.key(1.6, lying, E_IN)
    c.key(2.0, P_(lying, {"root": [74, 0, 0], "head": [-22, 0, 14]}), E_OUT)

    for cl in clips:
        cl.done()
    return f


# --- previews -------------------------------------------------------------------------------------------

def preview_hidden(clip=None):
    """What the renderer hides by default: the machete and the flask in the hands, the slung shotgun (it is in his hand);
    for the slash the machete is drawn (the shotgun slung), for the throw the flask is in the hand."""
    if clip == "slash":
        return ["shotgun", "flask", "machete_sheathed"]
    if clip == "throw_water":
        return ["machete", "shotgun_slung", "flask_belt"]
    return ["machete", "flask", "shotgun_slung"]


def counts(m):
    return len(m.rig.bones), sum(len(b.cubes) for b in m.rig.bones)


# --- output ---------------------------------------------------------------------------------------------

def textures():
    out = {}
    m = None
    for v in VARIANTS:
        m = rig(v)
        out[v] = m.build(gutter=1, seed=7000)
    return m, out


def generate():
    m, tex = textures()
    m.rig.write(GEO + "rival_hunter.geo.json")
    for v, (t, _g) in tex.items():
        save(t, "entity", f"rival_hunter_{v}")
    anims().write(ANIM + "rival_hunter.animation.json")


if __name__ == "__main__":
    generate()
