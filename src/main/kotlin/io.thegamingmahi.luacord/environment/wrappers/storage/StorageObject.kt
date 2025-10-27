package io.thegamingmahi.luacord.environment.wrappers.storage

import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType
import io.thegamingmahi.luacord.environment.exception.StorageObjectException
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import org.luaj.vm2.LuaBoolean
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * The abstract Storage class.
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
abstract class StorageObject(
    private val plugin: LukkitPlugin,
    path: String,
    private val type: Storage
) : LuaTable() {

    private val storageFile: File
    private val self = this

    init {
        val filteredPath = if (path.startsWith(File.separator)) path else File.separator + path
        storageFile = File(plugin.dataFolder.absolutePath + filteredPath)

        if (!storageFile.exists()) {
            try {
                Files.createFile(storageFile.toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        set("getType", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return CoerceJavaToLua.coerce(self.type.type)
            }
        })

        set("exists", object : TwoArgFunction() {
            override fun call(storage: LuaValue, path: LuaValue): LuaValue {
                return self.exists(path.checkjstring())
            }
        })

        set("setDefaultValue", object : ThreeArgFunction() {
            override fun call(storage: LuaValue, path: LuaValue, value: LuaValue): LuaValue {
                return try {
                    self.setDefaultValue(path.checkstring(), value)
                } catch (e: StorageObjectException) {
                    LuaValue.NIL
                }
            }
        })

        set("setValue", object : ThreeArgFunction() {
            override fun call(storage: LuaValue, path: LuaValue, value: LuaValue): LuaValue {
                return try {
                    self.setValue(path.checkstring(), value)
                    LuaValue.TRUE
                } catch (e: StorageObjectException) {
                    LuaValue.FALSE
                }
            }
        })

        set("getValue", object : TwoArgFunction() {
            override fun call(storage: LuaValue, path: LuaValue): LuaValue {
                return try {
                    self.getValue(path.checkstring())
                } catch (e: StorageObjectException) {
                    LuaValue.NIL
                }
            }
        })

        set("clearValue", object : TwoArgFunction() {
            override fun call(storage: LuaValue, path: LuaValue): LuaValue {
                return try {
                    self.clearValue(path.checkstring())
                } catch (e: StorageObjectException) {
                    LuaValue.NIL
                }
            }
        })

        set("save", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                self.save()
                return LuaValue.NIL
            }
        })
    }

    override fun typename(): String = ObjectType.STORAGE_OBJECT.typeName

    override fun type(): Int = ObjectType.STORAGE_OBJECT.type

    /**
     * Checks if the key exists.
     */
    protected abstract fun exists(path: String): LuaBoolean

    /**
     * Sets a value if it doesn't exist.
     */
    @Throws(StorageObjectException::class)
    protected abstract fun setDefaultValue(path: LuaString, value: LuaValue): LuaBoolean

    /**
     * Sets the value of a key.
     */
    @Throws(StorageObjectException::class)
    protected abstract fun setValue(path: LuaString, value: LuaValue)

    /**
     * Clears a value
     */
    @Throws(StorageObjectException::class)
    protected abstract fun clearValue(path: LuaString): LuaBoolean

    /**
     * Gets the value of a key.
     */
    @Throws(StorageObjectException::class)
    protected abstract fun getValue(path: LuaString): LuaValue

    /**
     * Save the file.
     */
    protected abstract fun save()

    /**
     * Gets the absolute path of the storage file.
     */
    protected fun getFilePath(): String = storageFile.absolutePath

    /**
     * Gets the associated LukkitPlugin.
     */
    protected fun getPlugin(): LukkitPlugin = plugin

    /**
     * Gets the storage file.
     */
    protected fun getStorageFile(): File = storageFile

    /**
     * Pre save file check.
     */
    protected fun preSaveCheck() {
        if (!storageFile.exists()) {
            try {
                Files.createFile(storageFile.toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Storage type enum
     */
    enum class Storage(val type: String) {
        /**
         * JSON storage.
         */
        JSON("json"),
        /**
         * YAML storage.
         */
        YAML("yaml")
    }
}