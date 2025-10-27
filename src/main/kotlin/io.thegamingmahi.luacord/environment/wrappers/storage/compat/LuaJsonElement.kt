package io.thegamingmahi.luacord.environment.wrappers.storage.compat

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.thegamingmahi.luacord.environment.LuaEnvironment
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * Lua JSON element converter
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite)
 */
class LuaJsonElement(jsonElement: JsonElement) {

    val element: LuaValue = getValueFromElement(jsonElement)

    private fun getTableFromElement(element: JsonElement): LuaTable {
        val finalTable = LuaTable()

        if (element.isJsonArray) {
            val array = element.asJsonArray
            for (i in 0 until array.size()) {
                // Add one to i to match Lua array indexing standards
                finalTable.set(i + 1, getValueFromElement(array.get(i)))
            }
        } else {
            val obj = element.asJsonObject
            for ((key, value) in obj.entrySet()) {
                finalTable.set(key, getValueFromElement(value))
            }
        }

        return finalTable
    }

    private fun getValueFromElement(element: JsonElement): LuaValue {
        return when {
            element.isJsonArray || element.isJsonObject -> {
                getTableFromElement(element)
            }
            element.isJsonNull -> {
                LuaValue.NIL
            }
            element.isJsonPrimitive -> {
                val primitiveValue = element.asJsonPrimitive
                when {
                    primitiveValue.isBoolean -> {
                        LuaValue.valueOf(primitiveValue.asBoolean)
                    }
                    primitiveValue.isString -> {
                        LuaValue.valueOf(primitiveValue.asString)
                    }
                    primitiveValue.isNumber -> {
                        val numberValue = primitiveValue.asNumber
                        when (numberValue) {
                            is Double -> LuaValue.valueOf(numberValue)
                            is Int -> LuaValue.valueOf(numberValue)
                            is Short -> LuaValue.valueOf(numberValue.toInt())
                            is Long -> LuaValue.valueOf(numberValue.toDouble())
                            is Float -> LuaValue.valueOf(numberValue.toDouble())
                            is Byte -> LuaValue.valueOf(numberValue.toInt())
                            else -> LuaValue.NIL
                        }
                    }
                    else -> LuaValue.NIL
                }
            }
            else -> {
                val error = LuaError(
                    "A LuaJsonElement object was passed an unsupported value other than that " +
                            "supported by LuaJ. Value: $element"
                )
                LuaEnvironment.addError(error)
                error.printStackTrace()
                LuaValue.NIL
            }
        }
    }
}