package io.thegamingmahi.luacord

import com.mashape.unirest.http.Unirest

/**
 * LuaCord update checking class.
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
object UpdateChecker {
    // If the repo ever gets moved again this should make it easier.
    private const val GITHUB_ORG = "artex-development"

    /**
     * Check for updates against the GitHub repo releases page.
     *
     * @param pluginVersion the locally installed plugin version
     */
    @JvmStatic
    fun checkForUpdates(pluginVersion: String) {
        try {
            val res = Unirest.get("https://api.github.com/repos/$GITHUB_ORG/Lukkit/releases/latest").asJson()
            val tagName = res.body.`object`.getString("tag_name").replace("v", "")

            if (isOutOfDate(pluginVersion.split("-")[0], tagName)) {
                Main.logger?.info("A new version of LuaCord has been released: $tagName")
                Main.logger?.info("You can download it from https://www.spigotmc.org/resources/lukkit.32599/ or https://github.com/jammehcow/Lukkit/releases")
            } else {
                Main.logger?.info("You're up to date with the latest version of LuaCord.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if the local version is out of date by comparing the current version with the remote.
     *
     * @param current the currently installed version
     * @param remote  the remote version (GitHub releases)
     * @return whether the local version is lower than the remote version.
     */
    @JvmStatic
    fun isOutOfDate(current: String, remote: String): Boolean {
        // Version are formatted as "x.x.x" so we split on every period and convert to ints
        val currentVersion = getIntegers(current.split(".").toTypedArray())
        val remoteVersion = getIntegers(remote.split(".").toTypedArray())

        for (i in currentVersion.indices) {
            when {
                currentVersion[i] < remoteVersion[i] -> return true
                currentVersion[i] > remoteVersion[i] -> return false
            }
        }

        return false
    }

    private fun getIntegers(numbers: Array<String>): List<Int> {
        return numbers.map { it.toInt() }
    }
}