package dev.hokagy.novacassino.model;

import org.bukkit.Location;

public class CasinoStation {

    private final int id;
    private final String type;
    private final Location centerLocation;
    private final Location displayStart;
    private final Location displayEnd;

    public CasinoStation(int id, String type, Location centerLocation, Location displayStart, Location displayEnd) {
        this.id = id;
        this.type = type;
        this.centerLocation = centerLocation;
        this.displayStart = displayStart;
        this.displayEnd = displayEnd;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public Location getDisplayStart() {
        return displayStart;
    }

    public Location getDisplayEnd() {
        return displayEnd;
    }
}
