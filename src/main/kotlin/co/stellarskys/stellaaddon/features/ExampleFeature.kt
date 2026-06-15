package co.stellarskys.stellaaddon.features

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Capsule
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config

/**
 * Automatically discovers, instantiates, and registers targeted companion objects or classes
 * into the main feature registry without manual array mapping in the core initializer.
 *
 * If you create a feature it must have this annotation (or anything else you want to run at stella's init)
 */
@Module
/**
 * Feature is used to create a feature.
 *
 * @param configName The unique key identifying this feature in configuration files.
 * @param skyblockOnly When true, gates the feature's events so they only fire when actively
 * connected to a Hypixel SkyBlock instance.
 * @param island Optional island constraint parameter targeting specific maps (e.g., Dwarven Mines).
 * @param area Optional area constraint
 * @oaram dungeonFloor optional Dungeon floor constraint
 */
object ExampleFeature: Feature("xpTracker", skyblockOnly = true) {

    /**
     * Creates a subcategory in the config interface.
     *
     * @param name The display name shown inside the UI layout.
     * @param category The primary configuration category directory.
     * @param configName The underlying unique programmatic key for delegate reference
     * @param description Optional hover tooltip for more info
     */
    private val xpTracker = config.subcategory("Skill Tracker", "Addon", "xpTracker")

    /**
     * Creates a reactive toggle property delegate backed by the config.
     *
     * @param id The underlying unique programmatic key for delegate reference.
     * @param name The display name shown inside the UI layout
     * @param desc Optional hover tooltip for more info
     * @param def Default state
     * @param show when to show the toggle ex { settings -> settings["xpTracker"] }
     * will only show this toggle when the xp tracker subcategory is enabled
     */
    private val persist by xpTracker.toggle("xpTracker.persist", "Persist", "Persist XP between sessions")

    private val xpRegex = """\+([\d,.]+)\s+(\w+)\s+\(([\d,.]+)[\s/]+([\d,.]+)\)""".toRegex()

    private var sessionXp = mutableMapOf<String, Double>()
    private val lastKnownXp = mutableMapOf<String, Double>()

    data class xpCache(var xp: MutableMap<String, Double> = mutableMapOf())

    /**
     * Persistant data solution.
     *
     * @param T The payload data contract blueprint.
     * @param fileName The save files name in config/stella.
     * @param default The default to fall back to if the file doesn't exist.
     */
    private val cache = Capsule("xp", xpCache())

    /**
     * Fired exactly once when the feature instance is initialized. Use this to subscribe to events
     * and register Huds
     */
    override fun initialize() {
        /**
         * Subscribes to an event witch dynamically follows the features lifecycle
         *
         * @param T The event to subscribe to
         * @param block what to run when the event is called.
         */
        on<ChatEvent.ActionBar> { event ->
            println("${event.stripped} yes")
            event matches xpRegex run { match ->
                val skillName = match.groupValues[2].uppercase()
                val currentXp = match.groupValues[3].replace(",", "").toDoubleOrNull() ?: 0.0

                val lastXp = lastKnownXp[skillName]
                lastKnownXp[skillName] = currentXp

                val gainedXp = when {
                    lastXp == null -> match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                    currentXp < lastXp -> match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                    else -> currentXp - lastXp
                }

                if (gainedXp <= 0.0) return@run

                val currentTotal = sessionXp.getOrDefault(skillName, 0.0)
                sessionXp[skillName] = currentTotal + gainedXp
            }
        }
    }

    /**
     * Triggered directly after the feature is enabled and all constraints have been met.
     * Think of it as an init function for that "use".
     */
    override fun onRegister() { if (persist) sessionXp = cache().xp }

    /**
     * Opposite of onRegister()
     * Use this to flush values
     */
    override fun onUnregister() {
        if (persist) updateCache()
        lastKnownXp.clear()
    }

    fun updateCache() { cache.update { xp = sessionXp } }
    fun getSessionData() = sessionXp

    fun resetData() {
        sessionXp.clear()
        lastKnownXp.clear()
        updateCache()
    }
}