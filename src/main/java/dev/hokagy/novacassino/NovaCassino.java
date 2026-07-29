package dev.hokagy.novacassino;

import dev.hokagy.novacassino.commands.CasinoCommand;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.manager.CasinoManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class NovaCassino extends JavaPlugin {

    private static NovaCassino instance;
    private CasinoManager casinoManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Инициализация Vault
        if (VaultHook.setupEconomy()) {
            getLogger().info("Успешное подключение к Vault (Экономика)!");
        } else {
            getLogger().warning("Vault не найден! Функции ставок за деньги отключены.");
        }

        // 2. Инициализация менеджера казино
        casinoManager = new CasinoManager(this);
        casinoManager.loadStations();

        // 3. Регистрация команд
        if (getCommand("procasino") != null) {
            getCommand("procasino").setExecutor(new CasinoCommand(this));
        }

        getLogger().info("====================================");
        getLogger().info(" NovaCassino v1.0.0 успешно запущен!");
        getLogger().info(" Автор: Hokagydev");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        if (casinoManager != null) {
            casinoManager.saveStations();
            // Очищаем сущности из мира при выключении/перезагрузке
            casinoManager.getStations().values().forEach(st -> st.clear());
        }
        getLogger().info("NovaCassino выключен.");
    }

    public static NovaCassino getInstance() {
        return instance;
    }

    public CasinoManager getCasinoManager() {
        return casinoManager;
    }
}
