"""Purgatory's creatures (v0.39): the souls of vampires and werewolves (names in purgatory/PurgatoryAssets.java).

A soul wears its living kin's rig and clips (legacy_art's `vampire` / `werewolf`): the kin's model is built in memory exactly
as legacy_art.write_creature builds it (same packing, same seed), nothing of the kin's is written, and only the colours change:
heavily desaturated, shifted to a ghostly pale grey-blue with the darks lifted (they are what is left of a monster, not flesh),
the same alpha mask texel for texel. Their eyes burn with pale cyan-white soul-light in the glowmask, wherever the kin's
eyes glowed.
"""

import legacy_art
from common import save
from pixelkit import Tex

SOULS = {"purgatory_vampire": ("vampire", legacy_art.vampire_rig), "purgatory_werewolf": ("werewolf", legacy_art.werewolf_rig)}


def _clamp(v):
    return max(0, min(255, int(round(v))))


def ghost(c):
    """A texel of the living kin as its soul: mostly luminance, tinted pale grey-blue, darks lifted, a trace of the old hue."""
    r, g, b, a = c
    if not a:
        return c
    lum = 0.3 * r + 0.59 * g + 0.11 * b
    lit = 58 + lum * 0.9
    base = (lit * 0.84, lit * 0.95, lit * 1.06 + 16)
    keep = 0.12
    return (_clamp(base[0] + (r - lum) * keep), _clamp(base[1] + (g - lum) * keep), _clamp(base[2] + (b - lum) * keep), a)


def soul_light(c):
    """A glowing texel as soul-light: pale cyan-white, as bright as the kin's glow was."""
    r, g, b, a = c
    if not a:
        return c
    k = max(r, g, b) / 255
    k = 0.55 + 0.45 * k
    return (_clamp(196 * k + 30), _clamp(236 * k + 12), _clamp(255 * k), a)


def recolour(t, fn):
    out = Tex(t.w, t.h, t.seed)
    for y in range(t.h):
        for x in range(t.w):
            out.rows[y][x] = fn(t.rows[y][x])
    return out


def soul(name):
    """The soul's (texture, glowmask, rig)."""
    kin, rig_fn = SOULS[name]
    m = rig_fn()
    t, g = m.build(gutter=1, seed=sum(map(ord, kin)))
    return recolour(t, ghost), recolour(g, soul_light), m.rig


def generate():
    for name in SOULS:
        t, g, _ = soul(name)
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")


if __name__ == "__main__":
    generate()
