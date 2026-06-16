# Stella API Reference

Quick reference for the Stella APIs addons build against. Examples are minimal — check Stella's source for full signatures if needed.

## Registration

### `@Module`

Marks an `object` for auto-discovery. Stella's KSP processor finds every `@Module`-annotated class at compile time and registers it at boot — no manual wiring.

```kotlin
@Module
object MyThing { init { /* runs at Stella init */ } }
```

### `Feature`

Base class for a toggleable feature. Wraps config-gated enable/disable, optional SkyBlock/island/area/dungeon-floor scoping, and event lifecycle.

```kotlin
@Module
object MyFeature : Feature("myFeature", skyblockOnly = true, island = SkyBlockIsland.THE_CATACOMBS) {
    override fun initialize() { on<ChatEvent.Receive> { /* ... */ } }
    override fun onRegister() { /* feature just turned on */ }
    override fun onUnregister() { /* feature just turned off */ }
}
```

- `configName`: ties the feature to a `toggle()` config key; `null` means always-on.
- `skyblockOnly` / `island` / `area` / `dungeonFloor`: gate `onRegister`/events to a location. `island`/`area`/`dungeonFloor` accept a single value or a `List`.
- `on<T> { }` inside a `Feature` auto (un)registers with the feature's lifecycle — prefer it over `EventBus.on` directly in feature code.

### `@Command`

Marks an `Atlas` object for auto-registration as a Brigadier command.

```kotlin
@Command
object MyCommand : Atlas("mycmd", "mc") {
    init { runs { Signal.modMessage("hi") } }
}
```

## Config DSL

Addons don't build their own `Config` — they extend Stella's existing one. `co.stellarskys.stella.utils.config` is the single global `Config` instance backing Stella's own settings.json and config screen; addon settings should live in it too, so they show up in the same UI and file instead of a second screen the user has to find.

`Config.subcategory(name, category, configName, desc)` is the extension point: it looks for an existing subcategory with that name across every category first, and only creates a new one (in the given category, creating that category if needed) if it doesn't already exist — so calling it never wipes out Stella's own categories/subcategories. This is exactly what `ExampleFeature.kt` does:

```kotlin
private val xpTracker = config.subcategory("Skill Tracker", "Addon", "xpTracker")
private val persist by xpTracker.toggle("xpTracker.persist", "Persist", "Persist XP between sessions")
```

That drops a new "Skill Tracker" subcategory into an "Addon" category (created on first use) inside Stella's normal config screen. Passing an existing Stella category name (e.g. `"Dungeons"`) instead of `"Addon"` slots your subcategory in alongside Stella's own.

The returned `ConfigSubcategory` has the same element functions Stella's internal `config.kt` uses. **Prefer the one-liner function form** — it returns a correctly-typed property delegate directly, no `as Boolean`/cast needed at the read site:

```kotlin
private val glow by xpTracker.toggle("xpTracker.glow", "Glow", def = true) // Boolean, not Any
private val scale by xpTracker.slider("xpTracker.scale", "Scale", min = 0.5f, max = 2f, def = 1f) // Float
```

One-liner signatures (all take `id, name, desc = "", ...type-specific..., show: ((Config) -> Boolean)? = null`):

| Element | One-liner | Delegate type |
|---|---|---|
| `toggle` | `toggle(id, name, desc, def = false, show)` | `Boolean` |
| `slider` | `slider(id, name, desc, min, max, def, show)` | `Float` |
| `stepslider` | `stepslider(id, name, desc, min, max, step, def, show)` | `Int` |
| `dropdown` | `dropdown(id, name, desc, options = listOf(...), def = 0, show)` | `Int` (index) |
| `colorpicker` | `colorpicker(id, name, desc, def = Color.WHITE, show)` | `java.awt.Color` |
| `textinput` | `textinput(id, name, desc, def = "", show, onChange)` | `String` |
| `keybind` | `keybind(id, name, desc, def = Zenith.Keys.NONE, show)` | `Keybind.Handler` |

`show` is the conditional-visibility hook — pass it inline instead of reaching for the builder block:

```kotlin
private val extra by xpTracker.toggle("xpTracker.extra", "Extra", show = { it["xpTracker.glow"] as Boolean })
```

(UI-only — hides the row, doesn't gate `Feature.isEnabled()`.)

A subcategory given a `configName` (like `"xpTracker"` above) acts as a master toggle, readable via `Feature("xpTracker")`. Reading a value elsewhere: `config["xpTracker.glow"] as Boolean`, or `config.property<Boolean>("xpTracker.glow")` as a delegate.

> **Sidenote:** every element also has a `{ }` builder-block form (`xpTracker.toggle { configName = "..."; name = "..."; default = true }`), and it's the *only* form for `button` (needs `onclick { }`) and `textparagraph` (static text) since those have no one-liner. Reach for the block form for those two, or when you need to mutate the element object itself rather than just its declared params.
>
> Separately: `Config` isn't special to Stella — `Config(modID) { category(...) { subcategory(...) { } } }` builds a fully standalone config with its own file/screen, same engine. Only worth it if you explicitly want a config separate from Stella's; extending the shared `config` above is the normal path.

## Commands — `Atlas`

Brigadier wrapper. Two ways to declare arguments, pick per-command:

**1. Delegate properties (`by arg.<type>()`)** — for multiple arguments, or when you want the value usable outside the `runs { }` block. Each declaration adds one node to a pending chain; the next `runs { }`/`literal { }` call consumes the whole chain to build the nested argument tree, in declaration order.

```kotlin
@Command
object Greet : Atlas("greet") {
    private val target by arg.string(suggestions = listOf("Steve", "Alex"))
    private val times by arg.int(min = 1, max = 5)

    init { runs { repeat(times) { Signal.modMessage("Hello, $target!") } } } // /greet <target> <times>
}
```

If you don't pass a `name`, the **argument's name is inferred from the property name** it's delegated to (`provideDelegate` fills it in) — so `target` above is registered as the node named `"target"`, not `"arg"`. Pass a name explicitly only if you want it to differ from the variable.

`arg` builders: `string(name, vararg suggestions)`, `greedy(name)` (rest-of-line), `int(name, min, max)`, `bool(name)`. Call `.optional()` on the result to make it nullable instead of required.

**2. Inline typed `runs<T>(name) { value -> }`** — for a single `String`/`Greedy` argument with no need to reference it outside that one execution block. Builds the argument node and its handler in one call instead of a separate property:

```kotlin
@Command
object Echo : Atlas("echo") {
    init {
        runs<Greedy> { msg -> Signal.modMessage(msg.string) } // /echo <message...>
    }
}
```

Use delegates for anything beyond one argument, or when subcommands (`literal { }`) need to share an argument declared above them; use `runs<T> { }` for a quick single-arg leaf.

## Events — `EventBus`

Global bus: `co.stellarskys.stella.events.EventBus`. Inside a `Feature`, use the inherited `on<T> { }` instead (auto lifecycle).

```kotlin
EventBus.on<ChatEvent.Receive> { event ->
    if (event.stripped == "You have completed the dungeon!") { /* ... */ }
}
```

Scoped subscription (only fires when in that island/area/floor):

```kotlin
EventBus.on<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS, skyblockOnly = true) { /* ... */ }
```

Common event families (`co.stellarskys.stella.events.core`): `ChatEvent`, `TickEvent`, `KeyEvent`, `RenderEvent`, `GuiEvent`, `EntityEvent`, `PlayerEvent`, `ServerEvent`, `GameEvent`, `ScoreboardEvent`, `TablistEvent`, `SoundEvent`, `PacketEvent`, `LocationEvent`, `DungeonEvent`.

Cancelable events expose `.cancel()`; check `Event.cancelable`/`Event.cancelled` if writing your own.

### Chat regex helper

`ChatEvent` has `matches(regex) run { match -> }` to skip the null-check dance:

```kotlin
on<ChatEvent.Receive> { event ->
    event matches Regex("""^(\w+) joined the dungeon!$""") run { m ->
        Signal.modMessage("${m.groupValues[1]} joined!")
    }
}
```

On `ChatEvent.Modify.*` (fired from `MODIFY_GAME`), chain `modify { }` instead of `run` to rewrite the line in place:

```kotlin
on<ChatEvent.Modify.Receive> { event ->
    event matches regex modify { m -> "rewritten: ${m.groupValues[1]}" }
}
```

## Handlers (`co.stellarskys.stella.api.handlers`)

### `Signal` — chat output

```kotlin
Signal.fakeMessage("§bLocal-only message")      // not sent to server
Signal.modMessage("Styled with Stella's prefix") // "[Stella] " + text
Signal.sendMessage("hello")                       // actually sent to server
```

### `Chronos` — scheduling & time

```kotlin
Chronos.Tick after 20 run { /* fires in 20 ticks (~1s) */ }
Chronos.Tick every 100 run { /* repeats every 100 ticks */ }
Chronos.Tick post { /* fires next tick */ }

val mark = Chronos.now
if (mark.since.inWholeSeconds > 5) { /* 5+ seconds elapsed */ }
```

### `Quasar` — async HTTP

```kotlin
Quasar.fetch<MyDto>("https://example.com/api") { result ->
    result.onSuccess { dto -> /* parsed via Gson */ }
        .onFailure { /* network/parse error */ }
}
```

Runs on `Stella.scope` (a `CoroutineScope`) and hops back to the client thread before invoking the callback.

### `Capsule<T>` — JSON-backed persistence

```kotlin
data class MyData(var count: Int = 0)
val store = Capsule("myfile", MyData())   // config/stella/myfile.json

store.update { count++ }   // mutate + autosave
val current = store()      // read current value
```

### `Atlas` — see Commands above.

### `Spark<T>` / `Flare<T>` — reactive values (`co.stellarskys.stella.api.handlers.Spark`)

Fine-grained reactivity: `Spark` is a settable cell, `Flare` is a derived value that recomputes when any `Spark`/`Flare` it reads changes.

```kotlin
var hp by Spark(20)
val isLow by Flare(false) { hp < 5 }   // recomputes whenever `hp` changes
```

### `Ether` — remote asset sync

Internal to Stella (downloads/extracts a versioned asset bundle on boot). Addons generally won't call this directly; mentioned for completeness.

## `HypixelApi` (`co.stellarskys.stella.api.hypixel`)

```kotlin
HypixelApi.fetchSkyblockProfile(uuid) { member -> /* SkyblockResponse.SkyblockMember? */ }
HypixelApi.fetchSecrets(uuid) { count -> /* Int? */ }
HypixelApi.getName(uuid) { name -> /* String? */ }
HypixelApi.getUuid("Notch") { uuid -> /* String? */ }
```

All cache results for 5 minutes by default (`cacheMs` param); pass `force = true` to bypass.

## Zenith — client/world shortcuts (`co.stellarskys.stella.api.zenith`)

Top-level inline shortcuts avoid typing `Zenith.` everywhere:

```kotlin
import co.stellarskys.stella.api.zenith.player
import co.stellarskys.stella.api.zenith.world

player?.sendSystemMessage(Component.literal("hi"))
val biome = world?.getBiome(player?.blockPosition())
```

Also available via `Zenith`: `Zenith.Mouse` (`rawX`/`rawY`, `isPressed(code)`), `Zenith.Keys` (named GLFW key codes, e.g. `Zenith.Keys.R_BRACKET`), `Zenith.Res` (`scaledWidth`/`scaledHeight`/`scaleFactor`).

## Astrum — world-space rendering (`co.stellarskys.stella.api.astrum`)

Queue draw calls anywhere; Astrum batches and flushes them once per frame.

```kotlin
Astrum.queueBox(aabb, Color.RED, filled = false)
Astrum.queueLine(start, end, Color.CYAN, width = 2f)
Astrum.queueText("Secret!", pos, color = 0xFFFFFFFF.toInt())
```

`depth = false` renders through walls (ESP-style).

## HUD elements (`co.stellarskys.stella.hud.HUDManager`)

Registers a draggable, position-persisted HUD element (editable via Stella's HUD editor).

```kotlin
HUDManager.register("myHud", "Default text", configKey = "myFeature.hud")

// or fully custom rendering:
HUDManager.registerCustom("myHud", width = 100, height = 20, configKey = "myFeature.hud") { ctx ->
    HUDManager.renderHud("myHud", ctx) { /* draw at 0,0 — translation/scale handled for you */ }
}
```
