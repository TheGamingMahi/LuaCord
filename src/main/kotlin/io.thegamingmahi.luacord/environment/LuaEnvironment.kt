package io.thegamingmahi.luacord.environment

import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginException
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.util.*
import java.util.stream.Stream

/**
 * Lua environment management with error tracking
 *
 * Manages Lua globals, local requires, and error stack for debugging.
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite + improvements)
 */
object LuaEnvironment {
    private var isDebug: Boolean = false

    /**
     * Error stack with max size of 10 errors.
     * Most recent errors are pushed to the top (LIFO).
     */
    private val errors = Stack<Exception>().apply {
        setSize(10)
    }

    /**
     * Helps collate all of the types together and allows easy changing of IDs
     */
    enum class ObjectType(val type: Int, val typeName: String) {
        WRAPPER(100, "Wrapper"),
        STORAGE_OBJECT(101, "StorageObject"),
        COMMAND_EVENT(102, "CommandEvent")
    }

    /**
     * Initialize the Lua environment with debug mode setting
     *
     * @param debug whether to enable Lua debug globals
     */
    @JvmStatic
    fun init(debug: Boolean) {
        isDebug = debug
    }

    /**
     * Create new Lua globals for a plugin with require_local support
     *
     * This sets up:
     * - Standard or debug Lua globals based on config
     * - Local require system for plugin resources
     * - Package caching to avoid reloading
     *
     * @param plugin the LukkitPlugin to create globals for
     * @return configured Globals instance
     */
    @JvmStatic
    fun getNewGlobals(plugin: LukkitPlugin): Globals {
        // Create the globals based on the config debug setting
        val g = if (isDebug) JsePlatform.debugGlobals() else JsePlatform.standardGlobals()

        // Make a table to store local requires (caching)
        g.set("__lukkitpackages__", LuaTable())

        // Add require_local function for loading plugin resources
        g.set("require_local", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                // Get the path as a Java String
                var path = arg.checkjstring()

                // Normalize path: remove leading slash, ensure .lua extension
                if (path.startsWith("/")) {
                    path = path.replaceFirst("/", "")
                }
                if (!path.endsWith(".lua")) {
                    path = "$path.lua"
                }

                // Check if already loaded from cache
                val possiblyLoadedScript = g.get("__lukkitpackages__").checktable().get(path)
                if (possiblyLoadedScript != null && possiblyLoadedScript != LuaValue.NIL) {
                    return possiblyLoadedScript
                }

                // Load the resource from plugin file
                val inputStream = plugin.getResource(path)
                if (inputStream != null) {
                    try {
                        val calledScript = g.load(
                            InputStreamReader(inputStream, "UTF-8"),
                            path.replace("/", ".")
                        ).call()

                        // Cache the loaded script
                        g.get("__lukkitpackages__").checktable().set(path, calledScript)
                        return calledScript
                    } catch (e: LukkitPluginException) {
                        e.printStackTrace()
                        addError(e)
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                        addError(e)
                    }
                }

                throw LukkitPluginException("Requested Lua file at $path but it does not exist.")
            }
        })

        return g
    }

    /**
     * Get the most recent error from the stack
     *
     * @return Optional containing the last error, or empty if no errors
     */
    @JvmStatic
    fun getLastError(): Optional<Exception> {
        return Optional.ofNullable(errors.peek())
    }

    /**
     * Get all errors from the stack as a stream
     *
     * Filters out null values that may exist due to stack sizing.
     *
     * @return Optional containing stream of errors, or empty if no errors
     */
    @JvmStatic
    fun getErrors(): Optional<Stream<Exception>> {
        val nonNullErrors = errors.stream().filter { it != null }
        return if (nonNullErrors.count() == 0L) {
            Optional.empty()
        } else {
            Optional.of(errors.stream().filter { it != null })
        }
    }

    /**
     * Add an error to the stack
     *
     * Errors are stored in LIFO order (most recent on top).
     * Stack has max size of 10, oldest errors are discarded.
     *
     * @param e the exception to add
     */
    @JvmStatic
    fun addError(e: Exception) {
        errors.push(e)
    }

    /**
     * Clear all errors from the stack
     *
     * Useful for testing or resetting error state.
     */
    @JvmStatic
    fun clearErrors() {
        errors.clear()
        errors.setSize(10)
    }

    /**
     * Get the total number of errors currently in the stack
     *
     * @return count of non-null errors
     */
    @JvmStatic
    fun getErrorCount(): Int {
        return errors.stream().filter { it != null }.count().toInt()
    }

    /**
     * Check if there are any errors in the stack
     *
     * @return true if errors exist, false otherwise
     */
    @JvmStatic
    fun hasErrors(): Boolean {
        return errors.stream().anyMatch { it != null }
    }
}