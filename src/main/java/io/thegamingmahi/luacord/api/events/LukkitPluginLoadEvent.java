package io.thegamingmahi.luacord.api.events;

import io.thegamingmahi.luacord.environment.plugin.LukkitPluginFile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LukkitPluginLoadEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final LukkitPluginFile pluginFile;
    private boolean cancelled = false;

    public LukkitPluginLoadEvent(LukkitPluginFile pluginFile) {
        this.pluginFile = pluginFile;
    }

    public LukkitPluginFile getPluginFile() {
        return pluginFile;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}
