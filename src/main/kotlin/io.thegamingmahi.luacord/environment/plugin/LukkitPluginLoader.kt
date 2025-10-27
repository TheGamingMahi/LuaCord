package io.thegamingmahi.luacord.environment.plugin

import io.thegamingmahi.luacord.Main
import io.thegamingmahi.luacord.api.events.LukkitPluginDisableEvent
import io.thegamingmahi.luacord.api.events.LukkitPluginEnableEvent
import io.thegamingmahi.luacord.api.events.LukkitPluginLoadEvent
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.lang.reflect.Field
import java.util.regex.Pattern

/**
 * The LuaCord plugin loader.
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class LukkitPluginLoader(private val server: Server) : PluginLoader {

    companion object {
        /**
         * The file filters for plugin files.
         */
        @JvmField
        val fileFilters = arrayOf(
            Pattern.compile("^(.*)\\.lkt$")
        )
    }

    /**
     * The list of available plugins installed in the LuaCord data folder.
     * Plugins aren't loaded by default due to dependency requirements.
     * If we get a list of every plugin installed we can check dependency requests
     * against the plugins available and throw errors when they don't exist (for hard depends).
     */
    val loadedPlugins = mutableListOf<LukkitPlugin>()

    override fun loadPlugin(file: File): Plugin? {
        val pluginFile = LukkitPluginFile(file)

        try {
            val descriptionFile = PluginDescriptionFile(pluginFile.getPluginYML())
            println("[${descriptionFile.name}] Loading ${descriptionFile.fullName}")
        } catch (e: InvalidDescriptionException) {
            e.printStackTrace()
            throw InvalidPluginException("Error while loading ${file.name}.")
        }

        val event = LukkitPluginLoadEvent(pluginFile)
        Bukkit.getServer().pluginManager.callEvent(event)

        return if (!event.isCancelled) {
            val plugin = LukkitPlugin(this, pluginFile)
            loadedPlugins.add(plugin)
            plugin
        } else {
            null
        }
    }

    override fun getPluginDescription(file: File): PluginDescriptionFile {
        return try {
            PluginDescriptionFile(FileReader(file))
        } catch (e: FileNotFoundException) {
            throw InvalidDescriptionException("The provided file doesn't exist!")
        }
    }

    override fun getPluginFileFilters(): Array<Pattern> {
        return fileFilters
    }

    override fun createRegisteredListeners(listener: Listener, plugin: Plugin): Map<Class<out Event>, Set<RegisteredListener>> {
        return Main.instance?.pluginLoader?.createRegisteredListeners(listener, plugin) ?: emptyMap()
    }

    override fun enablePlugin(plugin: Plugin) {
        plugin.logger.info("Enabling ${plugin.description.fullName}")
        val event = LukkitPluginEnableEvent(plugin as LukkitPlugin)
        Bukkit.getServer().pluginManager.callEvent(event)

        if (!event.isCancelled) {
            plugin.onEnable()
        } else {
            if (plugin.isEnabled) {
                Bukkit.getPluginManager().disablePlugin(plugin)
            }
        }
    }

    override fun disablePlugin(plugin: Plugin) {
        plugin.logger.info("Disabling ${plugin.description.fullName}")
        val event = LukkitPluginDisableEvent(plugin as LukkitPlugin)
        Bukkit.getServer().pluginManager.callEvent(event)
        HandlerList.unregisterAll(plugin)
        plugin.onDisable()
        loadedPlugins.remove(plugin)
    }

    /**
     * Reload the specified plugin.
     *
     * @param plugin the [LukkitPlugin] object
     */
    fun reloadPlugin(plugin: LukkitPlugin) {
        // Check if the plugin is a dev plugin.
        if (!plugin.isDevPlugin()) {
            throw LukkitPluginException("Cannot reload a standard LuaCord plugin, use /reload instead. This is a developer-only feature.")
        }

        // Disable and unload the whole plugin
        unloadPlugin(plugin)

        val pluginFile = plugin.getFile()
        // Create the plugin and load it.
        val newPlugin = server.pluginManager.loadPlugin(pluginFile)
        newPlugin?.let { server.pluginManager.enablePlugin(it) }
    }

    /**
     * Unload the specified plugin.
     *
     * @param plugin the [LukkitPlugin] object
     */
    fun unloadPlugin(plugin: LukkitPlugin) {
        // Check if the plugin is a dev plugin.
        if (!plugin.isDevPlugin()) {
            throw LukkitPluginException("Cannot unload a standard LuaCord plugin, use /stop instead. This is a developer-only feature.")
        }

        val pName = plugin.name
        val pluginManager = server.pluginManager

        // Disable the plugin (also unregisters all events).
        pluginManager.disablePlugin(plugin)

        // Get the fields from the plugin manager where the plugin is stored
        val pluginsField: Field = pluginManager.javaClass.getDeclaredField("plugins")
        pluginsField.isAccessible = true
        val loaders: Field = pluginManager.javaClass.getDeclaredField("lookupNames")
        loaders.isAccessible = true
        val plugins = pluginsField.get(pluginManager) as MutableList<*>
        val names = loaders.get(pluginManager) as MutableMap<*, *>

        // Remove the plugin from spigot
        synchronized(pluginManager) {
            if (plugins.contains(plugin)) {
                plugins.remove(plugin)
            }

            if (names.containsKey(pName)) {
                names.remove(pName)
            }
        }

        // Ask java to clean up
        System.gc()
    }
}