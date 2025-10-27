package io.thegamingmahi.luacord.tests

import io.thegamingmahi.luacord.UpdateChecker
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for version comparison logic
 *
 * @author jammehcow
 * @author TheGamingMahi (Kotlin rewrite + improvements)
 */
class UpdateCheckerTests {

    @Test
    fun `should detect older versions needing update`() {
        // Patch version updates
        assertTrue(UpdateChecker.isOutOfDate("1.1.12", "1.1.13"))

        // Minor version updates
        assertTrue(UpdateChecker.isOutOfDate("1.1.12", "1.2.12"))
        assertTrue(UpdateChecker.isOutOfDate("1.1.12", "1.2.0"))

        // Major version updates
        assertTrue(UpdateChecker.isOutOfDate("1.1.12", "2.1.12"))
        assertTrue(UpdateChecker.isOutOfDate("1.1.12", "2.0.0"))

        // Edge cases
        assertTrue(UpdateChecker.isOutOfDate("0.1.0", "1.0.0"))
        assertTrue(UpdateChecker.isOutOfDate("1.0.0", "1.0.1"))
    }

    @Test
    fun `should not update when running newer version`() {
        // These happen when running dev snapshots
        assertFalse(UpdateChecker.isOutOfDate("1.1.13", "1.1.12"))
        assertFalse(UpdateChecker.isOutOfDate("1.2.12", "1.1.12"))
        assertFalse(UpdateChecker.isOutOfDate("2.1.12", "1.1.12"))
        assertFalse(UpdateChecker.isOutOfDate("2.0.0", "1.1.12"))

        // Beta ahead of stable
        assertFalse(UpdateChecker.isOutOfDate("1.0.1", "1.0.0"))
    }

    @Test
    fun `should not update when versions are identical`() {
        assertFalse(UpdateChecker.isOutOfDate("1.1.12", "1.1.12"))
        assertFalse(UpdateChecker.isOutOfDate("1.2.12", "1.2.12"))
        assertFalse(UpdateChecker.isOutOfDate("2.1.12", "2.1.12"))
        assertFalse(UpdateChecker.isOutOfDate("2.0.0", "2.0.0"))
        assertFalse(UpdateChecker.isOutOfDate("0.1.0", "0.1.0"))
    }

    @Test
    fun `should handle complex version comparisons`() {
        // Major takes precedence
        assertTrue(UpdateChecker.isOutOfDate("1.9.9", "2.0.0"))

        // Minor takes precedence over patch
        assertTrue(UpdateChecker.isOutOfDate("1.1.99", "1.2.0"))

        // Large version jumps
        assertTrue(UpdateChecker.isOutOfDate("1.0.0", "10.0.0"))
    }
}