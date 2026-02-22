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
- **JAR Support** - Package plugins as `.jar` files for easy distribution on CurseForge/Modrinth
- **Paper Compatible** - Works flawlessly on modern Paper servers
- **100% Backwards Compatible** - All existing Lukkit `.lkt` plugins work without modification
- **Clean Console Output** - No more ugly class path spam in logs
- **Hot Reload** - Reload dev plugins without restarting the server
- **Modern Codebase** - 87% Kotlin for better maintainability and null safety

---

## 📦 Installation

### For Server Owners

1. Download `LuaCord-0.2.0-BETA.jar` from [Releases](https://github.com/TheGamingMahi/LuaCord/releases)
2. Place it in your server's `/plugins/` folder
3. Place your `.lkt` or `.jar` plugin files in the same `/plugins/` folder
4. Start/restart your server

**Requirements:**
- Spigot or Paper (any version)
- Java 8 or higher

### For Plugin Developers

Check out the [JAR Generator](https://luacordmc.github.io/generator.html) to convert your `.lkt` plugins to `.jar` files for distribution!

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

**Option 2: `.jar` file** (For distribution on CurseForge/Modrinth):
- Use the [JAR Generator](https://luacordmc.github.io/generator.html) to wrap your `.lkt` as a `.jar`
- Shows up in `/plugins` command ✅
- Can be uploaded to mod platforms ✅

**Option 3: Folder** (For development):
```
/plugins/MyPlugin.lkt/ # Folder name ending with .lkt
├── main.lua
├── plugin.yml
└── config.yml
```

### Creating Your First Plugin

**main.lua:**

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

**plugin.yml:**
```yaml
name: MyPlugin
version: 1.0
main: main.lua
author: YourName
description: My first LuaCord plugin
```

**For .lkt:** ZIP these files and rename to `MyPlugin.lkt`  
**For .jar:** Use the [JAR Generator](https://luacordmc.github.io/generator.html)  
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

## 📚 Commands

- `/lukkit` - Show help message
- `/lukkit plugins` - List all loaded LuaCord plugins
- `/lukkit dev` - Show developer commands
- `/lukkit dev reload <plugin>` - Hot reload a dev plugin (folder-based plugins only)
- `/lukkit dev errors` - View error stack for debugging
- `/lukkit dev pack <plugin>` - Package a dev plugin folder into `.lkt`
- `/lukkit dev unpack <plugin>` - Unpack a `.lkt` into a folder for development

---

## 🔌 Plugin API

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

## 📦 JAR Distribution

### Why JAR Support?

Before 0.2.0, LuaCord plugins were `.lkt` files - great for development but:
- ❌ Can't be uploaded to CurseForge/Modrinth (only accept `.jar`)
- ❌ Don't show up in `/plugins` command on Paper (due to bypass mode requirement)
- ❌ Look unprofessional/unfamiliar to users

**Now with JAR support in 0.2.0:**
- ✅ Upload to CurseForge, Modrinth, SpigotMC
- ✅ Shows in `/plugins` command!
- ✅ Professional distribution
- ✅ Still 100% Lua under the hood!

### How to Create a JAR

1. Write your plugin as a `.lkt` file
2. Go to [luacordmc.github.io/generator.html](https://luacordmc.github.io/generator.html)
3. Upload your `.lkt` and fill in metadata
4. Download the generated `.jar`
5. Upload to CurseForge/Modrinth!

**The JAR generator is 100% client-side** - your files never leave your browser.

---

## 🐛 Known Issues

When using `bypass-plugin-registration: true` (Paper mode):

- ~~LuaCord plugins don't appear in `/plugins`~~ **FIXED in 0.2.0!** JAR-wrapped plugins now show up ✅

**Note:** `.lkt` files still don't show in `/plugins` due to bypass mode. Use JAR plugins for better compatibility.

---

## 📝 Changelog

### 0.2.0-BETA *(Current)*

**JAR Support Release - The Distribution Update**

**New Features:**
- ✨ `.jar` plugin wrapper support alongside `.lkt` files
- ✨ Web-based JAR generator at [luacordmc.github.io/generator.html](https://luacordmc.github.io/generator.html)
- ✨ Public `LuaCordAPI` for JAR wrappers to load Lua plugins
- ✨ **JAR plugins now show up in `/plugins` command!**
- ✨ 87% Kotlin rewrite - modern, maintainable codebase
- ✨ Improved error messages with download links

**Technical:**
- Rewritten in Kotlin (87% conversion)
- Clean console logging (no more `[nz.co.jammehcow.lukkit...]` spam)
- Package renamed to `io.thegamingmahi.luacord`
- Better error handling with stack management
- JAR connector template for plugin distribution

**Compatibility:**
- 100% backwards compatible with Lukkit plugins
- Full Paper compatibility maintained
- Works with any Spigot/Paper version
- Java 8+

**Inherited from Lukkit 2.2.0 (by TheGamingMahi):**
- ✅ Paper server compatibility fix
- ✅ Bypass mode for Paper's plugin restrictions
- ✅ Configurable loading modes
- ✅ Debug mode for troubleshooting

### 0.1.0-BETA

**Initial LuaCord release - forked from Lukkit 2.2.0**

- ✨ Rewritten in Kotlin
- ✨ Clean console logging
- ✨ Full Paper compatibility
- ✨ Package renamed to `io.thegamingmahi.luacord`
- ✨ Improved error handling

---

## 🎯 Roadmap

### 0.3.0 - Quality of Life Update
- [ ] **Command fixes** - Fix and improve `/lukkit` commands (for the LuaCord core plugin itself)
- [ ] **bStats integration** - Built-in support for plugin developers to easily add bStats to their Lua plugins
- [ ] **Basic LuaCord Wiki** - Simple documentation with essential guides for plugin development
- [ ] More example plugins

### Future / Up to 1.0.0 - Stable Release
- [ ] Complete Kotlin migration (100%)
- [ ] Extended Paper API implementations
- [ ] VSCode extension for Lua plugin development
- [ ] Plugin marketplace/repository
- [ ] Video tutorials

---

## 📜 License

**LuaCord Core:** GPL v3 - This ensures the framework remains open source.

**Plugin Wrapper Template (LuaCord-Connector):** MIT - Maximum freedom for plugin developers.

See [LICENSE](LICENSE) for details.

---

## 🙏 Credits

### LuaCord
- **TheGamingMahi** - Creator, Kotlin rewrite, Paper fix (Lukkit 2.2.0), JAR support, maintenance

### Original Lukkit Team
- jammehcow
- AL_1
- mathhulk
- ArtexDevelopment

**Special thanks to the Lukkit community for keeping the project alive!**

Original Lukkit repository: https://github.com/jammehcow/Lukkit (archived)

---

## 🔗 Links

- **Website:** https://luacordmc.github.io
- **JAR Generator:** https://luacordmc.github.io/generator.html
- **Documentation:** https://docs.lukkit.net (Lukkit docs - LuaCord compatible)
- **Issues:** https://github.com/TheGamingMahi/LuaCord/issues
- **CurseForge:** https://www.curseforge.com/minecraft/bukkit-plugins/luacord
- **Modrinth:** https://modrinth.com/plugin/luacord

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
