package io.thegamingmahi.luacord

import io.thegamingmahi.luacord.environment.LuaEnvironment
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginFile
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginLoader
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.InvalidDescriptionException
import org.bukkit.plugin.InvalidPluginException
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.LuaError
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Logger
import java.util.regex.Pattern

/**
 * The Main entry class of the plugin.
 *
 * @author jammehcow
 * @author TheGamingMahi (Paper compatibility fix + Kotlin rewrite)
 */
class Main : JavaPlugin() {
    companion object {
        // Config version
        private const val CFG_VERSION = 4

        /**
         * The instance of the plugin. Used for external access by plugin wrappers etc..
         */
        @JvmField
        var instance: Main? = null

        /**
         * The Logger for LuaCord.
         * Exposed as a field, not a property, to avoid conflicts with JavaPlugin.getLogger()
         */
        @JvmField
        var logger: Logger? = null

        private var loadTime: Long = 0

        private fun isLukkitPluginFile(fileName: String): Boolean {
            return LukkitPluginLoader.fileFilters.any { it.matcher(fileName).find() }
        }

        private fun getHelpMessage(): String {
            return """
                ${ChatColor.GREEN}LuaCord commands:
                ${ChatColor.YELLOW}  - "/luacord" ${ChatColor.GRAY}- The root command for all commands (shows this message)
                ${ChatColor.YELLOW}  - "/luacord help" ${ChatColor.GRAY}- Displays this message
                ${ChatColor.YELLOW}  - "/luacord run (lua code)" ${ChatColor.GRAY}- Runs the specified code as command arguments
                ${ChatColor.YELLOW}  - "/luacord plugins" ${ChatColor.GRAY}- Lists all enabled plugins
                ${ChatColor.YELLOW}  - "/luacord dev" ${ChatColor.GRAY}- Contains all developer commands. Prints out the dev help message
            """.trimIndent()
        }

        private fun getDevHelpMessage(): String {
            return """
                ${ChatColor.GREEN}LuaCord dev commands:
                ${ChatColor.YELLOW}  - "/luacord dev" ${ChatColor.GRAY}- The root command for developer actions (shows this message)
                ${ChatColor.YELLOW}  - "/luacord dev reload (plugin name)" ${ChatColor.GRAY}- Reloads the source file and clears all loaded requires
                ${ChatColor.YELLOW}  - "/luacord dev unload (plugin name)" ${ChatColor.GRAY}- Unloads the source file and clears all loaded requires
                ${ChatColor.YELLOW}  - "/luacord dev pack (plugin name)" ${ChatColor.GRAY}- Packages the plugin (directory) into a .lkt file for publishing
                ${ChatColor.YELLOW}  - "/luacord dev unpack (plugin name)" ${ChatColor.GRAY}- Unpacks the plugin (.lkt) to a directory based plugin
                ${ChatColor.YELLOW}  - "/luacord dev last-error" ${ChatColor.GRAY}- Gets the last error thrown by a plugin and sends the message to the sender. Also prints the stacktrace to the console.
                ${ChatColor.YELLOW}  - "/luacord dev errors [index]" ${ChatColor.GRAY}- Either prints out all 10 errors with stacktraces or prints out the specified error at the given index [1 - 10]
                ${ChatColor.YELLOW}  - "/luacord dev help" ${ChatColor.GRAY}- Shows this message
            """.trimIndent()
        }
    }

    // The server-wide PluginManager
    private lateinit var pluginManager: PluginManager
    private var pluginLoader: LukkitPluginLoader? = null

    // Config options (loaded from config.yml)
    private var debugMode = false
    private var bypassPluginRegistration = true

    private fun debug(message: String) {
        if (debugMode) {
            logger?.info("[DEBUG] $message")
        }
    }

    override fun onEnable() {
        // Check for updates if it's enabled in the config
        if (config.get("update-checker") == true) {
            UpdateChecker.checkForUpdates(description.version)
        }

        // Set up the tab completer for the /luacord command
        getCommand("lukkit")?.setTabCompleter(TabCompleter())

        // Subtract one to count for LuaCord being loaded
        val totalPlugins = pluginLoader?.loadedPlugins?.size ?: 0

        when {
            totalPlugins > 0 -> {
                val pluginText = if (totalPlugins != 1) "$totalPlugins LuaCord plugins were loaded" else "1 LuaCord plugin was loaded"
                logger?.info("$pluginText in ${loadTime}ms.")
            }
            else -> logger?.info("No LuaCord plugins were loaded.")
        }

        // If we bypassed plugin registration, manually enable plugins
        if (bypassPluginRegistration) {
            debug("Bypass mode enabled - manually enabling plugins")
            pluginLoader?.loadedPlugins?.forEach { plugin ->
                debug("Enabling plugin: ${plugin.name}")
                pluginLoader?.enablePlugin(plugin)
            }
        }
    }

    override fun onDisable() {
        // If we bypassed plugin registration, manually disable plugins
        if (bypassPluginRegistration) {
            pluginLoader?.loadedPlugins?.toList()?.forEach { plugin ->
                pluginLoader?.disablePlugin(plugin)
            }
        }
    }

    override fun onLoad() {
        debug("onLoad() called!")

        // Set the instance and logger
        instance = this
        Main.logger = getLogger()

        // Create the data folder directory if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }

        // Check the config
        checkConfig()

        // Load config options
        debugMode = config.getBoolean("debug-mode", false)
        bypassPluginRegistration = config.getBoolean("bypass-plugin-registration", true)

        debug("Debug mode: $debugMode")
        debug("Bypass plugin registration: $bypassPluginRegistration")

        // Initialize the Lua env (sets up globals)
        LuaEnvironment.init(config.getBoolean("lua-debug"))

        // Save the plugin manager for future use
        pluginManager = server.pluginManager
        // Register our custom plugin loader on the plugin manager
        pluginManager.registerInterface(LukkitPluginLoader::class.java)

        // CREATE PLUGIN LOADER FOR BYPASS MODE
        // This ensures pluginLoader is available for JAR-wrapped plugins
        if (bypassPluginRegistration) {
            pluginLoader = LukkitPluginLoader(server)
            debug("Created LukkitPluginLoader for bypass mode")
        }

        logger?.info("Loading LuaCord plugins...")

        // PAPER FIX: Use getDataFolder() to get correct plugins folder
        // Paper remaps plugins to .paper-remapped folder, but .lkt files stay in /plugins/
        val pluginsFolder = dataFolder.parentFile
        debug("Plugins folder: ${pluginsFolder.absolutePath}")

        val plugins = pluginsFolder.listFiles()

        if (plugins != null) {
            debug("Found ${plugins.size} files in plugins folder")

            // Set the start time of loading
            val startTime = System.currentTimeMillis()

            for (file in plugins) {
                debug("Checking file: ${file.name}")

                // Skip if the file isn't for LuaCord
                if (isLukkitPluginFile(file.name)) {
                    debug("Found .lkt file: ${file.name}")

                    try {
                        if (bypassPluginRegistration) {
                            // BYPASS MODE: Load plugin manually without PluginManager
                            debug("Using BYPASS mode - loading manually")

                            val pluginFile = LukkitPluginFile(file)

                            // pluginLoader already created above
                            val plugin = LukkitPlugin(pluginLoader!!, pluginFile)
                            debug("Created LukkitPlugin: ${plugin.name}")

                            pluginLoader!!.loadedPlugins.add(plugin)
                            debug("Added to loadedPlugins list. Total: ${pluginLoader!!.loadedPlugins.size}")

                            plugin.onLoad()
                            debug("Called plugin.onLoad()")
                        } else {
                            // NORMAL MODE: Use PluginManager (original behavior)
                            debug("Using NORMAL mode - loading via PluginManager")
                            pluginManager.loadPlugin(file)
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is InvalidPluginException, is InvalidDescriptionException, is LuaError -> {
                                debug("ERROR loading plugin: ${e.message}")
                                LuaEnvironment.addError(e)
                                e.printStackTrace()
                            }
                            else -> throw e
                        }
                    }
                }
            }

            // Get the total time to load plugins and save to loadTime member
            loadTime = System.currentTimeMillis() - startTime
            debug("Finished loading plugins in ${loadTime}ms")
        } else {
            debug("plugins array is NULL!")
        }

        // If using normal mode, find the plugin loader from registered plugins
        if (!bypassPluginRegistration) {
            for (plugin in pluginManager.plugins) {
                if (plugin is LukkitPlugin) {
                    pluginLoader = plugin.pluginLoader as LukkitPluginLoader
                    break
                }
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!command.name.startsWith("lukkit")) return false

        if (args.isEmpty()) {
            sender.sendMessage(getHelpMessage())
            return true
        }

        // Set the String "cmd" to the first arg and remove the arg from the "args" array.
        val cmd = args[0]
        // Get a new array with the first arg omitted
        val newArgs = args.drop(1).toTypedArray()

        when (cmd.lowercase()) {
            "help" -> sender.sendMessage(getHelpMessage())

            "plugins" -> {
                val sb = StringBuilder()
                    .append(ChatColor.GREEN).append("LuaCord Plugins:")
                    .append(ChatColor.YELLOW)

                iteratePlugins { p ->
                    sb.append("\n  - ").append(p.name)
                    p.description.description?.let { desc ->
                        sb.append(": ").append(desc)
                    }
                }

                sender.sendMessage(sb.toString())
            }

            "dev" -> handleDevCommand(sender, newArgs)

            else -> sender.sendMessage(getHelpMessage())
        }

        return true
    }

    private fun handleDevCommand(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(getDevHelpMessage())
            return
        }

        val subCommand = args[0].lowercase()
        val subArgs = args.drop(1).toTypedArray()

        when (subCommand) {
            "reload" -> {
                val plugins = mutableMapOf<String, LukkitPlugin>()
                iteratePlugins { p -> plugins[p.name.lowercase()] = p }

                val pluginName = subArgs.joinToString(" ").lowercase()

                plugins[pluginName]?.let { plugin ->
                    try {
                        (plugin.pluginLoader as LukkitPluginLoader).reloadPlugin(plugin)
                        sender.sendMessage("${ChatColor.GREEN}Successfully reloaded ${plugin.name}")
                    } catch (e: Exception) {
                        sender.sendMessage("${ChatColor.RED}There was an error reloading this plugin: ${e.message}\nCheck the console for more information.")
                        e.printStackTrace()
                    }
                } ?: sender.sendMessage("The specified plugin \"${subArgs.firstOrNull()}\" does not exist.")
            }

            "unload" -> {
                val plugins = mutableMapOf<String, LukkitPlugin>()
                iteratePlugins { p -> plugins[p.name.lowercase()] = p }

                val pluginName = subArgs.joinToString(" ").lowercase()

                plugins[pluginName]?.let { plugin ->
                    try {
                        (plugin.pluginLoader as LukkitPluginLoader).unloadPlugin(plugin)
                        sender.sendMessage("${ChatColor.GREEN}Successfully unloaded ${plugin.name}")
                    } catch (e: Exception) {
                        sender.sendMessage("${ChatColor.RED}There was an error unloading this plugin: ${e.message}\nCheck the console for more information.")
                        e.printStackTrace()
                    }
                } ?: sender.sendMessage("The specified plugin \"${subArgs.firstOrNull()}\" does not exist.")
            }

            "pack" -> zipOperation(ZipOperation.PACKAGE, sender, subArgs)

            "unpack" -> zipOperation(ZipOperation.UNPACK, sender, subArgs)

            "last-error" -> {
                val err = LuaEnvironment.getLastError()
                if (err.isPresent) {
                    sender.sendMessage(err.get().message ?: "Unknown error")
                    err.get().printStackTrace()
                } else {
                    sender.sendMessage("There was no error to get.")
                }
            }

            "errors" -> {
                val errors = LuaEnvironment.getErrors()

                if (errors.isPresent) {
                    if (subArgs.isEmpty()) {
                        errors.get().forEach { exception: Exception ->
                            sender.sendMessage(exception.message ?: "Unknown error")
                            exception.printStackTrace()
                        }
                    } else {
                        try {
                            val errorArray = errors.get().toArray()
                            val error = errorArray[subArgs[0].toInt()] as LuaError
                            sender.sendMessage(error.message ?: "Unknown error")
                            error.printStackTrace()
                        } catch (e: NumberFormatException) {
                            sender.sendMessage("${ChatColor.RED}${subArgs[0]} cannot be converted to an integer.")
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            sender.sendMessage("${ChatColor.RED}${subArgs[0]} is out of bounds in the stack. Should be between 1 & ${errors.get().count()}")
                        }
                    }
                } else {
                    sender.sendMessage("There are no errors to display!")
                }
            }

            else -> sender.sendMessage(getDevHelpMessage())
        }
    }

    private fun checkConfig() {
        // Get the config by relative path
        val cfg = File(dataFolder.absolutePath + File.separator + "config.yml")
        // Save the config if it doesn't exist
        if (!cfg.exists()) saveDefaultConfig()

        // Check the config version against the internal version
        if (config.getInt("cfg-version") != CFG_VERSION) {
            logger?.info("Your config is out of date. Replacing the config with the default copy and moving the old version to config.old.yml")

            // Create a new place for the old config to live
            val bkpCfg = File(dataFolder.absolutePath + File.separator + "config.old.yml")
            try {
                // Copy the config to the new path and delete the old one, essentially moving it
                Files.copy(cfg.toPath(), bkpCfg.toPath(), StandardCopyOption.REPLACE_EXISTING)
                Files.delete(cfg.toPath())
                // Save the internal config to the data folder
                saveDefaultConfig()
            } catch (e: IOException) {
                logger?.severe("There was an issue with moving the old config or replacing. Check the stacktrace for more.")
                e.printStackTrace()
            }
        }
    }

    internal fun iteratePlugins(action: (LukkitPlugin) -> Unit) {
        if (bypassPluginRegistration) {
            // In bypass mode, iterate our internal list
            pluginLoader?.loadedPlugins?.forEach(action)
        } else {
            // In normal mode, iterate registered plugins
            pluginManager.plugins.filterIsInstance<LukkitPlugin>().forEach(action)
        }
    }

    private fun zipOperation(operation: ZipOperation, sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("You didn't specify a plugin to ${if (operation == ZipOperation.PACKAGE) "package" else "unpack"}!")
            return
        }

        val plugins = mutableMapOf<String, LukkitPlugin>()
        iteratePlugins { p -> plugins[p.name.lowercase()] = p }

        val plugin = plugins[args[1]]

        if (plugin != null) {
            if ((operation == ZipOperation.PACKAGE) == plugin.isDevPlugin()) {
                if (operation == ZipOperation.PACKAGE) {
                    ZipUtil.unexplode(plugin.getFile())
                    sender.sendMessage("${ChatColor.GREEN}Successfully packed ${plugin.name}")
                } else {
                    ZipUtil.explode(plugin.getFile())
                    sender.sendMessage("${ChatColor.GREEN}Successfully unpacked ${plugin.name}")
                }
            } else {
                sender.sendMessage("The specified plugin \"${plugin.name}\" is already ${if (operation == ZipOperation.PACKAGE) "packaged" else "unpacked"}.")
            }
        } else {
            sender.sendMessage("The specified plugin \"${args[1]}\" does not exist.")
        }
    }

    private enum class ZipOperation {
        /**
         * Zip operation.
         */
        PACKAGE,
        /**
         * Unzip operation.
         */
        UNPACK
    }
}