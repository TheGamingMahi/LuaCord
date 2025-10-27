package io.thegamingmahi.luacord.environment.plugin.commands

import io.thegamingmahi.luacord.Utilities
import io.thegamingmahi.luacord.environment.LuaEnvironment
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginException
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.scheduler.BukkitScheduler
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * LuaCord command implementation
 *
 * @author AL_1
 * @author TheGamingMahi (Kotlin rewrite)
 */
class LukkitCommand(
    private val plugin: LukkitPlugin,
    private val function: LuaFunction,
    name: String,
    description: String = "",
    usage: String = "",
    aliases: Array<String> = emptyArray()
) : Command(name.lowercase(), description, usage, aliases.toList()) {

    companion object {
        private val ERROR_MISSING_ARGS = "${ChatColor.DARK_RED}${ChatColor.BOLD}ERROR!${ChatColor.RED} Missing args."
        private val ERROR_TOO_MANY_ARGS = "${ChatColor.DARK_RED}${ChatColor.BOLD}ERROR!${ChatColor.RED} Too many args."
        private val ERROR_NO_PERMISSION = "${ChatColor.DARK_RED}${ChatColor.BOLD}ERROR!${ChatColor.RED} No permission."

        private fun getPrivateField(obj: Any, field: String): Any? {
            val clazz = obj.javaClass
            val objectField = clazz.getDeclaredField(field)
            objectField.isAccessible = true
            val result = objectField.get(obj)
            objectField.isAccessible = false
            return result
        }
    }

    private var tabCompleteFunction: LuaFunction? = null
    private var registered = false
    var isRunAsync = false
    var minArgs = 0
    var maxArgs = -1

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun register() {
        if (name.isEmpty() || description.isEmpty() || registered) return

        val bukkitCommandMap = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        bukkitCommandMap.isAccessible = true
        val commandMap = bukkitCommandMap.get(Bukkit.getServer()) as CommandMap

        commandMap.register(plugin.description.name, this)
        registered = true
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun unregister() {
        val result = getPrivateField(Bukkit.getServer().pluginManager, "commandMap")
        val commandMap = result as SimpleCommandMap
        val knownCommands: HashMap<String, Command>

        // Try to get known commands the old way, below 1.13
        knownCommands = try {
            val knownCommandsMap = getPrivateField(commandMap, "knownCommands")
            @Suppress("UNCHECKED_CAST")
            knownCommandsMap as HashMap<String, Command>
        } catch (ignored: NoSuchFieldException) {
            // Use 1.13+ CraftCommandMap.getKnownCommands method
            try {
                @Suppress("UNCHECKED_CAST")
                commandMap.javaClass.getMethod("getKnownCommands").invoke(commandMap) as HashMap<String, Command>
            } catch (ignored2: Exception) {
                return
            }
        }

        knownCommands.remove(name)
        for (alias in aliases) {
            if (knownCommands.containsKey(alias) && knownCommands[alias].toString().contains(this.name)) {
                knownCommands.remove(alias)
            }
        }
    }

    override fun execute(sender: CommandSender, command: String, args: Array<String>): Boolean {
        if (!this.testPermissionSilent(sender)) {
            sender.sendMessage(ERROR_NO_PERMISSION)
            return true
        }

        try {
            when {
                args.size > maxArgs && maxArgs >= 0 -> {
                    sender.sendMessage(ERROR_TOO_MANY_ARGS)
                }
                args.size < minArgs -> {
                    sender.sendMessage(ERROR_MISSING_ARGS)
                }
                else -> {
                    if (isRunAsync) {
                        val scheduler: BukkitScheduler = Bukkit.getScheduler()
                        scheduler.runTaskAsynchronously(plugin, Runnable {
                            function.invoke(arrayOf(CommandEvent(sender, command, *args)))
                        })
                    } else {
                        function.invoke(arrayOf(CommandEvent(sender, command, *args)))
                    }
                }
            }
        } catch (e: LukkitPluginException) {
            e.printStackTrace()
            LuaEnvironment.addError(e)
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        val def = super.tabComplete(sender, alias, args)

        tabCompleteFunction?.let { func ->
            val value = func.invoke(
                CoerceJavaToLua.coerce(sender),
                CoerceJavaToLua.coerce(alias),
                CoerceJavaToLua.coerce(args)
            ).arg1()

            if (value != LuaValue.NIL) {
                val tbl = value.checktable()
                val obj = Utilities.convertTable(tbl)
                if (obj is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    return obj as List<String>
                }
            }
        }

        return def
    }

    fun onTabComplete(f: LuaValue) {
        tabCompleteFunction = f.checkfunction()
    }
}