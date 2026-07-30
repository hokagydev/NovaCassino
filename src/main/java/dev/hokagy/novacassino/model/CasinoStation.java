package dev.hokagy.novacassino.model;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

public class CasinoStation {
    private final int id;
    private final String type;
    private Location centerLocation;
    private Location displayStart;
    private double radius;
    private ArmorStand hologram;

    public CasinoStation(int id, String type, Location centerLocation) {
        this(id, type, centerLocation, 3.5);
    }

    public CasinoStation(int id, String type, Location centerLocation, double radius) {
        this.id = id;
        this.type = type;
        this.centerLocation = centerLocation;
        this.radius = radius;
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

    public void updateHologram(Component text) {
        if (centerLocation == null || centerLocation.getWorld() == null) return;
        
        if (hologram == null || !hologram.isValid()) {
            Location holoLoc = centerLocation.clone().add(0, 2.2, 0);
            hologram = (ArmorStand) centerLocation.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
            hologram.setGravity(false);
            hologram.setCustomNameVisible(true);
            hologram.setVisible(false);
            hologram.setMarker(true);
        }
        
        hologram.customName(text);
    }

    public void resetHologram() {
        if (hologram != null && hologram.isValid()) {
            hologram.remove();
            hologram = null;
        }
    }

    public void remove() {
        resetHologram();
    }
}
