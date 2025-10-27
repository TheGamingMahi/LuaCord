package io.thegamingmahi.luacord

import io.thegamingmahi.luacord.environment.LuaEnvironment
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

/**
 * Tab completer for LuaCord commands
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class TabCompleter : org.bukkit.command.TabCompleter {

    companion object {
        private val subCommands = arrayOf("help", "plugins", "dev", "run")
        private val devSubCommands = arrayOf("reload", "unload", "pack", "unpack", "last-error", "errors")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        val tabComplete = mutableListOf<String>()

        if (command.name.startsWith("lukkit")) {
            when (args.size) {
                1 -> return getFilteredCompletions(args[0], subCommands)

                2 -> {
                    val cmd = args[0]
                    val subArgs = args.drop(1).toTypedArray()

                    if (cmd.equals("dev", ignoreCase = true)) {
                        val plugins = mutableListOf<String>()
                        Main.instance?.iteratePlugins { p -> plugins.add(p.name) }

                        return when {
                            subArgs.size == 1 -> getFilteredCompletions(subArgs[0], devSubCommands)

                            subArgs.size == 2 && subArgs[0].let {
                                it.equals("reload", ignoreCase = true) ||
                                        it.equals("unload", ignoreCase = true) ||
                                        it.equals("pack", ignoreCase = true) ||
                                        it.equals("unpack", ignoreCase = true)
                            } -> getFilteredCompletions(subArgs[1], plugins.toTypedArray())

                            subArgs[0].equals("errors", ignoreCase = true) -> {
                                LuaEnvironment.getErrors().ifPresent { errors ->
                                    var count = 0
                                    errors.forEach { _ ->
                                        tabComplete.add(count.toString())
                                        count++
                                    }
                                }
                                tabComplete
                            }

                            else -> emptyList()
                        }
                    }
                }
            }
        }

        return tabComplete
    }

    private fun getFilteredCompletions(arg: String, subCommands: Array<String>): List<String> {
        return if (arg.isNotEmpty()) {
            subCommands.filter { it.startsWith(arg) }
        } else {
            subCommands.toList()
        }
    }
}