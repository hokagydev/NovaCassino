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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                    CasinoStation station = new CasinoStation(plugin, id, type, loc, radius);

                    if (section.contains(key + ".displayStart")) {
                        double dsX = section.getDouble(key + ".displayStart.x");
                        double dsY = section.getDouble(key + ".displayStart.y");
                        double dsZ = section.getDouble(key + ".displayStart.z");
                        station.setDisplayStart(new Location(world, dsX, dsY, dsZ));
                    }

                    if (section.contains(key + ".displayEnd")) {
                        double deX = section.getDouble(key + ".displayEnd.x");
                        double deY = section.getDouble(key + ".displayEnd.y");
                        double deZ = section.getDouble(key + ".displayEnd.z");
                        station.setDisplayEnd(new Location(world, deX, deY, deZ));
                    }

                    if (section.contains(key + ".hologram")) {
                        double hX = section.getDouble(key + ".hologram.x");
                        double hY = section.getDouble(key + ".hologram.y");
                        double hZ = section.getDouble(key + ".hologram.z");
                        float yaw = (float) section.getDouble(key + ".hologram.yaw", 0);
                        float pitch = (float) section.getDouble(key + ".hologram.pitch", 0);
                        station.setHologramLocation(new Location(world, hX, hY, hZ, yaw, pitch));
                    }

                    station.spawnAllEntities();
                    stations.put(id, station);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки станции ID: " + key + " - " + e.getMessage());
            }
        }
    }

    public void saveStations() {
        if (stationsConfig == null) {
            initStationsConfig();
        }

        stationsConfig.set("stations", null);

        for (CasinoStation station : stations.values()) {
            String path = "stations." + station.getId();
            if (station.getCenterLocation() == null || station.getCenterLocation().getWorld() == null) continue;
            
            stationsConfig.set(path + ".type", station.getType());
            stationsConfig.set(path + ".world", station.getCenterLocation().getWorld().getName());
            stationsConfig.set(path + ".x", station.getCenterLocation().getX());
            stationsConfig.set(path + ".y", station.getCenterLocation().getY());
            stationsConfig.set(path + ".z", station.getCenterLocation().getZ());
            stationsConfig.set(path + ".radius", station.getRadius());

            if (station.getDisplayStart() != null) {
                stationsConfig.set(path + ".displayStart.x", station.getDisplayStart().getX());
                stationsConfig.set(path + ".displayStart.y", station.getDisplayStart().getY());
                stationsConfig.set(path + ".displayStart.z", station.getDisplayStart().getZ());
            }

            if (station.getDisplayEnd() != null) {
                stationsConfig.set(path + ".displayEnd.x", station.getDisplayEnd().getX());
                stationsConfig.set(path + ".displayEnd.y", station.getDisplayEnd().getY());
                stationsConfig.set(path + ".displayEnd.z", station.getDisplayEnd().getZ());
            }

            if (station.getHologramLocation() != null) {
                stationsConfig.set(path + ".hologram.x", station.getHologramLocation().getX());
                stationsConfig.set(path + ".hologram.y", station.getHologramLocation().getY());
                stationsConfig.set(path + ".hologram.z", station.getHologramLocation().getZ());
                stationsConfig.set(path + ".hologram.yaw", station.getHologramLocation().getYaw());
                stationsConfig.set(path + ".hologram.pitch", station.getHologramLocation().getPitch());
            }
        }

        try {
            stationsConfig.save(stationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить stations.yml! " + e.getMessage());
        }
    }

    public CasinoStation createStation(String type, Location loc) {
        int id = generateUniqueId();
        double radius = plugin.getConfig().getDouble("station.radius", 3.5);
        CasinoStation station = new CasinoStation(plugin, id, type, loc, radius);
        
        station.spawnAllEntities();

        stations.put(id, station);
        saveStations();
        return station;
    }

    public boolean moveHologram(int id, Location newLoc) {
        CasinoStation station = stations.get(id);
        if (station == null) return false;

        station.setHologramLocation(newLoc);
        // Вызываем обновление позиции с сохранением прежнего текста
        station.refreshHologramPosition();

        saveStations();
        return true;
    }

    public boolean deleteStation(int id) {
        if (stations.containsKey(id)) {
            CasinoStation station = stations.remove(id);
            if (station != null) station.remove();
            saveStations();
            return true;
        }
        return false;
    }

    public void removeAllEntities() {
        List<CasinoStation> copy = new ArrayList<>(stations.values());
        for (CasinoStation station : copy) {
            try {
                if (station != null) station.remove();
            } catch (Exception ignored) {}
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

    public Map<Integer, CasinoStation> getStations() {
        return stations;
    }
}
