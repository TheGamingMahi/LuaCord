package io.thegamingmahi.luacord.environment.wrappers

import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import org.bukkit.Material
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Material wrapper for Lua
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class MaterialWrapper(private val plugin: LukkitPlugin) : LuaTable() {

    init {
        set("valueOf", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(Material.valueOf(c.checkjstring()))
            }
        })

        set("matchMaterial", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(Material.matchMaterial(c.checkjstring()))
            }
        })

        set("getMaterial", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(Material.getMaterial(c.checkjstring()))
            }
        })

        set("getMaterialById", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(Material.getMaterial(c.checkint()))
            }
        })

        set("values", object : OneArgFunction() {
            override fun call(c: LuaValue): LuaValue {
                return CoerceJavaToLua.coerce(Material.values())
            }
        })

        // Add every material to the table
        for (mat in Material.values()) {
            set(mat.name, CoerceJavaToLua.coerce(mat))
        }
    }

    override fun typename(): String = ObjectType.WRAPPER.typeName

    override fun type(): Int = ObjectType.WRAPPER.type
}