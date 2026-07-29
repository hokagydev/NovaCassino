package dev.hokagy.novacassino.manager;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.model.CasinoStation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class CasinoManager {

    private final NovaCassino plugin;
    private final Map<Integer, CasinoStation> stations = new HashMap<>();
    private int nextId = 0;

    public CasinoManager(NovaCassino plugin) {
        this.plugin = plugin;
    }

    public CasinoStation createStation(String type, Location location) {
        CasinoStation station = new CasinoStation(nextId, type.toUpperCase(), location, 3.5);
        station.spawn();
        stations.put(nextId, station);
        nextId++;
        saveStations();
        return station;
    }

    public boolean deleteStation(int id) {
        CasinoStation station = stations.remove(id);
        if (station != null) {
            station.clear();
            saveStations();
            return true;
        }
        return false;
    }

    public CasinoStation getStation(int id) {
        return stations.get(id);
    }

    public Map<Integer, CasinoStation> getStations() {
        return stations;
    }

    public void loadStations() {
        // Очищаем старые
        stations.values().forEach(CasinoStation::clear);
        stations.clear();

        FileConfiguration config = plugin.getConfig();
        if (!config.contains("stations")) return;

        for (String key : config.getConfigurationSection("stations").getKeys(false)) {
            int id = Integer.parseInt(key);
            String type = config.getString("stations." + key + ".type");
            String world = config.getString("stations." + key + ".world");
            double x = config.getDouble("stations." + key + ".x");
            double y = config.getDouble("stations." + key + ".y");
            double z = config.getDouble("stations." + key + ".z");
            double radius = config.getDouble("stations." + key + ".radius", 3.5);

            Location loc = new Location(Bukkit.getWorld(world), x, y, z);
            CasinoStation station = new CasinoStation(id, type, loc, radius);
            station.spawn();
            stations.put(id, station);

            if (id >= nextId) {
                nextId = id + 1;
            }
        }
    }

    public void saveStations() {
        FileConfiguration config = plugin.getConfig();
        config.set("stations", null); // Сброс раздела

        for (CasinoStation station : stations.values()) {
            String path = "stations." + station.getId();
            config.set(path + ".type", station.getType());
            config.set(path + ".world", station.getCenterLocation().getWorld().getName());
            config.set(path + ".x", station.getCenterLocation().getX());
            config.set(path + ".y", station.getCenterLocation().getY());
            config.set(path + ".z", station.getCenterLocation().getZ());
            config.set(path + ".radius", station.getRadius());
        }
        plugin.saveConfig();
    }
}
