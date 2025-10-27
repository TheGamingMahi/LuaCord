package io.thegamingmahi.luacord.api.events

import io.thegamingmahi.luacord.environment.plugin.LukkitPlugin
import io.thegamingmahi.luacord.environment.plugin.LukkitPluginFile
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Event fired when a LuaCord plugin is loaded
 */
class LukkitPluginLoadEvent(val pluginFile: LukkitPluginFile) : Event(), Cancellable {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    private var cancelled = false

    override fun getHandlers(): HandlerList = Companion.handlers

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}

/**
 * Event fired when a LuaCord plugin is enabled
 */
class LukkitPluginEnableEvent(val plugin: LukkitPlugin) : Event(), Cancellable {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    private var cancelled = false

    override fun getHandlers(): HandlerList = Companion.handlers

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}

/**
 * Event fired when a LuaCord plugin is disabled
 */
class LukkitPluginDisableEvent(val plugin: LukkitPlugin) : Event() {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}