"""The Archangel Michael (v0.12), phases V-VI: his true form, a celestial general some 4.5 blocks tall (73 px at scale 1
to the crown of the helm; the crest, the halo and the lance rise above that).

  * plate armour of white and gold engraved with Enochian script (michael_art.Plate): a great helm with a brow
    ridge, cheek guards, a flared skirt, a narrow visor slit of light (`visor_light`) and a crest of feathers swept
    back; a gorget; a V-shaped cuirass (a broad breastplate with swelling pectorals, flanks drawn in to a belted
    waist, abdominal lames) and a deep backplate; great pauldrons of a dome and three lames flaring past the
    shoulders with a small feather crest; rerebraces, couters, vambraces, gauntlets with finger plates; a fauld of
    six swinging plates (`fauld_0..5`); cuisses over heavy thighs, knee cops, greaves and pointed sabatons. Under
    every plate a body of pale light (it shows, and glows, when the `plate_*` bones fall in the death clip);
  * a cape of light in five chained segments (`cape_0..4`), narrow between the wing roots, widening, fading to
    nothing at a ragged, flame-like hem;
  * SIX wings of metal feathers. Each `<wing>` (the humerus) -> `<wing>_fore` -> `<wing>_hand`; on the hand twelve
    primaries `<wing>_p00..p11` (inner to outer), on the forearm eight secondaries `<wing>_s00..s07` (outer, at the
    wrist, to inner), and four covert rows: `<wing>_m` (scapulars on the humerus), `<wing>_c0` (lesser) and
    `<wing>_c1` (greater) on the forearm, `<wing>_hc` (primary coverts) on the hand. Built fully spread in the
    frontal plane; the bones' rest rotations fold them into the standing pose;
  * a halo behind the head: a double ring of gold engraved with Enochian script and twelve long spears of light.
    `halo` (the inner ring) -> `halo_spin` (the outer ring) -> `halo_spear_00..11` (spear k points outward at k*30
    degrees clockwise from the top, as seen from the front);
  * the Lance of Michael in his right hand (`lance`), leaning out with its butt by his right foot.

Procedural bones (MichaelBones.PROCEDURAL, the renderer drives them; no clip keys them): `halo_spin`, `visor_light`,
`cape_0..4`. Phase VI: the renderer hides halo_spear_01/04/07/10; the cracked texture splits every plate with light.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body (hips) -- fauld_0..5, plate_lames
                      -- chest -- head -- visor_light, crest_0..6
                               -- plate_gorget, plate_breast, plate_back
                               -- right_arm -- plate_pauldron_r -- plate_pauldron_r_1 -- plate_pauldron_r_2
                                            -- plate_rerebrace_r
                                            -- right_forearm -- plate_vambrace_r, right_hand -- lance
                               -- left_arm ... left_hand
                               -- halo -- halo_spin -- halo_spear_*
                               -- cape_0 -- cape_1 -- cape_2 -- cape_3 -- cape_4
                               -- wings -- wing_r1, wing_l1, wing_r2, wing_l2, wing_r3, wing_l3
       -- right_leg -- plate_cuisse_r, right_shin -- plate_greave_r   (and the left)
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01, solid
from common import save
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, aim, flex as flex_, lagged, write_compact, parent_offset, ruffle,
                         to_preview, world_point, GOLD, LIGHT, STEEL, WHITE, WHITE_METAL, Gilt, MetalFeather, Plate, Under,
                         add_feather, covert_cube, delta_pose, glyph_bit, lance_parts, mirror_rot, tone)
from pixelkit import Ramp, fbm, hexc, mix, shade

A = "animation.michael_archangel."
PROCEDURAL = ("halo_spin", "visor_light", "cape_0", "cape_1", "cape_2", "cape_3", "cape_4")
BROKEN_SPEARS = (1, 4, 7, 10)

# --- proportions (px) ---------------------------------------------------------------------------------------
HIP_Y, KNEE_Y, WAIST_Y, CHEST_Y, SHOULDER_Y, NECK_Y = 36, 20, 42, 46, 57, 61
ARM_X, LEG_X = 15.5, 5.0
BACK_Z = 8.6                      # the backplate's outer face
HALO_C = (0.0, 67.0, 9.4)
HALO_R = (9.5, 11.75)             # inner and outer ring radii
SPEAR_R0, SPEAR_L, HALO_N = 12.75, 14.0, 12
LANCE_TILT = 14.5

# Wings: pair -> (root (x, y, z) of the right wing, scale, rest rotations of wing/fore/hand for the right wing)
WINGS = {
    1: ((-5.5, 55.0, BACK_Z + 0.4), 1.00, (0, 14, 44), (0, 8, -4), (0, 6, -8)),
    2: ((-5.5, 50.0, BACK_Z + 0.6), 0.86, (0, 20, 6), (0, 8, -6), (0, 6, -10)),
    3: ((-6.0, 45.0, BACK_Z + 0.8), 0.72, (0, 26, -30), (0, 8, -4), (0, 6, -6)),
}
SEG = (13.0, 21.0, 18.0)  # humerus, forearm, hand at scale 1
FAN = {1: 0.0, 2: 8.0, 3: 24.0}  # extra outward fan (degrees) so the lower wings' feathers clear the cape
PRIMARIES, SECONDARIES = 12, 8

VISOR = hexc("#e9f6ff")
CAPE_TOP, CAPE_LOW = hexc("#eef6ff"), hexc("#4f84dc")
FEATHER = Ramp("#7d8796", "#a3acb9", "#c6ccd5", "#dfe3e9", "#f0f2f5", "#ffffff")
FULL = lambda face, x, y, w, h, c: c


def P(seed, d=2, **kw):
    return Plate(seed, d, **kw)


def cube(m, bone, origin, size, mat, d=2, **kw):
    """A cube painted with a Plate/Gilt/Under (uses its .mat and .glow)."""
    return m.cube(bone, origin, size, mat.mat, mat.glow, density=d, **kw)


# =====================================================================================================
# The rig
# =====================================================================================================

def rig():
    m = Model("michael_archangel", 1024, 1024)
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 39, 0), "root")
    m.bone("chest", (0, CHEST_Y, 0), "body")
    m.bone("head", (0, NECK_Y, 0), "chest")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (14 * s, SHOULDER_Y, 0), "chest")
        m.bone(f"{side}_forearm", (ARM_X * s, 43, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (ARM_X * s, 29.5, 0), f"{side}_forearm")
        m.bone(f"{side}_leg", (LEG_X * s, HIP_Y, 0), "root")
        m.bone(f"{side}_shin", (LEG_X * s, KNEE_Y, 0), f"{side}_leg")
    build_torso(m)
    build_head(m)
    build_arms(m)
    build_legs(m)
    build_fauld(m)
    build_lance(m)
    build_halo(m)
    build_cape(m)
    build_wings(m)
    return m


def build_torso(m):
    u = Under(7101)
    # The body of light: pelvis, a narrow waist, a chest that widens to the shoulders, the neck.
    cube(m, "body", (-7.5, 33, -5), (15, 9, 10), u, 1, tag="pelvis")
    cube(m, "body", (-6.5, WAIST_Y, -4.5), (13, 5, 9), u, 1, tag="waist")
    cube(m, "chest", (-9.5, 46, -6), (19, 12, 13), u, 1, tag="torso")
    cube(m, "chest", (-2.75, 57, -2.75), (5.5, 5, 5.5), u, 1, tag="neck")
    # Belt with engraved plaques and a buckle set with a gem of light.
    cube(m, "body", (-8, 40.5, -6.6), (16, 2.5, 13.2), Gilt(7102, 2), tag="belt")
    for px, pz, rot in ((-4.5, -6.9, 0), (4.5, -6.9, 0), (-4.5, 6.6, 0), (4.5, 6.6, 0), (-8.3, 0, 90), (8.3, 0, 90)):
        cube(m, "body", (px - 1.1, 40.85, pz - 0.2), (2.2, 1.8, 0.4), Gilt(7130, 4, engrave=True), tag="belt_plaque",
             rotation=(0, rot, 0), pivot=(px, 41.7, pz))
    m.cube("body", (-1.75, 40, -7.3), (3.5, 3.5, 1), lambda f, x, y, w, h: GOLD[5] if x in (0, w - 1) or y in (0, h - 1)
           else LIGHT[4], lambda f, x, y, w, h, c: c, density=2, tag="buckle")
    # Abdominal lames: three overlapping bands between the belt and the breastplate, each edged in gold.
    lames = m.bone("plate_lames", (0, 44, -6), "body")
    for i, (w, z) in enumerate(((14.5, -7.2), (15.5, -7.6), (16.5, -8.0))):
        cube(m, lames, (-w / 2, 42.6 + i * 1.5, z), (w, 2.0, 1.2), P(7120 + i, 2), tag="lame",
             rotation=(-6, 0, 0), pivot=(0, 42.6 + i * 1.5, z))
    # Gorget: five lames round the neck, narrowing upward.
    gor = m.bone("plate_gorget", (0, 59, 0), "chest")
    for i in range(5):
        r = 7.0 - i * 0.65
        cube(m, gor, (-r, 56.6 + i * 0.95, -r), (2 * r, 1.3, 2 * r), P(7103 + i, 2, band=i % 2 == 0), tag="gorget")
    # Breastplate: broad and deep, a swelling chest plate carrying a winged sunburst, flanks drawn in to the waist.
    br = m.bone("plate_breast", (0, 52, -7), "chest")
    cube(m, br, (-11, 50.5, -9.0), (22, 8.5, 3), P(7105, 3), tag="breast_upper")
    cube(m, br, (-8.5, 45.5, -8.5), (17, 5.5, 2.5), P(7106, 3), tag="breast_lower")
    swell = dict(rotation=(-8, 0, 0), pivot=(0, 54, -9.6))
    cube(m, br, (-9.5, 50.6, -10.3), (19, 7.4, 1.3), P(7107, 3), tag="breast_swell", **swell)
    m.cube(br, (-9, 50.8, -10.45), (18, 7, 0), sunburst, sunburst_glow, density=4, faces=("north",), tag="sunburst", **swell)
    # The Enochian line under it, glowing.
    m.cube(br, (-7.5, 47.6, -8.55), (15, 1.5, 0), script_line(7140), lambda f, x, y, w, h, c: LIGHT[4], density=4,
           faces=("north",), tag="script")
    cube(m, br, (-0.9, 45.5, -9.4), (1.8, 5.0, 1.8), Gilt(7109, 2), tag="ridge", rotation=(0, 45, 0), pivot=(0, 48, -8.5))
    for s in (-1, 1):
        cube(m, br, (s * 10.5 - 1.5, 46.5, -7.0), (3, 12, 14.5), P(7111 + s, 3), tag="flank",
             rotation=(0, 0, -12 * s), pivot=(s * 10.5, 58.5, 0))
    # Backplate: deep, a framed upper plate, a spine of gold, shoulder-blade plates (the wings rise between them).
    bk = m.bone("plate_back", (0, 52, 7), "chest")
    cube(m, bk, (-11, 49.5, 6.0), (22, 9.5, BACK_Z - 6.0), P(7110, 3, motif="frame"), tag="back_upper")
    cube(m, bk, (-8.5, 45, 5.5), (17, 5, 2.5), P(7113, 3), tag="back_lower")
    cube(m, bk, (-0.9, 45, BACK_Z - 0.3), (1.8, 13.5, 1.0), Gilt(7114, 2), tag="spine")
    for s in (-1, 1):
        cube(m, bk, (s * 6 - 4, 51, BACK_Z - 0.2), (8, 7, 1), P(7115 + s, 3), tag="scapula",
             rotation=(0, 12 * s, 0), pivot=(s * 6, 54, BACK_Z))


def script_line(seed):
    def m(face, x, y, w, h):
        cy = y - (h - 5) // 2
        cx = x - 2
        if 0 <= cy < 5 and 0 <= cx and cx % 4 < 3 and cx < w - 4 and glyph_bit(seed, cx // 4, 0, cx % 4, cy):
            return LIGHT[4]
        return None
    return m


def sunburst(face, x, y, w, h):
    """A winged sun across the chest: a disc of light in a gold ring, rays, and a wing of gold feathers each side."""
    cx, cy = (w - 1) / 2, h * 0.5
    dx, dy = x - cx, y - cy
    r = math.hypot(dx, dy)
    if r < 4.5:
        return mix(WHITE, LIGHT[3], r / 4.5)
    if r < 6.2:
        return GOLD[5] if dy < 0 else GOLD[3]
    ang = math.degrees(math.atan2(dy, dx)) % 30
    if r < 12 and abs(ang - 15) < 2.2 and dy < 2:
        return GOLD[4]
    ax = abs(dx)
    if 7 <= ax <= w / 2 - 1:
        q = (ax - 7) / (w / 2 - 8)              # 0 at the sun .. 1 at the wing tip
        top = cy - 3 - 8 * q ** 0.8             # the leading edge sweeps up
        feather = int((ax - 7) // 4.5)
        tip = cy + 6 - 4 * q - (2.0 if (ax - 7) % 4.5 < 2.2 else 0)  # scalloped feather tips
        if top <= y <= tip:
            if y < top + 1.2:
                return GOLD[5]
            return GOLD[4] if feather % 2 == 0 else GOLD[3]
    return None


def sunburst_glow(face, x, y, w, h, c):
    if c[2] > c[0]:
        return c
    return shade(c, 0.45)


def build_head(m):
    head = "head"
    hp = dict(motif="plain")
    # Tapering great helm: stacked shells narrowing to the crown.
    for i, (hw, y0, hh, d0, dd) in enumerate(((11, 61, 6, -6, 12), (10, 67, 3.5, -5.7, 11.2), (8.4, 70.5, 2.5, -5.2, 10.2),
                                             (6.4, 73, 1.4, -4.5, 8.6))):
        cube(m, head, (-hw / 2, y0, d0), (hw, hh, dd), P(7201 + i, 3 if i < 2 else 2, band=i != 1), tag="helm")
    cube(m, head, (-0.8, 74.2, -4.6), (1.6, 1.2, 9.5), Gilt(7206, 2), tag="comb")
    helm = P(7210, 3)
    slit = hexc("#141a28")

    def helm_g(face, x, y, w, h, c):
        return LIGHT[1] if c == slit else helm.glow(face, x, y, w, h, c)
    # The prow: two face plates meeting in a ridge, the visor slit cut across them, a brow over it.
    for s in (-1, 1):
        x0 = -5.6 if s < 0 else 0.0

        def prow(face, x, y, w, h, s=s):
            if face == "north" and y in (14, 15, 16):
                return slit
            if face == "north" and y > 19 and (x == (w - 1 if s < 0 else 0)) and y % 3:
                return slit  # breaths down the ridge
            return helm.mat(face, x, y, w, h)
        m.cube(head, (x0, 61.0, -7.6), (5.6, 10.5, 1.2), prow, helm_g, density=3, tag="prow",
               rotation=(0, -22 * s, 0), pivot=(0, 66, -7.2))
        cube(m, head, (x0 - 0.2 * s, 68.4, -8.2), (5.8, 1.4, 1.6), Gilt(7204 + s, 2, engrave=True), tag="brow",
             rotation=(8, -22 * s, 0), pivot=(0, 68.6, -7.6))
        # Cheek guards, angled back along the jaw.
        cube(m, head, (s * 5.4 - 1.0, 60.2, -6.6), (2.0, 7.5, 7.5), P(7207 + s, 3), tag="cheek",
             rotation=(0, 28 * s, -8 * s), pivot=(s * 5.4, 67, -6.4))
        # Small wings of gold at the temples.
        for i in range(3):
            mf = MetalFeather("crest", s, 7210 + i, WHITE_METAL)
            m.cube(head, (s * 5.6 - 0.4, 68.5, -1 + i * 1.6), (0.8, 5 - i, 1.4), mf.mat, mf.glow, density=2, tag="temple_wing",
                   rotation=(-40 - i * 12, 0, -20 * s), pivot=(s * 5.6, 68.5, i * 1.6 - 0.5))
    cube(m, head, (-0.6, 61.0, -8.3), (1.2, 7.6, 1.2), Gilt(7205, 2), tag="face_ridge", rotation=(0, 45, 0), pivot=(0, 64, -7.7))
    # The skirt flaring over the gorget.
    cube(m, head, (-6.6, 59.6, -6.9), (13.2, 1.6, 13.6), P(7209, 2), tag="skirt", rotation=(-4, 0, 0), pivot=(0, 61, 0))
    # The visor slit of light (procedural: the renderer breathes it).
    vl = m.bone("visor_light", (0, 66.5, -7.6), head)
    for s in (-1, 1):
        x0 = -5.0 if s < 0 else 0.0
        m.cube(vl, (x0, 66.0, -7.8), (5.0, 0.75, 0.25), lambda f, x, y, w, h: (VISOR if y == 1 else LIGHT[3]) if f == "north"
               else LIGHT[2], FULL, density=4, tag="visor", rotation=(0, -22 * s, 0), pivot=(0, 66, -7.2))
    # Crest: seven white feathers tipped gold, rising from the comb and sweeping back, fanned a little to the sides.
    for i in range(7):
        z = -4.2 + i * 1.5
        b = m.bone(f"crest_{i}", (0, 75.2, z), head, rotation=(-22 - i * 10, 0, (6 + i) * (1 if i % 2 else -1)))
        L = 8 + 7 * math.sin(math.pi * (i + 1.5) / 8.5)
        mf = MetalFeather("crest", -1 if i % 2 else 1, 7220 + i, WHITE_METAL)
        m.cube(b, (-1.3, 75.2, z - 0.25), (2.6, L, 0.5), mf.mat, mf.glow, density=3, tag="crest")
        m.cube(b, (-0.25, 75.2, z - 0.1), (0.5, L * 0.9, 0.5), solid(WHITE_METAL[5]), lambda *a: WHITE_METAL[3], density=2,
               tag="crest_shaft")


def build_arms(m):
    u = Under(7301)
    for side, s in (("right", -1), ("left", 1)):
        sd = side[0]
        cx = ARM_X * s
        arm, fore, hand = f"{side}_arm", f"{side}_forearm", f"{side}_hand"
        cube(m, arm, (cx - 3.4, 43, -3.4), (6.8, 14, 6.8), u, 1, tag="upper_arm")
        cube(m, fore, (cx - 3.0, 30, -3.0), (6.0, 13, 6.0), u, 1, tag="forearm")
        # Rerebrace with a lame above and below.
        rb = m.bone(f"plate_rerebrace_{sd}", (cx, 50, 0), arm)
        cube(m, rb, (cx - 3.8, 45.5, -3.8), (7.6, 7.5, 7.6), P(7330 + s, 3, motif="ridge"), tag="rerebrace")
        for k, y0 in enumerate((44.0, 53.0)):
            cube(m, rb, (cx - 4.0, y0, -4.0), (8.0, 1.6, 8.0), P(7333 + k, 2), tag="rerebrace_lame")
        # Great pauldron: a scalloped dome with a crest of feathers, three lames fanning out and down beneath it.
        p0 = m.bone(f"plate_pauldron_{sd}", (cx, 58, 0), arm)
        rot = (0, 0, -16 * s)
        cube(m, p0, (cx - 6.5, 55.5, -6.5), (13, 6, 13), P(7302 + s, 3, scallop=True), tag="pauldron", rotation=rot,
             pivot=(cx, 58, 0))
        cube(m, p0, (cx - 5.0, 61.3, -5.0), (10, 1.2, 10), Gilt(7304, 2), tag="pauldron_cap", rotation=rot, pivot=(cx, 58, 0))
        for i in range(5):
            mf = MetalFeather("crest", s, 7340 + i, WHITE_METAL)
            px = cx + s * (0.5 + i * 1.3)
            m.cube(p0, (px - 0.8, 62.2, -2.4 + i * 1.1), (1.6, 5.5 - i * 0.6, 0.5), mf.mat, mf.glow, density=3,
                   tag="pauldron_feather", rotation=(-38, 0, -s * (22 + i * 13) - 16 * s), pivot=(px, 62.2, i * 1.1 - 2.4))
        prev = p0.name
        for k in (1, 2, 3):
            if k < 3:
                pk = m.bone(f"plate_pauldron_{sd}_{k}", (cx + s * 1.6 * k, 56.5 - 2.6 * (k - 1), 0), prev)
                prev = pk.name
            y0 = 52.8 - 2.6 * (k - 1)
            w, d = 11.0 + k * 0.8, 12.6 + k * 0.5
            px = cx + s * 1.7 * k
            cube(m, prev, (px - w / 2, y0, -d / 2), (w, 3.4, d), P(7305 + k, 2, scallop=True), tag="pauldron_lame",
                 rotation=(0, 0, -(14 + 9 * k) * s), pivot=(px, y0 + 3.4, 0))
        # Vambrace with a ridge, couter with a rosette and a fan on its outer side.
        vb = m.bone(f"plate_vambrace_{sd}", (cx, 37, 0), fore)
        cube(m, vb, (cx - 3.5, 31, -3.5), (7, 10, 7), P(7310 + s, 3, motif="ridge"), tag="vambrace")
        cube(m, vb, (cx - 4.0, 40.5, -4.0), (8, 4.5, 8), P(7312, 3, motif="rosette"), tag="couter")
        cube(m, vb, (cx + s * 4.0 - 0.5, 39.5, -3.0), (1, 6, 6), Gilt(7313, 2), tag="couter_fan",
             rotation=(45, 0, 0), pivot=(cx + s * 4.0, 42.5, 0))
        # Gauntlet: a flared cuff, the back of the hand with knuckle plates, finger plates curled in, the thumb.
        cube(m, hand, (cx - 4.0, 27.0, -4.0), (8, 3.5, 8), P(7314, 2), tag="cuff")
        cube(m, hand, (cx - 3.2, 23.5, -3.2), (6.4, 4, 6.4), P(7315, 2), tag="gauntlet")
        for i in range(4):
            fz = -2.8 + i * 1.6
            cube(m, hand, (cx - s * 3.4 - 0.7, 21.5, fz - 0.7), (1.4, 3.0, 1.4), Gilt(7320 + i, 2), tag="finger")
            cube(m, hand, (cx + s * 3.25 - 0.5, 24.0, fz - 0.6), (1.0, 1.2, 1.2), Gilt(7326 + i, 2), tag="knuckle")
        cube(m, hand, (cx - s * 1.0 - 0.8, 23.0, -4.2), (1.6, 3.0, 1.6), Gilt(7325, 2), tag="thumb")


def build_legs(m):
    u = Under(7401)
    for side, s in (("right", -1), ("left", 1)):
        sd = side[0]
        cx = LEG_X * s
        leg, shin = f"{side}_leg", f"{side}_shin"
        cube(m, leg, (cx - 4.2, KNEE_Y, -4.2), (8.4, HIP_Y - KNEE_Y + 1, 8.4), u, 1, tag="thigh")
        cube(m, shin, (cx - 3.4, 4, -3.4), (6.8, KNEE_Y - 4, 6.8), u, 1, tag="calf")
        cu = m.bone(f"plate_cuisse_{sd}", (cx, 30, -4.5), leg)
        cube(m, cu, (cx - 4.8, 23, -5.0), (9.6, 12, 5.5), P(7402 + s, 3, motif="engrave"), tag="cuisse")
        cube(m, cu, (cx - 3.4, 18.5, -6.2), (6.8, 5.5, 3), P(7404, 3, motif="rosette"), tag="knee_cop")
        cube(m, cu, (cx + s * 3.4 - 0.5, 18.0, -5.5), (1, 6, 6), Gilt(7405, 2), tag="knee_fan",
             rotation=(45, 0, 0), pivot=(cx + s * 3.4, 21, -2.5))
        gr = m.bone(f"plate_greave_{sd}", (cx, 12, 0), shin)
        cube(m, gr, (cx - 4.0, 4.5, -4.0), (8, 14, 8), P(7406 + s, 3, motif="ridge"), tag="greave")
        # Sabaton: ankle lame, the foot, three lames down the instep, a long pointed toe.
        cube(m, shin, (cx - 4.2, 3.0, -4.4), (8.4, 2.2, 8.8), P(7409, 2), tag="ankle")
        cube(m, shin, (cx - 3.8, 0, -6.5), (7.6, 3.4, 10.5), P(7410 + s, 2), tag="sabaton")
        for i in range(3):
            w = 7.4 - 0.7 * i
            cube(m, shin, (cx - w / 2, 3.0 - 0.45 * i, -7.4 - 1.5 * i), (w, 0.9, 2.2), P(7414 + i, 2), tag="sabaton_lame",
                 rotation=(14, 0, 0), pivot=(cx, 3.0 - 0.45 * i, -6.4 - 1.5 * i))
        cube(m, shin, (cx - 2.6, 0, -10), (5.2, 2.6, 4), P(7412, 2), tag="toe")
        cube(m, shin, (cx - 1.3, 0, -12.5), (2.6, 1.6, 3.0), Gilt(7413, 2), tag="toe_point")


PTERUGES = Ramp("#9a917c", "#b9b09a", "#d2cab4", "#e6dfcb", "#f4efe1", "#fffbf0")


def pteryx(face, x, y, w, h):
    if face in ("up",):
        return PTERUGES[3]
    if y >= h - 2:
        return GOLD[4] if y == h - 2 else GOLD[2]  # a gold-tipped fringe
    return tone(PTERUGES, 4.0 - 1.6 * y / max(1, h) - (0.5 if face in ("east", "west", "south") else 0))


def build_fauld(m):
    """Six plates hanging from the belt: front pair, a side each, back pair. Each swings on its own bone, with a lower
    lame overlapping it and three pteruges (strips of white linen tipped gold) hanging from under it."""
    specs = [  # (x, z, rest rotation, width, kind)
        (-3.9, -7.0, (-10, 0, 4), 8, "front"), (3.9, -7.0, (-10, 0, -4), 8, "front"),
        (-8.8, 0.0, (0, 0, 14), 10, "side"), (8.8, 0.0, (0, 0, -14), 10, "side"),
        (-3.9, 6.6, (10, 0, 4), 8, "back"), (3.9, 6.6, (10, 0, -4), 8, "back"),
    ]
    for i, (x, z, rot, w, kind) in enumerate(specs):
        top = WAIST_Y - 1.0
        b = m.bone(f"fauld_{i}", (x, top, z), "body", rotation=rot)
        L1, L2, Lp = 8.0, 6.5, 16.0
        mo = "engrave" if kind == "front" else "plain"
        if kind == "side":
            inner = -0.7 if x > 0 else 0.7
            cube(m, b, (x - 0.6, top - L1, z - w / 2), (1.2, L1, w), P(7501 + i, 2, motif=mo), tag="fauld")
            cube(m, b, (x - 0.6 + (0.5 if x > 0 else -0.5), top - L1 - L2 + 1, z - w / 2 + 0.5), (1.2, L2, w - 1),
                 P(7511 + i, 2), tag="fauld_lame")
            for k in range(3):
                pz = z - w / 2 + 1.6 + k * (w - 3.2) / 2
                m.cube(b, (x + inner - 0.2, top - Lp, pz - 0.8), (0.4, Lp, 1.6), pteryx, H_GLOW, density=2, tag="pteryx")
        else:
            dz = -0.5 if kind == "front" else 0.5
            cube(m, b, (x - w / 2, top - L1, z - 0.6), (w, L1, 1.2), P(7501 + i, 3, motif=mo), tag="fauld")
            cube(m, b, (x - w / 2 + 0.5, top - L1 - L2 + 1, z - 0.6 + dz), (w - 1, L2, 1.2), P(7511 + i, 2), tag="fauld_lame")
            inner = 0.9 if kind == "front" else -0.9
            for k in range(3):
                px = x - w / 2 + 1.4 + k * (w - 2.8) / 2
                m.cube(b, (px - 0.8, top - Lp, z + inner - 0.2), (1.6, Lp, 0.4), pteryx, H_GLOW, density=2, tag="pteryx")


H_GLOW = lambda face, x, y, w, h, c: shade(c, 0.12)


def build_lance(m):
    # The fist holds it at the grip; it leans out to his right, the butt by his right foot (see LANCE_TILT).
    grip = (-ARM_X, 25.5, -0.5)
    b = m.bone("lance", grip, "right_hand", rotation=(0, 0, -LANCE_TILT))
    length = 90.0
    lance_parts(m, b, (grip[0], grip[1] - 25.2, grip[2]), length, 1.8, grip=25.2 / length)


def build_halo(m):
    cx, cy, cz = HALO_C
    halo = m.bone("halo", HALO_C, "chest")
    spin = m.bone("halo_spin", HALO_C, halo.name)
    for ring, (R, bone, n, th) in enumerate(((HALO_R[0], halo, 30, 1.0), (HALO_R[1], spin, 36, 1.25))):
        L = 2 * math.pi * R / n + 0.35

        def ring_m(f, x, y, w, h, ring=ring):
            if f in ("north", "south") and 1 <= y < h - 1 and 1 <= x < w - 1 and h >= 5:
                gx, cxx = divmod(x - 1, 4)
                if cxx < 3 and 0 <= y - 1 < 5 and y - 1 < h - 2 and glyph_bit(7600 + ring, gx, 0, cxx, min(4, y - 1)):
                    return LIGHT[4]
            return tone(GOLD, 4.2 - 1.6 * y / max(1, h) - (0.8 if f in ("east", "west", "down") else 0))

        def ring_g(f, x, y, w, h, c):
            return c if c == LIGHT[4] else shade(c, 0.45)
        for k in range(n):
            m.cube(bone, (cx - L / 2, cy + R - th / 2, cz - 0.5), (L, th, 1), ring_m, ring_g, density=4, tag="halo_ring",
                   rotation=(0, 0, k * 360 / n), pivot=(cx, cy, cz))
    for k in range(HALO_N):
        a = k * 360 / HALO_N
        px, py = cx + SPEAR_R0 * math.sin(math.radians(a)), cy + SPEAR_R0 * math.cos(math.radians(a))
        b = m.bone(f"halo_spear_{k:02d}", (px, py, cz), spin.name, rotation=(0, 0, a))
        build_spear(m, b, (px, py, cz), SPEAR_L)


def spear_leaf(face, x, y, w, h):
    p = 1 - y / max(1, h - 1)
    half = (w - 1) / 2 * (math.sin(math.pi * (0.2 + 0.8 * p)) ** 0.9) + 0.3
    dx = abs(x - (w - 1) / 2)
    if dx > half:
        return None
    return hexc("#ffffff") if dx < 0.8 else mix(LIGHT[5], LIGHT[3], dx / max(0.5, half))


def build_spear(m, b, base, length):
    """A spear of light along +Y from `base`: a slender shaft, a collar, and a leaf head of two crossed planes (it reads
    from the side too). Also the light_spear projectile."""
    x, y, z = base
    shaft = length * 0.58
    m.cube(b, (x - 0.25, y, z - 0.25), (0.5, shaft, 0.5), lambda f, xx, yy, w, h: mix(LIGHT[3], LIGHT[5], yy / max(1, h)), FULL,
           density=4, tag="spear_shaft")
    m.cube(b, (x - 0.6, y + shaft - 0.5, z - 0.6), (1.2, 0.75, 1.2), solid(GOLD[5]), FULL, density=4, tag="spear_collar")
    hl = length - shaft + 0.25
    m.cube(b, (x - 1.4, y + shaft, z), (2.8, hl, 0), spear_leaf, FULL, density=4, faces=("north", "south"), tag="spear_head")
    m.cube(b, (x, y + shaft, z - 1.4), (0, hl, 2.8), spear_leaf, FULL, density=4, faces=("east", "west"), tag="spear_head")


def cape_mat(seg, w_px):
    """Light: bright pale blue at the shoulders fading to nothing at a ragged, flame-like hem; soft vertical folds."""
    def m(face, x, y, w, h):
        t = (seg + y / max(1, h - 1)) / 5.0
        # Flame tongues: the hem's cut-off rises and falls along the width.
        if seg >= 3:
            n = fbm(x * 1.0, 0, 7790 + seg, 64, 8, 3, 8.0)
            cut = (0.62 if seg == 4 else 1.4) + 0.35 * n + 0.12 * math.sin(x * 0.9)
            if y / max(1, h - 1) > cut:
                return None
        fold = math.sin(x * 2 * math.pi / 7.0 + seg * 0.8)
        col = mix(CAPE_TOP, CAPE_LOW, min(1.0, t * 1.15))
        col = mix(col, (255, 255, 255, 255), max(0.0, fold) * 0.25 * (1 - t)) if fold > 0 else mix(col, LIGHT[0], -fold * 0.25)
        a = int(235 * (1 - t) ** 0.7 + 20)
        if face in ("north", "south") and (x == 0 or x == w - 1) and seg < 3:
            col = GOLD[4]
        return (col[0], col[1], col[2], max(20, min(240, a)))

    def g(face, x, y, w, h, c):
        return (int(c[0] * 0.85), int(c[1] * 0.85), int(c[2] * 0.85), c[3])
    return m, g


def build_cape(m):
    widths = (8, 9, 15, 19, 23)
    prev, y, z = "chest", 59.0, BACK_Z + 0.2
    L = 11.5
    for i, w in enumerate(widths):
        b = m.bone(f"cape_{i}", (0, y, z), prev, rotation=(5, 0, 0) if i == 0 else (2.5, 0, 0))
        cm, cg = cape_mat(i, w)
        m.cube(b, (-w / 2, y - L - 0.3, z), (w, L + 0.3, 0), cm, cg, density=2, faces=("north", "south"), tag="cape")
        prev, y = b.name, y - L


def build_wings(m):
    grp = m.bone("wings", (0, 50, BACK_Z), "chest")
    for pair, (root_r, k, r_wing, r_fore, r_hand) in WINGS.items():
        for side, s in (("r", -1), ("l", 1)):
            build_wing(m, grp.name, f"wing_{side}{pair}", (abs(root_r[0]) * s, root_r[1], root_r[2]), s, k, r_wing, r_fore,
                       r_hand, pair)


def build_wing(m, parent, name, root, s, k, r_wing, r_fore, r_hand, pair):
    Lh, Lf, Lw = (v * k for v in SEG)
    x0, y0, z0 = root
    E = (x0 + s * Lh, y0, z0)
    W = (E[0] + s * Lf, y0, z0)
    T = (W[0] + s * Lw, y0, z0)
    wing = m.bone(name, root, parent, rotation=mirror_rot(r_wing, s))
    fore = m.bone(f"{name}_fore", E, wing.name, rotation=mirror_rot(r_fore, s))
    hand = m.bone(f"{name}_hand", W, fore.name, rotation=mirror_rot(r_hand, s))
    seed = 8000 + pair * 300 + (0 if s < 0 else 150)
    fan = FAN[pair]

    def bar(bone, a, b_, hgt, depth, tag, sd):
        xa, xb = sorted((a[0], b_[0]))
        cube(m, bone, (xa, y0 - hgt / 2, z0 - 0.9), (xb - xa, hgt, depth), P(sd, 2, rivets=False, engrave=hgt > 3), tag=tag)
        cube(m, bone, (xa, y0 + hgt / 2 - 0.4, z0 - 1.0), (xb - xa, 0.8, depth + 0.2), Gilt(sd + 1, 2), tag="wing_edge")
    # The leading edge: armoured bones of the wing, rimmed in gold.
    bar(wing, root, E, 4.0 * k, 3.4, "wing_bone", seed)
    bar(fore, E, W, 3.4 * k, 3.0, "wing_bone", seed + 2)
    bar(hand, W, T, 2.8 * k, 2.6, "wing_bone", seed + 4)
    cube(m, hand, (T[0] - (2.4 * k if s > 0 else 0), y0 - 1.4 * k, z0 - 0.6), (2.4 * k, 2.8 * k, 2.2), Gilt(seed + 6, 2),
         tag="wing_claw", rotation=(0, 0, -s * 30), pivot=T)
    # Scale plates overlapping along the top of each bone, like a bird's marginal coverts in metal.
    for bone, a, b_, hgt in ((wing, root, E, 4.0 * k), (fore, E, W, 3.4 * k), (hand, W, T, 2.8 * k)):
        for q in range(4):
            px = a[0] + (b_[0] - a[0]) * (q + 0.5) / 4
            sw = abs(b_[0] - a[0]) / 4 + 0.6
            cube(m, bone, (px - sw / 2, y0 + hgt / 2 - 0.2, z0 + 0.4), (sw, 1.4 * k, 2.4), (Gilt if q % 2 else P)(seed + 90 + q, 2),
                 tag="wing_scale", rotation=(0, 0, -s * 8), pivot=(px, y0 + hgt / 2, z0 + 1.6))

    def shaft(bone, root_, L):
        """A raised shaft down the dorsal side of a flight feather."""
        m.cube(bone, (root_[0] - 0.25, root_[1] - L * 0.7, root_[2] + 0.5), (0.5, L * 0.7, 0.3),
               lambda f, x, y, w, h: tone(STEEL, 4.2 - 1.6 * y / max(1, h)), lambda f, x, y, w, h, c: shade(c, 0.2), density=2,
               tag="rachis")

    def var(i, salt):
        return h01("w", seed, salt, i) - 0.5
    # Primaries on the hand: inner (near the wrist) to outer, fanning from down to along the arm.
    for i in range(PRIMARIES):
        f = i / (PRIMARIES - 1)
        t = 0.04 + 0.96 * f ** 0.9
        px = W[0] + s * Lw * t
        L = (27 + 15 * f) * k * (0.93 if i == PRIMARIES - 1 else 1.0) * (1 + 0.07 * var(i, "l"))
        a = 14 + 72 * f ** 1.1 + 4 * var(i, "a") + fan * (1 - f)
        mf = MetalFeather("primary", s, seed + 10 + i)
        # Each further out sits a little further back, cupped back and twisted so its outer edge tucks under the next.
        r_ = (px, y0 - 0.5, z0 + 0.3 + 0.28 * i)
        fb = add_feather(m, hand, f"{name}_p{i:02d}", r_, L, 6.6 * k, a, s, mf.mat, mf.glow, tag="primary",
                         cup=3 + 9 * f, twist=6 + 16 * f)
        shaft(fb, r_, L)
    # Secondaries on the forearm: s00 at the wrist to s07 by the elbow.
    for j in range(SECONDARIES):
        f = j / (SECONDARIES - 1)
        t = 1.0 - 0.92 * f
        px = E[0] + s * Lf * t
        mf = MetalFeather("secondary", s, seed + 30 + j)
        r_ = (px, y0 - 0.5, z0 + 0.25 - 0.2 * j)
        L = (21 - 3 * f) * k * (1 + 0.08 * var(j, "sl"))
        fb = add_feather(m, fore, f"{name}_s{j:02d}", r_, L, 6.2 * k, 9 - 11 * f + 3 * var(j, "sa") + fan, s, mf.mat, mf.glow,
                         tag="secondary", cup=4 - 2 * f, twist=8)
        shaft(fb, r_, L)
    # Covert rows (each a bone holding its feathers), overlapping like scales and tilted a little off the wing.
    c1 = m.bone(f"{name}_c1", E, fore.name)
    c0 = m.bone(f"{name}_c0", E, fore.name)
    for j in range(9):
        f = j / 8
        px = E[0] + s * Lf * (1.0 - 0.95 * f)
        mf = MetalFeather("covert", s, seed + 50 + j)
        covert_cube(m, c1, (px, y0 - 1.2, z0 + 1.2), 11.5 * k, 4.2 * k, 12 - 12 * f + fan, s, mf.mat, mf.glow, tilt=6)
        mf0 = MetalFeather("covert", s, seed + 60 + j, WHITE_METAL)
        covert_cube(m, c0, (px + s * 0.8, y0 + 0.4, z0 + 1.9), 6.5 * k, 3.8 * k, 16 - 10 * f + fan, s, mf0.mat, mf0.glow, tilt=8)
    hc = m.bone(f"{name}_hc", W, hand.name)
    for i in range(7):
        f = i / 6
        px = W[0] + s * Lw * (0.04 + 0.86 * f)
        mf = MetalFeather("covert", s, seed + 70 + i)
        covert_cube(m, hc, (px, y0 - 0.6, z0 + 1.3), (12 + 2 * f) * k, 3.8 * k, 18 + 62 * f, s, mf.mat, mf.glow, tilt=6)
    for i in range(3):  # the alula: three small feathers at the wrist, pointing forward along the edge
        mf = MetalFeather("covert", s, seed + 77 + i, WHITE_METAL)
        covert_cube(m, hc, (W[0] + s * i * 0.8, y0 + 0.6, z0 + 2.2), (7 - i) * k, 2.6 * k, 95 + 6 * i, s, mf.mat, mf.glow, tilt=10)
    sc = m.bone(f"{name}_m", root, wing.name)
    for i in range(6):
        f = i / 5
        px = x0 + s * Lh * (0.08 + 0.88 * f)
        mf = MetalFeather("covert", s, seed + 80 + i, WHITE_METAL)
        covert_cube(m, sc, (px, y0 - 1.0, z0 + 1.1), (11 + 4 * f) * k, 4.2 * k, -2 + 10 * f + fan, s, mf.mat, mf.glow, tilt=5)


# =====================================================================================================
# Animations (phase A/B: a minimal idle), poses and textures
# =====================================================================================================

def wing_names():
    return [f"wing_{sd}{p}" for p in (1, 2, 3) for sd in ("r", "l")]


SEGS = [w + x for w in [f"wing_{sd}{p}" for p in (1, 2, 3) for sd in ("r", "l")] for x in ("", "_fore", "_hand")]
FLIGHT_BONES = [f"wing_{sd}{p}_{k}{i:02d}" for p in (1, 2, 3) for sd in ("r", "l") for k, n in (("p", PRIMARIES), ("s", SECONDARIES))
                for i in range(n)]
PRIM_BONES = [f"wing_{sd}{p}_p{i:02d}" for p in (1, 2, 3) for sd in ("r", "l") for i in range(PRIMARIES)]
ROWS = [f"wing_{sd}{p}_{r}" for p in (1, 2, 3) for sd in ("r", "l") for r in ("c0", "c1", "hc", "m")]
CRESTS = [f"crest_{i}" for i in range(7)]
FAULDS = [f"fauld_{i}" for i in range(6)]
PLATES = ["plate_pauldron_r", "plate_pauldron_l", "plate_gorget", "plate_vambrace_r", "plate_vambrace_l", "plate_breast",
          "plate_rerebrace_r", "plate_rerebrace_l", "plate_back", "plate_lames", "plate_cuisse_r", "plate_cuisse_l",
          "plate_greave_r", "plate_greave_l"]

# Wing poses: pair -> (wing, fore, hand) absolute rotations of the right wing (the left mirrors).
WP = {
    "rest": {p: (v[2], v[3], v[4]) for p, v in WINGS.items()},
    "open": {1: ((0, 10, 24), (0, 0, -4), (0, 0, -6)), 2: ((0, 16, 0), (0, 0, -4), (0, 0, -6)), 3: ((0, 22, -26), (0, 0, -4), (0, 0, -6))},
    "wide": {1: ((0, 6, 36), (0, 0, 0), (0, 0, 2)), 2: ((0, 4, 6), (0, 0, 0), (0, 0, -2)), 3: ((0, 10, -26), (0, 0, 0), (0, 0, -4))},
    "up": {1: ((0, 14, 72), (0, 0, 12), (0, 0, 16)), 2: ((0, 20, 46), (0, 0, 8), (0, 0, 12)), 3: ((0, 26, 16), (0, 0, 6), (0, 0, 8))},
    "down": {1: ((0, 4, -14), (0, 0, -10), (0, 0, -18)), 2: ((0, 10, -34), (0, 0, -10), (0, 0, -16)),
             3: ((0, 16, -54), (0, 0, -8), (0, 0, -12))},
    "fold": {1: ((0, 72, 26), (0, 24, -8), (0, 18, -8)), 2: ((0, 78, 2), (0, 24, -8), (0, 18, -8)), 3: ((0, 80, -24), (0, 22, -6), (0, 16, -6))},
    "back": {1: ((0, 58, 46), (0, 14, 0), (0, 10, 0)), 2: ((0, 62, 16), (0, 14, -4), (0, 10, -4)), 3: ((0, 66, -14), (0, 12, -4), (0, 10, -4))},
    "forward": {1: ((0, -14, 30), (0, -16, 0), (0, -10, 0)), 2: ((0, -8, 6), (0, -14, 0), (0, -10, 0)),
                3: ((0, -2, -20), (0, -12, 0), (0, -8, 0))},
    "droop": {1: ((0, 62, 10), (0, 20, -20), (0, 10, -20)), 2: ((0, 66, -16), (0, 18, -18), (0, 10, -18)),
              3: ((0, 66, -42), (0, 16, -14), (0, 10, -14))},
}


def awing(m, name, k=1.0):
    """Deltas taking all six wings to wing pose `name` (k blends from rest: 0 = rest, 1 = the pose)."""
    out = {}
    for pair, segs in WP[name].items():
        for sd, s in (("r", -1), ("l", 1)):
            for suffix, tgt in zip(("", "_fore", "_hand"), segs):
                n = f"wing_{sd}{pair}{suffix}"
                rest = m.rig.get(n).rotation
                t = mirror_rot(tgt, s)
                out[n] = [(t[i] - rest[i]) * k for i in range(3)]
    return out


def afan(m, k, lift=0.0):
    """Flight-feather deltas: k < 1 closes the fans, k > 1 opens them; `lift` turns them out of the wing's plane."""
    out = {}
    for n in FLIGHT_BONES:
        rz = m.rig.get(n).rotation[2]
        out[n] = [lift, 0, rz * (k - 1)]
    return out


def akneel(head=24):
    return {"@root": [0, -16.5, 0], "left_leg": [-82, 0, -4], "left_shin": [84, 0, 0], "right_leg": [4, 0, 6], "right_shin": [86, 0, 0],
            "body": [10, 0, 0], "chest": [10, 0, 0], "head": [head, 0, 0], "left_arm": [-20, 0, -10], "left_forearm": [-30, 0, 0]}


HIT_TIMES = {"lance_sweep_big": 0.6, "lance_thrust": 0.45, "lance_throw": 0.55, "dive": 0.9, "death": 8.0, "smite": 0.9,
             "wing_buffet": 0.68, "feather_storm": 1.2, "halo_volley": 0.6, "roar": 1.2, "emerge": 0.95}


def preview_hidden(clip):
    return []


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=PROCEDURAL))
        return clips[-1]

    def lance_at(pose, d):
        return P_(pose, {"lance": aim(r, pose, "lance", (0, 1, 0), d)})

    def wings_lag(c, fore=0.07, hand=0.14, feathers=0.2, flex=7.0):
        for w in wing_names():
            lagged(c, w + "_fore", fore)
            lagged(c, w + "_hand", hand)
        L = c.length
        flex_(c, PRIM_BONES, flex, feathers)

    # idle: breathing, a slow look; wings breathe pair by pair, feathers and crest ruffle each on its own phase.
    c = clip("idle", 5.0, loop=True)
    for i, t in enumerate((0, 1.25, 2.5, 3.75, 5.0)):
        k = math.sin(2 * math.pi * t / 5.0)
        kk = (1 - math.cos(2 * math.pi * t / 5.0)) / 2
        pose = {"chest": [-1.6 * kk, 0, 0], "@body": [0, 0.4 * kk, 0], "head": [1.5 * kk, 6 * k, 0],
                "right_arm": [-1.0 * kk, 0, 1.0], "left_arm": [-1.5 * kk, 0, -1.5 * kk]}
        for p in (1, 2, 3):
            ph = math.sin(2 * math.pi * t / 5.0 - p * 0.7)
            for sd, sg in (("r", -1), ("l", 1)):
                pose[f"wing_{sd}{p}"] = [0, -2 * sg * ph, -3.5 * sg * ph]
        c.key(t, pose, E_IO if i else None)
    for w in wing_names():
        lagged(c, w + "_fore", 0.25, 0.8)
        lagged(c, w + "_hand", 0.5, 0.8)
    ruffle(c, PRIM_BONES, 2.5, 2, 71)
    ruffle(c, ROWS, 1.5, 2, 72)
    ruffle(c, CRESTS, 3.0, 3, 73, axis=(1, 0, 0.5))
    ruffle(c, FAULDS, 1.0, 1, 74)

    # walk: heavy, deliberate; the fauld swings with the thighs.
    c = clip("walk", 1.8, loop=True)
    for i in range(9):
        t = 1.8 * i / 8
        ph = 2 * math.pi * i / 8
        sn = math.sin(ph)
        pose = {"right_leg": [-22 * sn, 0, 0], "left_leg": [22 * sn, 0, 0],
                "right_shin": [36 * max(0, math.sin(ph + 1.3)), 0, 0], "left_shin": [36 * max(0, -math.sin(ph + 1.3)), 0, 0],
                "left_arm": [-12 * sn, 0, -3], "right_arm": [5 * sn, 0, 2], "left_forearm": [-10, 0, 0],
                "body": [2, 4 * sn, 0], "chest": [0, -3 * sn, 0], "@root": [0, -0.7 * abs(math.cos(ph)), 0],
                "fauld_0": [-11 * sn, 0, 0], "fauld_1": [11 * sn, 0, 0], "fauld_4": [6 * sn, 0, 0], "fauld_5": [-6 * sn, 0, 0],
                "fauld_2": [0, 0, 3 * abs(sn)], "fauld_3": [0, 0, -3 * abs(sn)]}
        for p in (1, 2, 3):
            for sd, sg in (("r", -1), ("l", 1)):
                pose[f"wing_{sd}{p}"] = [0, 0, -2.5 * sg * abs(math.cos(ph))]
        c.key(t, pose, E_IO if i else None)
    ruffle(c, PRIM_BONES, 2.0, 2, 75)

    # emerge: born out of the light, kneeling and folded; he rises, the wings burst open, the halo flares.
    c = clip("emerge", 2.6)
    start = P_(akneel(30), awing(m, "fold"), afan(m, 0.4), {"%halo": [0.01, 0.01, 0.01], "%root": [0.85, 0.85, 0.85]})
    c.key(0, start)
    c.key(0.45, P_(akneel(16), awing(m, "back"), afan(m, 0.6), {"%halo": [0.2, 0.2, 0.2], "%root": [0.95, 0.95, 0.95]}), E_OUT)
    c.key(0.95, P_({"chest": [-12, 0, 0], "head": [-18, 0, 0], "right_arm": [-14, 0, 22], "left_arm": [-30, 0, -50]},
                   awing(m, "wide"), afan(m, 1.25), {"%halo": [1.2, 1.2, 1.2]}), E_BACK)
    c.key(1.7, P_({"chest": [-4, 0, 0]}, awing(m, "open", 0.5), {"%halo": [1, 1, 1]}), E_IO)
    c.key(2.6, {"%halo": [1, 1, 1]}, E_IO)
    wings_lag(c, flex=10)

    # transition (to phase VI): the halo breaks -- he reels, hunches over the pain, then rises roaring.
    c = clip("transition", 3.0)
    c.key(0, {})
    c.key(0.25, P_({"chest": [-14, 0, 0], "head": [-24, 0, 6], "@root": [0, 0, 2], "%halo": [1.3, 1.3, 1.3], "left_arm": [-20, 0, -40]},
                   awing(m, "up", 0.5)), E_SNAP)
    c.key(1.2, P_({"chest": [20, 0, 0], "body": [8, 0, 0], "head": [24, 0, 0], "left_arm": [-50, 0, -10], "left_forearm": [-60, 0, 0],
                   "%halo": [0.9, 0.9, 0.9], "@root": [0, -2, 1]}, awing(m, "fold", 0.6), afan(m, 0.6)), E_OUT)
    roar_p = P_({"chest": [-16, 0, 0], "head": [-32, 0, 0], "left_arm": [-6, 0, -55], "right_arm": [0, 0, 30], "%halo": [1.1, 1.1, 1.1]},
                awing(m, "wide"), afan(m, 1.3))
    c.key(2.1, roar_p, E_BACK)
    c.key(3.0, {"%halo": [1, 1, 1]}, E_IO)
    c.layer("halo", lambda t: [0, 0, 6 * math.sin(t * 60) * (1 if t < 1.0 else 0)])
    c.layer("chest", lambda t: [1.2 * math.sin(t * 75) * (1 if 1.9 < t < 2.6 else 0), 0, 0])
    wings_lag(c)

    # lance_sweep_big: the lance swung level in a great arc (hit 0.6), the wings counter the turn.
    c = clip("lance_sweep_big", 1.6)
    s0 = P_({"right_arm": [-84, 60, 58], "right_forearm": [-20, 0, 0], "chest": [0, 36, 0], "body": [0, 12, 0], "head": [0, -20, 0],
             "left_arm": [-34, 0, -34], "@root": [0, -2, 0], "right_leg": [-10, 0, 0], "left_leg": [10, 0, 0]}, awing(m, "back", 0.5))
    s1 = P_({"right_arm": [-84, -36, 0], "right_forearm": [-6, 0, 0], "chest": [6, -26, 0], "body": [2, -10, 0], "head": [0, 14, 0],
             "left_arm": [-10, 0, -44], "@root": [0, -2.6, 0], "right_leg": [-14, 0, 0], "left_leg": [14, 0, 0]}, awing(m, "forward", 0.4))
    s2 = P_({"right_arm": [-72, -80, -14], "right_forearm": [-4, 0, 0], "chest": [8, -44, 0], "body": [2, -14, 0], "head": [0, 24, 0],
             "left_arm": [-6, 0, -48], "@root": [0, -2.2, 0], "right_leg": [-12, 0, 0], "left_leg": [12, 0, 0]}, awing(m, "open", 0.4))
    c.key(0, {})
    c.key(0.4, lance_at(s0, (-1, 0, 0.45)), E_OUT)
    c.key(0.6, lance_at(s1, (0.3, 0, -1)), E_SNAP)
    c.key(0.95, lance_at(s2, (1, 0, -0.1)), E_OUT)
    c.key(1.6, {}, E_IO)
    wings_lag(c)

    # lance_thrust: drawn back, driven forward (hit 0.45), held, recovered.
    c = clip("lance_thrust", 1.2)
    back = P_({"right_arm": [-26, 14, 14], "right_forearm": [-64, 0, 0], "chest": [-6, 24, 0], "head": [0, -16, 0], "left_arm": [-44, 0, -24],
               "right_leg": [12, 0, 0], "left_leg": [-16, 0, 0]}, awing(m, "back", 0.6))
    lunge = P_({"right_arm": [-86, 0, 4], "right_forearm": [-4, 0, 0], "chest": [16, -10, 0], "body": [6, 0, 0], "head": [-10, 6, 0],
                "left_arm": [10, 0, -24], "left_leg": [-40, 0, 0], "left_shin": [32, 0, 0], "right_leg": [26, 0, 0], "@root": [0, -3, -6]},
               awing(m, "forward", 0.3), afan(m, 1.1, 12))
    c.key(0, {})
    c.key(0.3, lance_at(back, (0, 0.05, -1)), E_OUT)
    c.key(0.45, lance_at(lunge, (0, -0.05, -1)), E_SNAP)
    c.key(0.75, lance_at(lunge, (0, -0.08, -1)))
    c.key(1.2, {}, E_IO)
    wings_lag(c)

    # lance_throw: rear back, hurl (release 0.55), follow through; the wings swing back then forward with it.
    c = clip("lance_throw", 1.4)
    wind = P_({"right_arm": [-150, 26, 40], "right_forearm": [-55, 0, 0], "chest": [-12, 30, 0], "head": [0, -24, 0], "left_arm": [-86, 0, -18],
               "left_leg": [-24, 0, 0], "right_leg": [16, 0, 0]}, awing(m, "back", 0.6), afan(m, 0.8))
    rel = P_({"right_arm": [-104, -18, 4], "right_forearm": [-4, 0, 0], "chest": [16, -18, 0], "head": [-4, 14, 0], "left_arm": [10, 0, -22],
              "left_leg": [-30, 0, 0], "right_leg": [24, 0, 0], "@root": [0, -1.5, -3]}, awing(m, "forward", 0.6), afan(m, 1.15))
    fol = P_({"right_arm": [-40, -34, -8], "right_forearm": [-6, 0, 0], "chest": [22, -26, 0], "head": [0, 20, 0], "left_arm": [16, 0, -16],
              "left_leg": [-26, 0, 0], "right_leg": [22, 0, 0], "@root": [0, -1.5, -3]}, awing(m, "forward", 0.3))
    c.key(0, {})
    c.key(0.38, lance_at(wind, (0, 0.25, -1)), E_OUT)
    c.key(0.55, lance_at(rel, (0, -0.05, -1)), E_SNAP)
    c.key(0.85, lance_at(fol, (0, -0.4, -1)), E_OUT)
    c.key(1.4, {}, E_IO)
    wings_lag(c)

    # lance_recall: the open hand held out, the lance slams back into it (~0.75).
    c = clip("lance_recall", 1.2)
    reach = {"right_arm": [-115, 0, 32], "right_forearm": [-4, 0, 0], "head": [-8, -10, 0], "chest": [-2, 10, 0]}
    c.key(0, {})
    c.key(0.55, lance_at(reach, (0, 1, -0.3)), E_OUT)
    c.key(0.75, lance_at(P_(reach, {"right_arm": [-95, 0, 26], "chest": [6, 6, 0], "@root": [0, -1, 1.5]}), (0, 1, -0.2)), E_SNAP)
    c.key(1.2, {}, E_IO)

    # halo_volley: he points; the halo swells and looses its spears (0.6), the arm recoils.
    c = clip("halo_volley", 2.0)
    point = P_({"left_arm": [-92, 0, -4], "left_forearm": [-4, 0, 0], "head": [-6, 8, 0], "chest": [-4, 12, 0]}, awing(m, "up", 0.35))
    c.key(0, {"%halo": [1, 1, 1]})
    c.key(0.45, P_(point, {"%halo": [1.25, 1.25, 1.25]}), E_OUT)
    c.key(0.6, P_(point, {"%halo": [0.9, 0.9, 0.9], "left_arm": [-100, 0, -4], "chest": [-8, 12, 0], "@root": [0, 0, 1]}), E_SNAP)
    c.key(0.95, P_(point, {"%halo": [1.06, 1.06, 1.06]}), E_OUT)
    c.key(1.4, P_(point, {"%halo": [1, 1, 1]}), E_IO)
    c.key(2.0, {"%halo": [1, 1, 1]}, E_IO)
    wings_lag(c)

    # feather_storm: wings flung wide, every feather fanned and shivering as the steel flies.
    c = clip("feather_storm", 2.4)
    storm = P_({"chest": [-10, 0, 0], "head": [-12, 0, 0], "left_arm": [-30, 0, -70], "right_arm": [-10, 0, 30]}, awing(m, "wide"), afan(m, 1.3))
    c.key(0, {})
    c.key(0.5, P_(storm, {}), E_BACK)
    c.key(2.0, P_(storm, {"chest": [-12, 0, 0]}), E_IO)
    c.key(2.4, {}, E_IO)
    ruffle(c, FLIGHT_BONES, 12.0, 8, 76, t0=0.4, t1=2.1)
    ruffle(c, ROWS, 6.0, 10, 77, t0=0.4, t1=2.1)
    wings_lag(c, flex=4)

    # wing_buffet: drawn back and up, then a great forward stroke (0.68) with the fans spread.
    c = clip("wing_buffet", 1.6)
    c.key(0, {})
    c.key(0.45, P_({"chest": [-10, 0, 0], "head": [-8, 0, 0]}, awing(m, "back"), afan(m, 0.7)), E_OUT)
    c.key(0.68, P_({"chest": [14, 0, 0], "head": [6, 0, 0], "@root": [0, -1.5, -2]}, awing(m, "forward"), afan(m, 1.25, 18)), E_SNAP)
    c.key(1.0, P_({"chest": [8, 0, 0]}, awing(m, "open", 0.6), afan(m, 1.1, 8)), E_OUT)
    c.key(1.6, {}, E_IO)
    wings_lag(c, flex=10)

    # command: the lance raised to heaven, then levelled at the enemy; the left hand sweeps forward.
    c = clip("command", 2.0)
    up = P_({"right_arm": [-165, 0, 14], "right_forearm": [-8, 0, 0], "head": [-14, 0, 0], "chest": [-6, 0, 0], "left_arm": [-20, 0, -30]},
            awing(m, "up", 0.4))
    lvl = {"right_arm": [-98, 0, 10], "right_forearm": [-6, 0, 0], "chest": [6, 0, 0], "left_arm": [-60, 0, -40]}
    c.key(0, {})
    c.key(0.5, lance_at(up, (0.1, 1, -0.2)), E_BACK)
    c.key(1.2, lance_at(lvl, (0, 0.12, -1)), E_IO)
    c.key(1.5, lance_at(lvl, (0, 0.12, -1)))
    c.key(2.0, {}, E_IO)
    wings_lag(c)

    # smite: the left fist raised, brought down like a hammer (0.9).
    c = clip("smite", 1.8)
    c.key(0, {})
    c.key(0.6, P_({"left_arm": [-168, 0, -10], "left_forearm": [-24, 0, 0], "chest": [-12, 0, 0], "head": [-14, 0, 0]}, awing(m, "up", 0.6)), E_OUT)
    hit = P_({"left_arm": [-40, 0, 0], "left_forearm": [0, 0, 0], "chest": [20, 0, 0], "body": [6, 0, 0], "head": [10, 0, 0],
              "@root": [0, -3, -1.5], "right_leg": [-12, 0, 0], "left_leg": [-22, 0, 0], "right_shin": [22, 0, 0], "left_shin": [26, 0, 0]},
             awing(m, "down", 0.6), afan(m, 1.1))
    c.key(0.9, hit, E_SNAP)
    c.key(1.2, P_(hit, {"chest": [16, 0, 0]}))
    c.key(1.8, {}, E_IO)
    wings_lag(c)

    # ask_yes: calm, an open hand held out, head inclined; the wings lower.
    c = clip("ask_yes", 3.0)
    ask = P_({"left_arm": [-52, -12, -6], "left_forearm": [-38, 0, 0], "left_hand": [0, -60, 0], "head": [8, 0, 8], "chest": [3, -6, 0]},
             awing(m, "droop", 0.25))
    c.key(0, {})
    c.key(0.9, ask, E_IO)
    c.key(2.3, P_(ask, {"head": [10, 0, 10]}), E_IO)
    c.key(3.0, {}, E_IO)

    # roar: gathers in, then throws his head back, arms and wings flung wide, crest and feathers shaking.
    c = clip("roar", 2.5)
    c.key(0, {})
    c.key(0.45, P_({"chest": [14, 0, 0], "head": [14, 0, 0], "left_arm": [-20, 0, -6], "@root": [0, -1.5, 0]}, awing(m, "fold", 0.4), afan(m, 0.7)), E_OUT)
    big = P_({"chest": [-18, 0, 0], "head": [-34, 0, 0], "left_arm": [-4, 0, -55], "right_arm": [0, 0, 30], "@root": [0, 0.5, 0]},
             awing(m, "wide"), afan(m, 1.3))
    c.key(0.9, big, E_BACK)
    c.key(1.9, big)
    c.key(2.5, {}, E_IO)
    c.layer("chest", lambda t: [1.3 * math.sin(t * 80) * (1 if 0.9 < t < 1.9 else 0), 0, 0])
    ruffle(c, CRESTS, 8.0, 6, 78, t0=0.8, t1=2.0)
    ruffle(c, PRIM_BONES, 6.0, 5, 79, t0=0.8, t1=2.0)
    wings_lag(c)

    # stagger: struck, rocked back a step, recovers.
    c = clip("stagger", 1.5)
    c.key(0, {})
    c.key(0.15, P_({"chest": [-16, 0, 8], "head": [-20, 0, 10], "@root": [0, 0, 4], "right_arm": [-10, 0, 24], "left_arm": [-30, 0, -40]},
                   awing(m, "up", 0.5), afan(m, 1.15)), E_SNAP)
    c.key(0.55, P_({"chest": [6, 0, -4], "head": [8, 0, 0], "@root": [0, -1.5, 5], "right_leg": [20, 0, 0], "right_shin": [20, 0, 0],
                    "left_leg": [-10, 0, 0]}, awing(m, "open", 0.4)), E_OUT)
    c.key(1.5, {}, E_IO)
    wings_lag(c)

    # flight
    dangle = {"right_leg": [12, 0, 4], "left_leg": [6, 0, -4], "right_shin": [24, 0, 0], "left_shin": [18, 0, 0], "body": [6, 0, 0],
              "left_arm": [-10, 0, -14], "fauld_0": [10, 0, 0], "fauld_1": [8, 0, 0], "fauld_4": [14, 0, 0], "fauld_5": [14, 0, 0]}
    c = clip("takeoff", 1.6)
    c.key(0, {})
    c.key(0.45, P_({"@root": [0, -4, 0], "right_leg": [-34, 0, 0], "left_leg": [-34, 0, 0], "right_shin": [58, 0, 0], "left_shin": [58, 0, 0],
                    "body": [16, 0, 0], "chest": [8, 0, 0], "left_arm": [10, 0, -20]}, awing(m, "up"), afan(m, 0.85)), E_OUT)
    c.key(0.75, P_({"@root": [0, 4, 0], "right_leg": [12, 0, 0], "left_leg": [8, 0, 0], "chest": [-6, 0, 0], "left_arm": [-20, 0, -26]},
                   awing(m, "down"), afan(m, 1.15)), E_SNAP)
    c.key(1.2, P_(dangle, awing(m, "up"), afan(m, 0.95)), E_IO)
    c.key(1.6, P_(dangle, awing(m, "rest")), E_IO)
    wings_lag(c, flex=9)

    c = clip("hover", 1.2, loop=True)
    c.key(0, P_(dangle, awing(m, "up"), {"@root": [0, -1.2, 0]}))
    c.key(0.6, P_(dangle, awing(m, "down"), {"@root": [0, 1.2, 0]}), E_IO)
    c.key(1.2, P_(dangle, awing(m, "up"), {"@root": [0, -1.2, 0]}), E_IO)
    wings_lag(c, flex=9)

    c = clip("dive", 1.6)
    stoop = P_({"body": [40, 0, 0], "chest": [18, 0, 0], "head": [-40, 0, 0], "right_leg": [30, 0, 0], "left_leg": [26, 0, 0],
                "left_arm": [20, 0, -12], "right_arm": [-60, 0, 6]}, awing(m, "fold"), afan(m, 0.5))
    c.key(0, P_(dangle, awing(m, "rest")))
    c.key(0.35, lance_at(stoop, (0, -0.5, -1)), E_OUT)
    c.key(0.75, lance_at(P_(stoop, {"body": [55, 0, 0], "head": [-52, 0, 0]}), (0, -0.7, -1)), E_IO)
    impact = P_({"@root": [0, -5, -2], "body": [16, 0, 0], "chest": [12, 0, 0], "head": [6, 0, 0], "right_leg": [-40, 0, 0], "left_leg": [-18, 0, 0],
                 "right_shin": [70, 0, 0], "left_shin": [46, 0, 0], "right_arm": [-60, 0, 8], "left_arm": [-30, 0, -44]},
                awing(m, "open"), afan(m, 1.2))
    c.key(0.9, lance_at(impact, (0, -1, -0.35)), E_SNAP)
    c.key(1.15, lance_at(P_(impact, {"@root": [0, -4.6, -2]}), (0, -1, -0.35)))
    c.key(1.6, {}, E_IO)
    wings_lag(c, flex=8)

    c = clip("land", 1.2)
    c.key(0, P_(dangle, awing(m, "up")))
    c.key(0.2, P_({"@root": [0, -4, 0], "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0], "right_shin": [54, 0, 0], "left_shin": [54, 0, 0],
                   "body": [14, 0, 0], "chest": [8, 0, 0], "left_arm": [-20, 0, -24]}, awing(m, "open"), afan(m, 1.1)), E_SNAP)
    c.key(0.6, P_({"@root": [0, -1, 0], "body": [4, 0, 0]}, awing(m, "rest")), E_OUT)
    c.key(1.2, {}, E_IO)
    wings_lag(c)

    # death (8 s, held): struck, he sinks to one knee leaning on the lance; the plates fall away one by one, the
    # wings and the halo fade, and the body of light rises out of the empty armour.
    c = clip("death", 8.0, hold=True)
    kn = P_(akneel(30), {"right_arm": [-38, 0, 8], "right_forearm": [-20, 0, 0], "left_arm": [6, 0, -6], "left_forearm": [-6, 0, 0]},
            awing(m, "droop"), afan(m, 0.8))
    kn = lance_at(kn, (0, 1, -0.15))
    c.key(0, {})
    c.key(0.3, P_({"chest": [-18, 0, 0], "head": [-26, 0, 0], "@root": [0, 0, 3], "right_arm": [-10, 0, 20], "left_arm": [-20, 0, -40]},
                  awing(m, "up", 0.6), afan(m, 1.2)), E_SNAP)
    c.key(1.2, P_({"chest": [12, 0, 0], "head": [16, 0, 0], "@root": [0, -2, 2], "right_leg": [-14, 0, 0], "left_leg": [10, 0, 0],
                   "left_arm": [-20, 0, -10], "%halo": [0.4, 0.4, 0.4]}, awing(m, "droop", 0.5)), E_IO)
    c.key(2.4, P_(kn, {"%halo": [0.03, 0.03, 0.03]}), E_IN)
    pk = to_preview(kn)
    falls = {}
    for i, name in enumerate(PLATES):
        b = r.get(name)
        w0 = world_point(r, pk, name, b.pivot)
        sx = 1 if w0[0] > 0 else -1
        tgt = (w0[0] + sx * (2 + 4 * h01("px", name)), 1.2 + 1.2 * h01("py", name), w0[2] + (h01("pz", name) - 0.5) * 8)
        off = parent_offset(r, pk, name, [tgt[j] - w0[j] for j in range(3)])
        rot = [(h01("rx", name) - 0.5) * 120, (h01("ry", name) - 0.5) * 80, (h01("rz", name) - 0.5) * 140]
        falls[name] = (2.8 + 0.2 * i, off, rot)
    t_fall = sorted(set([2.4] + [round(v[0], 3) for v in falls.values()] + [round(v[0] + 0.5, 3) for v in falls.values()] + [5.6, 8.0]))
    risen = P_(kn, {"@body": [0, 16, 0], "%wings": [0.02, 0.02, 0.02], "%halo": [0.01, 0.01, 0.01]})
    pr = to_preview(risen)
    for t in t_fall[1:]:
        pose = P_(kn, {"%wings": [max(0.02, 1 - (t - 3.0) / 3.0)] * 3 if t > 3.0 else [1, 1, 1],
                       "%halo": [0.01, 0.01, 0.01]})
        if t >= 5.6:
            rise = min(1.0, (t - 5.6) / 2.4)
            pose["@body"] = [0, 16 * rise, 0]
        for name, (t0, off, rot) in falls.items():
            if t <= t0:
                continue
            k = min(1.0, (t - t0) / 0.5)
            o, rr = [v * k for v in off], [v * k for v in rot]
            if t >= 5.6 and k >= 1:
                # Keep the fallen plate on the ground while the body it hung from rises.
                pv = to_preview(P_(pose, {"@" + name: o, name: rr}))
                w_now = world_point(r, pv, name, r.get(name).pivot)
                pk2 = to_preview(P_(kn, {"@" + name: off, name: rot}))
                w_want = world_point(r, pk2, name, r.get(name).pivot)
                fix = parent_offset(r, pv, name, [w_want[j] - w_now[j] for j in range(3)])
                o = [o[j] + fix[j] for j in range(3)]
            pose["@" + name] = o
            pose[name] = rr
        c.key(t, pose, E_IN if t < 5.6 else E_IO)

    for cl in clips:
        cl.done()
    return f


OPEN = {1: ((0, 10, 24), (0, 0, -4), (0, 0, -6)),
        2: ((0, 16, 0), (0, 0, -4), (0, 0, -6)),
        3: ((0, 22, -26), (0, 0, -4), (0, 0, -6))}


def open_pose(m):
    """Test pose: all six wings spread wide."""
    t = {}
    for pair, (rw, rf, rh) in OPEN.items():
        for side, s in (("r", -1), ("l", 1)):
            n = f"wing_{side}{pair}"
            t[n], t[n + "_fore"], t[n + "_hand"] = mirror_rot(rw, s), mirror_rot(rf, s), mirror_rot(rh, s)
    return delta_pose(m, t)


def textures(m):
    t, g = m.build(gutter=1, seed=7000)
    Plate.CRACKED = True
    try:
        tc, gc = m.build(gutter=1, seed=7000)
    finally:
        Plate.CRACKED = False
    return t, g, tc, gc


def generate():
    m = rig()
    t, g, tc, gc = textures(m)
    m.rig.write(GEO + "michael_archangel.geo.json")
    save(t, "entity", "michael_archangel")
    save(g, "entity", "michael_archangel_glowmask")
    save(tc, "entity", "michael_archangel_cracked")
    save(gc, "entity", "michael_archangel_cracked_glowmask")
    write_compact(anims(), ANIM + "michael_archangel.animation.json")


if __name__ == "__main__":
    generate()
