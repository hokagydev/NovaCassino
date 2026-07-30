package dev.hokagy.novacassino;

import dev.hokagy.novacassino.commands.CasinoCommand;
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

        // 1. Создание и загрузка дефолтного config.yml
        saveDefaultConfig();

        // 2. Инициализация интеграции с Vault (Экономика)
        if (VaultHook.setupEconomy()) {
            getLogger().info("Успешная интеграция с Vault! Экономика подключена.");
        } else {
            getLogger().warning("Vault или плагин экономики не найден! Ставки за деньги работать не будут.");
        }

        // 3. Инициализация менеджера казино и загрузка станций из конфига
        casinoManager = new CasinoManager(this);
        casinoManager.loadStations();

        // 4. Регистрация команд (/procasino, /casino, /novacasino)
        if (getCommand("procasino") != null) {
            getCommand("procasino").setExecutor(new CasinoCommand(this));
        }

        // 5. Регистрация слушателя событий (ПКМ по станции и меню ставок)
        getServer().getPluginManager().registerEvents(new CasinoListener(this), this);

        getLogger().info("====================================");
        getLogger().info(" NovaCassino v1.0.0 успешно запущен!");
        getLogger().info(" Автор: Hokagydev");
        getLogger().info(" Версия: Paper 1.21.1");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        // Сохранение данных и очистка спавна перед отключением плагина
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
