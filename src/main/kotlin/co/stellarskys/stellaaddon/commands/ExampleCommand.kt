package co.stellarskys.stellaaddon.commands

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.api.handlers.Atlas
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stellaaddon.features.ExampleFeature

/**
 * Marks an Atlas command registry object for automatic discovery and processing during mod loading.
 */
@Command
/**
 * Atlas acts as the DSL wrapper engine for command compilation, mapping arguments and literals
 * into a structured tree layout.
 *
 * @param name The primary root command label (e.g., /skilltracker)
 * @param aliases Optional alternative root command shortcuts (e.g., /st)
 */
object ExampleCommand: Atlas("skilltracker", "st") {
    init {
        /**
         * Creates a hardcoded subcommand path node branch.
         *
         * @param name The literal word that triggers this branch (e.g., /st reset)
         */
        literal("reset") {
            /**
             * Defines what to run when the current branch is called.
             */
            runs {
                ExampleFeature.resetData()
                Signal.modMessage("§aSession skill trackers have been reset!")
            }
        }

        /**
         * Defines the root fallback execution path if no subcommands match.
         * Runs when a user simply types the base command (e.g., /st)
         */
        runs {
            val data = ExampleFeature.getSessionData()

            if (data.isEmpty()) {
                Signal.modMessage("§cNo XP gained yet this session. Go grind!")
                return@runs
            }

            Signal.modMessage("§b--- Current Session XP Progress ---")
            data.forEach { (skill, totalXp) ->
                val formattedXp = String.format("%,.1f", totalXp)
                Signal.fakeMessage("§7- §e$skill: §a+$formattedXp XP")
            }
        }
    }
    /**
     * Determines if the command should be registered at init
     */
    override fun isEnabled(): Boolean = true
}