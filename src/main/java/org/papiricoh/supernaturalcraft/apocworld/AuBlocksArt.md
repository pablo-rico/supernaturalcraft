# Apocalypse World blocks (v0.41) — art list for agent A

Block ids are registered in `ApocWorldRegistry`. Textures go to `assets/supernaturalcraft/textures/block/<texture>.png`
(16×16, vanilla style). The block models are written by B's datagen (`ApocWorldData.blockStates`) exactly as listed here.

| Block id | Model | Texture file(s) | Look |
|---|---|---|---|
| `scorched_stone` | cube_all | `scorched_stone` | The land's stone: dark charcoal-brown rock, coarse, faint rust-red streaks. |
| `scorched_soil` | cube_bottom_top | `scorched_soil_top`, `scorched_soil` (side and bottom) | Charred earth: top black-brown with grey ash flecks and a few thin orange ember cracks; side dry dark-brown dirt, a charred band at the top. |
| `au_ash_block` | cube_all | `au_ash_block` | Soft pale grey ash (like dull snow-sand), darker grey and black flecks. |
| `ash_layer` | 2px slab (no own texture) | reuses `au_ash_block` | A thin drift of the same ash over the ground. |
| `smouldering_ash` | cube_all | `smouldering_ash` | The ash with buried embers: dark grey ash, glowing orange-red specks and a crack or two (it emits light 6). |
| `burned_log` | log (pillar) | `burned_log` (bark), `burned_log_top` (rings) | Charred trunk: black cracked bark, alligator charring, a deep orange glow only in the deepest cracks; rings charcoal grey with a black rim. |
| `burned_planks` | cube_all | `burned_planks` | Blackened, scorched planks, grain still visible, a few burnt-through holes. |
| `rubble` | cube_all | `rubble` | A heap of broken grey concrete chunks, soot, bits of rusted rebar. |
| `ruined_bricks` | cube_all | `ruined_bricks` | A soot-stained red brick wall, cracked, missing mortar, black scorch smears. |
| `wrecked_metal` | cube_all | `wrecked_metal` | A burned car's body steel: patches of faded paint (dull red, dull blue) under rust and soot, a dent line. |
| `citadel_stone` | cube_all | `citadel_stone` | Michael's fortress: black polished basalt bricks with thin dark-gold seams between them. |
| `camp_lock` | cube_all | `camp_lock` | The prisoner cage's lock: a riveted steel plate crossed by two chains, a heavy padlock in the middle. |
| `au_rift` | nether_portal_ns/ew, translucent | `au_rift` | A tear into the burned world: black at the edges, ember-orange light and grey ash swirling (animate it like `purgatory_rift` if that one is animated). |

Also (items): `au_michael_trophy` (trophy block like `alastair_trophy`: a small bust of AU Michael in his greatcoat on a
plinth), `au_michael_greatcoat` (item sprite: a long dark military greatcoat with silver insignia; worn as a chestplate,
armour layer texture `textures/models/armor/au_michael_greatcoat_layer_1.png`), spawn eggs use `template_spawn_egg`.
(`michaels_orders` is, for now, a vanilla lodestone compass renamed "Michael's Orders": its sprite is not used yet.)

Sky: `textures/environment/apocalypse_world_sky.png` (smoke-brown overcast with ember streaks, tileable like the End sky) and
`textures/environment/apocalypse_world_sun.png` (a dim red disc).
