package io.thegamingmahi.luacord.environment.wrappers

import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import org.bukkit.ChatColor
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * ChatColor wrapper for Lua
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class ChatColorWrapper(private val plugin: LukkitPlugin) : LuaTable() {

    init {
        set("getByChar", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(ChatColor.getByChar(c.checkjstring()))
            }
        })

        set("getLastColors", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(ChatColor.getLastColors(c.checkjstring()))
            }
        })

        set("stripColor", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(ChatColor.stripColor(c.checkjstring()))
            }
        })

        set("valueOf", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                // Needs to be a string so that it can be concatenated with a string in lua with ease
                return CoerceJavaToLua.coerce(ChatColor.valueOf(c.checkjstring()).toString())
            }
        })

        set("translateAlternateColorCodes", object : TwoArgFunction() {
            override fun call(c: LuaValue, s: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(
                    ChatColor.translateAlternateColorCodes(
                        c.checkjstring()[0],
                        s.checkjstring()
                    )
                )
            }
        })

        set("values", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                // Convert ChatColors to string to be usable by lua
                val colors = ChatColor.values().map { it.toString() }
                return CoerceJavaToLua.coerce(colors.toTypedArray())
            }
        })

        // Add every color to the table
        // Needs to be a string so that it can be concatenated with a string in lua with ease
        for (color in ChatColor.values()) {
            set(color.name, CoerceJavaToLua.coerce(color.toString()))
        }
    }

    override fun typename(): String = ObjectType.WRAPPER.typeName

    override fun type(): Int = ObjectType.WRAPPER.type
}