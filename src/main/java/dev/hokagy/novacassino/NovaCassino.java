package dev.hokagy.novacassino;

import dev.hokagy.novacassino.command.CasinoCommand;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.listener.CasinoListener;
import dev.hokagy.novacassino.manager.CasinoManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class NovaCassino extends JavaPlugin {

    private static NovaCassino instance;
    private CasinoManager casinoManager;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Инициализация конфигураций
        saveDefaultConfig();
        reloadConfig(); // Гарантирует корректную загрузку config.yml из jar
        createMessagesConfig();

        // 2. Инициализация хука экономики (Vault)
        if (!VaultHook.setupEconomy()) {
            getLogger().warning("Vault или плагин экономики не найден! Денежные ставки работать не будут.");
        }

        // 3. Инициализация менеджера станций
        this.casinoManager = new CasinoManager(this);
        this.casinoManager.loadStations();

        // 4. Регистрация слушателей событий
        getServer().getPluginManager().registerEvents(new CasinoListener(this), this);

        // 5. Регистрация команд
        if (getCommand("novacassino") != null) {
            CasinoCommand commandExecutor = new CasinoCommand(this);
            getCommand("novacassino").setExecutor(commandExecutor);
            getCommand("novacassino").setTabCompleter(commandExecutor);
        }

        getLogger().info("NovaCassino успешно включен!");
    }

    @Override
    public void onDisable() {
        if (this.casinoManager != null) {
            this.casinoManager.saveStations();
        }
        getLogger().info("NovaCassino выключен.");
    }

    // --- Управление messages.yml ---

    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }

        reloadMessagesConfig();
    }

    /**
     * Геттер для сообщений из messages.yml
     */
    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            reloadMessagesConfig();
        }
        return this.messagesConfig;
    }

    /**
     * Перезагрузка файла messages.yml
     */
    public void reloadMessagesConfig() {
        if (messagesFile == null) {
            messagesFile = new File(getDataFolder(), "messages.yml");
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    /**
     * Метод для полного релоада всех конфигураций (для команды /novacassino reload)
     */
    public void reloadAllConfigs() {
        reloadConfig();
        reloadMessagesConfig();
        if (casinoManager != null) {
            casinoManager.loadStations();
        }
    }

    // --- Геттеры главного класса ---

    public static NovaCassino getInstance() {
        return instance;
    }

    public CasinoManager getCasinoManager() {
        return casinoManager;
    }
}
