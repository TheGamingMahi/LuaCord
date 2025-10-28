# LuaCord

**Write real Minecraft plugins in Lua.**

LuaCord is a modern fork of [Lukkit](https://github.com/jammehcow/Lukkit), rewritten in Kotlin (87%) with full Paper compatibility and extended API support. Create powerful Minecraft plugins using Lua scripting with access to the complete Spigot/Paper API.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Spigot](https://img.shields.io/badge/Spigot-Compatible-orange.svg)](https://www.spigotmc.org/)
[![Paper](https://img.shields.io/badge/Paper-Compatible-blue.svg)](https://papermc.io/)

---

## 🎯 Features

- **Real Minecraft Plugins** - Not just scripts, full plugin functionality with commands, events, and permissions
- **Full Spigot API Access** - Use the entire Bukkit/Spigot API from Lua
- **Paper Compatible** - Works flawlessly on modern Paper servers
- **100% Backwards Compatible** - All existing Lukkit `.lkt` plugins work without modification
- **Clean Console Output** - No more ugly class path spam in logs (`[Example Plugin]` instead of `[nz.co.jammehcow.lukkit...]`)
- **Hot Reload** - Reload dev plugins without restarting the server
- **Modern Codebase** - 87% Kotlin for better maintainability and null safety

---

## 📦 Installation

1. Download `LuaCord-0.1.0-BETA.jar` from [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/luacord)
2. Place it in your server's `/plugins/` folder
3. Place your `.lkt` plugin files in the same `/plugins/` folder
4. Start/restart your server

**Requirements:**
- Spigot or Paper (any version)
- Java 8 or higher

---

## 🚀 Quick Start

### Plugin Structure

A LuaCord plugin can be either:

**Option 1: `.lkt` file** (ZIP archive containing):
```
MyPlugin.lkt/
├── main.lua          # Your plugin code
├── plugin.yml        # Plugin metadata
└── config.yml        # Optional default config
```

**Option 2: Folder** (for development):
```
/plugins/MyPlugin/
├── main.lua
├── plugin.yml
└── config.yml
```

### Creating Your First Plugin

Create `main.lua`:

```lua
plugin:onEnable(function()
    logger:info("My plugin is enabled!")
end)

plugin:addCommand({
    name = "hello",
    description = "Say hello!",
    usage = "/hello"
}, function(event)
    local sender = event:getSender()
    sender:sendMessage("Hello from Lua!")
end)

plugin:registerEvent("PlayerJoinEvent", function(event)
    local player = event:getPlayer()
    player:sendMessage("Welcome to the server!")
end)
```

Create `plugin.yml`:
```yaml
name: MyPlugin
version: 1.0
main: main.lua
author: YourName
description: My first LuaCord plugin
```

**For .lkt:** ZIP these files and rename to `MyPlugin.lkt`  
**For folder:** Just place the folder in `/plugins/`

That's it! Restart your server and your plugin is live.

---

## ⚙️ Configuration

`config.yml` options:

```yaml
# Enable debug mode for verbose logging
debug-mode: false

# Paper compatibility mode (recommended for Paper servers)
bypass-plugin-registration: true

# Enable Lua debug globals
lua-debug: false

# Check for updates on startup
update-checker: false

# Allow /lukkit run command
can-run-code: true
```

**For Paper servers:** Keep `bypass-plugin-registration: true`  
**For Spigot servers:** You can set it to `false`

---

## 📚 Documentation

### Commands

- `/lukkit` - Show help message
- `/lukkit plugins` - List all loaded LuaCord plugins
- `/lukkit dev` - Show developer commands
- `/lukkit dev reload <plugin>` - Hot reload a dev plugin (folder-based plugins only)
- `/lukkit dev errors` - View error stack for debugging
- `/lukkit dev pack <plugin>` - Package a dev plugin folder into `.lkt`
- `/lukkit dev unpack <plugin>` - Unpack a `.lkt` into a folder for development

### Plugin API

```lua
-- Lifecycle hooks
plugin:onLoad(function() end)
plugin:onEnable(function() end)
plugin:onDisable(function() end)

-- Commands
plugin:addCommand({ name = "cmd", ... }, function(event) end)

-- Events
plugin:registerEvent("EventName", function(event) end)

-- Server access
local server = plugin:getServer()

-- Config
config:getValue("path")
config:set("path", value)
config:save()

-- Logging
logger:info("message")
logger:warn("message")
logger:severe("message")
```

For full API documentation, see the [Lukkit Wiki](https://github.com/jammehcow/Lukkit/wiki) (LuaCord is backwards compatible).

---

## 🐛 Known Issues

When using `bypass-plugin-registration: true` (Paper mode):

- LuaCord plugins don't appear in `/plugins` (use `/lukkit plugins` instead)
- Other plugins can't detect LuaCord plugins via PluginManager
- Plugin dependencies aren't automatically handled

**These limitations don't affect plugin functionality!** Your plugins will work perfectly.

---

## 📝 Changelog

### 0.1.0-BETA *(Current)*

**Initial LuaCord release - forked from Lukkit 2.2.0**

- ✨ Rewritten in Kotlin (87% conversion)
- ✨ Clean console logging (no more `[nz.co.jammehcow.lukkit...]` spam)
- ✨ Full Paper compatibility maintained
- ✨ Package renamed to `io.thegamingmahi.luacord`
- ✨ 100% backwards compatible with Lukkit plugins
- ✨ Improved error handling with stack management
- ✨ Better documentation and modern codebase

**Inherited from Lukkit 2.2.0 (by TheGamingMahi):**
- ✅ Paper server compatibility fix
- ✅ Bypass mode for Paper's plugin restrictions
- ✅ Configurable loading modes
- ✅ Debug mode for troubleshooting

---

## 🎯 Roadmap

### 0.2.0 - JAR Support
- [ ] `.jar` plugin wrapper support alongside `.lkt` files
- [ ] JAR generator web tool for easy packaging
- [ ] Official website and documentation
- [ ] CurseForge release

### Future / Up to 1.0.0
- [ ] Complete Kotlin migration (100%)
- [ ] Extended Paper API implementations
- [ ] Comprehensive test suite
- [ ] VSCode extension
- [ ] Plugin marketplace/repository
- [ ] More example plugins
- [ ] Video tutorials
- [ ] Production-ready stability

---

## 📜 License

**LuaCord Core:** GPL v3 - This ensures the framework remains open source.

**Plugin Wrapper Template (Coming Soon):** MIT - Maximum freedom for plugin developers.

See [LICENSE](LICENSE) for details.

---

## 🙏 Credits

### LuaCord
- **TheGamingMahi** - Creator, Kotlin rewrite, Paper fix (Lukkit 2.2.0), maintenance

### Original Lukkit Team
- jammehcow
- AL_1
- mathhulk
- ArtexDevelopment

**Special thanks to the Lukkit community for keeping the project alive!**

Original Lukkit repository: https://github.com/jammehcow/Lukkit (archived)

---

## 🔗 Links

- **GitHub:** https://github.com/TheGamingMahi/LuaCord
- **Issues:** https://github.com/TheGamingMahi/LuaCord/issues
- **Website:** *(Coming Soon)*
- **CurseForge:** *(Coming Soon)*

---

## 💬 Support

**Found a bug?** Open an issue on [GitHub Issues](https://github.com/TheGamingMahi/LuaCord/issues)

**Need help?** Check the [Lukkit Wiki](https://github.com/jammehcow/Lukkit/wiki) (LuaCord is backwards compatible)

---

## ⚠️ Important Notes

- LuaCord is in **BETA** - expect bugs and potential breaking changes
- This is a fork/continuation of the archived Lukkit project
- Not affiliated with the original Lukkit developers
- The original Lukkit repository is no longer maintained

---

**Write plugins. Ship faster. LuaCord makes it easy.** 🚀
