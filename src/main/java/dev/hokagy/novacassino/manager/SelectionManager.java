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
}
