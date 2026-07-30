package dev.hokagy.novacassino.model;

import org.bukkit.Location;

public class CasinoStation {
    private final int id;
    private final String type;
    private Location centerLocation;
    private Location displayStart;
    private double radius;

    public CasinoStation(int id, String type, Location centerLocation) {
        this.id = id;
        this.type = type;
        this.centerLocation = centerLocation;
        this.radius = 3.5;
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

    public void setCenterLocation(Location centerLocation) { 
        this.centerLocation = centerLocation; 
    }

    public Location getDisplayStart() { 
        return displayStart; 
    }

    public void setDisplayStart(Location displayStart) { 
        this.displayStart = displayStart; 
    }

    public double getRadius() { 
        return radius; 
    }

    public void setRadius(double radius) { 
        this.radius = radius; 
    }
}
