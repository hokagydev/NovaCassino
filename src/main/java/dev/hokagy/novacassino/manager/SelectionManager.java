package dev.hokagy.novacassino.manager;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public void setPos1(UUID playerId, Location loc) {
        pos1Map.put(playerId, loc);
    }

    public void setPos2(UUID playerId, Location loc) {
        pos2Map.put(playerId, loc);
    }

    public Location getPos1(UUID playerId) {
        return pos1Map.get(playerId);
    }

    public Location getPos2(UUID playerId) {
        return pos2Map.get(playerId);
    }

    public boolean hasSelection(UUID playerId) {
        return pos1Map.containsKey(playerId) && pos2Map.containsKey(playerId);
    }

    public Location getMinCorner(UUID playerId) {
        Location p1 = pos1Map.get(playerId);
        Location p2 = pos2Map.get(playerId);
        if (p1 == null || p2 == null || !p1.getWorld().equals(p2.getWorld())) return null;

        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());

        return new Location(p1.getWorld(), minX, minY, minZ);
    }
}
