package dev.hokagy.novacassino;

import dev.hokagy.novacassino.command.BetCommand;
import dev.hokagy.novacassino.command.CasinoCommand;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.listener.CasinoListener;
import dev.hokagy.novacassino.listener.SlotInteractionListener;
import dev.hokagy.novacassino.listener.WandListener;
import dev.hokagy.novacassino.manager.CasinoManager;
import dev.hokagy.novacassino.manager.SelectionManager;
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
    private SelectionManager selectionManager;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();
        createMessagesConfig();

        if (!VaultHook.setupEconomy()) {
            getLogger().warning("Vault или плагин экономики не найден! Денежные ставки работать не будут.");
        }

        this.selectionManager = new SelectionManager();
        this.casinoManager = new CasinoManager(this);
        this.casinoManager.loadStations();

        getServer().getPluginManager().registerEvents(new CasinoListener(this), this);
        getServer().getPluginManager().registerEvents(new SlotInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);

        if (getCommand("procasino") != null) {
            CasinoCommand commandExecutor = new CasinoCommand(this);
            getCommand("procasino").setExecutor(commandExecutor);
            getCommand("procasino").setTabCompleter(commandExecutor);
        }

        if (getCommand("bet") != null) {
            getCommand("bet").setExecutor(new BetCommand(this));
        }

        getLogger().info("NovaCassino успешно включен!");
    }

    @Override
    public void onDisable() {
        if (this.casinoManager != null) {
            this.casinoManager.saveStations();
            this.casinoManager.removeAllEntities();
        }
        getLogger().info("NovaCassino выключен.");
    }

    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }

        reloadMessagesConfig();
    }

    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            reloadMessagesConfig();
        }
        return this.messagesConfig;
    }

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

    public void reloadAllConfigs() {
        reloadConfig();
        reloadMessagesConfig();
        if (casinoManager != null) {
            casinoManager.loadStations();
        }
    }

    public static NovaCassino getInstance() {
        return instance;
    }

    public CasinoManager getCasinoManager() {
        return casinoManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
}
