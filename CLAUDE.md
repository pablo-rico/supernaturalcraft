# SupernaturalCraft — guía técnica

Mod NeoForge **1.21.1** (Java 21, NeoForge 21.1.221) inspirado en *Supernatural*. Paquete `org.papiricoh.supernaturalcraft`,
mod id `supernaturalcraft`. Dependencias: **GeckoLib 4.9.3** (obligatoria; solo la API 4.x, la 5.x es para MC 1.21.5+);
**JEI**, **Curios** y **PlayerAnimationLib** son opcionales y solo `compat/` importa sus APIs.

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
./gradlew runClient [-Ppal]              # cliente de desarrollo (con PlayerAnimationLib)
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
| `magic/`, `ritual/`, `bowl/`, `hex/` | Maná y sigilos; rituales (patrón, receta, `effect/`, bloques); cuenco de hechizos; bolsas de maleficio |
| `entity/boss/` | `BossAttack` + `AttackScheduler` genéricos y un paquete por jefe (lucifer, azazel, lilith, metatron, amara, chorus, uncaged, chuck, horsemen, michael, gabriel, raphael, naomi, zachariah). Números puros en `*Balance`; contrato con el arte en `*Assets`/`*Animations`/`*Bones` |
| `arena/` | `ArenaController` (snapshot copy-on-write, `mutate`, `protect`), `ArenaTheme`, `ArenaRescue`, `ArenaTerrain` |
| `crossroads/` | Demonio de encrucijada y el trato (`DealTerms`, `BossProgression` puros), deudas y sabuesos; `wild/` encrucijada natural |
| `hell/`, `heaven/`, `memory/` | Infierno (sima, grietas, Jaula); Cielo personal (parcelas, puertas, Roadhouse de Ash, hogar); recuerdos |
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
- **Solo datos**: `data/supernaturalcraft/supernaturalcraft/sigil/<id>.json` (`kind`, `behavior`, `tier`, `mana_cost`, `cooldown`,
  `reagents`, `params`, `color`). Los modificadores usan `behavior: supernaturalcraft:modifier` (`params`: `mana_multiplier`,
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
- El maná máximo base es 175 y cada rango de facción/orden suma +25 (`Ranks.manaBonus`): ningún ritual puede costar más de lo que cabe.
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
  por punto vanilla. Implementa `CappedBoss`, vida real de `Balance.bossHealth(Boss)`, cada golpe por `BossDamage.softCap(...)` y un
  `BossHealthGuard` (`tick` antes de `super.tick()`, `accept` tras cada cambio propio). Para fijar vida en comandos/tests/previews:
  `BossHealthGuard.set(entity, h)`. Sus golpes salen por `BossStrike` (85 % normal + 15 % Divine Wrath) × `Balance.bossDamage(Boss)`.
  Los umbrales de mecánicas son fracciones de la vida real; esbirros y mobs normales no escalan.
- **Arena**: `LuciferSummoning.openArena(level, c, r, theme[, exclusive])` y **todo** cambio de terreno con `ArenaController.mutate`
  (nunca `setBlock`), que se restaura solo; `revertAfter` para cambios temporales. Lo que deba apagarse o cambiar no puede estar en
  `#arena_immune`. Decals irrompibles (raíles de Azazel, lápidas de Lilith, aceite de Rafael) también van por `mutate`. Suelos temáticos
  grandes con `HorsemenGround` (`mapped`/`fixed`, ~300 bloques/tick); el tema de arena (`ArenaTheme`) fija altura, rescate de caídas y
  `minSnapshot`; colores y música por fase en `client/arena/ArenaStyles`.
- **Visual**: cada jefe tiene su `*FxPayload` (tipos y argumentos en su javadoc) y cinemáticas con `CinematicPayload` +
  `CinematicLocks.play`. Barras propias: el HUD del cliente reconoce las claves `entity.supernaturalcraft.<jefe>.bar*`.

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
- **Hombres de Letras**: orden aparte de la facción (`Legacy`/`Archive` attachments, `Legacies.set/update`). Lo procedural de
  `legacy/gen` es determinista por semilla + UUID + índice con topes duros; el Archivo es una pestaña aparte y el Diario nunca lista
  sus entradas. Expedientes de criaturas: nunca contra `#bosses`.
- **El Cielo** (v0.18): puertas por rito, parcela por cazador en espiral (`PlotGrid`), `PlotWriter`/`DecorWriter` por lotes. Recuerdos
  en `MEMORY_LOG`; `MemoryStage` escribe cada escena con una arena privada y la restaura al salir. Naomi y Zachariah usan arenas no
  exclusivas (`exclusive=false`); la oficina infinita es una tesela de 16 que `OfficeWrap` repite. `/supernatural heaven|memory …`.
- **Cuenco de hechizos**: recetas `recipe/bowl_spell/<id>.json` (líquidos, ingredientes, `incantation`, `effect`); `BowlInput.isEmpty()`
  está sobrescrito (si no, una mezcla solo de líquidos nunca casa). Tests: `SpellBowlBlockEntity.tryLight` + `resolveRecitation`.
- **Libro del Cazador**: se dibuja en espacio de libro bajo un pose escalado: `book.scissor(...)` y `book.tooltip(...)`, nada de listas
  vanilla ni `renderEntityInInventoryFollowsMouse`. El cliente no ve logros ocultos: espejo en `ClientHunterLog` vía `HunterLogSyncPayload`.
  `RoadmapTest`: padres a la izquierda, celdas únicas, ids existentes y un nodo por jefe de `BossProgression` en la Jaula.
- **Cinemáticas**: JSON en `assets/supernaturalcraft/cinematics/<id>.json` (`shots` con `duration`, `path` Catmull-Rom, `look`, `fov`,
  `roll`, `shake`, `ease`), coordenadas relativas al ancla (+z adelante, +x izquierda). `CinematicLocks.play(ancla, id, ticks, rango)`.
- **Eclipse y clima** son globales: cada test que los use en su propio batch y los termina al acabar.
- **El Colt**: mata de un disparo todo lo que no es jefe, `#colt_immune` ni jugador; a un jefe le quita el 5 % de su vida real (Chuck
  1,5 %) sin pasar un umbral de fase. Todo va por `ColtShotPayload`/`ColtActionPayload` (el tirador predice). Brazos en 1.ª persona en
  `ColtArmsLayer`: la pila de manos vanilla está alineada con el mundo, gira los vectores "de cámara" con `camera.rotation()`.
  PlayerAnimationLib nombra los brazos cruzados.
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
| Armas | `weapons` (`SN_WEAPON_FROM=n`), `balance`, `wings`, `colt` (`SN_COLT_FROM=225` + `-Ppal` = solo 3.ª persona), `bowl` |
| Eclipse, Amara, Coro | `eclipse`, `amara`, `chorus_model`, `chorus`, `spire`, `spire_real:x,z` |
| Infierno | `hell`, `uncaged`, `rift` |
| Azazel, Lilith, Metatron | `azazel`, `azazel_fight`, `lilith`, `lilith_fight`, `metatron`, `metatron_fight`, `metatron_hand` |
| Chuck | `chuck`, `chuck_fight`, `chuck_arena` (`SN_ARENA_FROM=n`), `chuck_model`, `chuck_fx`, `chuck_cabin` |
| Jinetes, Miguel | `horsemen`, `war_fight`, `famine_fight`, `pestilence_fight`, `death_fight`, `michael_model`, `michael_fight`, `michael_arena`, `michael_hud` |
| Facciones, Gabriel, Rafael | `allegiance`, `gabriel`, `gabriel_fight`, `gabriel_pranks`, `raphael` (`SN_RAPHAEL_ONLY=a,b`), `raphael_house`, `raphael_fight` |
| Hombres de Letras | `legacy_models`, `legacy_bunker`, `legacy_research`, `legacy_case` |
| El Cielo | `heaven_plot`, `heaven_memories`, `roadhouse`, `naomi_model`, `naomi_fight`, `zachariah_model`, `zachariah_fight`, `heaven_sky`, `crossroads_natural`, `heaven_book` |

- El cliente abre una ventana en el equipo del usuario: úsalo con moderación. Escena nueva: un `case` en `DevPreview.scenes()`.
- El arnés desactiva `pauseOnLostFocus`, pulsa "sin copia" al abrir el mundo y oculta la GUI (`hideGui` también oculta los títulos).
  Tras cambiar el ítem en la mano espera ~50 ticks antes de capturar.
- El mundo `sn_preview` sale del de GameTests: no genera estructuras, guarda eclipses y clima de escenas anteriores (ciérralos si hace
  falta día despejado) y arrastra la Jaula del origen, así que las escenas del Overworld se montan lejos de 0,0.
- `spire_real` necesita un mundo real: `runs/server` con `enable-rcon=true`, `./gradlew runServer`, por RCON `locate structure
  supernaturalcraft:hymnal_spire`, `forceload add …`, `save-all flush`, `stop`, y copiar `runs/server/world` a `sn_preview`.
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

- **Por probar en partidas reales**: equilibrio y duración de todos los jefes; multijugador real (ilusión de Guerra, posesión de Miguel,
  concurso de Gabriel, PvP y pactos de facciones); rituales en mundo real; búnker y estructuras en mundos generados.
- **v0.18 (el Cielo)**: nada visto aún en un cliente real (faltan las capturas de sus escenas); pestaña Memories con la piel del
  roadmap (falta su rampa en `book_art.py`); escenas de recuerdo sencillas.
- **Sonido**: casi toda la música es provisional (vanilla remezclada) y los sonidos generados solo se han medido, no escuchado. En
  Windows `tools/soundgen/oggenc.py` no encuentra codificador (busca GStreamer/ffmpeg en rutas de macOS).
- **Limitaciones conocidas**: tras recargar a mitad de pelea los suelos de Miguel, Gabriel y Rafael se vuelven a fijar y pueden
  desplazarse; `AutoGlowingGeoLayer` con `GeoObjectRenderer` (Curios) dibuja las alas a plena luz; los ojos de facción asumen la skin
  de Steve; un Angel Blade invocado guardado en un cofre no caduca; las criaturas de un caso en chunks sin cargar se borran; perder
  contra Lucifer gasta el Last Seal; vigilar que el Colt no sea la única estrategia.
- **Ideas**: Caballeros del Infierno, iglesias abandonadas con páginas de sigilo, Rowena y las brujas (aparcadas).
