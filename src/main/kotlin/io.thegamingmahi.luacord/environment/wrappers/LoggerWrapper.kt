package io.thegamingmahi.luacord.environment.wrappers

import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import java.util.logging.Logger

/**
 * Logger wrapper for Lua
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class LoggerWrapper(private val plugin: LukkitPlugin) : LuaTable() {
    private val logger: Logger = plugin.logger

    init {
        set("info", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                logger.info(arg.checkjstring())
                return LuaValue.NIL
            }
        })

        set("warn", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                logger.warning(arg.checkjstring())
                return LuaValue.NIL
            }
        })

        set("severe", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                logger.severe(arg.checkjstring())
                return LuaValue.NIL
            }
        })

        set("debug", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                logger.fine(arg.checkjstring())
                return LuaValue.NIL
            }
        })
    }

    override fun typename(): String = ObjectType.WRAPPER.name

    override fun type(): Int = ObjectType.WRAPPER.type
}