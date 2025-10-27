package io.thegamingmahi.luacord.environment.plugin.commands

import io.thegamingmahi.luacord.environment.LuaEnvironment
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Command event wrapper for Lua
 *
 * @author AL_1
 * @author TheGamingMahi (Kotlin rewrite)
 */
class CommandEvent(
    private val sender: CommandSender,
    private val command: String,
    vararg args: String
) : LuaTable() {

    private val args: Array<out String> = args

    init {
        set("isPlayerSender", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return if (sender is Player) TRUE else FALSE
            }
        })

        set("isConsoleSender", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return if (sender is ConsoleCommandSender) TRUE else FALSE
            }
        })

        set("isBlockSender", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return if (sender is BlockCommandSender) TRUE else FALSE
            }
        })

        set("isEntitySender", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return if (sender is Entity) TRUE else FALSE
            }
        })

        set("getSender", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return CoerceJavaToLua.coerce(sender)
            }
        })

        set("getArgs", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val tbl = LuaTable()
                for (i in args.indices) {
                    tbl.set(i + 1, args[i])
                }
                return tbl
            }
        })

        set("getCommand", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return CoerceJavaToLua.coerce(command)
            }
        })
    }

    override fun typename(): String {
        return LuaEnvironment.ObjectType.COMMAND_EVENT.typeName
    }

    override fun type(): Int {
        return LuaEnvironment.ObjectType.COMMAND_EVENT.type
    }
}
