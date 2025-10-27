package io.thegamingmahi.luacord.environment.wrappers.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import io.thegamingmahi.luacord.Utilities
import io.thegamingmahi.luacord.environment.exception.StorageObjectException
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import org.luaj.vm2.LuaBoolean
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.*

/**
 * JSON storage implementation
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class JsonStorage(plugin: LukkitPlugin, path: String) : StorageObject(plugin, path, Storage.JSON) {

    private val gson: Gson = GsonBuilder().create()
    private var jsonObject: JsonObject

    init {
        jsonObject = try {
            JsonParser().parse(JsonReader(FileReader(getStorageFile()))).asJsonObject
        } catch (e: FileNotFoundException) {
            JsonObject()
        } catch (e: IllegalStateException) {
            JsonObject()
        }
    }

    override fun exists(path: String): LuaBoolean {
        return if (jsonObject.has(path)) LuaValue.TRUE else LuaValue.FALSE
    }

    @Throws(StorageObjectException::class)
    override fun setDefaultValue(path: LuaString, value: LuaValue): LuaBoolean {
        return if (jsonObject.has(path.checkjstring())) {
            setValue(path, value)
            LuaValue.TRUE
        } else {
            LuaValue.FALSE
        }
    }

    @Throws(StorageObjectException::class)
    override fun setValue(path: LuaString, value: LuaValue) {
        jsonObject.add(path.checkjstring(), gson.toJsonTree(Utilities.getObjectFromLuavalue(value)))
    }

    @Throws(StorageObjectException::class)
    override fun clearValue(path: LuaString): LuaBoolean {
        return if (jsonObject.has(path.checkjstring())) {
            jsonObject.remove(path.checkjstring())
            LuaValue.TRUE
        } else {
            LuaValue.FALSE
        }
    }

    @Throws(StorageObjectException::class)
    override fun getValue(path: LuaString): LuaValue {
        return CoerceJavaToLua.coerce(jsonObject.get(path.checkjstring()))
    }

    override fun save() {
        preSaveCheck()
        try {
            FileWriter(getStorageFile()).use { writer ->
                gson.toJson(jsonObject, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}