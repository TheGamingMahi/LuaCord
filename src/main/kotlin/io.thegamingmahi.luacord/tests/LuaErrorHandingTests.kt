package io.thegamingmahi.luacord.tests

import io.thegamingmahi.luacord.environment.LuaEnvironment
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for Lua error handling and stack management
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite + improvements)
 */
class LuaErrorHandlingTests {

    @Before
    fun setup() {
        // Initialize the Lua environment before each test
        LuaEnvironment.init(false)
    }

    @Test
    fun `error stack should start empty`() {
        // Clear any previous errors
        LuaEnvironment.clearErrors()

        // Verify stack is empty
        assertFalse(LuaEnvironment.getErrors().isPresent)
        assertFalse(LuaEnvironment.getLastError().isPresent)
    }

    @Test
    fun `should store and retrieve single error`() {
        LuaEnvironment.clearErrors()

        // Add a test error
        val testError = Exception("Test error message")
        LuaEnvironment.addError(testError)

        // Verify we can retrieve it
        val result = LuaEnvironment.getLastError()
        assertTrue(result.isPresent)
        assertEquals("Test error message", result.get().message)
    }

    @Test
    fun `should maintain LIFO order for errors`() {
        LuaEnvironment.clearErrors()

        // Add multiple errors
        val error1 = Exception("First error")
        val error2 = Exception("Second error")
        val error3 = Exception("Third error")

        LuaEnvironment.addError(error1)
        LuaEnvironment.addError(error2)
        LuaEnvironment.addError(error3)

        // Last error should be the most recent
        val lastError = LuaEnvironment.getLastError()
        assertTrue(lastError.isPresent)
        assertEquals("Third error", lastError.get().message)
    }

    @Test
    fun `should track multiple errors in stack`() {
        LuaEnvironment.clearErrors()

        // Add two errors
        LuaEnvironment.addError(Exception("Error 1"))
        LuaEnvironment.addError(Exception("Error 2"))

        // Verify both are in the stack
        val errorsStream = LuaEnvironment.getErrors()
        assertTrue(errorsStream.isPresent)
        assertEquals(2L, errorsStream.get().count())
    }

    @Test
    fun `should handle LukkitPluginException specifically`() {
        LuaEnvironment.clearErrors()

        // Add a plugin-specific exception
        val pluginError = LukkitPluginException("Plugin failed to load")
        LuaEnvironment.addError(pluginError)

        // Verify it's stored correctly
        val result = LuaEnvironment.getLastError()
        assertTrue(result.isPresent)
        assertTrue(result.get() is LukkitPluginException)
        assertEquals("Plugin failed to load", result.get().message)
    }

    @Test
    fun `should respect maximum stack size`() {
        LuaEnvironment.clearErrors()

        // Add more than 10 errors (stack size limit)
        for (i in 1..15) {
            LuaEnvironment.addError(Exception("Error $i"))
        }

        // Stack should contain at most 10 errors
        val errorsStream = LuaEnvironment.getErrors()
        assertTrue(errorsStream.isPresent)
        assertTrue(errorsStream.get().count() <= 10L)
    }

    @Test
    fun `should clear all errors when requested`() {
        // Add some errors
        LuaEnvironment.addError(Exception("Error 1"))
        LuaEnvironment.addError(Exception("Error 2"))

        // Clear them
        LuaEnvironment.clearErrors()

        // Verify stack is empty
        assertFalse(LuaEnvironment.getErrors().isPresent)
        assertFalse(LuaEnvironment.getLastError().isPresent)
    }

    @Test
    fun `should filter out null errors from stack`() {
        LuaEnvironment.clearErrors()

        // Add some real errors
        LuaEnvironment.addError(Exception("Real error"))

        // The internal stack may have nulls due to setSize(10)
        // Verify getErrors() filters them out
        val errors = LuaEnvironment.getErrors()
        assertTrue(errors.isPresent)

        // Count should only include non-null errors
        errors.get().forEach { error ->
            assertNotNull(error)
        }
    }
}