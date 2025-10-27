package io.thegamingmahi.luacord.environment.wrappers

import io.thegamingmahi.luacord.environment.LuaEnvironment
import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginException
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.util.concurrent.*
import java.util.stream.Stream

/**
 * Utilities wrapper for Lua
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class UtilitiesWrapper(private val plugin: LukkitPlugin) : LuaTable() {
    private val runDelayedThreadPool: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    init {
        set("getTableFromList", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val list: Array<Any> = when (val userData = arg.checkuserdata()) {
                    is Collection<*> -> userData.toTypedArray()
                    is Stream<*> -> userData.toArray()
                    else -> throw LukkitPluginException("util.tableFromList(obj) was passed something other than an instance of Collection or Stream.")
                }

                val t = LuaTable()
                list.forEachIndexed { i, item ->
                    t.set(LuaValue.valueOf(i + 1), CoerceJavaToLua.coerce(item))
                }

                return t
            }
        })

        set("getTableFromArray", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val list = arg.touserdata() as Array<*>

                val t = LuaTable()
                list.forEachIndexed { i, item ->
                    t.set(LuaValue.valueOf(i + 1), CoerceJavaToLua.coerce(item))
                }

                return t
            }
        })

        set("getTableFromMap", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val map = when (val userData = arg.checkuserdata()) {
                    is Map<*, *> -> userData
                    else -> throw LukkitPluginException("util.tableFromMap(obj) was passed something other than a implementation of Map.")
                }

                val t = LuaTable()
                map.forEach { (k, v) ->
                    t.set(CoerceJavaToLua.coerce(k), CoerceJavaToLua.coerce(v))
                }

                return t
            }
        })

        set("getTableLength", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checktable().keyCount())
            }
        })

        set("runAsync", object : TwoArgFunction() {
            override fun call(function: LuaValue, delay: LuaValue): LuaValue {
                val thread = Thread {
                    try {
                        if (delay != LuaValue.NIL) {
                            Thread.sleep(delay.checklong())
                        }
                        function.checkfunction().call()
                    } catch (ignored: InterruptedException) {
                    }
                }

                thread.start()
                return LuaValue.NIL
            }
        })

        set("runDelayed", object : TwoArgFunction() {
            override fun call(function: LuaValue, time: LuaValue): LuaValue {
                val future: ScheduledFuture<LuaValue> = runDelayedThreadPool.schedule(
                    { function.call() },
                    time.checklong(),
                    TimeUnit.MILLISECONDS
                )

                try {
                    future.get()
                } catch (e: Exception) {
                    plugin.logger.warning("The thread spawned by runDelayed was terminated or threw an exception")
                    LuaEnvironment.addError(e)
                    e.printStackTrace()
                }

                return LuaValue.NIL
            }
        })

        set("getBukkitRunnable", object : OneArgFunction() {
            override fun call(function: LuaValue): LuaValue {
                val func = function.checkfunction()
                val task = object : BukkitRunnable() {
                    override fun run() {
                        func.call()
                    }
                }
                return CoerceJavaToLua.coerce(task)
            }
        })

        set("getClass", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                return try {
                    CoerceJavaToLua.coerce(Class.forName(path.checkjstring()))
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    NIL
                }
            }
        })

        set("getSkullMeta", object : OneArgFunction() {
            override fun call(item: LuaValue): LuaValue {
                if (!item.isnil() && item.checkuserdata() !is ItemStack) {
                    throw LukkitPluginException("bukkit.getSkullMeta was passed something other than an ItemStack.")
                }

                return CoerceJavaToLua.coerce(
                    SkullWrapper(if (item.isnil()) null else item.touserdata() as ItemStack)
                )
            }
        })

        set("getBannerMeta", object : OneArgFunction() {
            override fun call(item: LuaValue): LuaValue {
                if (!item.isnil() && item.checkuserdata() !is ItemStack) {
                    throw LukkitPluginException("bukkit.getBannerMeta was passed something other than an ItemStack.")
                }

                return CoerceJavaToLua.coerce(
                    BannerWrapper(if (item.isnil()) null else item.touserdata() as ItemStack)
                )
            }
        })

        set("parseItemStack", object : OneArgFunction() {
            override fun call(item: LuaValue): LuaValue {
                if (!item.isnil() && item.checkuserdata() !is ItemStack) {
                    throw LukkitPluginException("parseItemStack was given something other than an ItemStack")
                }

                return CoerceJavaToLua.coerce(ItemStackWrapper(item.touserdata() as ItemStack))
            }
        })

        set("cast", object : TwoArgFunction() {
            override fun call(userdata: LuaValue, clazz: LuaValue): LuaValue {
                val className = clazz.checkjstring()
                val obj = userdata.checkuserdata()

                return try {
                    val caster = Class.forName(className)
                    userdataOf(caster.cast(obj))
                } catch (e: ClassNotFoundException) {
                    plugin.logger.warning("Could not find class $className")
                    NIL
                } catch (e: ClassCastException) {
                    plugin.logger.warning("Provided userdata cannot be casted to $className")
                    NIL
                } catch (e: LinkageError) {
                    plugin.logger.warning("There was an unknown issue casting the object to $className")
                    e.printStackTrace()
                    NIL
                }
            }
        })

        set("instanceof", object : TwoArgFunction() {
            override fun call(obj: LuaValue, classToCheck: LuaValue): LuaValue {
                val clazz = classToCheck.checkuserdata() as Class<*>
                return CoerceJavaToLua.coerce(clazz.isInstance(obj.checkuserdata()))
            }
        })
    }

    fun close() {
        runDelayedThreadPool.shutdown()
    }

    override fun typename(): String = ObjectType.WRAPPER.name

    override fun type(): Int = ObjectType.WRAPPER.type
}