package dev.hokagy.novacassino.manager;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.model.CasinoStation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CasinoManager {

    private final NovaCassino plugin;
    private final Map<Integer, CasinoStation> stations = new HashMap<>();
    private File stationsFile;
    private FileConfiguration stationsConfig;

    public CasinoManager(NovaCassino plugin) {
        this.plugin = plugin;
        initStationsConfig();
    }

    private void initStationsConfig() {
        stationsFile = new File(plugin.getDataFolder(), "stations.yml");
        if (!stationsFile.exists()) {
            try {
                stationsFile.getParentFile().mkdirs();
                stationsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать stations.yml!");
            }
        }
        stationsConfig = YamlConfiguration.loadConfiguration(stationsFile);
    }

    public void loadStations() {
        // 🔥 ВАЖНО: Очищаем существующие блоки и голограммы перед перезагрузкой
        removeAllEntities();
        stations.clear();

        stationsConfig = YamlConfiguration.loadConfiguration(stationsFile);

        ConfigurationSection section = stationsConfig.getConfigurationSection("stations");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String type = section.getString(key + ".type", "ROULETTE");
                String worldName = section.getString(key + ".world");
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                double radius = section.getDouble(key + ".radius", 3.5);

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);
                    CasinoStation station = new CasinoStation(id, type, loc, radius);
                    stations.put(id, station);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки станции ID: " + key);
            }
        }
    }

    public void saveStations() {
        stationsConfig.set("stations", null);

        for (CasinoStation station : stations.values()) {
            String path = "stations." + station.getId();
            stationsConfig.set(path + ".type", station.getType());
            stationsConfig.set(path + ".world", station.getCenterLocation().getWorld().getName());
            stationsConfig.set(path + ".x", station.getCenterLocation().getX());
            stationsConfig.set(path + ".y", station.getCenterLocation().getY());
            stationsConfig.set(path + ".z", station.getCenterLocation().getZ());
            stationsConfig.set(path + ".radius", station.getRadius());
        }

        try {
            stationsConfig.save(stationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить stations.yml!");
        }
    }

    public CasinoStation createStation(String type, Location loc) {
        int id = generateUniqueId();
        double radius = plugin.getConfig().getDouble("station.radius", 3.5);
        CasinoStation station = new CasinoStation(id, type, loc, radius);
        stations.put(id, station);
        saveStations();
        return station;
    }

    public boolean deleteStation(int id) {
        if (stations.containsKey(id)) {
            CasinoStation station = stations.remove(id);
            station.remove();
            saveStations();
            return true;
        }
        return false;
    }

    /**
     * Удаляет абсолютно все видимые сущности (голограммы и блоки) всех станций
     */
    public void removeAllEntities() {
        for (CasinoStation station : stations.values()) {
            station.remove();
        }
    }

    private int generateUniqueId() {
        int id = 1;
        while (stations.containsKey(id)) {
            id++;
        }
        return id;
    }

    public CasinoStation getStation(int id) {
        return stations.get(id);
    }

    public CasinoStation getStationAt(Location loc) {
        for (CasinoStation station : stations.values()) {
            if (station.getCenterLocation().getWorld().equals(loc.getWorld())) {
                if (station.getCenterLocation().distance(loc) <= station.getRadius()) {
                    return station;
                }
            }
        }
        return null;
    }

    public Map<Integer, CasinoStation> getStations() {
        return stations;
    }
}
