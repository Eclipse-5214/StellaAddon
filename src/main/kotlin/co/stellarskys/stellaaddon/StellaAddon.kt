package co.stellarskys.stellaaddon

import net.fabricmc.api.ClientModInitializer

/**
 * The primary client-side entry point for the Stella Addon, loaded by the Fabric Loader.
 */
object StellaAddon : ClientModInitializer {

    /**
     * The unique programmatic identifier namespace for this addon.
     * Used for getting assets.
     */
    @JvmStatic
    val NAMESPACE: String = "stella-addon"

    /**
     * Invoked exactly once by the Fabric Loader environment during the early game bootstrapping phase.
     * This usually is not needed because @Module usually handles this job, but it's here if you want it.
     */
    override fun onInitializeClient() {}
}