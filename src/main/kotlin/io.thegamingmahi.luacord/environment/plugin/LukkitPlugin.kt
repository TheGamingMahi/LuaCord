package io.thegamingmahi.luacord.environment.plugin

import com.avaje.ebean.EbeanServer
import io.thegamingmahi.luacord.Main
import io.thegamingmahi.luacord.Utilities
import io.thegamingmahi.luacord.environment.LuaEnvironment
import io.thegamingmahi.luacord.environment.plugin.commands.LukkitCommand
import io.thegamingmahi.luacord.environment.wrappers.*
import io.thegamingmahi.luacord.environment.wrappers.storage.JsonStorage
import io.thegamingmahi.luacord.environment.wrappers.storage.StorageObject
import io.thegamingmahi.luacord.environment.wrappers.storage.YamlStorage
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.*
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Logger

/**
 * The LuaCord plugin class.
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class LukkitPlugin(
    private val pluginLoader: LukkitPluginLoader,
    private val pluginFile: LukkitPluginFile
) : Plugin {

    private val name: String
    private val pluginMain: LuaValue
    private val descriptor: PluginDescriptionFile
    private val dataFolder: File
    private val logger: Logger
    private val commands = mutableListOf<LukkitCommand>()
    private val eventListeners = mutableMapOf<Class<out Event>, MutableList<LuaFunction>>()
    private lateinit var utilitiesWrapper: UtilitiesWrapper
    private var loadCB: LuaFunction? = null
    private var enableCB: LuaFunction? = null
    private var disableCB: LuaFunction? = null
    private lateinit var pluginConfig: File
    private lateinit var config: FileConfiguration
    private var enabled = false
    private var naggable = true

    init {
        val desc = try {
            PluginDescriptionFile(pluginFile.getPluginYML())
        } catch (e: InvalidDescriptionException) {
            e.printStackTrace()
            throw InvalidPluginException("The description provided was invalid or missing.")
        }

        descriptor = desc
        name = descriptor.name
        logger = Logger.getLogger(name)

        val globals = LuaEnvironment.getNewGlobals(this)

        pluginMain = globals.load(
            InputStreamReader(pluginFile.getResource(descriptor.main), StandardCharsets.UTF_8),
            descriptor.main
        )

        dataFolder = File(Main.instance?.dataFolder?.parentFile?.absolutePath + File.separator + name)
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }

        pluginConfig = File(dataFolder.absolutePath + File.separator + "config.yml")
        config = YamlConfiguration()
        loadConfigWithChecks()

        setupPluginGlobals(globals)

        // Sets callbacks (if any) and loads the commands & events into memory.
        checkPluginValidity()?.let { error ->
            throw InvalidPluginException("An issue occurred when loading the plugin: \n$error")
        }

        try {
            pluginMain.call()
            onLoad()
        } catch (e: LukkitPluginException) {
            e.printStackTrace()
            LuaEnvironment.addError(e)
        }
    }

    override fun getDataFolder(): File = dataFolder

    override fun getDescription(): PluginDescriptionFile = descriptor

    override fun getConfig(): FileConfiguration = config

    override fun getResource(path: String): InputStream? = pluginFile.getResource(path)

    override fun saveConfig() {
        config.save(pluginConfig)
    }

    override fun saveDefaultConfig() {
        pluginFile.getDefaultConfig()?.let { defaultConfig ->
            try {
                Files.copy(
                    defaultConfig,
                    File(dataFolder.absolutePath + File.separator + "config.yml").toPath()
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun saveResource(resourcePath: String, replace: Boolean) {
        var path = resourcePath
        if (path.startsWith("/")) {
            path = path.replaceFirst("/", "")
        }

        val fileName = path.split("/").last()
        val resourceOutput = File(dataFolder.absolutePath + File.separator + fileName)
        val inputStream = pluginFile.getResource(path)

        inputStream?.let { stream ->
            if (!resourceOutput.exists() || replace) {
                try {
                    Files.copy(stream, resourceOutput.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (e: IOException) {
                    logger.severe("There was an issue copying a resource to the data folder.")
                    e.printStackTrace()
                }
            } else {
                logger.info("Will not export resource $path to ${dataFolder.name} as it already exists and has not been marked to be replaced.")
            }
        } ?: logger.warning("The resource requested doesn't exist. Unable to find $path in ${pluginFile.getPath()}")
    }

    override fun reloadConfig() {
        if (::pluginConfig.isInitialized) {
            try {
                config.load(dataFolder)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InvalidConfigurationException) {
                e.printStackTrace()
            }
        }
    }

    override fun getPluginLoader(): PluginLoader = pluginLoader

    override fun getServer(): Server = Main.instance?.server ?: throw IllegalStateException("Main instance not initialized")

    override fun isEnabled(): Boolean = enabled

    override fun onEnable() {
        enabled = true
        try {
            enableCB?.call(CoerceJavaToLua.coerce(this))
        } catch (e: LukkitPluginException) {
            e.printStackTrace()
            LuaEnvironment.addError(e)
        }

        eventListeners.forEach { (event, list) ->
            list.forEach { function ->
                server.pluginManager.registerEvent(
                    event,
                    object : Listener {},
                    EventPriority.NORMAL,
                    { _, e -> function.call(CoerceJavaToLua.coerce(e)) },
                    this,
                    false
                )
            }
        }
    }

    override fun onDisable() {
        enabled = false
        try {
            disableCB?.call(CoerceJavaToLua.coerce(this))
        } catch (e: LukkitPluginException) {
            e.printStackTrace()
            LuaEnvironment.addError(e)
        }
        unregisterAllCommands()
        utilitiesWrapper.close()
    }

    override fun onLoad() {
        try {
            loadCB?.call()
        } catch (e: LukkitPluginException) {
            e.printStackTrace()
            LuaEnvironment.addError(e)
        }
    }

    override fun isNaggable(): Boolean = naggable

    override fun setNaggable(isNaggable: Boolean) {
        naggable = isNaggable
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? {
        return Main.instance?.getDefaultWorldGenerator(worldName, id)
    }

    override fun getLogger(): Logger = logger

    override fun getName(): String = name

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        strings: Array<String>
    ): List<String>? {
        // TODO
        return null
    }

    fun getFile(): File = File(pluginFile.getPath())

    fun isDevPlugin(): Boolean = pluginFile.isDevPlugin()

    fun setLoadCB(cb: LuaFunction) {
        loadCB = cb
    }

    fun setEnableCB(cb: LuaFunction) {
        enableCB = cb
    }

    fun setDisableCB(cb: LuaFunction) {
        disableCB = cb
    }

    fun registerEvent(event: Class<out Event>, function: LuaFunction): Listener? {
        getEventListeners(event).add(function)
        if (enabled) {
            val listener = object : Listener {}
            server.pluginManager.registerEvent(
                event,
                listener,
                EventPriority.NORMAL,
                { _, e -> function.call(CoerceJavaToLua.coerce(e)) },
                this,
                false
            )
        }
        return null
    }

    fun getPluginFile(): LukkitPluginFile = pluginFile

    private fun getEventListeners(event: Class<out Event>): MutableList<LuaFunction> {
        return eventListeners.getOrPut(event) { mutableListOf() }
    }

    private fun loadConfigWithChecks() {
        val internalConfig = pluginFile.getDefaultConfig()

        when {
            !pluginConfig.exists() && internalConfig == null -> {
                // No need to do anything, there is no config.
            }
            !pluginConfig.exists() -> {
                // There is no external config so we'll export one from the .lkt
                try {
                    Files.createFile(pluginConfig.toPath())
                    Files.copy(internalConfig, pluginConfig.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    loadConfig()
                } catch (e: IOException) {
                    logger.warning("Unable to export the internal config. We have a problem.")
                    e.printStackTrace()
                }
            }
            else -> {
                // There is a config externally and one internally. External is fine, just load that.
                loadConfig()
            }
        }
    }

    private fun loadConfig() {
        val brokenConfig: File

        try {
            config.load(pluginConfig)
            return
        } catch (e: InvalidConfigurationException) {
            brokenConfig = File(dataFolder.absolutePath + File.separator + "config.broken.yml")
        } catch (e: IOException) {
            logger.severe("There was an error creating the file to move the broken config to.")
            e.printStackTrace()
            return
        }

        try {
            Files.copy(pluginConfig.toPath(), brokenConfig.toPath())
            pluginFile.getDefaultConfig()?.let { defaultConfig ->
                Files.copy(defaultConfig, pluginConfig.toPath())
            }
            config.load(pluginConfig)
        } catch (e: IOException) {
            logger.severe("There was an error copying either the broken config to its new file or the default config to the data folder.")
            e.printStackTrace()
            return
        } catch (e: InvalidConfigurationException) {
            logger.severe("The internal config is invalid. If you are the plugin maintainer please verify it. If you believe this is a bug submit an issue on GitHub with your configuration.")
            e.printStackTrace()
        }

        logger.warning("The config at ${pluginConfig.absolutePath} was invalid. It has been moved to config.broken.yml and the default config has been exported to config.yml.")
    }

    /**
     * Set up convenience methods on Lua globals
     *
     * @param globals globals to set up properties on
     */
    private fun setupPluginGlobals(globals: Globals) {
        globals.set("plugin", PluginWrapper(this))
        globals.set("logger", LoggerWrapper(this))
        // use a member as its internal threadpool needs to be shutdown upon disabling the plugin
        utilitiesWrapper = UtilitiesWrapper(this)
        globals.set("util", utilitiesWrapper)
        globals.set("config", ConfigWrapper(this))

        val oldRequire = globals.get("require") as OneArgFunction

        globals.set("require", object : OneArgFunction() {
            override fun call(luaValue: LuaValue): LuaValue {
                var path = luaValue.checkjstring()
                if (!path.endsWith(".lua")) {
                    path += ".lua"
                }

                // Replace all but last dot
                path = path.replace("\\.(?=[^.]*\\.)".toRegex(), "/")

                val resource = pluginFile.getResource(path)

                return if (resource == null) {
                    oldRequire.call(luaValue)
                } else {
                    try {
                        globals.load(InputStreamReader(resource, "UTF-8"), luaValue.checkjstring()).call()
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                        LuaValue.NIL
                    }
                }
            }
        })

        globals.set("import", object : OneArgFunction() {
            override fun call(luaValue: LuaValue): LuaValue {
                return try {
                    var path = luaValue.checkjstring()
                    when {
                        path.startsWith("$") -> path = "org.bukkit" + path.substring(1)
                        path.startsWith("#") -> path = "nz.co.jammehcow.lukkit.environment" + path.substring(1)
                    }
                    CoerceJavaToLua.coerce(Class.forName(path))
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    LuaValue.NIL
                }
            }
        })

        globals.set("newInstance", object : VarArgFunction() {
            override fun invoke(vargs: Varargs): LuaValue {
                var classPath = vargs.checkjstring(1)
                val args = vargs.optvalue(2, LuaValue.NIL)

                // Parse classpath shorthands
                when {
                    classPath.startsWith("$") -> classPath = "org.bukkit" + classPath.substring(1)
                    classPath.startsWith("#") -> classPath = "nz.co.jammehcow.lukkit.environment" + classPath.substring(1)
                }

                // Validate the classpath isn't just bullshit
                if (!Utilities.isClassPathValid(classPath)) {
                    val classPathException = LukkitPluginException(
                        "An invalid classpath \"$classPath\" was provided to the \"newInstance\" method"
                    )
                    LuaEnvironment.addError(classPathException)
                    throw classPathException
                }

                val classPathValue = LuaValue.valueOf(classPath)
                val newInstanceMethod = globals.get("luajava").get("newInstance")

                return when (args.type()) {
                    LuaValue.TNIL -> newInstanceMethod.invoke(classPathValue).checkvalue(1)

                    LuaValue.TTABLE -> {
                        val argTable = args.checktable()
                        val varargArray = Array<LuaValue>(argTable.length() + 1) { LuaValue.NIL }
                        varargArray[0] = classPathValue

                        for (iKey in 1 until varargArray.size) {
                            varargArray[iKey] = argTable.get(iKey)
                        }

                        newInstanceMethod.invoke(LuaValue.varargsOf(varargArray)).checkvalue(1)
                    }

                    else -> {
                        val exception = LukkitPluginException(
                            "Second argument of newInstance must be of type table, not ${args.typename()}"
                        )
                        LuaEnvironment.addError(exception)
                        throw exception
                    }
                }
            }
        })
    }

    private fun checkPluginValidity(): String? {
        return when {
            pluginMain == null -> "Unable to load the main Lua file. It may be missing from the plugin file or corrupted."
            descriptor == null -> "Unable to load the plugin's description file. It may be missing from the plugin file or corrupted."
            else -> null
        }
    }

    fun registerCommand(command: LukkitCommand) {
        commands.add(command)
        try {
            command.register()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    fun unregisterCommand(command: LukkitCommand) {
        commands.remove(command)
        try {
            command.unregister()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    fun unregisterAllCommands() {
        // Create new array to get rid of concurrent modification
        val cmds = commands.toList()
        cmds.forEach { unregisterCommand(it) }
    }
}