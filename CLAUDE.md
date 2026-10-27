# SupernaturalCraft — guía técnica

Mod NeoForge **1.21.1** (Java 21, NeoForge 21.1.221) inspirado en *Supernatural*. Paquete `org.papiricoh.supernaturalcraft`,
mod id `supernaturalcraft`. Dependencias: **GeckoLib 4.9.3** (obligatoria; solo la API 4.x, la 5.x es para MC 1.21.5+);
**PlayerAnimationLib 1.1.6** (obligatoria en cliente desde v0.20; solo `compat/pal` importa su API); **JEI** y **Curios** son
opcionales y solo `compat/` importa sus APIs.

El contenido del juego y los comentarios del código van **en inglés**; la comunicación con el usuario, en español.

## Reglas de trabajo

- **No hacer commits** salvo que el usuario lo pida expresamente.
- Pregunta al usuario las decisiones de diseño de contenido nuevo (le gusta que se le pregunte).
- Cada cambio debe dejar en verde `./gradlew build` (compila + JUnit) y `./gradlew runGameTestServer`.
- Si tocas datagen: `rm -rf src/generated/resources && ./gradlew runData`. Un mismo recurso no puede estar en `src/main/resources`
  y en `src/generated/resources` (`processResources` usa `DuplicatesStrategy.FAIL`).
- Todo el arte (texturas, modelos GeckoLib, animaciones) sale de `tools/artgen/` (Python sin dependencias, **no hay Pillow**). Nunca
  edites PNG/JSON de arte a mano: cambia el script y regenera. `generate.py` no es determinista (orden de sets): tras regenerarlo todo,
  restaura con git el arte antiguo reescrito sin cambios reales.
- **Todo lo nuevo va al diario**: cada mob, ítem clave, jefe, estructura, hechizo o mecánica tiene su entrada en `datagen/journal/`
  (texto en inglés + condición de desbloqueo) y, si es progresión, su nodo en el roadmap. `JournalEntriesTest` falla si un mob o un
  jefe del mod no tiene entrada (los mobs nuevos van también a su lista `CREATURES`).
- En Windows: Git Bash con sintaxis POSIX y `python` (no `python3`).

## Comandos

```bash
./gradlew build                          # compila + JUnit (src/test)
./gradlew test --tests '*FooTest'        # un test JUnit
./gradlew runData                        # modelos, lang, loot, tags, recetas, sonidos, logros, worldgen
./gradlew runGameTestServer              # GameTests (src/main/java/.../gametest): "All N required tests passed"
./gradlew runClient                      # cliente de desarrollo
./gradlew runGameTestServer -Paudit      # con contadores de red/tiempos: vuelca runs/gameTestServer/sn_audit.md al parar
python tools/artgen/generate.py          # regenera TODO el arte
python tools/structgen/empty_template.py # plantillas NBT vacías para GameTests
python tools/soundgen/generate.py        # sonidos sintetizados → OGG mono (libvorbis de GStreamer; si no, ffmpeg estéreo)
./gradlew test --tests '*LayoutDumpTest' # vuelca los planos del kit a build/layouts/*.json
python tools/structview/structview.py build/layouts/X.json --out build/layouts/png   # renderiza un plano (iso, cenital, cortes)
```

## Mapa del código

| Paquete | Qué hay |
|---|---|
| `SupernaturalCraft`, `SNConfig`, `SNClientConfig` | Orden de registro y listeners; config SERVER (por jefe, `balance`) y CLIENT |
| `registry/` | `All*` con `DeferredRegister` + `init()` vacío; `SNRegistries` (registros de datapack `sigil`, `ritual_pattern`) |
| `balance/` | Curva de poder: `ProgressionScale` (puro), `Balance` (con config), `Vitality`, `DefenceEvents` (Aegis, corazones, facción) |
| `hunter/`, `weapon/` | Sal, trampas, agua bendita, `CombatEvents`; perfiles de arma (data map), runas, melee, catalizadores, maldiciones, Forja Infernal, `ascension/` |
| `magic/`, `ritual/`, `bowl/`, `hex/` | Maná, sigilos, escuelas (`school/`), maestría (`mastery/`), coste único (`ArcanaCost`), FX (`fx/MagicFx`); rituales (patrón, receta, `effect/`, bloques); cuenco = brujería (`bowl/witch/`, componentes personales, páginas); bolsas de maleficio |
| `mark/`, `coven/`, `cain/` | Marca de Caín (sed); brujas, iglesia, aquelarre, Rowena (`coven/rowena`) y el desenlace (`coven/ending`); granja de Caín |
| `entity/boss/` | `BossAttack` + `AttackScheduler` genéricos y un paquete por jefe (lucifer, azazel, lilith, metatron, amara, chorus, uncaged, chuck, horsemen, michael, gabriel, raphael, naomi, zachariah, cain, abaddon, rowena, grandwitch). Números puros en `*Balance`; contrato con el arte en `*Assets`/`*Animations`/`*Bones` |
| `arena/` | `ArenaController` (snapshot copy-on-write, `mutate`, `protect`), `ArenaTheme`, `ArenaRescue`, `ArenaTerrain` |
| `crossroads/` | Demonio de encrucijada y el trato (`DealTerms`, `BossProgression` puros), deudas y sabuesos; `wild/` encrucijada natural |
| `hell/`, `heaven/`, `memory/` | Infierno (sima, grietas, Jaula); Cielo personal (parcelas, puertas, Roadhouse de Ash, hogar); recuerdos |
| `leviathan/`, `entity/leviathan/` | Leviatanes (v0.25): reglas puras (`LeviathanRules`), bórax (`Borax`, `BoraxSensitive`), sedación, infiltración y apertura (`LeviathanSpawns`), viscosidad (`LeviathanGooBlock`); jefe en `entity/boss/dick` |
| `allegiance/`, `legacy/`, `trickster/`, `author/` | Facciones y poderes; Hombres de Letras (búnker, casos, investigación, `gen/` procedural); bromas de Gabriel; cabaña del Autor |
| `journal/`, `client/book/` | Libro del Cazador: datos comunes (`HunterLog`, entradas, roadmap) y la pantalla con sus pestañas |
| `structure/`, `grave/`, `buildkit/`, `layout/` | Hymnal Spire; tumbas; kit **puro** de construcción (`Canvas`, `Palette`, `Walls`, `Roofs`, `Furniture`…) y `LayoutPlan` |
| `reward/`, `eclipse/`, `cinematic/`, `light/`, `weather/` | Recompensas de jefes; eclipse ritual; cámaras; luz temporal; `StormLock` |
| `network/` | Payloads + `SNNetworking`; los handlers de cliente solo se referencian desde lambdas |
| `client/` | Renderers, HUD, pantallas, un paquete por jefe o sistema, `dev/DevPreview` |
| `compat/`, `datagen/`, `command/`, `gametest/` | Integraciones opcionales; datagen (`SNDataGenerators`, `SNLang`); `/supernatural …` (nivel 2); GameTests (`SNGameTests`) |

## Recetas para añadir cosas

### Un ítem o bloque simple
1. Regístralo en `registry/AllItems` / `AllBlocks` (los bloques antes que los ítems).
2. Arte en `tools/artgen/items.py` o `blocks.py`.
3. Datagen: modelo (`SNItemModelProvider` / `SNBlockStateProvider`), loot (`SNLootTableProvider`), tags (`SNTagsProviders`), receta
   (`SNRecipeProvider`). El nombre inglés sale del id (`SNLanguageProvider.titleCase`; excepciones en `NAMES`). La pestaña creativa
   lista todo sola.

### Un sigilo (hechizo)
- **Solo datos**: `data/supernaturalcraft/supernaturalcraft/sigil/<id>.json` (`kind`, `behavior`, `tier` 1–4, `mana_cost`, `cooldown`,
  `reagents`, `params`, `color`, `school` en los efectos). Los modificadores usan `behavior: supernaturalcraft:modifier` (`params`: `mana_multiplier`,
  `potency_multiplier`, `duration_multiplier`, `range_multiplier`, `area_bonus`, `echo`).
- Forma o efecto nuevo: `SpellBehavior.Form`/`Effect` en `SpellForms`/`SpellEffects` (`registerAll()`). Glifo en `GLYPHS` (y `COLORS`)
  de `tools/artgen/gui.py`; texto con `sigil(...)` en `SNLang.sigils`. Reacciones especiales: `SpellHooks.Bindable/Revealable/Exorcisable`.
- Todo lo que busca un sigilo pasa por `magic/spell/SigilLookup` (si no, las fórmulas generadas fallan en silencio).

### Un ritual
- Patrón `data/supernaturalcraft/supernaturalcraft/ritual_pattern/<id>.json`: una capa a la altura del altar; `A` = altar, espacio =
  cualquier cosa, el resto claves con `BlockPredicate`.
- Receta `data/supernaturalcraft/recipe/ritual/<id>.json` (`type: supernaturalcraft:ritual`, `pattern`, `ingredients` ≤ 8 sin orden,
  `activator`, `consume_activator`, `conditions`, `duration`, `mana_cost`, `effect`). Condiciones: `time`, `dimension`,
  `requires_advancement` (admite lista), `eclipse`, `weather` (`any|rain|thunder`), `allegiance` (`faction`, `rank|min_rank`, `chosen`).
- Efecto nuevo: `RitualEffect` con `MapCodec` en `RitualEffect.bootstrap()`; `jei.supernaturalcraft.effect.<ns>.<path>` en `SNLang`.
- El maná máximo base es **100** (+50 Morningstar's Spark, +25 Void Mark, +25 por rango de facción, +5/10/20 de maestría, +25 por
  trato hasta +100, +10 recuerdos Kin, +50 por Rite of Deepening sin tope): ningún ritual puede costar más de 100.
- **Ritos del pozo** (`magic/mana/DepthRules` puro, `DeepenWellEffect`): Deepening en el altar (+50 máx.), Wellspring en el cuenco
  (página `wellspring`; regeneración = 2 % de la barra/s + 1 % por rito, para todos). Cuestan la barra entera
  (`RitualEffect.manaCost` / `BowlSpellEffect.manaCost`) y la piden llena; tramo por nº de ritos (1–5, 6–10 ×2, 11+ ×4 + Lucifer's
  Grace / Azazel's Blood); descanso 1, 2, 4, 8 días por tipo; cada 10 ritos un título (`main/well_10|20|30`). Toda receta (ritual, cuenco) lleva `"school"`.
- `"time": "night"` nunca se cumple en dimensiones de hora fija (Nether, Infierno, Cielo).

### Un arma
1. Ítem en `AllItems` (melee 3D: `extends GeoSwordItem`; catalizador: `extends CatalystItem`).
2. Perfil en `data/supernaturalcraft/data_maps/item/weapon_profile.json` (`tier`, `rune_slots`, `kind`, `holy`, `cursed`).
3. Obtención: ritual `craft_item` (`bind_to_ritualist` para las ligadas) o botín de jefe. Tags `HOLY_WEAPONS`/`DEMON_BANE`/`SWORDS`.
4. Arte: sprite en `weapons_art.py` o rig en `weapon_models.py` + `geoWeapon(...)` en `SNItemModelProvider` + `registerGeo` en
   `SNClientEvents`. Habilidades escaladas con `Ascension.scale(stack, base)` (contra `#bosses`: `Ascension.vsBoss`).
5. Texto `tooltip.supernaturalcraft.weapon.<id>` (`SNLang.arsenal`); GameTest en `ArsenalTests`; `SN_PREVIEW=weapons`.

### Un mob (GeckoLib)
1. Arte: módulo en `tools/artgen/` (ver `demon_art.py`: `humanoid()` o `geomodel.Rig`, `paint.py`, `animkit.AnimFile`) en `MODULES`.
2. Entidad `GeoEntity` (ver `DemonEntity`): controlador base (idle/walk) y `action` con `triggerableAnim`; `triggerAnim("action", n)`.
3. `AllEntities`, atributos y spawn en `entity/SNEntityEvents`, renderer en `client/SNClientEvents`, huevo en `AllItems`
   (`template_spawn_egg`), loot en `SNEntityLoot`, tags, entrada de bestiario (`.creature(tipo)`) y `JournalEntriesTest.CREATURES`.

### Un jefe
- Subclase de `LuciferEntity` (ganchos sobrescribibles: `maxPhase`, `threshold`, `pool`, `healthScale`, `walks`, `vulnerability`,
  `isAerialPhase`, cinemáticas, `tickTransitionMotion`, `leaveBehind`, `onDefeated`…; sus valores por defecto son el Lucifer de
  siempre, no los cambies sin sus tests). Política de daño en `hurt()`, umbrales de fase sin saltos, muerte larga con `die()` al final.
- Ataques con `BossAttack` + `AttackScheduler` (instancia nueva por uso). Avisa siempre en el suelo en el windup
  (`TelegraphMarker.circle/ring/line/cone`) y haz daño solo en ACTIVE. Colores: rojo fuego, azul hielo, violeta telequinesis, dorado
  sagrado, verde zona segura, amarillo humo.
- Añádelo a `BossProgression.Boss` (orden, `optional`, logro de muerte) y a `ProgressionScale.STATS`: con eso recibe shards, corazones,
  crédito de jefe y lore. A mano: `#supernaturalcraft:bosses`, `BossCommands.Boss`, nodo en el roadmap de la Jaula, `JournalEntriesTest`.
- **Vida y daño**: todos tienen 1000 de vida vanilla (`VANILLA_BASE`; el atributo está capado a 1024) y `healthScale()` = puntos reales
  por punto vanilla. Implementa `CappedBoss`, vida real de `Balance.bossHealth(Boss)` × `difficulty().health()`, cada golpe por
  `BossDamage.scale(...)` y un `BossHealthGuard` (`tick` antes de `super.tick()`, `accept` tras cada cambio propio).
  **v0.24: se diseña por daño.** `ProgressionScale` guarda golpes objetivo por jefe y la vida = golpes × `referenceHit(tier)` (la
  Angel Blade ascendida **sin** el ×2 del arsenal; `bladeHit` es la real) × `bane` (×1,5 para los demonios). No hay tope por golpe: solo el suelo de fase, el del Colt y
  `perHitCapFraction` (config, apagado). El daño de las armas del mod pasa por `WeaponAttributes.scaled` / `WeaponDamage.ability` (×2), y el de sigilos y
  poderes de facción por `WeaponDamage.magic` (×2).
  **Dificultad** (`BossDifficulty` puro + `DifficultyLock`): se fija al empezar la pelea (NBT `Difficulty`) y escala vida, daño,
  windup, pausa, recover (`AttackScheduler.Host.recoverScale`) y esbirros (`minionCount`/`minionSpots`). Pacífico = modo
  historia (los jefes no desaparecen). El servidor de GameTests corre en Normal. Para fijar vida en comandos/tests/previews:
  `BossHealthGuard.set(entity, h)`. Sus golpes salen por `BossStrike` (85 % normal + 15 % Divine Wrath) × `Balance.bossDamage(Boss)`.
  Los umbrales de mecánicas son fracciones de la vida real; esbirros y mobs normales no escalan.
- **Arena**: `LuciferSummoning.openArena(level, c, r, theme[, exclusive])` y **todo** cambio de terreno con `ArenaController.mutate`
  (nunca `setBlock`), que se restaura solo; `revertAfter` para cambios temporales. Lo que deba apagarse o cambiar no puede estar en
  `#arena_immune`. Decals irrompibles (raíles de Azazel, lápidas de Lilith, aceite de Rafael) también van por `mutate`. Suelos temáticos
  grandes con `HorsemenGround` (`mapped`/`fixed`, ~300 bloques/tick); el tema de arena (`ArenaTheme`) fija altura, rescate de caídas y
  `minSnapshot`; colores y música por fase en `client/arena/ArenaStyles`.
- **Visual**: cada jefe tiene su `*FxPayload` (tipos y argumentos en su javadoc) y cinemáticas con `CinematicPayload` +
  `CinematicLocks.play`. Barras propias: el HUD del cliente reconoce las claves `entity.supernaturalcraft.<jefe>.bar*`.
  Los dos Lucifer comparten el HUD de la Jaula (`client/lucifer/`, arte en `lucifer_gui_art.py`): barrotes por fase en
  `CageBars` (puro), góticas propias (`Gothic`, `glyphs.json`; lo que no cubre el atlas sale en fuente vanilla) y sus títulos
  van por `LuciferFxPayload.TITLE` (el `CinematicPayload` de esos momentos ya no lleva texto).
  Durante una secuencia de cámara (`CameraDirector`) se cancelan todas las capas de GUI salvo las registradas con
  `CameraDirector.showDuringSequences(capa)`: **toda capa con tarjetas de título debe registrarse**, y si además dibuja HUD
  de juego, ocultarlo ella misma con `CameraDirector.active()` (así Miguel, Gabriel y el Cielo). Lo comprueba a ojo la
  escena `camera_titles` (la tarjeta de cada jefe con la cámara en marcha).

### La cadena de mando: cambiarla o ampliarla (guía rápida)
- **Mapa:**
  - **Reglas puras con test:** `chain/` (`Hierarchy`, `Officer`, `ChainRank`, `Conclave`, `Feud`, `Bonds`, `BondDrift`, `Membership`,
    `Front`, `War`, `Temperament`) y `chain/narrative/`.
  - **Servidor:**
    - `ChainEvents` (tick 1/s, cónclave diario, sync);
    - `ChainCourt` (interregno y peleas internas);
    - `ChainLife` (vínculos, retos, guerra, encargos, puestos de jugador);
    - `OfficerSpawner` (materializar);
    - `FeudStage`/`DuelStage`/`ClashStage` (peleas en el mundo);
    - `ChainTalk` (diálogo y mensajes);
    - `OfficerVoice`.
  - **Cliente:** solo lee `ClientChain` (es el contrato: añade ahí los helpers). HUD en `client/chain/` y libro en `client/book/chain/`.
- **Cambiar números:** son constantes de la clase pura que manda (`ChainRank` topes, `ChainRules`, `Feud`, `Bonds`, `War`, `Front`,
  `Membership`, `ChainTalk`, `DuelStage`). Los tests derivan de las constantes: no fijes literales.
- **Datos nuevos:**
  - Un campo en `ChainSavedData` se lee con `contains(...)` y un valor por defecto (los mundos viejos no lo tienen) y se limpia en `reset`.
  - Si cambias `ChainSyncPayload`, sube `SNNetworking.VERSION`.
- **Texto nuevo** (frase, crónica, opción de diálogo):
  - Añade al **final** de `Situation` / `Chronicle.KEYS` / `TalkOption`.
  - Pon el recuento en `VoiceLines` y el inglés en `datagen/chain/ChainNarrativeLang`, más el español. `ChainNarrativeTest` falla si falta algo.
  - Registra los eventos con `data.log(new History(…))` usando los prefijos de `detail` (`player:`, `rank:`, `sector:`, `days:`) u `otherFaction/otherId`.
- **Un mob nuevo que pueda ser oficial:**
  - su pool en `ChainRoster`;
  - `ChainEvents.factionOf` (y con ello `eligible`);
  - el `officerId` sincronizado en la entidad (`OfficerSpawner.apply`);
  - `ClientChain.entryFor`.
- **Una rama nueva** (como `FALLEN`): `ChainFaction.Branch`, `ChainFaction.hasFallen`, `Officer.camp()`, `Voice`,
  `ChainNames.epithet` y `Kin` (de qué lado lucha).
- **Una facción nueva** (v0.28 ya añadió la tercera, `LEVIATHAN`; sigue su rastro con `grep -rn "LEVIATHAN" src/main/java`):
  - **El perfil manda:** el valor va **al final** de `ChainFaction` (se guarda por id y por ordinal) con su `Side` (HELL, HEAVEN o
    NONE = nadie la sirve), `talks()`, `seatOffset` (ángulo desde la sede del Foso; las dos primeras no se mueven nunca) y, si
    hace falta, `ownTitles()` / `hasFallen()`. Todo lo genérico ya pregunta a esto: `rivals()`, `hostile()`, `ofSide()`,
    `Officer.camp()` (por quién pelea), `ChainEvents.sameSide`, `Conclave.run(Map…)`, `Front` (presión por sector y facción;
    `admit` le da su parte al despertar), `War` (dos bandos cualesquiera, `warSides`), `ChainMark` (calor por id).
  - **Los `switch` sobre el enum** son lo que el compilador te obliga a completar: `SeatPlans` (plano y límites),
    `SeatBuilder`/`SeatPlacement` (botín, cimientos), `ChainRoster`, `ChainEvents.recruits`, `ChainAtlas`/`WarPanel`/`ClientWarTable`.
  - **Después, en orden:**
    1. sede: un plano puro con el contrato de `DungeonAnchors` (más anclas propias en `SeatWorld.Anchors.point`) y sus `Bounds`;
    2. roster, mobs y arte (texturas `_<facción>` gemelas de las hojas de GUI: `ChainAtlas.slot`, `ChainOverlay.sheet/row`);
    3. nombres (`ChainNames`, también `BANNED`) y títulos (`chain.supernaturalcraft.rank.<facción>.<rango>` si `ownTitles`);
    4. voz (`Voice`; si no habla, `talks()` y solo las situaciones del mundo + PARLEY_REFUSE/COWARD) y narrativa;
    5. despertar en `ChainGates` (y `ChainLife.admit` en el frente);
    6. cliente: botón de facción (`ChainSection.listed`), lado del HUD (`ChainOverlay.side`), colores;
    7. tests (`ThreeFactionsTest`) y GameTests.
  - **Un jefe en el trono** (`Officer.boss()`, hoy Dick Roman): cuenta como `held()` (ni dados ni materializar), se pelea en su
    sede (`SucroPenthouse`) y al morir marca `founderFallen`; si la facción `fallsWithFounder`, queda decapitada (`headless`).
- **Probar:**
  - comandos `/supernatural chain …` (`ChainCommands`, `ChainLifeCommands`);
  - GameTests en `ChainTests`/`ChainLifeTests`;
  - previews del libro con datos falsos en `client/dev/ChainBookPreview` (`SN_PREVIEW=chain_modal,chain_bonds,chain_war,chain_duel,chain_talk`).

## Reglas técnicas por sistema

Solo lo que no se deduce del código; el diseño de cada pelea está en su paquete y en el diario.

- **Jefes con partes** (Amara, Broken Chorus): `PartEntity` como el Ender Dragon (ids reservados con `ENTITY_COUNTER`, no se guardan ni
  se envían, daño por `hurtPart`). Una sola fuente de posiciones compartida por hitboxes y renderer (`partOffset`, `ChorusGeometry`, que
  reproduce la cadena de GeckoLib; lo comprueban `ChorusGeometryTest`/`ChorusAssetsTest`). Base de tiempo `getGameTime() + partial`, nunca
  `tickCount`. Las animaciones JSON no pueden tocar huesos que mueve el renderer (también Colt, Miguel, Chuck: `*AssetsTest`).
- **Constructos no vivos** (mano y libro de Metatron): `Entity implements GeoEntity`, sin hitbox ni daño, movidos con `order(pos, ticks)`.
- **Sitios únicos ocultos a `/locate`** (cabaña del Autor, búnker): `*Site` repite el cálculo por semilla, `*Placement`, y
  `*World.ensure…` los levanta en mundos viejos. Sin block entities en lo que restaura una arena.
- **Estructuras procedurales** (Hymnal Spire, tumbas, encrucijada natural): `findGenerationPoint` barato, el plan puro en el
  `GenerationStub` y guardado en la pieza; nada de `SavedData` en `postProcess`; decisiones por hash de posición + semilla.
- **Planos "de construcción real"** (`buildkit/`, Cielo, salas de jefe, escenas de recuerdo): `LayoutQuality` exige ids válidos (lista
  1.21.1 en `src/test/resources`), formas, soportes, accesibilidad, luz interior, variedad y profundidad de paredes. Revisa los PNG de
  `structview` antes de dar un plano por bueno. `HorsemenGround.state` convierte un estado mal escrito en aire sin avisar.
- **Dimensiones** (Infierno `SNHell`, Cielo `SNHeaven`): entradas de datapack en datagen. El servidor de GameTests **no** las tiene:
  la lógica recibe el nivel destino (las parcelas del Cielo viven en el Overworld en (16384, 100, 16384): `HeavenPlots.level`).
  Cargar una `DensityFunction` en JUnit falla: lo testeable va en clases sin tipos de Minecraft (`PitShape`, `CageLayout`, `PlotGrid`).
- **Facciones**: nunca `getType().is(DEMONS|ANGELS)` a secas; usa `Kin.isDemon/isAngel/freeWill` (vale para jugadores). Cambios con
  `Allegiances.set` (sync inmediato) o `update`; el sync llega también a quien te rastrea. Los rangos son logros imposibles por código.
  La esencia (v0.20) va siempre en **fracción de la barra** (`EssenceRules.*_SHARE`, `Rest`): se rellena sola en 5 min en todos los
  rangos (rezar/fuego más rápido, en `Passives.regen`); los costes de los poderes son absolutos. Rueda: `Power.WHEEL_MAX` activos
  (`WheelLayout`). `PowerRules.Verdict` y `AllegianceFx.MESSAGES`/`Passives.WHISPER_*` solo admiten añadir al final. Los poderes
  de v0.20 están en `AngelPowers`/`DemonPowers`/`Crossings`; sus objetivos pasan por `PowerTargets.foe` (los de área no tocan inocentes).
- **Hombres de Letras**: orden aparte de la facción (`Legacy`/`Archive` attachments, `Legacies.set/update`). Lo procedural de
  `legacy/gen` es determinista por semilla + UUID + índice con topes duros; el Archivo es una pestaña aparte y el Diario nunca lista
  sus entradas. Expedientes de criaturas: nunca contra `#bosses`. Las notas de campo (v0.19) no se llevan encima: se archivan
  como contadores en `Archive.notes` con `NoteLedger` (bajas, jefes, casos y cofres que abre un miembro) y la investigación las cobra
  de ahí. El ítem `field_notes` solo queda como nota suelta, y un miembro la archiva al recogerla o usarla.
- **El Cielo** (v0.18): puertas por rito, parcela por cazador en espiral (`PlotGrid`), `PlotWriter`/`DecorWriter` por lotes. Recuerdos
  en `MEMORY_LOG`; `MemoryStage` escribe cada escena con una arena privada y la restaura al salir. Naomi y Zachariah usan arenas no
  exclusivas (`exclusive=false`); la oficina infinita es una tesela de 16 que `OfficeWrap` repite. `/supernatural heaven|memory …`.
- **War Table** (v0.19, `legacy/cases/table/`): 9 `map_table` en 3×3 se funden solo al colocarlas a mano (`setPlacedBy`, nunca en
  `onPlace`: worldgen y arenas escriben piezas sin fundir); la BE vive en el centro (`isValidBlockState`). Una pieza suelta conserva el
  comportamiento antiguo. El terreno se predice del generador en un hilo aparte (`TerrainSurvey`, sin cargar chunks) y se superpone lo
  cargado; calibración con `/supernatural legacy table survey`. La brújula de los Hombres de Letras apunta al caso o a una chincheta
  (componente `compass_target`).
- **Progresión que no se pierde** (v0.19): los ítems de `#progression_keys` que gasta una invocación (`RitualAltarBlockEntity`
  reserva el activador; `Stakes.offer` → `ArenaSavedData.create`) son la `Stake` de su arena: se devuelven si no hay victoria
  (también al parar el servidor), por `PendingReturns` si el dueño no está. Un `BossSummonEffect` se rechaza antes de gastar nada
  si otra pelea ocupa la dimensión. Esos ítems caen uno por participante (`BossShares`) y el crédito del jefe llega también a
  caídos y ausentes. Un jefe que cuenta con la stake no suelta su compensación (`LuciferEntity.staked()`); Michael y Death la
  "prestan" (`lendStake`). Páginas, mapa del Autor y llave del búnker se recuperan (`LostPages`, `LegacyServerHandlers.spareKey`).
  Los calendarios usan `WorldClock.day` (vale con `doDaylightCycle` apagado); el fin de una arena suelta el eclipse y la Jaula
  (`ArenaEvents.released`).
  Quien muere dentro de una pelea conserva inventario, Curios y experiencia (`ArenaKeep`, config `arena.keepInventory`): se
  aparta en `LivingDeathEvent` LOWEST antes de soltar nada (los mods de tumbas no ven nada) y vuelve en `PlayerEvent.Clone`. Una
  tumba o cofre sobre suelo de la pelea queda sostenido por un bloque al restaurar (`ArenaController.bearsAGrave`).
  La llave del búnker solo hace falta una vez: girarla marca `Legacy.doorKnown` y la puerta se abre a mano; Henry ya no da repuesto.
- **Red y rendimiento** (v0.19): todo envío al cliente pasa por `network/Send` (mide con `AuditStats`: `-Paudit` o
  `/supernatural debug stats on|net|perf|dump [nombre]`; `debug crowd spawn N|gather|clear` mete jugadores sintéticos;
  `runClient -Pjoin=localhost -Puser=Nombre` entra directo a un servidor); lo que pide un cliente se limita con `RateLimiter` y sus cadenas van acotadas.
  Syncs: el Arcana solo manda las listas si cambian, el diario agrupa bajas, `Legacies.batch` junta cambios, la esencia solo va
  al dueño. Busca jugadores con `Players.byUuid` (índice de entidades), nunca `getPlayerByUUID` en bucles. Efectos de cliente que
  dependen de un jefe miran antes `ClientPresence`. Lo que guarda memoria por jugador se limpia en `PlayerCleanup`.
- **Pose de vuelo** (v0.20): quien vuela con alas (las de ángel o las Seraph Wings) se tumba al volar. `client/flight/FlightPose`
  (puro, con test) decide la pose por velocidad, subida y giro; `compat/pal/client/PalFlight` la aplica como capa de
  PlayerAnimationLib (por debajo de la del Colt). El hueso raíz `body` gira el jugador entero en la cintura y la librería
  le **invierte X** (cabecear adelante es X positivo); los miembros van con el signo de `ModelPart` (X positivo = hacia atrás).
  `Flyers` comparte el "está volando" con las alas, también para otros jugadores, cuyas habilidades no llegan al cliente.
- **Magia (v0.21)**: sigilos = combate; cuenco = brujería dirigida; rituales = grandes obras (los de crafteo siguen). Escuela de un
  hechizo = la de su primer efecto (`School`, `NONE` para formas/modificadores). Descuentos solo por `ArcanaCost.discount` (humano 15 %,
  amuleto 10 %, maestría; aditivo, tope 40 %; pergamino ×0,5 aparte). Maestría (`Mastery`): XP al lanzar; el rango limita **aprender**
  páginas de tier 2+ (nunca quita lo sabido). Ids viejos por `SigilAliases` (`bind`→`snare`). FX de magia solo por `MagicFx` →
  `MagicFxPayload`/`RitualChargePayload` (append-only), el cliente pone sonido y partículas; nada de partículas de servidor.
- **Brujería**: `requires_target` exige taglock o sangre con dueño (`PersonalComponent`); nunca maldice jefes ni jugadores sin PvP;
  Ward of the Name/Mirror Hex las paran. Páginas en el registro `bowl_spell_page` (una página enseña su grupo y su propio id). El
  cuenco solo se recita al encenderlo con mechero (pista en la barra de acción). Reactivos solo del cuenco: chrism, bound blood, hexed ink.
- **Marca de Caín**: un único medidor, la sed (`MarkRules` puro, `Marks`); sustituye al hambre de la First Blade y al bloodlust del
  Caballero. El rito de Caballero pide llevarla (`"mark": true`). Quitarla (`Unmarking`) conserva el rango; retomarla en la tumba
  empieza a 0,5. Desenlace por jugador en `RowenaRelation.book` (GIVEN → Gran Bruja, KEPT → Rowena; `WitchBossSlot`).
- **Cuenco de hechizos**: recetas `recipe/bowl_spell/<id>.json` (líquidos, ingredientes, `incantation`, `effect`); `BowlInput.isEmpty()`
  está sobrescrito (si no, una mezcla solo de líquidos nunca casa). Tests: `SpellBowlBlockEntity.tryLight` + `resolveRecitation`.
  Todo castigo dice por qué (`BowlBacklash.Reason`, en el chat); una mezcla fallida nombra el hechizo conocido más cercano y qué
  falta o sobra (`MixDiagnosis`, nunca uno sin aprender). `everyRecipeCastsFromWhatItsPageShows` comprueba que la página del libro es la receta.
- **Libro del Cazador**: se dibuja en espacio de libro bajo un pose escalado: `book.scissor(...)` y `book.tooltip(...)`, nada de listas
  vanilla ni `renderEntityInInventoryFollowsMouse`. El cliente no ve logros ocultos: espejo en `ClientHunterLog` vía `HunterLogSyncPayload`.
  `RoadmapTest`: padres a la izquierda, celdas únicas, ids existentes y un nodo por jefe de `BossProgression` en la Jaula.
- **Cinemáticas**: JSON en `assets/supernaturalcraft/cinematics/<id>.json` (`shots` con `duration`, `path` Catmull-Rom, `look`, `fov`,
  `roll`, `shake`, `ease`), coordenadas relativas al ancla (+z adelante, +x izquierda). `CinematicLocks.play(ancla, id, ticks, rango)`.
- **Eclipse y clima** son globales: cada test que los use en su propio batch y los termina al acabar.
- **El Colt**: mata de un disparo todo lo que no es jefe, `#colt_immune` ni jugador; a un jefe le quita el 10 % de su vida real (Chuck
  3 %; config `colt.bossShare`) sin pasar un umbral de fase. Todo va por `ColtShotPayload`/`ColtActionPayload` (el tirador predice). Brazos en 1.ª persona en
  `ColtArmsLayer`: la pila de manos vanilla está alineada con el mundo, gira los vectores "de cámara" con `camera.rotation()`.
  PlayerAnimationLib nombra los brazos cruzados.
- **Ángeles en la Tierra** (v0.22, `angel/`, `entity/angel/`): `angel` tras Lilith y `fallen_angel` tras Metatron (banderas en
  `HostsSavedData`, abiertas por los logros; `AngelSpawns.allowed` en el spawn placement). La Caída (`TheFall`) es solo visual
  (`SmiteFxPayload`). Toda muerte de un ángel no jefe (`#angels`/`#fallen_angels`) por un golpe directo con `#angelic_blades` pasa
  por `SmiteDeath`: cancela la muerte, lo congela `SMITTEN_TICKS` y luego `die(fuente original)` (loot, XP y ganchos `die()` de los
  esbirros intactos). Los ángeles salvajes reciben ×0,2 sin hoja (`AngelRules`). Caídos: `Kin.isFallen`, del lado del Infierno en
  `Kin.sameSide`. Debug: `/supernatural debug fall start|stop|star`, `debug angels open|close|status`, `debug smite`.
  Las hojas angélicas (Angel, Archangel, Gabriel) son 3D (`AngelicBladeItem`/`AngelicBladeAssets`, `geoDagger` en datagen:
  agarre invertido); su luz es el hueso `light`, a escala 0 en `idle`, y se enciende con `charge`/`flash` desde el servidor.
  En 1.ª persona `client/render/AngelicBladeHands` cancela `RenderHandEvent` y dibuja el brazo cruzado con la hoja en el puño
  (`CROSS`, `ROLL`); `SN_WEAPON_SET=angelic` captura además la carga (`_fp_charge`).
- **Leviatanes** (v0.25): no hay spawn natural; aparecen solo tras matar a Raphael (`HostsSavedData.leviathans`, abierta por su
  logro o en el login de quien ya lo mató) suplantando aldeanos de noche (`LeviathanSpawns.infiltrate`, topes en `LeviathanRules`) y en
  casos de aldea (`CaseGenerator.withLeviathans`, solo al repartir el caso). Disfraz con el formato del cambiaformas, dibujado por
  `client/render/DisguiseMask` (compartido con `ShapeshifterRenderer`). Todo lo que el bórax afecta implementa `BoraxSensitive`; el
  bórax entra siempre por `Borax.splash/touch/burn/dry`. La viscosidad solo existe en la arena de Dick y se seca/moja con
  `ArenaController.mutate` (`DickRomanEntity.dryGoo/surge`). Los dobles de Dick son `LeviathanEntity` con `master` (revientan con bórax).
  La Righteous Bone ungida (`#progression_keys`) es la Stake del ritual `summon_dick_roman`. FX por `DickFxPayload` (append-only).
- **Cadena de mando viva** (v0.27, `chain/`): temperamento, trasfondo y voz salen de `nameSeed` (nunca se guardan). Las frases son
  `Voice.key(voz, Situation, temperamento|any, n)`, con los recuentos en `narrative/VoiceLines`. La crónica sale de
  `Chronicle.KEYS` × 3. Todo esto va en `datagen/chain/ChainNarrativeLang`, y `ChainNarrativeTest` exige el inglés y el español.
  - **Claves solo se añaden al final:** `Situation`, `TalkOption` y `Chronicle.KEYS`.
  - **`History`:** lleva `otherFaction`/`otherId`. El `detail` usa los prefijos `player:`, `rank:`, `sector:` y `days:`.
  - **Vínculos:** viven en `ChainSavedData.bonds()` (`Bonds`, puro). Un actor es `"demon:12"` o `"player:<uuid>"`.
  - **Puestos de jugador:** son `Officer` con `player != null`.
    - Nunca se materializan, y `Conclave` y `Feud.heir` los ignoran (usa `Hierarchy.npcs()`).
    - Si un `Feud` cae en uno, se convierte en `Challenge`.
    - `ChainLife.membership` los sincroniza con la facción y el rango (sondeo cada 5 s).
  - **Duelos:**
    - `DuelStage` lleva cazador contra oficial: entre los del mismo bando se rinde quien cae bajo `Challenge.YIELD_FRACTION`; entre bandos es a muerte y cuenta como muerte bien hecha. Su `duelling` salta `MobReactions` y `ChainEvents.target`.
    - `ClashStage` lleva las peleas entre bandos (escaramuza o campeón) y `FeudStage` las peleas internas.
  - **Conversaciones:** van por `ChainTalkPayload` + `AllegianceDialogue.remember` (diálogo `chain_talk`), con nodos `ally|audience|parley:<f>:<id>` y `challenge:<id>`.
    - Los mensajes del libro (`ChainMessagePayload`) se contestan al amanecer (`ChainTalk.dawn`).
    - En una ORDER, `target` es el id propio, `-10 - id` para el otro bando o `-2` para declarar la guerra.
  - **Frente y guerra:** `Front` (12 sectores) y `War`. `Territory.of(…, front)` repliega a los oficiales a sus sectores; usa
    `ChainLife.territory(server, o)`.
  - **Comandos:** `/supernatural chain bonds|bond|challenge|talk|say|clash|join|leave|war|front …`.
- **La Junta de SucroCorp** (v0.28, tercera facción `LEVIATHAN`): despierta con `HostsSavedData.leviathans()` (tras Raphael), hostil
  a todo jugador, nunca habla ni admite cazadores, títulos corporativos. Mobs `BoardLeviathan` (`leviathan_executive|guard|chemist`,
  mismo rig que el leviatán, solo textura). Muerte bien hecha = bórax (`LeviathanEntity.BORAX_FINISHED`); saco o distancia, no.
  Reto con `challenge_officer_leviathan` (libro + azúcar, activador `borax_spray`). Su primer Lord es **Dick Roman** (`Officer.BOSS_TYPE`):
  la primera pelea es en el ático de la torre (`SucroPenthouse`: Righteous Bone ungido en `boardroom_door`, arena en el sitio,
  `DickRomanEntity.fightInPlace`, sin `BoardroomGround`). Su muerte **decapita** la Junta (`ChainFaction.fallsWithFounder`,
  `ChainSavedData.headless`, `ChainLife.behead`): sin interregno ni heredero, trono sellado, nadie nuevo (ni cónclave, ni corte, ni
  reclutas por muerte de jugador), fuera de guerras y escaramuzas, sus sectores a los vecinos (`Front.release`) y los que quedan
  replegados en la torre (`Territory`). Solo entonces el rito `summon_dick_roman` lo vuelve a llamar (revancha con arena;
  `SucroPenthouse.riteRefusal`). Sede: `chain/seat/tower/SucroTower`.
- **Gremios** (v0.29, facciones de jugadores):
  - **Ranuras:** `ChainFaction.GUILD_1..4` (`guild()`, listas `FIXED`/`GUILDS`). El perfil vive en `ChainSavedData.guild(slot)` (`Guild`)
    y una ranura libre duerme siempre (`setAwake` la ignora). Al refundar sube `generation`.
  - **Reglas puras con test** (`GuildsTest`): `GuildRules` (mitad de cada rango para NPCs `npcCap`, gracia, precios, nombres, sitio
    del estandarte, heredero) y `Diplomacy` (hostil unilateral, paz mutua; SucroCorp y fijas↔fijas cerradas). La hostilidad se pregunta
    siempre a `data.hostile(a, b)`, nunca a `ChainFaction.hostile`.
  - **Servidor:** `GuildLife` (fundar, estandarte, acciones del libro `GuildActionPayload`, contratar/desertar/atar, interregno,
    disolución, fuego amigo); `GuildMembers` (índice jugador→gremio y trato de los mobs salvajes); `GuildStandardBlock`/`Item` (la sede,
    sin block entity); ritos `found_guild` y `bind_officer` (`GuildRiteEffects`); comandos `/supernatural chain guild …`.
  - **Mobs de oficial:** un mob de gremio sincroniza su id con la ranura encima (`OfficerSpawner.syncedId`, cliente `OfficerTag`).
  - **El frente** tiene 24 sectores (los guardados de 12 se parten en `Front.restore`).
  - **Nunca** hay `Membership.swap` entre un NPC y un jugador en un gremio: rompería `npcCap`.
- **Ingrediente** `supernaturalcraft:written_name` (`{"type": …, "name": "Metatron"}`): libro con ese nombre escrito.

## Convenciones de GeckoLib (verificadas en bytecode y en capturas)

- Unidades en píxeles, Y arriba, origen en los pies. El modelo mira al **norte (−Z)**.
- GeckoLib **invierte X** al hornear: **+X Bedrock = lado IZQUIERDO** de la entidad (el brazo derecho va en x negativa).
- Rotaciones en grados Bedrock (GeckoLib niega X e Y): **−X** adelanta un miembro colgante / la cabeza mira arriba; **+Y** gira a la
  derecha de la entidad; **+Z** lleva un miembro colgante hacia la derecha.
- `geomodel.py`: UV por cara con desplegado `[east][north][west][south]`, arriba `[up][down]`; admite rotación por cubo (`rotation=` +
  `pivot=`) y `Cube(density=k)`. Máscaras de brillo: `<textura>_glowmask.png` (`AutoGlowingGeoLayer`).
- Ocultar un hueso **y** sus hijos: `setHidden(true)` + `setChildrenHidden(true)`. Reiniciar un clip que ya suena:
  `forceAnimationReset()` antes de `tryTriggerAnimation`. GeckoLib devuelve a reposo los huesos que un clip no toca.
- **Ítems 3D**: modelo de pie centrado en el origen (`GeoItemRenderer` ya traslada a 0.5, 0.51, 0.5); `GeoSwordItem` o `GeoItem` +
  `registerSyncedAnimatable`; renderer a mano en `SNClientEvents.registerGeo`. Desde el servidor:
  `triggerAnim(player, GeoItem.getOrAssignId(stack, level), "main", nombre)`. `geoWeapon(item, alto)` / `geoTome` en
  `SNItemModelProvider` (los libros giran 200° en 1.ª persona por el espejo de X).

## Ver el arte y el juego

Sin abrir el juego (abre los PNG con la herramienta de lectura de imágenes):

```bash
cd tools/artgen
python sheet.py ../../src/main/resources/assets/supernaturalcraft/textures/item /tmp/x.png 6   # hoja de contacto
python -c "import lucifer_art, preview3d; r,p=lucifer_art.rig(); t,g=lucifer_art.Painter(r,p,4).paint(); preview3d.render(r,t,'/tmp/l.png', scale=4)"
```
`preview3d.render(..., pose=preview3d.pose_from(anim_json, nombre, t))` muestra una pose de animación.

En el juego, con el arnés `client/dev/DevPreview` (inerte sin `SN_PREVIEW`; las escenas del Cielo están en `HeavenPreview`):

```bash
rm -rf runs/client/saves/sn_preview && cp -R runs/gameTestServer/world runs/client/saves/sn_preview
find runs/client -name "sn_*.png" -delete
SN_PREVIEW=escena1,escena2 ./gradlew runClient -Ppreview   # capturas en runs/client/screenshots/sn_*.png; se cierra solo
```

| Sistema | Escenas (`SN_PREVIEW=`) y variables |
|---|---|
| Lucifer, rituales | `lucifer1..4`, `demons`, `ritual`, `arena`, `fight` (combate real), `cinematic` |
| GUI y libro | `gui`, `book` (`SN_BOOK_TABS=home,journal,scriptorium,roadmap,archive`, `SN_BOOK_SCALES=2,3`, `SN_BOOK_FACTION=angel\|human`) |
| Armas | `weapons` (`SN_WEAPON_FROM=n`, `SN_WEAPON_SET=angelic` = solo las hojas angélicas), `balance`, `wings`, `colt` (`SN_COLT_FROM=225` = solo 3.ª persona), `flight` (pose de vuelo de perfil), `bowl` |
| Eclipse, Amara, Coro | `eclipse`, `amara`, `chorus_model`, `chorus`, `spire`, `spire_real:x,z` |
| Infierno | `hell`, `uncaged`, `rift`, `lucifer_hud`, `uncaged_hud` (barra y títulos de la Jaula, se pueden encadenar) |
| Cámara | `camera_titles` (la tarjeta de título de cada jefe durante una secuencia de cámara) |
| Tour (Modrinth) | `tour` (`TourPreview`, mundo real `sn_tour`, `runClient -Ptour`): `SN_TOUR=places,bosses,rituals,trade`, `SN_TOUR_ONLY=amara,cain` repite solo esas paradas; jefes en 1.ª persona con su barra (5 candidatas, sin cinemáticas); capturas `tour_NN_*.png`, elegidas en `branding/screenshots/` |
| Azazel, Lilith, Metatron | `azazel`, `azazel_fight`, `lilith`, `lilith_fight`, `metatron`, `metatron_fight`, `metatron_hand` |
| Chuck | `chuck`, `chuck_fight`, `chuck_arena` (`SN_ARENA_FROM=n`), `chuck_model`, `chuck_fx`, `chuck_cabin` |
| Jinetes, Miguel | `horsemen`, `war_fight`, `famine_fight`, `pestilence_fight`, `death_fight`, `michael_model`, `michael_fight`, `michael_arena`, `michael_hud` |
| Facciones, Gabriel, Rafael | `allegiance`, `gabriel`, `gabriel_fight`, `gabriel_pranks`, `raphael` (`SN_RAPHAEL_ONLY=a,b`), `raphael_house`, `raphael_fight` |
| Hombres de Letras | `legacy_models`, `legacy_bunker`, `legacy_research`, `legacy_case`, `legacy_war_table` (en el mundo plano de preview el terreno sale uniforme) |
| Magia | `magic_fx`, `magic_rituals`, `magic_bowl`, `magic_witchcraft` |
| Brujas, Rowena | `witches`, `coven`, `church`, `rowena_house`, `rowena_npc`, `rowena_fight`, `grand_witch_fight`, `witch_ending` |
| Caín, Abaddon | `cain`, `cain_farm`, `cain_fight`, `mark_hud`, `abaddon`, `abaddon_fight` |
| Ángeles (v0.22) | `angels` (vasijas), `angel_smite` (muerte con hoja angélica y quemadura), `the_fall` (cielo de la Caída) |
| El Cielo | `heaven_plot`, `heaven_memories`, `roadhouse`, `naomi_model`, `naomi_fight`, `zachariah_model`, `zachariah_fight`, `heaven_sky`, `crossroads_natural`, `heaven_book` |

- El cliente abre una ventana en el equipo del usuario: úsalo con moderación. Escena nueva: un `case` en `DevPreview.scenes()`.
  Las escenas se pueden mezclar en una lista (`DevPreview.env()`/`stop()`), salvo dos de una clase de escena única. Graba a 70° de FOV.
- FX: nunca guardes un `VertexConsumer` tras pedir otro `RenderType` ("Not building!"); lo que se acerca al ojo se desvanece (`FxDraw`).
- El arnés desactiva `pauseOnLostFocus`, pulsa "sin copia" al abrir el mundo y oculta la GUI (`hideGui` también oculta los títulos).
  Tras cambiar el ítem en la mano espera ~50 ticks antes de capturar.
- El mundo `sn_preview` sale del de GameTests: no genera estructuras, guarda eclipses y clima de escenas anteriores (ciérralos si hace
  falta día despejado) y arrastra la Jaula del origen, así que las escenas del Overworld se montan lejos de 0,0.
- `spire_real` necesita un mundo real: `runs/server` con `enable-rcon=true`, `./gradlew runServer`, por RCON `locate structure
  supernaturalcraft:hymnal_spire`, `forceload add …`, `save-all flush`, `stop`, y copiar `runs/server/world` a `sn_preview`.
- `sn_tour` es un mundo normal generado con `runServer` (semilla de `runs/server/server.properties`; arrancar, esperar a `Done`,
  `stop` por RCON y copiar `runs/server/world` a `runs/client/saves/sn_tour`). El tour busca las estructuras como `/locate` (o por
  su `*Site`), y si no hay ninguna cerca la construye con su comando `place` en suelo llano.
- `screencapture` de macOS no funciona en este entorno.

## GameTests: trampas conocidas

- Plantillas **sin namespace**: `"gametest/empty_11x6x11"` (constantes en `SNGameTests`).
- Jugadores: `FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "..."))` + `setGameMode(SURVIVAL)` (el mock vanilla falla
  con Curios). Los FakePlayer **no** están en `level.players()` ni en `getEntitiesOfClass` (expón variantes con objetivos explícitos),
  son **invulnerables** (`CurseTests.mortal(...)`), no se tickean ni aplican los atributos del arma (`CurseTests.armed(p)`) y
  `PlayerAdvancements.award` los ignora (`getOrStartProgress(adv).grantProgress(c)`, o `JournalTests.Witness` para logros reales).
- No programes `runAfterDelay`/`onEachTick` dentro de otro callback: planifica todo al inicio.
- `level.isNight()` no cambia hasta el tick siguiente a `setDayTime`; los batches comparten mundo y hora.
- Jefes: cada test en su **batch** propio y `cleanup(helper)` al **empezar**. Lo que mide daño exacto o se extiende (fuego, colapsos),
  eclipse y tormenta, también en su batch.
- En la plantilla grande (`ARENA`) la luz de bloque apenas se propaga: no dependas de `getBrightness` ahí.
- Comprueba que un test nuevo detecta el fallo: rompe la lógica a propósito, ejecútalo y restáurala.

## API de NeoForge 1.21.1 ya resuelta

- `@EventBusSubscriber(modid = …)` sin `bus` (deprecado; se deduce del evento).
- `StreamCodec.composite` admite como máximo 6 campos; para más, `StreamCodec.of(...)`.
- Los attachments no se sincronizan solos (`ArcanaData.dirty` + `SNNetworking.syncArcana`, también en login, respawn y cambio de dimensión).
- Ingredientes con componentes: `{"type": "neoforge:components", "items": ..., "components": {...}}`.
- JEI 19.21: `getTooltip(ITooltipBuilder, …)` e intérpretes de subtipo para ítems con componentes.
- Fuentes para consultar firmas: `build/neoForm/*/steps/patchUserDev/outputs.jar` y
  `~/.gradle/caches/modules-2/.../neoforge-21.1.221-sources.jar`; GeckoLib/JEI con `javap` sobre sus jars.

## Pendiente

- **v0.21 (La Marca y el Aquelarre)**: visto en capturas, no jugado; equilibrio de Caín, Abaddon, Rowena y la Gran Bruja; los
  sonidos generados solo medidos; el flujo completo elección → jefa → quitar la Marca en partida real.
- **Por probar en partidas reales** (el usuario lo prueba todo a mano mientras desarrolla; el multijugador real ya se probó con
  Lucifer): equilibrio fino de los jefes; multijugador del resto (ilusión de Guerra, posesión de Miguel, concurso de Gabriel,
  PvP y pactos de facciones); rituales en mundo real; búnker y estructuras en mundos generados.
- **v0.18 (el Cielo)**: nada visto aún en un cliente real (faltan las capturas de sus escenas); pestaña Memories con la piel del
  roadmap (falta su rampa en `book_art.py`); escenas de recuerdo sencillas.
- **Sonido**: casi toda la música es provisional (vanilla remezclada) y los sonidos generados solo se han medido, no escuchado. En
  Windows `tools/soundgen/oggenc.py` no encuentra codificador (busca GStreamer/ffmpeg en rutas de macOS).
- **Limitaciones conocidas**: tras recargar a mitad de pelea los suelos de Miguel, Gabriel y Rafael se vuelven a fijar y pueden
  desplazarse; `AutoGlowingGeoLayer` con `GeoObjectRenderer` (Curios) dibuja las alas a plena luz; los ojos de facción asumen la skin
  de Steve; un Angel Blade invocado guardado en un cofre no caduca; las criaturas de un caso en chunks sin cargar se borran;
  vigilar que el Colt no sea la única estrategia. Del Infierno solo se sale por grieta (20 min), el rito de huida o Descend (Rey del Infierno). Medido en
  servidor dedicado (v0.19, `build/audit/report.md`): 40 jugadores ≈ 9 ms/tick, el mod ~3 ms/s.
- **Ideas**: Caballeros del Infierno, iglesias abandonadas con páginas de sigilo, Rowena y las brujas (aparcadas).
