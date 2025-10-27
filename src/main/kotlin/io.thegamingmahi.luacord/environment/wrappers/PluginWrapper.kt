package io.thegamingmahi.luacord.environment.wrappers

import io.thegamingmahi.luacord.Utilities
import io.thegamingmahi.luacord.environment.LuaEnvironment
import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginException
import io.thegamingmahi.luacord.environment.plugin.commands.LukkitCommand
import io.thegamingmahi.luacord.environment.wrappers.storage.JsonStorage
import io.thegamingmahi.luacord.environment.wrappers.storage.StorageObject
import io.thegamingmahi.luacord.environment.wrappers.storage.YamlStorage
import org.bukkit.event.Event
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * The plugin wrapper providing access to plugin functions.
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class PluginWrapper(private val plugin: LukkitPlugin) : LuaTable() {
    private val cachedObjects = mutableMapOf<String, StorageObject>()

    init {
        set("onLoad", object : VarArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (callback.isfunction()) {
                    plugin.setLoadCB(callback.checkfunction())
                } else {
                    throw LukkitPluginException("There was an issue registering the onLoad callback - was provided a ${callback.typename()} instead of a function.")
                }
                return LuaValue.NIL
            }
        })

        set("onEnable", object : VarArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (callback.isfunction()) {
                    plugin.setEnableCB(callback.checkfunction())
                } else {
                    throw LukkitPluginException("There was an issue registering the onEnable callback - was provided a ${callback.typename()} instead of a function.")
                }
                return LuaValue.NIL
            }
        })

        set("onDisable", object : VarArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (callback.isfunction()) {
                    plugin.setDisableCB(callback.checkfunction())
                } else {
                    throw LukkitPluginException("There was an issue registering the onDisable callback - was provided a ${callback.typename()} instead of a function.")
                }
                return LuaValue.NIL
            }
        })

        set("addCommand", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                if (!arg1.istable() || !arg2.isfunction()) {
                    throw LukkitPluginException("There was an issue registering a command. Check that the command registration conforms to the layout here: ")
                }

                val cmd = arg1.checktable()
                val function = arg2.checkfunction()

                val cmdName = cmd.get("name").checkjstring()

                val cmdDescription = if (cmd.get("description") != LuaValue.NIL) {
                    cmd.get("description").checkjstring()
                } else ""

                val cmdUsage = if (cmd.get("usage") != LuaValue.NIL) {
                    cmd.get("usage").checkjstring()
                } else ""

                val command = LukkitCommand(plugin, function, cmdName, cmdDescription, cmdUsage)

                if (cmd.get("permission") != LuaValue.NIL) {
                    command.permission = cmd.get("permission").checkjstring()
                }

                if (cmd.get("permissionMessage") != LuaValue.NIL) {
                    command.permissionMessage = cmd.get("permissionMessage").checkjstring()
                }

                if (cmd.get("maxArgs") != LuaValue.NIL) {
                    command.maxArgs = cmd.get("maxArgs").checkint()
                }

                if (cmd.get("minArgs") != LuaValue.NIL) {
                    command.minArgs = cmd.get("minArgs").checkint()
                }

                if (cmd.get("runAsync") != LuaValue.NIL) {
                    command.isRunAsync = cmd.get("runAsync").checkboolean()
                }

                plugin.registerCommand(command)
                return CoerceJavaToLua.coerce(command)
            }
        })

        set("registerEvent", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val eventName = arg1.checkjstring()
                val callback = arg2.checkfunction()

                try {
                    // Try to see if the event is a class path, for custom events
                    val c = Class.forName(eventName)
                    if (Utilities.classIsEvent(c)) {
                        @Suppress("UNCHECKED_CAST")
                        return CoerceJavaToLua.coerce(plugin.registerEvent(c as Class<out Event>, callback))
                    }
                } catch (e: ClassNotFoundException) {
                    // Attempt to find the event in Bukkit packages
                    val events = arrayOf("block", "enchantment", "entity", "hanging", "inventory",
                        "player", "raid", "server", "vehicle", "weather", "world")

                    for (pkg in events) {
                        try {
                            val c = Class.forName("org.bukkit.event.$pkg.$eventName")
                            if (Utilities.classIsEvent(c)) {
                                @Suppress("UNCHECKED_CAST")
                                return CoerceJavaToLua.coerce(plugin.registerEvent(c as Class<out Event>, callback))
                            }
                        } catch (ignored: ClassNotFoundException) {
                            // This would spam the console anytime an event is registered if we print the stack trace
                        }
                    }
                }

                throw LukkitPluginException("There was an issue trying to register the event ${arg1.tostring()}. Is it a valid event name and properly capitalized?")
            }
        })

        set("getServer", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return CoerceJavaToLua.coerce(plugin.server)
            }
        })

        set("setNaggable", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                plugin.isNaggable = arg.checkboolean()
                return LuaValue.NIL
            }
        })

        set("isNaggable", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(plugin.isNaggable)
            }
        })

        set("exportResource", object : TwoArgFunction() {
            override fun call(path: LuaValue, replace: LuaValue): LuaValue {
                if (!plugin.getPluginFile().resourceExists(path.checkjstring())) {
                    return LuaValue.FALSE
                }

                plugin.saveResource(path.checkjstring(), replace.checkboolean())
                return LuaValue.TRUE
            }
        })

        set("getStorageObject", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val stringPath = path.checkjstring()
                val type: StorageObject.Storage = when {
                    stringPath.lowercase().endsWith(".json") -> StorageObject.Storage.JSON
                    stringPath.lowercase().endsWith(".yml") || stringPath.lowercase().endsWith(".yaml") -> StorageObject.Storage.YAML
                    else -> {
                        val error = LuaError("The provided file for a storage object was not a JSON or YAML file.")
                        LuaEnvironment.addError(error)
                        throw error
                    }
                }

                if (!cachedObjects.containsKey(stringPath)) {
                    val obj = if (type == StorageObject.Storage.JSON) {
                        JsonStorage(plugin, stringPath)
                    } else {
                        YamlStorage(plugin, stringPath)
                    }
                    cachedObjects[stringPath] = obj
                }

                return CoerceJavaToLua.coerce(cachedObjects[stringPath])
            }
        })

        set("getPlugin", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return CoerceJavaToLua.coerce(plugin)
            }
        })
    }

    override fun typename(): String = ObjectType.WRAPPER.name

    override fun type(): Int = ObjectType.WRAPPER.type
}