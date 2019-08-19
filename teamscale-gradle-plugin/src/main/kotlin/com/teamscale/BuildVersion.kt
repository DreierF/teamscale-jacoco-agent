package com.teamscale

/** Helper for getting the plugin version at runtime. */
object BuildVersion {

    /** Extracts the plugin's version from the jar's manifest file. */
    val buildVersion: String
        get() = "0.4.13"

    /** Extracts the agent's version from the jar's manifest file. */
    val agentVersion: String
        get() = "15.1.0"

}
