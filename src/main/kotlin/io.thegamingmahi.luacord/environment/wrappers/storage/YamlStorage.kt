package io.thegamingmahi.luacord.environment.wrappers.storage

import io.thegamingmahi.luacord.Utilities
import io.thegamingmahi.luacord.environment.exception.StorageObjectException
import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.luaj.vm2.LuaBoolean
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.FileReader
import java.io.IOException

/**
 * YAML storage implementation
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class YamlStorage(plugin: LukkitPlugin, path: String) : StorageObject(plugin, path, Storage.YAML) {

    private val yamlConfiguration: YamlConfiguration = YamlConfiguration()

    init {
        try {
            yamlConfiguration.load(FileReader(getStorageFile()))
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
        }
    }

    override fun exists(path: String): LuaBoolean {
        return if (yamlConfiguration.get(path) != null) LuaValue.TRUE else LuaValue.FALSE
    }

    @Throws(StorageObjectException::class)
    override fun setDefaultValue(path: LuaString, value: LuaValue): LuaBoolean {
        return if (yamlConfiguration.get(path.checkjstring()) == null) {
            yamlConfiguration.set(path.checkjstring(), Utilities.getObjectFromLuavalue(value))
            LuaValue.TRUE
        } else {
            LuaValue.FALSE
        }
    }

    @Throws(StorageObjectException::class)
    override fun setValue(path: LuaString, value: LuaValue) {
        yamlConfiguration.set(path.checkjstring(), Utilities.getObjectFromLuavalue(value))
    }

    @Throws(StorageObjectException::class)
    override fun clearValue(path: LuaString): LuaBoolean {
        return if (yamlConfiguration.get(path.checkjstring()) != null) {
            yamlConfiguration.set(path.checkjstring(), null)
            LuaValue.TRUE
        } else {
            LuaValue.FALSE
        }
    }

    @Throws(StorageObjectException::class)
    override fun getValue(path: LuaString): LuaValue {
        return CoerceJavaToLua.coerce(yamlConfiguration.get(path.tojstring()))
    }

    override fun save() {
        preSaveCheck()
        try {
            yamlConfiguration.save(getStorageFile())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}