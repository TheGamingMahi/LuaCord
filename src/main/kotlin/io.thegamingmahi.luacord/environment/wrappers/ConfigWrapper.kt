package io.thegamingmahi.luacord.environment.wrappers

import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * Config wrapper for Lua
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class ConfigWrapper(private val plugin: LukkitPlugin) : LuaTable() {
    private var autosave: Boolean = false

    init {
        set("getValue", object : OneArgFunction() {
            override fun call(key: LuaValue): LuaValue {
                return castObject(plugin.config.get(key.checkjstring()))
            }
        })

        set("setDefault", object : TwoArgFunction() {
            override fun call(path: LuaValue, value: LuaValue): LuaValue {
                if (!plugin.config.contains(path.checkjstring())) {
                    when (value) {
                        is LuaString -> plugin.config.set(path.tojstring(), value.tojstring())
                        is LuaInteger -> plugin.config.set(path.tojstring(), value.toint())
                        is LuaDouble -> plugin.config.set(path.tojstring(), value.todouble())
                        is LuaBoolean -> plugin.config.set(path.tojstring(), value.toboolean())
                        else -> plugin.config.set(path.tojstring(), value.touserdata())
                    }

                    if (autosave) plugin.saveConfig()
                    return LuaValue.TRUE
                }
                return LuaValue.FALSE
            }
        })

        set("set", object : TwoArgFunction() {
            override fun call(path: LuaValue, value: LuaValue): LuaValue {
                when (value) {
                    is LuaString -> plugin.config.set(path.tojstring(), value.tojstring())
                    is LuaInteger -> plugin.config.set(path.tojstring(), value.toint())
                    is LuaDouble -> plugin.config.set(path.tojstring(), value.todouble())
                    is LuaBoolean -> plugin.config.set(path.tojstring(), value.toboolean())
                    else -> plugin.config.set(path.tojstring(), value.touserdata())
                }

                if (autosave) plugin.saveConfig()
                return LuaValue.NIL
            }
        })

        set("clear", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                plugin.config.set(path.checkjstring(), null)
                plugin.saveConfig()
                return LuaValue.NIL
            }
        })

        set("setAutosave", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                autosave = arg.checkboolean()
                return LuaValue.NIL
            }
        })

        set("save", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                plugin.saveConfig()
                return LuaValue.NIL
            }
        })

        set("mapTableToKey", object : TwoArgFunction() {
            override fun call(path: LuaValue, table: LuaValue): LuaValue {
                plugin.config.createSection(path.checkjstring(), convertToMap(table.checktable()))
                return LuaValue.NIL
            }
        })
    }

    private fun convertToMap(table: LuaTable): Map<*, *> {
        val map = mutableMapOf<Any, Any?>()
        val rootKeys = table.keys()

        for (k in rootKeys) {
            if (table.get(k).istable()) {
                map[k] = convertToMap(table.get(k).checktable())
            } else {
                map[k] = table.get(k).touserdata()
            }
        }

        return map
    }

    override fun typename(): String = ObjectType.WRAPPER.name

    override fun type(): Int = ObjectType.WRAPPER.type

    companion object {
        @JvmStatic
        fun castObject(obj: Any?): LuaValue {
            return when (obj) {
                is String -> LuaValue.valueOf(obj)
                is Int -> LuaValue.valueOf(obj)
                is Double -> LuaValue.valueOf(obj)
                is Boolean -> LuaValue.valueOf(obj)
                else -> LuaValue.userdataOf(obj)
            }
        }
    }
}