package io.thegamingmahi.luacord

import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginFile
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginLoader
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Public API for JAR-wrapped Lua plugins to interact with LuaCord
 *
 * @author TheGamingMahi
 */
object LuaCordAPI {

    /**
     * Load a Lua plugin from a JAR wrapper's resources
     *
     * This method allows JavaPlugin wrappers to load embedded .lkt files
     * as LuaCord plugins. The .lkt file should be at the JAR root.
     *
     * @param hostPlugin The JavaPlugin that contains the embedded .lkt
     * @param lktResourcePath Path to .lkt in JAR resources (e.g., "plugin.lkt")
     * @return The loaded LukkitPlugin instance, or null if loading failed
     *
     * @throws IllegalStateException if LuaCord is not initialized
     */
    @JvmStatic
    fun loadLuaPlugin(hostPlugin: JavaPlugin, lktResourcePath: String): LukkitPlugin? {
        val logger = hostPlugin.logger

        try {
            logger.info("Loading Lua plugin from JAR: ${hostPlugin.name}")

            // Get Main instance
            val main = Main.instance
                ?: throw IllegalStateException("LuaCord not initialized")

            // Get the .lkt resource from the host plugin's JAR
            val lktStream: InputStream = hostPlugin.getResource(lktResourcePath)
                ?: throw Exception("Resource not found in JAR: $lktResourcePath")

            // Create temp directory for extracted .lkt files
            val tempDir = File(main.dataFolder.parentFile, ".luacord-temp")
            if (!tempDir.exists()) {
                tempDir.mkdir()
            }

            // Create temp .lkt file
            // Use JAR name to avoid conflicts
            val tempLkt = File(tempDir, "${hostPlugin.name}.lkt")

            // Extract .lkt from JAR to temp location
            logger.info("Extracting .lkt to: ${tempLkt.absolutePath}")
            FileOutputStream(tempLkt).use { output ->
                lktStream.copyTo(output)
            }

            // Verify the .lkt file was created
            if (!tempLkt.exists() || tempLkt.length() == 0L) {
                throw Exception("Failed to extract .lkt file")
            }

            logger.info("Successfully extracted .lkt (${tempLkt.length()} bytes)")

            // Create LukkitPluginFile wrapper
            val pluginFile = LukkitPluginFile(tempLkt)

            // Get the plugin loader from Main
            val pluginLoader = getPluginLoader(main)
                ?: throw Exception("Plugin loader not available")

            // Create the LukkitPlugin
            logger.info("Creating LukkitPlugin instance...")
            val luaPlugin = LukkitPlugin(pluginLoader, pluginFile)

            // Add to loaded plugins list
            pluginLoader.loadedPlugins.add(luaPlugin)
            logger.info("Added to loaded plugins list (total: ${pluginLoader.loadedPlugins.size})")

            // Call onLoad
            logger.info("Calling onLoad()...")
            luaPlugin.onLoad()

            // Enable the plugin
            logger.info("Enabling Lua plugin...")
            pluginLoader.enablePlugin(luaPlugin)

            logger.info("Successfully loaded Lua plugin: ${luaPlugin.name}")

            return luaPlugin

        } catch (e: Exception) {
            logger.severe("Failed to load Lua plugin from JAR: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Get the LukkitPluginLoader from Main instance
     * Uses reflection to access private field
     */
    private fun getPluginLoader(main: Main): LukkitPluginLoader? {
        return try {
            val field = main.javaClass.getDeclaredField("pluginLoader")
            field.isAccessible = true
            field.get(main) as? LukkitPluginLoader
        } catch (e: Exception) {
            Main.logger?.severe("Failed to access plugin loader: ${e.message}")
            null
        }
    }

    /**
     * Check if LuaCord is initialized and ready
     *
     * @return true if LuaCord is ready, false otherwise
     */
    @JvmStatic
    fun isReady(): Boolean {
        return Main.instance != null
    }

    /**
     * Get LuaCord version
     *
     * @return LuaCord version string
     */
    @JvmStatic
    fun getVersion(): String {
        return Main.instance?.description?.version ?: "unknown"
    }
}