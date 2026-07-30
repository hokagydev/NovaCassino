package dev.hokagy.novacassino;

import dev.hokagy.novacassino.commands.CasinoCommand;
import dev.hokagy.novacassino.commands.CasinoTabCompleter;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.listener.CasinoListener;
import dev.hokagy.novacassino.manager.CasinoManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class NovaCassino extends JavaPlugin {

    private static NovaCassino instance;
    private CasinoManager casinoManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Сохранение дефолтного config.yml
        saveDefaultConfig();

        // 2. Инициализация интеграции с Vault
        if (VaultHook.setupEconomy()) {
            getLogger().info("Успешная интеграция с Vault! Экономика подключена.");
        } else {
            getLogger().warning("Vault или плагин экономики не найден! Ставки за деньги отключены.");
        }

        // 3. Инициализация менеджера казино
        casinoManager = new CasinoManager(this);
        casinoManager.loadStations();

        // 4. Регистрация команд и автодополнения (TabCompleter)
        if (getCommand("procasino") != null) {
            getCommand("procasino").setExecutor(new CasinoCommand(this));
            getCommand("procasino").setTabCompleter(new CasinoTabCompleter(this));
        }

        // 5. Регистрация слушателя событий (GUI и 3D-анимации)
        getServer().getPluginManager().registerEvents(new CasinoListener(this), this);

        getLogger().info("====================================");
        getLogger().info(" NovaCassino v1.0.0 успешно запущен!");
        getLogger().info(" Автор: Hokagydev");
        getLogger().info(" Версия: Paper 1.21.1");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        // Очистка спавна станций и сохранение конфигурации перед выключением
        if (casinoManager != null) {
            casinoManager.saveStations();
            casinoManager.getStations().values().forEach(station -> station.clear());
        }

        getLogger().info("NovaCassino успешно выключен.");
    }

    public static NovaCassino getInstance() {
        return instance;
    }

    public CasinoManager getCasinoManager() {
        return casinoManager;
    }
}
