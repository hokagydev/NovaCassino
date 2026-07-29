package dev.hokagy.novacassino;

import dev.hokagy.novacassino.commands.CasinoCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class NovaCassino extends JavaPlugin {

    private static NovaCassino instance;

    @Override
    public void onEnable() {
        instance = this;

        // Регистрация команд
        if (getCommand("casino") != null) {
            getCommand("casino").setExecutor(new CasinoCommand(this));
        }

        getLogger().info("====================================");
        getLogger().info(" NovaCassino v1.0.0 загружен!");
        getLogger().info(" Автор: Hokagydev");
        getLogger().info(" Версия сервера: 1.21.1 (Paper)");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("NovaCassino выключен.");
    }

    public static NovaCassino getInstance() {
        return instance;
    }
}
