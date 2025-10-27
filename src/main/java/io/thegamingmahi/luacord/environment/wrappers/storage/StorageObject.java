package io.thegamingmahi.luacord.environment.wrappers.storage;

import io.thegamingmahi.luacord.environment.LuaEnvironment.ObjectType;
import io.thegamingmahi.luacord.environment.exception.StorageObjectException;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * The abstract Storage class.
 *
 * @author jammehcow
 */
public abstract class StorageObject extends LuaTable {
    private File storageFile;
    private LukkitPlugin plugin;
    private Storage type;
    private StorageObject self = this;

    /**
     * Instantiates a new StorageObject.
     *
     * @param plugin the lukkit plugin
     * @param path   the file path
     * @param type   the storage type
     */
    public StorageObject(LukkitPlugin plugin, String path, Storage type) {
        this.type = type;
        this.plugin = plugin;

        String filteredPath = (path.startsWith(File.separator)) ? path : File.separator + path;
        this.storageFile = new File(this.plugin.getDataFolder().getAbsolutePath() + filteredPath);

        if (!this.storageFile.exists()) {
            try {
                Files.createFile(this.storageFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.set("getType", new ZeroArgFunction() {
            @Override
            public LuaValue call() {

                return CoerceJavaToLua.coerce(self.getType().type);
            }
        });

        this.set("exists", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue storage, LuaValue path) {
                return self.exists(path.checkjstring());
            }
        });

        this.set("setDefaultValue", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue storage, LuaValue path, LuaValue value) {
                try {
                    return self.setDefaultValue(path.checkstring(), value);
                } catch (StorageObjectException e) {
                    return LuaValue.NIL;
                }
            }
        });

        this.set("setValue", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue storage, LuaValue path, LuaValue value) {
                try {
                    self.setValue(path.checkstring(), value);
                    return LuaValue.TRUE;
                } catch (StorageObjectException e) {
                    return LuaValue.FALSE;
                }
            }
        });

        this.set("getValue", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue storage, LuaValue path) {
                try {
                    return self.getValue(path.checkstring());
                } catch (StorageObjectException e) {
                    return LuaValue.NIL;
                }
            }
        });

        this.set("clearValue", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue storage, LuaValue path) {
                try {
                    return self.clearVaule(path.checkstring());
                } catch (StorageObjectException e) {
                    return LuaValue.NIL;
                }
            }
        });

        this.set("save", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                self.save();
                return LuaValue.NIL;
            }
        });
    }

    @Override
    public String typename() {
        return ObjectType.STORAGE_OBJECT.name;
    }

    @Override
    public int type() {
        return ObjectType.STORAGE_OBJECT.type;
    }

    /**
     * Gets the storage type.
     *
     * @return the type
     */
    private Storage getType() {
        return this.type;
    }

    /**
     * Checks if the key exists.
     *
     * @return the type
     */
    protected abstract LuaBoolean exists(String path);

    /**
     * Sets a value if it doesn't exist.
     *
     * @param path  the path of the key
     * @param value the value
     * @return true if the value is set, false if not
     * @throws StorageObjectException
     */
    protected abstract LuaBoolean setDefaultValue(LuaString path, LuaValue value) throws StorageObjectException;

    /**
     * Sets the value of a key.
     *
     * @param path  the path of the key
     * @param value the value
     * @throws StorageObjectException
     */
    protected abstract void setValue(LuaString path, LuaValue value) throws StorageObjectException;

    /**
     * Clears a value
     *
     * @param path the path of the key
     * @throws StorageObjectException
     */
    protected abstract LuaBoolean clearVaule(LuaString path) throws StorageObjectException;

    /**
     * Gets the value of a key.
     *
     * @param path the path of the key
     * @return the Object value
     * @throws StorageObjectException
     */
    protected abstract LuaValue getValue(LuaString path) throws StorageObjectException;

    /**
     * Save the file.
     */
    protected abstract void save();

    /**
     * Gets the absolute path of the storage file.
     *
     * @return the absolute file path
     */
    protected String getFilePath() {
        return this.storageFile.getAbsolutePath();
    }

    /**
     * Gets the associated {@link LukkitPlugin}.
     *
     * @return the LukkitPlugin
     */
    protected LukkitPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Gets the storage file.
     *
     * @return the storage file
     */
    protected File getStorageFile() {
        return this.storageFile;
    }

    /**
     * Pre save file check.
     */
    protected void preSaveCheck() {
        if (!this.storageFile.exists()) {
            try {
                Files.createFile(this.storageFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The enum Storage.
     */
    public enum Storage {
        /**
         * JSON storage.
         */
        JSON("json"),
        /**
         * YAML storage.
         */
        YAML("yaml");

        private final String type;

        Storage(String type) {
            this.type = type;
        }
    }
}
