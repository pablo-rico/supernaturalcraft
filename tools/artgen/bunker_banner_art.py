"""The Men of Letters' banner pattern (v0.17.1): the order's mark, the eye in the Aquarian star in its ring, as a white mask
the game dyes (gold on the bunker's black banners). Vanilla pattern layouts: on the banner sheet (64x64) the flag's front is
x 1..20, y 1..40 and its back x 22..41; on the shield sheet the face is x 1..12, y 1..22.

  entity/banner/men_of_letters   the flag
  entity/shield/men_of_letters   a shield bearing it
"""

from common import save
from legacy_items_art import emblem_field
from pixelkit import Tex

WHITE = (255, 255, 255, 255)
SOFT = (255, 255, 255, 150)


def stamp(t, field, ox, oy, mirror=False, size=0):
    for (x, y), kind in field.items():
        if kind == "eye_white":
            continue
        c = SOFT if kind == "iris" else WHITE
        t.set(ox + (size - 1 - x if mirror else x), oy + y, c)


def flag():
    t = Tex(64, 64)
    size = 18
    field = emblem_field(size)
    # The mark in the flag's upper half (where a banner's charge sits), on front and back; a thin rule under it.
    stamp(t, field, 1 + 1, 1 + 7, size=size)
    stamp(t, field, 22 + 1, 1 + 7, mirror=True, size=size)
    for x in range(3, 19):
        t.set(1 + x, 1 + 28, WHITE)
        t.set(22 + x, 1 + 28, WHITE)
    return t


def shield():
    t = Tex(64, 64)
    size = 10
    stamp(t, emblem_field(size, ring=False), 1 + 1, 1 + 5, size=size)
    return t


def generate():
    save(flag(), "entity/banner", "men_of_letters")
    save(shield(), "entity/shield", "men_of_letters")
