# SupernaturalCraft — guía técnica para desarrollar

Mod NeoForge **1.21.1** (Java 21, NeoForge 21.1.221) inspirado en *Supernatural*. Paquete
`org.papiricoh.supernaturalcraft`, mod id `supernaturalcraft`. Dependencias: **GeckoLib 4.9.3**
(obligatoria; usar solo la API 4.x, la 5.x es para MC 1.21.5+), **JEI** y **Curios** (opcionales).

El contenido del juego está **solo en inglés**. Los comentarios del código también; la comunicación
con el usuario es en español.

## Reglas de trabajo

- **No hacer commits** salvo que el usuario lo pida expresamente.
- Pregunta al usuario las decisiones de diseño de contenido nuevo (le gusta que se le pregunte).
- Cada cambio debe dejar en verde: `./gradlew build` (compila + JUnit) y `./gradlew runGameTestServer`.
- Si tocas datagen: `rm -rf src/generated/resources && ./gradlew runData`. Un mismo recurso no
  puede existir en `src/main/resources` y en `src/generated/resources`: `processResources` usa
  `DuplicatesStrategy.FAIL`.
- Todo el arte (texturas, modelos GeckoLib, animaciones) se genera con `tools/artgen/` (Python sin
  dependencias, **no hay Pillow**). Nunca edites PNG/JSON de arte a mano: cambia el script y regenera.
- **Todo lo nuevo va al diario.** Al implementar cualquier mob, ítem clave, jefe, estructura, hechizo
  o mecánica, añade o actualiza su entrada en `datagen/journal/SNJournal` (texto en inglés, con su
  condición de desbloqueo) y, si forma parte de la progresión, su nodo en `datagen/journal/SNRoadmap`.
  `JournalEntriesTest` falla si un mob o un jefe del mod no tiene entrada.

## Comandos

```bash
./gradlew build                       # compila + JUnit (src/test)
./gradlew runData                     # modelos, lang, loot, tags, recetas, sonidos, logros, worldgen
./gradlew runGameTestServer           # GameTests (src/main/java/.../gametest); "All N required tests passed"
./gradlew runClient                   # cliente de desarrollo
./gradlew runClient -Ppal             # ídem con PlayerAnimationLib (dependencia opcional) en el classpath
python3 tools/artgen/generate.py      # regenera TODO el arte
python3 tools/structgen/empty_template.py   # plantillas NBT vacías para GameTests
```

## Mapa del código

| Paquete | Qué hay |
|---|---|
| `SupernaturalCraft` | Constructor del mod: orden de registro y listeners del bus de juego |
| `SNConfig` | Config SERVER (balance de Lucifer y Amara, arena, rituales, eclipse) |
| `SNClientConfig` | Config CLIENT: cinemáticas, tecla de saltar, distorsión, cielo del eclipse, alucinaciones |
| `registry/` | `All*` con `DeferredRegister` + `init()` vacío. `SNRegistries` = registros de datapack (`sigil`, `ritual_pattern`) |
| `hunter/` | Sal, trampa del diablo, agua bendita, armas `DemonBane`, amuleto, `CombatEvents` |
| `entity/demon/` | `DemonEntity` (base GeckoLib, huida como humo), `BlackEyedDemon`, `DemonOccultist` |
| `magic/` | `mana/` (ArcanaData + ManaManager), `spell/` (modelo, resolución, coste, lanzador), `spell/form`, `spell/effect`, `item/` |
| `ritual/` | Patrón + geometría, receta `RitualRecipe`, `effect/` (efectos despachados por `type`), `block/` (altar, líneas) |
| `entity/boss/` | `BossAttack` + `AttackScheduler` genéricos (reutilizables para otros jefes) |
| `entity/boss/lucifer/` | `LuciferEntity`, `LuciferAttacks`, ilusiones, cinemáticas, invocación, nombres de animación |
| `entity/boss/amara/` | `AmaraEntity` + 12 `AmaraPart`, `AmaraAttacks` (17), `AmaraBalance` (números puros), `Consumption`, `AmaraShade`, `AmaraFx`, invocación |
| `entity/boss/chorus/` | `ChorusEntity` + 24 `ChorusPart`, `ChorusGeometry` (posiciones compartidas con el render), `ChorusBalance`, `ChorusAttacks` (16 + Himno), `ChorusGaze`/`ChorusLight` (mirada y sombras), `ChoirEchoEntity`, `ChorusFx`, invocación y cinemáticas |
| `chorus/` | `Melody` (7 notas, himno de 3), `ChoirBellBlock`, `ChoirAltarBlock(Entity)` (armar con el Shattered Hymn, tocar campanas, afinar en creativo) |
| `structure/` | Hymnal Spire: `HymnalSpireStructure` (busca el pico), `SpireLayout` (plan puro), `SpireBuilder` (bloques), `SpirePiece` (3 piezas), `HymnalSpire.placeDirect` |
| `weather/` | `StormLock` (tormenta forzada mientras dure un combate) |
| `weapon/` | `WeaponProfile(s)` (data map), `Rune`/`RuneSet`, `melee/`, `catalyst/` (Catalyst + rasgos), `curse/` (hambre, niveles, Marca), `forge/` (Forja Infernal) |
| `eclipse/` | `EclipseSavedData` (por dimensión), `Eclipses` (API), `EclipseEvents` (reglas) |
| `cinematic/` | `CameraSequence` (JSON, spline, puro) y `CinematicLocks` (servidor) |
| `light/` | `TempLights` (luz temporal), `LightWellBlock` (pozos de Amara) |
| `entity/hazard/` | `FlameTrail`, `VoidZone` |
| `arena/` | `ArenaController` (snapshot copy-on-write, `protect`, `floorRadius`), `ArenaTheme` (CAGE/DARKNESS/CHORUS y su altura), `ArenaRescue` (caídas), `ArenaSavedData`, `ArenaEvents`, `ArenaTerrain` (`collapseRing`, `breakChunk`), `ArenaBlock` |
| `entity/marker`, `entity/projectile`, `entity/magic` | Avisos en el suelo, proyectiles, SigilBolt/Ward |
| `reward/` | Archangel Blade, The Colt (`ColtItem` + `reward/colt/`: disparo, recarga, nombres de animación), Lucifer's Grace, Eclipse Sight, trofeos |
| `client/colt/` | Todo lo visual del Colt: renderer y capas (grabado, brazos en 1.ª persona), retroceso de cámara (`RecoilSpring`), FX, HUD del tambor, poses de brazo (`ColtArmPoses`, extensión de enum) |
| `compat/pal/` | PlayerAnimationLib opcional: único sitio que la importa |
| `hell/` | El Infierno: `HellDimension` (claves), `HellEvents`, `Torment`; `worldgen/` (`HellPit`/`PitShape` sima, `HellBiomeSource`/`HellBiomes`, `FixedPlacement`, ganchos, Crowley's Corridors); `rift/` (grietas: bloque `Portal`, `HellRifts`, datos guardados); `cage/` (`CageLayout` puro, `CageBuilder`, `CageStructure`, `CageController` del iris, `CagedLuciferEntity`) |
| `entity/boss/uncaged/` | Lucifer Uncaged: subclase de `LuciferEntity` (6 fases, vida escalada), `UncagedAttacks` (10), `UncagedBalance` (puro), terreno, cinemáticas, invocación |
| `entity/boss/azazel/` | Azazel (primer jefe): subclase de `LuciferEntity` (2 fases, 400 PV), `AzazelAttacks` (8 + Blink), `AzazelBalance` (puro), trampa de vías del Colt (`RailTrapLayout` puro, `ColtRailBlock`, `RailTrap`), `HurledDebris`, `PossessedEffect`, terreno, cinemáticas, invocación |
| `entity/boss/lilith/` | Lilith (segundo jefe): subclase de `LuciferEntity` (3 fases, 500 PV), `LilithAttacks` (7 propios + `HoundPack`/`Judgement`/`Teleport` prestados), `LilithBalance` y `ContractLedger` (puros), lápidas (`LilithHeadstones` puro, `HeadstoneBlock`), terreno, cinemáticas, invocación |
| `entity/boss/metatron/` | Metatron (tras Lucifer): subclase de `LuciferEntity` (4 fases, 1800 PV reales con `healthScale`), constructos no vivos `ScribeConstruct` (`ScribeHandEntity`, `ScribeBookEntity`), `MetatronAttacks` (cuerpo, mano, libro, Tablilla), `MetatronBalance`/`WordJudge`/`ScriptoriumLayout` (puros), `MetatronTerrain` (Reescribir), invocación, cinemáticas |
| `bowl/` | v0.8 Cuenco de hechizos: `BowlContents` (componente: ítems + dosis `Dose`/`BowlLiquid`), `BowlSpellRecipe` (`supernaturalcraft:bowl_spell`), `effect/` (registro `BowlSpellEffect`, `BowlCast`), bloque/BE/ítem, `Recitation`/`SpillRules`/`BowlMix` (puros), `BowlCarry` (derrames, mano libre bloqueada), `BowlBacklash`; `client/` (render del contenido, pose a dos manos, brazos en 1.ª persona, pantalla de recitado); `spell/` (Localizar, Purificar, Atar/Desterrar, Revivir mascota, Ocultación, vial de sangre, collar + `PetLedger`, estela de humo); `page/` (páginas de hechizo, botín, comercio, comandos) |
| `hex/` | Bolsas de maleficio: maldición (`CurseBagBlock`, `HexBags`: JINXED, desgracias, BLEEDING acumulativo) y protección |
| `entity/ghost/`, `grave/` | Fantasmas (`GhostEntity`: invisibles salvo al manifestarse o con Second Sight, frío, apagan luces, telequinesis; hierro/sal/sagrado los dispersan) atados a sus huesos (`GraveBonesBlock`: salar y quemar); estructura de tumbas (`GraveLayout` puro, `GraveBuilder`) |
| `crossroads/` | Demonio de encrucijada y el trato: `DealTerms`/`BossProgression` (puros), `Deals`, `Wishes`, `Debts` (plazo, cacería de sabuesos con `quarry`, romper el trato), `Boons`, `LostBelongings`, `CrossroadsHooks` |
| `entity/hellhound/` | `HellhoundEntity` (invisible salvo revelado; el render decide en `HellhoundRenderer.seen`) |
| `journal/` | v0.9 Libro del Cazador (común): `HunterLog` (attachment: vistos/matados, ítems, leídas, marcadores, biblioteca de 24 diseños), `HunterLogEvents` (registro + crédito de jefe compartido), formato de datos `JournalEntry`/`JournalBlock`/`Unlock`/`JournalChapter`, `RoadmapNode`/`RoadmapState`, `JournalLayout` (puro) |
| `client/book/` | El libro: `HunterBookScreen` (espacio de libro 440×272 escalado a píxeles enteros, pestañas), `BookSection`, `BookAtlas`/`BookStyle`, `BookData` (carga entradas y roadmap); `home/` (dashboard), `journal/` (índice, doble página, bloques, toasts), `scriptorium/` (compositor, vista previa, biblioteca, enciclopedia), `roadmap/` (lienzo) |
| `network/` | Payloads + `SNNetworking`. Los handlers de cliente solo se referencian desde lambdas |
| `client/` | Renderers, HUD, pantallas, cúpula/música, partículas, teclas, `dev/DevPreview`; `cinematic/` (`CameraDirector`), `eclipse/` (cielo, lightmap), `amara/` (efectos, consumo, `ClientPostFx`), `fx/` (`BeamFx`, `TubeFx`), `curse/` (alucinaciones) |
| `compat/jei`, `compat/curios` | Solo se cargan si el mod está presente; nada fuera de `compat/` importa sus APIs. `compat/curios/client` dibuja las Seraph Wings |
| `datagen/` | `SNDataGenerators` (entrada), providers, `SNLang` (textos libres por sistema) |
| `command/` | `/supernatural …` (nivel 2) para pruebas. `BossCommands`: `boss summon [lucifer\|amara\|chorus\|uncaged\|azazel\|lilith\|metatron] [pos]` (sin ritual; Amara trae su eclipse y el Chorus su tormenta), `boss phase <2-6>` (topado por jefe) y `boss health <fracción>` para cualquier jefe a 96 bloques. Un jefe nuevo se añade al enum `Boss` |
| `gametest/` | GameTests; `SNGameTests` tiene plantillas y helpers |

## Recetas para añadir cosas

### Un ítem o bloque simple
1. Regístralo en `registry/AllItems` / `AllBlocks` (los bloques van antes que los ítems).
2. Añade el arte en `tools/artgen/items.py` o `blocks.py` (función + entrada en el dict/`generate()`).
3. Datagen: modelo en `SNItemModelProvider` / `SNBlockStateProvider`, loot en `SNLootTableProvider`,
   tags en `SNTagsProviders`, receta en `SNRecipeProvider`. El nombre en inglés sale solo del id
   (`SNLanguageProvider.titleCase`); excepciones en `NAMES`.
4. La pestaña creativa lista todos los ítems registrados automáticamente.

### Un sigilo (hechizo)
- **Solo datos**: JSON en `src/main/resources/data/supernaturalcraft/supernaturalcraft/sigil/<id>.json`
  (`kind`, `behavior`, `tier`, `mana_cost`, `cooldown`, `reagents`, `params`, `color`). Los
  modificadores usan `behavior: supernaturalcraft:modifier` y son 100% datos (`params`:
  `mana_multiplier`, `potency_multiplier`, `duration_multiplier`, `range_multiplier`, `area_bonus`, `echo`).
- **Nueva forma o efecto**: implementa `SpellBehavior.Form` / `SpellBehavior.Effect` en
  `SpellForms` / `SpellEffects` y regístralo en su `registerAll()`.
- Glifo: añade los trazos en `GLYPHS` (y su color en `COLORS` si es efecto) en `tools/artgen/gui.py`.
- Texto: `sigil(...)` en `SNLang.sigils` (nombre + `.desc`).
- Entidades que reaccionan a un sigilo de forma especial: `SpellHooks.Bindable/Revealable/Exorcisable`.

### Un ritual
- Patrón: `data/supernaturalcraft/supernaturalcraft/ritual_pattern/<id>.json`. Una capa a la altura
  del altar; `A` = altar, espacio = cualquier cosa, el resto son claves con `BlockPredicate` vanilla
  (`blocks`, `state`).
- Receta: `data/supernaturalcraft/recipe/ritual/<id>.json` (`type: supernaturalcraft:ritual`,
  `pattern`, `ingredients` ≤ 8 sin orden, `activator`, `consume_activator`, `conditions`,
  `duration`, `mana_cost`, `effect`).
- Nuevo tipo de efecto: implementa `RitualEffect` con un `MapCodec` y regístralo en
  `RitualEffect.bootstrap()`; añade `jei.supernaturalcraft.effect.<ns>.<path>` en `SNLang` si no
  produce un ítem.

### Un arma nueva
1. Ítem en `AllItems` (melee 3D: `extends GeoSwordItem`; catalizador: `extends CatalystItem`, que
   implementa `Catalyst`: multiplicadores, `pay`, `shape` con `CatalystTraits`, ataque propio).
2. Perfil en `data/supernaturalcraft/data_maps/item/weapon_profile.json` (`tier`, `rune_slots`,
   `kind`, `holy`, `cursed`): el tier y las ranuras de la Forja salen de ahí.
3. Obtención: un ritual JSON `craft_item` en `recipe/ritual/` (`bind_to_ritualist` para las ligadas).
4. Arte: sprite en `weapons_art.py`, o rig en `weapon_models.py` + `geoWeapon(...)` en
   `SNItemModelProvider` y la extensión de cliente en `SNClientEvents.registerGeo`.
5. Texto: nombre (automático), mecánica en `tooltip.supernaturalcraft.weapon.<id>` (`SNLang.arsenal`).
6. GameTest en `ArsenalTests`; mírala con `SN_PREVIEW=weapons`.

### Un mob (GeckoLib)
1. Arte: un módulo en `tools/artgen/` (ver `demon_art.py`): rig con `humanoid()` o `geomodel.Rig`,
   pintado con `paint.py`, animaciones con `animkit.AnimFile`; añádelo a `MODULES` en `generate.py`.
2. Entidad: implementa `GeoEntity` (ver `DemonEntity`), con un controlador base (idle/walk) y uno
   `action` con `triggerableAnim`. Desde el servidor: `triggerAnim("action", nombre)`.
3. Regístralo en `AllEntities`, sus atributos y spawn en `entity/SNEntityEvents`, el renderer en
   `client/SNClientEvents`, el huevo en `AllItems` (modelo `template_spawn_egg`), el loot en
   `SNEntityLoot` y los tags.

### Un jefe nuevo
- Reutiliza `entity/boss/BossAttack` + `AttackScheduler` (el host decide pool, objetivo, pausa y
  ataques forzados). Cada ataque es una instancia nueva por uso: puede guardar estado en campos.
- Avisa siempre en el suelo durante el windup (`TelegraphMarker.circle/ring/line/cone`) y haz
  daño solo en ACTIVE. Colores: rojo fuego, azul hielo, violeta rayo (telequinesis), dorado sagrado, verde zona
  segura, amarillo (`YELLOW`) humo y mirada de Azazel.
- Arena: `LuciferSummoning.openArena` + `ArenaController.mutate()` para cualquier cambio de terreno
  (nunca `setBlock` directo) → se restaura sola. Cinemáticas con `CinematicPayload`.
- Copia el patrón de `LuciferEntity`: política de daño en `hurt()`, umbrales de fase sin saltos,
  muerte larga con `die()` al final y `tickDeath()` inmediato.
- **Daño exacto** (`entity/boss/BossDamage`): añade el jefe al tag `#supernaturalcraft:bosses` y pasa
  sus multiplicadores y su tope por golpe por `BossDamage.scaleAndCap(source, …)`. El daño del tag
  `exact_boss_damage` (las balas del Colt) se los salta; los suelos de fase, caparazones y fases
  invulnerables se aplican siempre. Sin esto el Colt hace 60 con multiplicadores y tope.

### Una variante de Lucifer / un jefe con más de 1024 de vida (Lucifer Uncaged)
- `LuciferEntity` expone ganchos sobrescribibles (`maxPhase`, `threshold`, `pool`, `baseGap`, `scale`, `healthScale`,
  `mundaneMultiplier`, `hitCap`, `attackDamageMultiplier`, `isAerialPhase`, cinemáticas, `tickEmergence`, `tickDyingMotion`,
  `leaveBehind`, `onDefeated`…). Sus valores por defecto son exactamente el Lucifer de siempre: no los cambies sin sus tests.
- **Vida por encima de 1024:** `healthScale()` = puntos reales por punto vanilla. La vida vanilla se queda en ≤1024 (la barra es la
  proporción) y `hurt` divide cada golpe (ya escalado y topado en puntos reales) por la escala. `trueHealth()`/`trueMaxHealth()`.
  El daño exacto del Colt sigue siendo exacto en puntos reales. Sin armadura (con escala alta la armadura vanilla se come más).
- Los ataques de Lucifer reciben `LuciferEntity`, así que sirven a la subclase; las ayudas de `LuciferAttacks` son públicas.
- Más ganchos (v0.5, Azazel): `vulnerability(source)` (por defecto ×1.25 en RECOVER), `transitionTicks`, `tickTransitionMotion`,
  `emergeSound`/`roarSound`/`transformSound`, `dyingParticles`, `tetherPoint`. `returnToCage` recibe la clave del mensaje
  (`message.supernaturalcraft.lucifer.*`); una subclase la reescribe a la suya.

### Un jefe humano sobre `LuciferEntity` y la trampa de vías (Azazel)
- Azazel no es un ángel: `isAerialPhase()` siempre false, escala 1, y su `canBeAffected` acepta TRAPPED/STUNNED solo mientras
  el propio jefe los aplica (`allowHold`): las trampas pintadas (3×3) no lo retienen y las quema; la sal no frena a ningún
  jefe (`SaltLineBlock.isWarded` excluye `#bosses`). Está en `#demons` (armas DemonBane, agua bendita; el Colt mira
  `#bosses` antes que `colt_executes`). Exorcismo (sigilo y ritual) pasan por `SpellHooks.Exorcisable`.
- **Trampa de vías**: `RailTrapLayout` (puro) rasteriza círculo + pentagrama (radio 5) en celdas con `Shape`
  (NS/EW/DIAG_A/DIAG_B/CROSS); `ColtRailBlock` es un decal irrompible (`SHAPE`, `CHARGED`) colocado con
  `ArenaController.mutate` (se restaura con la arena; **no** va en `#arena_immune`, que impediría recargarlo).
  Cargado devuelve `PathType.BLOCKED` solo para Azazel. `RailTrap` (estado en la entidad, NBT) lo atrapa al entrar,
  apaga los raíles y los reenciende uno a uno durante la recarga. El Smoke Dash va en línea recta: es el cebo.
- Si aparece por huevo o comando, la arena se centra en él: sale de los raíles con `tetherPoint`.

### Lilith: contratos, luz blanca y lápidas
- `ContractLedger` (puro) lleva los contratos abiertos: cada golpe de un jugador la paga (lo sagrado ×2,
  `LilithBalance.contractCredit`); al llegar a `contractBreakDamage` arde y la aturde; si vence, `houndsComeFor`
  suelta hellhounds **sin revelar** a por el marcado, registrados en `minions()` (se descartan con ella).
- `WhiteLight` solo se fuerza (cada 5/4/3 ataques): raycast `ClipContext.COLLIDER` de sus ojos a los de cada aspirante
  (`WhiteLight.blocked`); si para en una `HeadstoneBlock`, `crackHeadstone` suma una grieta y a la 2.ª la derrumba
  (`ArenaController.revert`). Tras cada estallido queda "vacía" 40 ticks (×1.4 vía el gancho `vulnerability`).
- Las lápidas se levantan con `mutate` (como los raíles de Azazel), se reponen 3 si quedan menos de 2 al cambiar de fase.
- Su glowmask lleva una copia tenue de todo el cuerpo (`inner_light`): sin eso, de noche se ve gris.
- Gancho nuevo en `LuciferEntity`: `clientEmergenceParticles()` (por defecto el fuego y humo de Lucifer).
- (ver también Metatron abajo)
- El silbato invoca un `BoundHellhoundEntity` (subclase de `HellhoundEntity`, `MobCategory.MISC`): siempre revelado,
  defiende y ayuda a su dueño, nunca ataca a jugadores, 60 s. Usa `HellhoundRenderer` tal cual.

### El cuenco de hechizos (v0.8)
- **Receta** `data/supernaturalcraft/recipe/bowl_spell/<id>.json`: `liquids` (multiconjunto de `water`, `holy_water`,
  `demon_blood`, `blood`, `honey`, `potion`, `dragon_breath`; ≤4), `ingredients` (≤8, exactos y sin orden), `incantation`
  (latín literal, va en el JSON), `mana_cost`, `conditions` (las de los rituales), `smoke_color`, `difficulty`, `page_weight`,
  `effect`. El hechizo que se aprende es `spell` o, si falta, el nombre del archivo; varias recetas comparten página con `spell`
  (los 8: locate, summon_crossroads, hex_bags, concealment, second_sight, purification, bind_banish, revive_pet).
  Nombre y descripción en lang: `bowl_spell.<ns>.<spell>` (+`.desc`).
- **Efectos**: `BowlSpellEffect` (`precheck` al encender, antes del maná; `perform` → false = "el humo no encuentra nada",
  conserva el contenido). `ritual` envuelve cualquier `RitualEffect`; `apply_effect` aplica un efecto sin partículas.
- **Flujo**: encender (pedernal/carga de fuego) → `OpenRecitationPayload` → `RecitationScreen` (tolerante: sin mayúsculas ni
  acentos, espacios opcionales, cada error resta tiempo) → `RecitationResultPayload` validado en el servidor (dueño, ≤6
  bloques, plazo + 40 de margen, plausibilidad). Sin receta → contragolpe; hechizo no aprendido → nada. Los tests usan
  `SpellBowlBlockEntity.tryLight` + `resolveRecitation`.
- **Componente con igualdad por valor**: `BowlContents` usa `ItemContainerContents`; se copia bloque↔ítem con
  `applyImplicitComponents`/`collectImplicitComponents` y el loot con `CopyComponentsFunction`. `BowlInput.isEmpty()` está
  sobrescrito: si no, una mezcla solo de líquidos nunca casa (vanilla salta las entradas "vacías").
- **Pose a dos manos**: `BowlArmPoses.CARRY` (extensión de enum) y brazos en 1.ª persona con `client/render/FirstPersonArms`
  (las cuentas de `ColtArmsLayer`, reutilizables).
- Los fantasmas y sabuesos usan `isCurrentlyGlowing()` solo en el cliente para que el contorno de Second Sight lo vea quien
  tiene el efecto. Ocultación nunca afecta a `#bosses` ni a un sabueso cuya `quarry()` es el oculto.
- Ver: `SN_PREVIEW=bowl` (cuenco en el suelo, recitado, humo, en mano 1.ª/3.ª persona, inventario, contragolpe, fantasma,
  tumba, demonio, estela).

### El Libro del Cazador (v0.9): diario, dashboard, scriptorium y roadmap
- Se abre usando el grimorio agachado (`SNClientHooks.openBook()`). Pestañas: Inicio (dashboard), Diario, Scriptorium
  (compositor de hechizos/scrolls) y Roadmap. `RecitationScreen` y `DealScreen` siguen usando `journal.png`.
- **Añadir una entrada del diario** (obligatorio con cada cosa nueva, ver Reglas): en `datagen/journal/` (las clases
  `Journal*` que llama `JournalContent`), con el builder de `SNJournal`:
  `entry(id, capítulo).order(n).icon(ítem).unlock(Unlock...).creature(tipo).title("…").text("…").entity(tipo, pie)
  .items(pie, ítems…).recipe("ns:ritual/x", pie).image(tex, w, h, pie)`. El texto inglés va ahí mismo (genera lang
  `journal.supernaturalcraft.entry.<id>.*`) y el JSON en `assets/supernaturalcraft/journal/entries/<id>.json`.
  `creature(...)` la convierte en entrada de bestiario (se abre al verlo, cuenta muertes). Un mob nuevo va también a
  `CREATURES` en `JournalEntriesTest`.
- **Roadmap con varios caminos** (desplegable en la tarjeta del título): `datagen/journal/RoadmapContent` define cada camino con
  `road(id, icono, "Título")` → `assets/supernaturalcraft/journal/roadmaps/<id>.json` (orden = orden del menú; el primero es
  `road_to_the_cage`, el único que sigue el dashboard). Hoy: la Jaula, el Cuenco y la Encrucijada. **Añadir un nodo** (si es
  progresión): `node(id, col, fila).icon().after(padres).boss().main().advancement("main/x")|.rite("hechizo")|.done(Unlock)
  .entry(id).name("…").hint("…")`. Los ids de nodo son únicos entre todos los caminos y los padres van en el mismo camino.
  `Unlock` también acepta `rite` (hechizo del cuenco aprendido). `RoadmapTest` exige padres a la izquierda, celdas únicas,
  logros/ítems/hechizos existentes y un nodo por jefe de `BossProgression` en la Jaula.
- **Sincronización**: el cliente no ve logros ocultos ni el registro: `HunterLogSyncPayload` (logros del mod hechos, registro,
  trato, mejoras) y `LibrarySyncPayload`; espejo en `ClientHunterLog` (`PROGRESS` para `Unlock.test`).
- **Crédito de jefe**: al morir un `#bosses`, todos los que luchaban (los `challengers()` de un Lucifer, o jugadores a 48
  bloques) reciben el logro de muerte (`BossProgression.Boss.entity` → logro).
- **Dibujar en el libro**: todo en espacio de libro bajo un pose escalado. El scissor vanilla ignora el pose: usa
  `book.scissor(...)`; nada de `renderEntityInInventoryFollowsMouse` ni listas vanilla; `EditBox` dibuja sombra (el
  scriptorium usa su `InkField`). Tooltips con `book.tooltip(...)`.
- **GameTests con logros**: NeoForge no da logros a un `FakePlayer` y el jugador falso vanilla choca con los payloads de
  otros mods al entrar; usa `JournalTests.Witness` (jugador real sin conexión).
- Ver: `SN_PREVIEW=book` (`SN_BOOK_TABS=home,journal,scriptorium,roadmap`, `SN_BOOK_SCALES=2,3`): cada `previewShots()`
  de cada pestaña → `sn_book_<pestaña>_<toma>_s<escala>.png`.

### Una dimensión (el Infierno)
- Todo son entradas de datapack en datagen (`SNHell`): `dimension_type`, `noise_settings` (router propio: el del Nether es
  `protected`; aquí se reconstruye con `DensityFunctions` + `BlendedNoise` a 256 de alto), biomas, `level_stem`. Tipos propios
  registrados en `AllWorldgen` (función de densidad `hell_pit`, fuente de biomas `hell`, colocación `fixed`, feature).
- Lo que se quiera probar con JUnit va en clases sin tipos de Minecraft (`PitShape`, `HellBiomes`, `CageLayout`): cargar una
  `DensityFunction` en JUnit falla (sus registros no existen).
- **El servidor de GameTests no tiene el Infierno** (crea un mundo plano sin dimensiones de datapack): los tests prueban sus
  registros, y la lógica recibe el nivel destino (`HellRifts.openTheWayBack(from, rift, to)`, la Jaula se construye en el
  origen del mundo de test con `CageBuilder`). En un mundo normal (o `sn_preview`) sí existe.
- Rituales con `"time": "night"` no funcionan en dimensiones de hora fija (Nether, Infierno): `isNight()` es falso.
- El maná máximo es 175 (100 + Gracia 50 + Marca 25): ningún ritual puede costar más.
- `PlayerAdvancements.award` ignora a los FakePlayer: en tests concede el progreso con `getOrStartProgress(adv).grantProgress(c)`.

### La Jaula de Lucifer
- Una sola estructura en el chunk 0,0 (`FixedPlacement`), una pieza que cubre `CageBuilder.extent()` (≤100 bloques del origen);
  `CageBuilder` coloca todo recortado a la caja. La sima la hace la densidad, no bloques.
- Bloques irrompibles con `cageProps`; los de la Jaula están en `#arena_immune`, pero el suelo de la isla (`abyssal_flagstone`)
  no: la fase 6 lo derrumba con `collapseRing` (la isla es una losa de 4 sobre un núcleo estrecho) y la arena lo restaura.
- El iris del suelo de la Jaula lo abre/cierra `CageController` (datos guardados por nivel); el patrón `cage_circle` usa
  `cage_ritual_stone`, que solo existe en el dais. `/supernatural cage open|close|place`, `/supernatural hell tp|return|rift`.

### Metatron: constructos, atril, la Palabra y un ingrediente propio
- **Constructos no vivos**: `ScribeConstruct extends Entity implements GeoEntity` (mano y libro). Sin hitbox ni daño
  (`isPickable`/`hurt` false), flotan junto a su dueño (`MetatronEntity.constructRest`) y los ataques los mueven con
  `order(pos, ticks)` (interpolado) y `release()`. Se renderizan con `GeoEntityRenderer` escalado (`ScribeConstructRenderer`);
  la mano en `entityTranslucent`. Desaparecen solos si su dueño no está.
- **Atril**: en la F3 levanta la tarima con escaleras (`ScriptoriumLayout.dais`) y sube flotando (`tickTransitionMotion`);
  desde ahí no se mueve: gancho nuevo `LuciferEntity.walks()` (por defecto true) y se le fija en `lecternSpot` cada tick.
- **La Palabra**: título vanilla (`ClientboundSetTitleTextPacket`) + `WordJudge.disobeys` puro (umbral 0.5 bloques de
  movimiento, producto escalar de mirada 0.8, agachado al final). Cumplir las tres da el logro `obeyed`.
- **Reescribir**: `ArenaController.mutate(..., revertAfter)` para que columnas y huecos vuelvan solos; nunca en `daisFootprint`.
- **Ingrediente `supernaturalcraft:written_name`** (`WrittenNameIngredient`, `ICustomIngredient` registrado en
  `AllRecipes.INGREDIENT_TYPES`): libro y pluma o libro escrito cuyo texto contenga el nombre (`WrittenName.holds`, puro).
  JSON: `{ "type": "supernaturalcraft:written_name", "name": "Metatron" }`.
- Las listas de clips de los constructos viven en `MetatronAnimations` (los tests JUnit no pueden cargar clases de entidad).
- **La Mano de Dios** (`tools/artgen/scribe_hand_art.py`): portal (3 anillos ofánicos que giran en ejes anidados `tilt_*`
  → `ring_*`, uno con ojos; nubes, disco y rayos), manga acampanada con pliegues inclinados (rotación por cubo) y mano de
  mármol con vetas de luz construida a tamaño natural y ampliada ×1.45 sobre la muñeca; atlas 1024×512. El origen es la
  punta de la pluma. Se dibuja a `ScribeHandEntity.SCALE` (0.85) y a brillo pleno; su culling cubre los ~10 bloques
  (`getBoundingBoxForCulling`). `SN_PREVIEW=metatron_hand` la muestra sola (aparición, reposo, escritura, palmada, barrido).

### Un jefe con partes (Amara)
- Partes: `AmaraPart extends PartEntity` (patrón del Ender Dragon). El jefe reserva ids con
  `setId(ENTITY_COUNTER.getAndAdd(n + 1) + 1)` y en `setId` da a la parte i el id + i + 1;
  `isMultipartEntity`/`getParts` y `placeParts()` en cada `tick` (ambos lados). Las partes no se
  guardan ni se envían; el daño entra por `hurtPart` y llega a la vida con `hurtCore` (flag `routing`).
- `partOffset(i, partial)` es la única fuente de posiciones: la usan los hitboxes y el modelo
  (`AmaraRenderer.Model.setCustomAnimations` mueve los huesos `ring_*`). El modelo se dibuja a
  `MODEL_SCALE` (2,2×): una posición del mundo pasa a píxeles de modelo con `16 / MODEL_SCALE`, y
  en X/Z como (dx, −dz) porque GeckoLib invierte X y ella mira al sur (yaw 0).
- Comprueba que modelo y hitboxes coinciden con `SN_PREVIEW=amara` (activa los hitboxes un instante).
- Números puros en `AmaraBalance` (JUnit); reglas en `AmaraTests` (cada test en su batch y
  `BossTests.cleanup` al empezar). Luz: `LightWellBlock`, `Consumption`, `TempLights`; el
  apagón usa `arena.mutate`, así que nada que deba apagarse puede estar en `#arena_immune`.

### Un jefe con partes que se mueven (Broken Chorus)
- Las posiciones de **todo** (ojos en ruedas giratorias, alas, caras) salen de `ChorusGeometry`, que reproduce
  la cadena de GeckoLib: puntos Bedrock con X negada, cada hueso rota con `Quaternionf().rotationZYX(z, y, x)`
  sobre su pivote (hijo primero), rotaciones horneadas como `(-x, -y, z)`, y al final escala y giro de 180°.
  El renderer pone en los huesos `body_turn`, `wheel_*`, `wing_*` y párpados **los mismos** números
  (`setRot*` en radianes, tal cual). Los ojos son hijos de su rueda con rotación de reposo: su hitbox es el
  pivote transformado. `ChorusGeometryTest` compara con una cadena `Matrix4f` y `ChorusAssetsTest` con el JSON.
- Base de tiempo: `level().getGameTime() + partial`, nunca `tickCount` (empieza en 0 en el cliente).
  Cambios de velocidad sin saltos: ángulo base + ancla sincronizados (`setWheelSpeed`).
- Las animaciones JSON **no** pueden tocar huesos que mueve el renderer (lo comprueba `ChorusAssetsTest`).
- Vida: el atributo vanilla `MAX_HEALTH` **está capado a 1024**. El Chorus guarda su vida real en las reservas
  de sus partes (`poolSum`) y la vida vanilla es solo la proporción. Cada golpe va a una parte (con tope) y
  se resta de las reservas: romper la última parte de una fase es lo que cambia de fase.
- Arte: `tools/artgen/chorus_art.py` (`geomodel` admite rotación por cubo: `rotation=` + `pivot=`, y
  `preview3d` la dibuja). `preview_pose(t)` reproduce la pose procedural para revisar sin abrir el juego.

### Una estructura procedural (Hymnal Spire)
- `HymnalSpireStructure.findGenerationPoint` solo busca el pico (~75 muestreos: lo ejecutan también
  `/locate` y los mapas). El resto se planifica en el `GenerationStub` con `SpireLayout.plan` (puro,
  testeado en `SpireLayoutTest`) y se guarda entero en cada `SpirePiece`, que se reconstruye igual en
  cualquier chunk. `SpireBuilder` pone los bloques recortando a la caja que le pasan.
- Reglas: piezas a menos de ~112 bloques del pico (las referencias solo miran 8 chunks); nada de
  `SavedData` ni estado del servidor en `postProcess`; decisiones por hash de la posición + semilla.
- Datos: `SNStructures` (estructura en `#minecraft:is_mountain`, set 96/40), tag
  `#supernaturalcraft:hymnal_spires`, botín en `SNChestLoot`. Colocar a mano: `/supernatural spire place [semilla]`.
- Mapa: el efecto de ritual genérico `locate_structure` (tag, decoración de mapa, nombre).

### Temas de arena
- `ArenaTheme` fija profundidad/altura y si se rescata a quien cae (`ArenaRescue`); `client/arena/ArenaStyles`
  los colores y la música. `ArenaTerrain.collapseRing` tira un anillo del suelo (escombros con `DebrisPayload`,
  solo visuales) y actualiza `floorRadius`. `protect(pos)` deja intocable una posición concreta.
- `SULFUR` (4) es la arena de Azazel: terreno tal cual, cúpula amarilla, raíles en el centro.
- `SEAL` (5) es la de Lilith: cúpula blanca, lápidas en un anillo de radio ~11.
- `SCRIPTORIUM` (6) es la de Metatron: altura 28, biblioteca de estanterías (se restaura) y tarima con escaleras.
- `StormLock.force/release`: el flag vive en la arena, así que cerrar la arena (por la vía que sea) despeja el cielo.

### El eclipse ritual
- Estado por dimensión en `eclipse/EclipseSavedData`; API en `eclipse/Eclipses` (`active`, `begin`,
  `end`, `lock` para que no termine mientras dure un combate). Un ritual lo exige con
  `"conditions": {"eclipse": true}`; lo inicia el efecto `supernaturalcraft:begin_eclipse`.
- Reglas en `eclipse/EclipseEvents`: demonios extra (`SNConfig` sección `eclipse`), los no-muertos
  no arden (se apaga el fuego que prende el sol en su propio tick) y el daño sagrado ×1.1.
- Cliente: `ClientEclipse.intensity(partial)` (fundido de 200 ticks), `EclipseOverworldEffects`
  (lightmap sin luz de cielo y sin nubes) y `EclipseSky` (bóveda, estrellas, corona, niebla).
- Tests: el eclipse es global a la dimensión. Cada test de eclipse va en su **propio batch** y lo
  termina al acabar; si no, cambia el daño sagrado de otros tests.

### Una cinemática de cámara
- JSON en `assets/supernaturalcraft/cinematics/<id>.json` (`cinematic/CameraSequence`): lista de
  `shots` con `duration`, `path` (puntos Catmull-Rom), `look`/`look_to`, `fov`/`fov_to`,
  `roll`/`roll_to`, `shake` y `ease` (`linear|in|out|in_out`). Coordenadas **relativas al ancla**
  en su marco: +z hacia donde mira, +x a su izquierda, +y arriba. La cámara sigue a la entidad
  ancla mientras viva (con la orientación del inicio).
- Servidor: `CinematicLocks.play(ancla, id, ticks, rango)` envía `CameraSequencePayload` y hace
  inmunes a los espectadores mientras dura. Úsalo junto a un `CinematicPayload` (letterbox y títulos).
- Cliente: `CameraDirector` (cámara = `Marker` solo cliente, entrada bloqueada, HUD oculto salvo
  la capa `cinematic`; se salta manteniendo Enter). Con `cinematics=false` se ignora.
- Ver: `SN_PREVIEW=cinematic ./gradlew runClient -Ppreview` (Lucifer: intro, fases 2–4 y muerte).

## Convenciones de GeckoLib (verificadas en bytecode y en capturas)

- Unidades en píxeles, Y arriba, origen en los pies. El modelo mira al **norte (−Z)**.
- GeckoLib **invierte X** al hornear: **+X Bedrock = lado IZQUIERDO** de la entidad. El brazo
  derecho va en x negativa, como el `rightArm` en `[-5,22,0]` del jugador de Bedrock.
- Rotaciones en grados Bedrock (GeckoLib niega X e Y):
  - **−X**: un miembro colgante se adelanta; la cabeza mira arriba.
  - **+Y**: gira hacia la derecha de la entidad.
  - **+Z**: lleva un miembro colgante hacia la derecha (brazo derecho hacia fuera, izquierdo hacia dentro).
- `geomodel.py` usa UV por cara con desplegado de caja `[east][north][west][south]`, arriba `[up][down]`.
- Las máscaras de brillo son `<textura>_glowmask.png` (`AutoGlowingGeoLayer`).
- Para ocultar un hueso **y** sus hijos: `setHidden(true)` + `setChildrenHidden(true)`.

### Ítems GeckoLib (armas 3D)
- Arte en `tools/artgen/weapon_models.py`: clase `Weapon`, modelo **de pie** (hoja o cabeza arriba).
  `build()` lo centra en el origen porque `GeoItemRenderer` ya traslada a (0.5, 0.51, 0.5). También
  escribe el icono `<id>_icon.png` (render a 16 px; ángulo en `ICON_ANGLE`).
- Java: extiende `GeoSwordItem` (melee) o implementa `GeoItem` y llama a `registerSyncedAnimatable`.
  El renderer se registra **a mano** en `SNClientEvents` (`registerGeo`). Animaciones desde el
  servidor: `triggerAnim(player, GeoItem.getOrAssignId(stack, level), "main", nombre)`.
- Modelo de ítem: `SNItemModelProvider.geoWeapon(item, alto)`: transforms de `handheld` vanilla
  girados −45° en Z y escalados por `min(1, 20/alto)`, con el icono en la GUI vía
  `neoforge:separate_transforms`. Los libros usan `geoTome` (portada = cara norte; en primera
  persona hace falta girar **200°** por el espejo de X).

### El Colt (pistola GeckoLib con brazos)
- Modelo a **4 unidades por píxel** (`colt_art.py`), origen en la mano que empuña, cañón hacia −Z.
  Los transforms de `SNItemModelProvider.colt()` son ¼ de escala; en 3.ª persona `(0, 180, 180)`
  lo pone recto en una mano levantada. En 1.ª persona coloca la mano `ColtItemExtensions.applyForgeHandTransform`.
- `geomodel.Cube(density=k)` da k texels por unidad a un cubo (el grabado NON TIMEBO MALA usa 4);
  un cubo con una sola cara se empaqueta solo con su rectángulo.
- Huesos procedurales (`cylinder`, `chamber_*`, `round_*`, `muzzle_flash`) los mueve `ColtModel.setCustomAnimations`;
  ninguna animación puede tener claves en ellos (lo comprueba `ColtAssetsTest`). Cada clip fija
  martillo y gatillo (`settle`): GeckoLib devuelve a reposo los huesos que el clip no toca.
- Todo se dispara por payloads propios (`ColtShotPayload`, `ColtActionPayload`), no por el sync de
  GeckoLib: el tirador predice su disparo y el eco del servidor solo añade trazador, impacto y ejecución.
  Para reiniciar un clip que ya suena hace falta `forceAnimationReset()` antes de `tryTriggerAnimation`.
- Brazos en 1.ª persona (`ColtArmsLayer`): en los huesos ancla `hand_r`/`hand_l` se busca la mano y el
  brazo se tiende hacia un hombro fijo (`GUN_SHOULDER`, `SUPPORT_SHOULDER`) con una base estable (sin
  giro sobre su eje). **Ojo:** la pila de las manos de vanilla empieza con la rotación de la cámara
  (`GameRenderer.renderItemInHand`): está centrada en la cámara pero alineada con el mundo, así que todo
  vector "de cámara" hay que girarlo con `camera.rotation()` (si no, al mirar abajo el brazo se da la vuelta).
- PlayerAnimationLib (opcional, `-Ppal`): sus brazos van con nombres **cruzados** (su `left_arm` es el
  brazo del arma de un diestro) y sus expresiones Molang con `query.head_x_rotation` no dan lo que
  parece: los clips de `player_anims.py` usan ángulos fijos. Apuntar al disparar lo hace `ColtArmPoses`.

## Ver el arte sin abrir el juego

```bash
cd tools/artgen
python3 sheet.py ../../src/main/resources/assets/supernaturalcraft/textures/item /tmp/x.png 6   # hoja de contacto
python3 -c "import lucifer_art, preview3d; r,p=lucifer_art.rig(); t,g=lucifer_art.Painter(r,p,4).paint(); preview3d.render(r,t,'/tmp/l.png', scale=4)"
```

`preview3d.render(..., pose=preview3d.pose_from(anim_json, nombre, t))` muestra una pose de animación.
Abre los PNG con la herramienta de lectura de imágenes para revisarlos.

## Ver el juego de verdad (capturas automáticas)

`screencapture` de macOS **no funciona** en este entorno. Usa el arnés `client/dev/DevPreview`,
inerte salvo con la variable `SN_PREVIEW`:

```bash
rm -rf runs/client/saves/sn_preview && cp -R runs/gameTestServer/world runs/client/saves/sn_preview
SN_PREVIEW=lucifer1,lucifer2,lucifer3,lucifer4,demons,ritual,arena ./gradlew runClient -Ppreview
SN_PREVIEW=fight ./gradlew runClient -Ppreview    # combate real (~2,5 min): captura cada 3 s y fuerza las fases
SN_PREVIEW=gui   ./gradlew runClient -Ppreview    # HUD de maná y el libro (scriptorium y diario)
SN_PREVIEW=book  ./gradlew runClient -Ppreview    # el Libro del Cazador: todas las pestañas y tomas, escalas 2 y 3
SN_PREVIEW=weapons ./gradlew runClient -Ppreview  # cada arma 3D en 1ª y 3ª persona, inventario y Forja
SN_PREVIEW=eclipse ./gradlew runClient -Ppreview  # mediodía despejado → eclipse subiendo → cenit
SN_PREVIEW=amara ./gradlew runClient -Ppreview    # Amara bajo el eclipse: intro, hitboxes, fases 2-4 y muerte (~95 s)
SN_WEAPON_FROM=5 SN_PREVIEW=weapons ./gradlew runClient -Ppreview   # empieza por el arma nº 5 de SHOWCASE
SN_PREVIEW=chorus_model ./gradlew runClient -Ppreview   # Broken Chorus: descenso, fases, hitboxes (~90 s)
SN_PREVIEW=chorus ./gradlew runClient -Ppreview         # combate real con altar, campanas y columnas (~3 min)
SN_PREVIEW=spire ./gradlew runClient -Ppreview          # Hymnal Spire sobre una montaña sintética + haz nocturno
SN_PREVIEW=wings ./gradlew runClient -Ppreview          # Seraph Wings (Curios): reposo, agachado, de frente, cayendo
SN_PREVIEW="spire_real:x,z" ./gradlew runClient -Ppreview   # una Spire de la worldgen real (ver abajo)
SN_PREVIEW=colt ./gradlew runClient -Ppreview            # Colt: 1.ª persona (reposo, disparo fotograma a fotograma,
                                                         # ejecución, recarga, inspección), 3.ª persona y noche
SN_COLT_FROM=225 SN_PREVIEW=colt ./gradlew runClient -Ppreview -Ppal   # solo 3.ª persona, con PlayerAnimationLib
SN_PREVIEW=hell ./gradlew runClient -Ppreview            # la Jaula desde una puerta, la isla, dentro, debajo; las 3 regiones; hellhounds
SN_PREVIEW=uncaged ./gradlew runClient -Ppreview         # la Jaula se abre, baja Lucifer Uncaged y sus 6 aspectos
SN_PREVIEW=rift ./gradlew runClient -Ppreview            # una grieta abierta en el Overworld
SN_PREVIEW=azazel ./gradlew runClient -Ppreview          # Azazel: intro, raíles cargados, atrapado, fase 2, humo, muerte
SN_PREVIEW=azazel_fight ./gradlew runClient -Ppreview    # combate real sobre el hombro (jugador invulnerable), fase 2 a mitad
SN_PREVIEW=lilith ./gradlew runClient -Ppreview          # Lilith: intro, lápidas, luz blanca, fases 2-3, muerte
SN_PREVIEW=lilith_fight ./gradlew runClient -Ppreview    # combate real de Lilith (fases 2 y 3 forzadas)
SN_PREVIEW=metatron ./gradlew runClient -Ppreview        # Metatron: intro, biblioteca, mano escribiendo, atril, libro, Tablilla, La Caída, Reescribir, muerte
SN_PREVIEW=metatron_fight ./gradlew runClient -Ppreview  # combate real de Metatron (fases 2-4 forzadas, ~70 s)
```

- Las capturas quedan en `runs/client/screenshots/sn_*.png` (bórralas antes con `find runs/client -name "sn_*.png" -delete`).
- El cliente se cierra solo al terminar.
- Mundo real para `spire_real`: `runs/server` con `enable-rcon=true` (la consola no recibe stdin a través de
  Gradle), `./gradlew runServer`, RCON: `locate structure supernaturalcraft:hymnal_spire`, `forceload add …`,
  `save-all flush`, `stop`; luego copia `runs/server/world` a `runs/client/saves/sn_preview`.
- Para escenas nuevas, añade un `case` en `DevPreview.scenes()`.
- El cliente abre una ventana en el Mac del usuario: úsalo con moderación.
- El arnés desactiva `pauseOnLostFocus`: sin eso la ventana sin foco pausa el juego y todas las
  capturas salen con el menú de pausa.
- Tras cambiar de ítem en la mano, espera ~50 ticks antes de capturar (la animación de equipar).
- El mundo `sn_preview` sale del de GameTests: **no genera estructuras** (por eso `spire_real` usa otro mundo y las escenas
  del Infierno construyen la Jaula con `CageBuilder`). Al tener una dimensión de datapack, Minecraft pide copia de
  seguridad al abrirlo; el arnés pulsa "sin copia" solo.
- El mundo `sn_preview` guarda eclipses y clima de escenas anteriores: si una escena necesita día
  despejado, ciérralos (`Eclipses.lock(level,false)` + `end`, `setWeatherParameters`).
- Los GameTests construyen la Jaula en el **origen del Overworld** del mundo de tests, así que `sn_preview` la arrastra:
  las escenas del Overworld deben montarse lejos de 0,0 (las de Azazel usan 400,412).

## GameTests: trampas conocidas

- Las plantillas se nombran **sin namespace**: `"gametest/empty_11x6x11"` (constantes en `SNGameTests`).
- Usa `FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "..."))` y `setGameMode(SURVIVAL)`.
  El `makeMockServerPlayerInLevel` de vanilla falla con Curios. Los FakePlayer **no** están en
  `level.players()`, así que `challengers()` del jefe no los ve.
- **No** programes `runAfterDelay`/`onEachTick` dentro de otro callback (ConcurrentModification):
  planifica todo al inicio.
- `level.isNight()` no se recalcula hasta el siguiente tick tras `setDayTime`. Los batches comparten
  mundo y hora: un test que dependa de la hora la fija él mismo y espera un tick.
- Tests de jefe: cada uno con su `batch` propio (solo puede haber un Lucifer por dimensión) y
  `cleanup(helper)` al **empezar**, porque un test fallido deja arenas activas.
- Los FakePlayer son **invulnerables** (`isInvulnerableTo` → true y `die` vacío). Para tests de
  daño al jugador usa `CurseTests.mortal(...)`: subclase que vuelve a ser vulnerable y pone
  `spawnInvulnerableTime` a 0 (los jugadores nuevos tienen 60 ticks de invulnerabilidad).
- Los jugadores de test no se tickean: **no** aplican los atributos del arma en la mano. Usa
  `CurseTests.armed(p)` (aplica los modificadores y carga el golpe) si el daño importa.
- En la plantilla grande (`ARENA`) la luz de bloque **apenas se propaga** durante el test (un
  glowstone sigue dando 0 a su lado tras 50 ticks). No hagas depender un test de
  `getBrightness` ahí: usa reglas explícitas (p. ej. `AmaraEntity.nearLitWell`) o la plantilla pequeña.
- Comprueba que un test nuevo detecta el fallo: rompe a propósito la lógica, ejecútalo y restáurala.
- Los FakePlayer tampoco aparecen en `getEntitiesOfClass`: los ataques que buscan víctimas no los encuentran.
  Expón una variante con la lista de objetivos explícita (p. ej. `TheHymn.unmaking(boss, targets)`).
- Los tests del batch por defecto corren juntos y su colocación cambia al añadir tests: algo que se
  extiende (rastros de fuego, colapsos) puede tocar al vecino. Si un test mide daño exacto, dale su batch.
- El clima es global: cualquier test con tormenta (`StormLock`, altar del coro) va en su propio batch y la quita.

## Detalles de la API de NeoForge 1.21.1 ya resueltos

- `@EventBusSubscriber(modid = …)` sin `bus` (el bus se deduce del evento; `bus` está deprecado).
- `StreamCodec.composite` admite como máximo 6 campos; para más, `StreamCodec.of(...)` (ver `CinematicPayload`).
- Los attachments no se sincronizan solos: `ArcanaData.dirty` + `SNNetworking.syncArcana` (también
  en login, respawn y cambio de dimensión).
- Ingredientes con componentes en JSON: `{"type": "neoforge:components", "items": ..., "components": {...}}`.
- JEI 19.21: `getTooltip(ITooltipBuilder, …)` (no `getTooltipStrings`, que está deprecado) e intérpretes de
  subtipo para ítems con componentes (`SIGIL_PAGE`, `SCROLL_SPELL`).
- Fuentes de Minecraft/NeoForge para consultar firmas: `build/neoForm/*/steps/patchUserDev/outputs.jar`
  y `~/.gradle/caches/modules-2/.../neoforge-21.1.221-sources.jar`. API de GeckoLib/JEI: `javap` sobre
  sus jars en la caché de Gradle.

## Pendiente / ideas

- v0.2: las ramas de progresión (qué ritual pide qué material) siguen abiertas; todo está en JSON.
- Las cinemáticas usan sonidos vanilla remezclados; la música propia de Amara es `music.end`.

- Pulir la composición de las 6 alas de P4 vistas desde atrás.
- **Vida de Amara capada**: su config (1400 + 50% por jugador) supera el tope de 1024 de `MAX_HEALTH`, así que
  en la práctica tiene 1024. El Chorus lo evita con sus reservas; Amara necesitaría lo mismo (o un atributo propio).
- v0.3: equilibrio del Broken Chorus por probar en partidas reales; música provisional (`music.credits`).
- v0.3.1: el Colt hace 60 exactos a cualquier jefe y las balas solo salen de ritual (8) o botín raro: vigilar
  que no se convierta en la única estrategia. Los brazos de 1.ª persona son el modelo vanilla tal cual.
- v0.4: el Infierno, la Jaula y Lucifer Uncaged. Equilibrio de sus 6 fases por probar en partidas reales; música provisional
  (`music.uncaged` → `music.dragon` grave). La Fallen Star aún no tiene uso.
- v0.5: Azazel es el primer jefe (2 fases, 400 PV) y su sangre forja la Llave de la Jaula. Equilibrio por probar en
  partidas reales; música provisional (`music.azazel` → `music.nether.basalt_deltas`). El Smoke Dash atrapado en los raíles
  está cubierto por la lógica (y el test de captura por teletransporte), no visto en capturas.
- v0.6: Lilith (3 fases, 500 PV) va entre Azazel y Lucifer: su ritual pide el logro `yellow_eyed` y `summon_lucifer`
  pide su **Last Seal** (se consume; perder contra Lucifer obliga a repetir Lilith: vigilar en partidas reales).
  Música provisional (`music.lilith` → `music.nether.soul_sand_valley`). El hellhound atado no tiene collar propio.
- v0.7: Metatron (tras Lucifer, 1800 PV reales, 4 fases) con mano y libro gigantes invulnerables y atril desde la F3.
  Música provisional (`music.metatron` → `music.end`). Los títulos de La Palabra no salen en las capturas del arnés
  (`hideGui` oculta los títulos); probado por GameTest y `WordJudgeTest`. Equilibrio por probar en partidas reales.
- `AutoGlowingGeoLayer` con `GeoObjectRenderer` (Curios) descoloca el brillo: las alas se dibujan a plena luz.
- Música propia del jefe y sonidos reales (.ogg) en lugar de los vanilla con otro tono.
- v0.8: cuenco de hechizos (8 hechizos), fantasmas y tumbas, bolsas de maleficio y el trato de la encrucijada. Hecho con
  varios agentes en paralelo (registros y stubs primero, luego cada parte en su paquete). Equilibrio y tiempos de recitado
  por probar en partidas reales; los brazos/pose del cuenco y el JEI/diario solo vistos en capturas. Robar sangre funciona
  aunque el PvP esté desactivado.
- v0.9: el Libro del Cazador (dashboard, diario de 72 entradas por capítulos con bestiario, scriptorium con vista previa,
  biblioteca y scrolls en lote, roadmap de 27 nodos). Hecho con 5 agentes en paralelo. Los toasts de entrada nueva y los
  clics (arrastrar el roadmap, guardar diseños) solo se han probado en código/GameTests, no a mano. Los fantasmas y
  sabuesos pueden verse invisibles en su página del bestiario (su renderer los oculta).
- Más amenazas: Caballeros del Infierno.
- Estructuras del mundo (iglesias abandonadas, encrucijadas) con loot de páginas de sigilo.
